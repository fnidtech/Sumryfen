/**
 * Sumryfen Backend — WebSocket server + HTTP health-check.
 *
 * Protokol:
 *   Client → Server: start, audio (base64 WAV chunk), stop
 *   Server → Client: transcript, summary, error
 */

const { WebSocketServer } = require('ws');
const express = require('express');
const config = require('./config');
const { transcribeAudio } = require('./stt');
const { generateSummary } = require('./summarizer');

// ---------------------------------------------------------------------------
// Express health-check
// ---------------------------------------------------------------------------
const app = express();
app.get('/health', (_req, res) => {
  res.json({ status: 'ok', uptime: process.uptime() });
});

// ---------------------------------------------------------------------------
// Server state
// ---------------------------------------------------------------------------
const activeSessions = new Map(); // ws -> session data
let sessionCount = 0;

/**
 * @typedef {Object} SessionData
 * @property {string}   sttBaseUrl
 * @property {string}   sttApiKey
 * @property {string}   sttModel
 * @property {string}   llmBaseUrl
 * @property {string}   llmApiKey
 * @property {string}   llmModel
 * @property {string[]} transcriptChunks
 * @property {string}   fullTranscript
 * @property {string|null} previousSummary
 * @property {number}   wordCount Since last summary
 * @property {number}   startTime
 * @property {number}   lastSummaryTime
 * @property {NodeJS.Timeout|null} summaryTimer
 * @property {boolean}  stopped
 */

// ---------------------------------------------------------------------------
// WebSocket server
// ---------------------------------------------------------------------------
const wss = new WebSocketServer({ noServer: true });

wss.on('connection', (ws) => {
  if (sessionCount >= config.maxSessions) {
    sendError(ws, 'Server sedang sibuk. Batas maksimum sesi tercapai.');
    ws.close(1013, 'Too many sessions');
    return;
  }

  /** @type {SessionData} */
  const session = {
    sttBaseUrl: '',
    sttApiKey: '',
    sttModel: '',
    llmBaseUrl: '',
    llmApiKey: '',
    llmModel: '',
    transcriptChunks: [],
    fullTranscript: '',
    previousSummary: null,
    wordCount: 0,
    startTime: 0,
    lastSummaryTime: 0,
    summaryTimer: null,
    stopped: false,
  };

  activeSessions.set(ws, session);
  sessionCount++;

  console.log(`[connect] Session aktif: ${sessionCount}`);

  ws.on('message', async (raw) => {
    let msg;
    try {
      msg = JSON.parse(raw.toString());
    } catch {
      sendError(ws, 'Format pesan tidak valid');
      return;
    }

    const { type } = msg;

    try {
      switch (type) {
        case 'start':
          await handleStart(ws, session, msg);
          break;
        case 'audio':
          await handleAudio(ws, session, msg);
          break;
        case 'stop':
          await handleStop(ws, session);
          break;
        default:
          sendError(ws, `Tipe pesan tidak dikenal: ${type}`);
      }
    } catch (err) {
      console.error('[error]', err);
      if (err.statusCode === 429) {
        sendError(ws, `Rate limit tercapai. Coba lagi dalam ${err.retryAfter} detik.`);
      } else {
        sendError(ws, `Error: ${err.message || 'Terjadi kesalahan'}`);
      }
    }
  });

  ws.on('close', () => {
    cleanupSession(ws, session);
  });

  ws.on('error', () => {
    cleanupSession(ws, session);
  });
});

// ---------------------------------------------------------------------------
// Handlers
// ---------------------------------------------------------------------------

async function handleStart(ws, session, msg) {
  const stt = msg.stt || {};
  const llm = msg.llm || {};

  // Use client-provided config, fallback to server env key
  session.sttBaseUrl = stt.base_url || 'https://api.groq.com/openai/v1';
  session.sttApiKey = stt.api_key || config.groqApiKey;
  session.sttModel = stt.model || 'whisper-large-v3-turbo';
  session.llmBaseUrl = llm.base_url || 'https://api.groq.com/openai/v1';
  session.llmApiKey = llm.api_key || session.sttApiKey || config.groqApiKey;
  session.llmModel = llm.model || 'llama-3-8b-instant';

  session.transcriptChunks = [];
  session.fullTranscript = '';
  session.previousSummary = null;
  session.wordCount = 0;
  session.startTime = Math.floor(Date.now() / 1000);
  session.lastSummaryTime = session.startTime;
  session.stopped = false;

  // Schedule periodic summarization every 60 seconds
  session.summaryTimer = setInterval(async () => {
    if (session.stopped) return;
    const elapsed = Math.floor(Date.now() / 1000) - session.startTime;
    if (elapsed > config.maxDurationSeconds) {
      sendError(ws, 'Batas maksimal 2 jam tercapai. Sesi dihentikan.');
      cleanupSession(ws, session);
      ws.close(1000, 'Max duration reached');
      return;
    }
    await runSummary(ws, session);
  }, 60_000);

  console.log(`[start] Sesi dimulai. STT: ${session.sttModel}, LLM: ${session.llmModel}`);
}

