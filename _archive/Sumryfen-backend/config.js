require('dotenv').config();

module.exports = {
  groqApiKey: process.env.GROQ_API_KEY || '',
  port: parseInt(process.env.PORT || '3000', 10),
  maxSessions: parseInt(process.env.MAX_SESSIONS || '5', 10),
  maxDurationSeconds: parseInt(process.env.MAX_DURATION_SECONDS || '7200', 10),

  // STT endpoint — client provides base_url, server appends the path
  sttPath: '/audio/transcriptions',

  // LLM endpoint
  llmPath: '/chat/completions',
};
