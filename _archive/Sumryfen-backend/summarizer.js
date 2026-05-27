/**
 * Summarizer module.
 * Calls LLM (Groq/OpenAI-compatible) to generate meeting summaries.
 */

const https = require('https');
const http = require('http');

/**
 * Generate summary from accumulated transcript text.
 * @param {string} baseUrl - Base URL
 * @param {string} apiKey - API key
 * @param {string} model - LLM model name
 * @param {string} transcript - Full accumulated transcript
 * @param {string|null} previousSummary - Previous summary for context, if any
 * @returns {Promise<string>} Generated summary text
 */
function generateSummary(baseUrl, apiKey, model, transcript, previousSummary = null) {
  return new Promise((resolve, reject) => {
    const cleanUrl = baseUrl.replace(/\/+$/, '');
    const url = new URL(`${cleanUrl}/chat/completions`);

    let systemPrompt = 'Anda adalah asisten yang membuat ringkasan meeting dalam bahasa Indonesia. '
      + 'Buat ringkasan poin-poin penting secara terstruktur dan jelas. '
      + 'Gunakan bahasa Indonesia yang baik dan benar.';

    let userMessage = '';
    if (previousSummary) {
      userMessage += `Ringkasan sebelumnya:\n${previousSummary}\n\n`;
    }
    userMessage += `Transkrip meeting:\n${transcript}\n\n`
      + 'Buat ringkasan terbaru dari seluruh transkrip di atas. '
      + 'Jika ada ringkasan sebelumnya, gabungkan dengan informasi baru. '
      + 'Gunakan poin-poin (bullet points) dalam bahasa Indonesia. '
      + 'Pisahkan setiap poin dengan baris baru.';

    const body = JSON.stringify({
      model: model,
      messages: [
        { role: 'system', content: systemPrompt },
        { role: 'user', content: userMessage },
      ],
      temperature: 0.3,
      max_tokens: 1024,
    });

    const options = {
      hostname: url.hostname,
      port: url.port || (url.protocol === 'https:' ? 443 : 80),
      path: url.pathname,
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${apiKey}`,
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(body),
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
          return reject(new Error(`LLM API error ${res.statusCode}: ${data}`));
        }
        try {
          const json = JSON.parse(data);
          const content = json.choices?.[0]?.message?.content || '';
          resolve(content);
        } catch (e) {
          reject(new Error(`LLM API parse error: ${e.message}`));
        }
      });
    });

    req.on('error', reject);
    req.write(body);
    req.end();
  });
}

module.exports = { generateSummary };