async function handleAudio(ws, session, msg) {
  if (session.stopped) return;

  const data = msg.data;
  if (!data) {
    sendError(ws, 'Data audio kosong');
    return;
  }

  let audioBuffer;
  try {
    audioBuffer = Buffer.from(data, 'base64');
  } catch {
    sendError(ws, 'Format audio tidak didukung');
    return;
  }

  if (!audioBuffer || audioBuffer.length < 44) {
    sendError(ws, 'Format audio tidak didukung');
    return;
  }

  try {
    const text = await transcribeAudio(
      session.sttBaseUrl,
      session.sttApiKey,
      session.sttModel,
      audioBuffer
    );

    if (text.trim()) {
      // Send transcript to client
      sendJson(ws, {
        type: 'transcript',
        text: text.trim(),
        timestamp: Math.floor(Date.now() / 1000),
      });

      // Accumulate
      session.transcriptChunks.push(text.trim());
      session.fullTranscript += ' ' + text.trim();

      // Count new words
      const newWords = text.trim().split(/\s+/).length;
      session.wordCount += newWords;

      // Trigger summary if >= 200 new words
      if (session.wordCount >= 200) {
        await runSummary(ws, session);
      }
    }
  } catch (err) {
    if (err.statusCode === 429) {
      sendError(ws, `Rate limit STT tercapai. Mohon tunggu.`);
    } else {
      throw err;
    }
  }
}

async function handleStop(ws, session) {
  if (session.stopped) return;
  session.stopped = true;

  // Run final summary if there's remaining transcript
  // runSummary already sends the summary event to the client
  if (session.wordCount > 0 || session.fullTranscript.trim()) {
    await runSummary(ws, session);
  } else {
    sendJson(ws, {
      type: 'summary',
      text: 'Tidak ada ringkasan.',
    });
  }

  cleanupSession(ws, session);
}

async function runSummary(ws, session) {
  try {
    if (!session.fullTranscript.trim()) return;

    const summary = await generateSummary(
      session.llmBaseUrl,
      session.llmApiKey,
      session.llmModel,
      session.fullTranscript.trim(),
      session.previousSummary
    );

    if (summary.trim()) {
      session.previousSummary = summary.trim();
      session.wordCount = 0;
      session.lastSummaryTime = Math.floor(Date.now() / 1000);

      sendJson(ws, {
        type: 'summary',
        text: summary.trim(),
      });
    }
  } catch (err) {
    if (err.statusCode === 429) {
      console.warn('[summary] Rate limit LLM, akan coba lagi nanti.');
    } else {
      console.error('[summary] Error:', err.message);
    }
  }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function sendJson(ws, obj) {
  if (ws.readyState === ws.OPEN) {
    ws.send(JSON.stringify(obj));
  }
}

function sendError(ws, message) {
  sendJson(ws, { type: 'error', message });
}

function cleanupSession(ws, session) {
  if (session.summaryTimer) {
    clearInterval(session.summaryTimer);
    session.summaryTimer = null;
  }
  if (activeSessions.has(ws)) {
    activeSessions.delete(ws);
    sessionCount = Math.max(0, sessionCount - 1);
    console.log(`[disconnect] Session aktif: ${sessionCount}`);
  }
}

// ---------------------------------------------------------------------------
// Start server
// ---------------------------------------------------------------------------
const server = app.listen(config.port, () => {
  console.log(`Sumryfen backend running on port ${config.port}`);
});

server.on('upgrade', (request, socket, head) => {
  if (request.url === '/ws/meeting') {
    wss.handleUpgrade(request, socket, head, (ws) => {
      wss.emit('connection', ws, request);
    });
  } else {
    socket.destroy();
  }
});
