/**
 * STT (Speech-to-Text) module.
 * Proxies audio to Groq/OpenAI-compatible Whisper API.
 */

const https = require('https');
const http = require('http');

/**
 * Send audio chunk to STT API for transcription.
 * @param {string} baseUrl - Base URL (e.g. https://api.groq.com/openai/v1)
 * @param {string} apiKey - API key
 * @param {string} model - Model name (e.g. whisper-large-v3-turbo)
 * @param {Buffer} audioBuffer - WAV audio data
 * @returns {Promise<string>} Transcribed text
 */
function transcribeAudio(baseUrl, apiKey, model, audioBuffer) {
  return new Promise((resolve, reject) => {
    // Remove trailing slash if present
    const cleanUrl = baseUrl.replace(/\/+$/, '');
    const url = new URL(`${cleanUrl}/audio/transcriptions`);

    const boundary = `----FormBoundary${Date.now()}`;
    const fileName = `audio_${Date.now()}.wav`;

    let body = '';
    body += `--${boundary}\r\n`;
    body += `Content-Disposition: form-data; name="model"\r\n\r\n`;
    body += `${model}\r\n`;
    body += `--${boundary}\r\n`;
    body += `Content-Disposition: form-data; name="file"; filename="${fileName}"\r\n`;
    body += `Content-Type: audio/wav\r\n\r\n`;

    const bodyStart = Buffer.from(body, 'utf-8');
    const bodyEnd = Buffer.from(`\r\n--${boundary}--\r\n`, 'utf-8');

    const postData = Buffer.concat([bodyStart, audioBuffer, bodyEnd]);

    const options = {
      hostname: url.hostname,
      port: url.port || (url.protocol === 'https:' ? 443 : 80),
      path: url.pathname,
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${apiKey}`,
        'Content-Type': `multipart/form-data; boundary=${boundary}`,
        'Content-Length': postData.length,
      },
    };

    const lib = url.protocol === 'https:' ? https : http;
    const req = lib.request(options, (res) => {
      let data = '';
      res.on('data', (chunk) => { data += chunk; });
      res.on('end', () => {
        if (res.statusCode === 429) {
          const retryAfter = parseInt(res.headers['retry-after'] || '5', 10);
          return reject({ retryAfter, statusCode: 429 });
        }
        if (res.statusCode !== 200) {
          return reject(new Error(`STT API error ${res.statusCode}: ${data}`));
        }
        try {
          const json = JSON.parse(data);
          resolve(json.text || '');
        } catch (e) {
          reject(new Error(`STT API parse error: ${e.message}`));
        }
      });
    });

    req.on('error', reject);
    req.write(postData);
    req.end();
  });
}

module.exports = { transcribeAudio };
