## 3. System Design Document (SDD)

### 3.1 Arsitektur Sistem (High-Level)
```
[Android App] --WebSocket--> [Backend Node.js] --HTTP/WebSocket--> [Groq STT (Whisper)]
                                      |                                 
                                      +---------> [Groq LLM (Llama 3)]
                                      |
                              [Penyimpanan file JSON / SQLite lokal di server?]
```
*Catatan:* Database meeting diletakkan di sisi Android (lokal), jadi backend cukup sebagai relay/proxy tanpa database kecuali cache sementara.

Untuk kemudahan deploy, backend dapat menggunakan **Node.js** dengan pustaka `ws` (WebSocket) dan `express` (untuk health-check). Berjalan di VPS murah atau layanan gratis seperti `render.com` (free tier 750 jam/bulan).

### 3.2 API Endpoint
- **WebSocket `/ws/meeting`**  
  *Digunakan oleh client untuk:*
  - Mengirim audio chunk (binary/base64).
  - Mengirim perintah start/stop.
  - Menerima event transkrip (`transcript`), ringkasan (`summary`), error, dan status.

  Format pesan dari client ke server:
  ```json
  {
    "type": "audio",
    "data": "<base64 encoded WAV chunk>"
  }
  ```
  ```json
  {
    "type": "start"
  }
  ```
  ```json
  {
    "type": "stop"
  }
  ```

  Format pesan dari server ke client:
  ```json
  {
    "type": "transcript",
    "text": "Halo, selamat pagi...",
    "timestamp": 1680000000
  }
  ```
  ```json
  {
    "type": "summary",
    "text": "Poin penting: ..."
  }
  ```
  ```json
  {
    "type": "error",
    "message": "..."
  }
  ```

### 3.3 Database (Local Android)
Gunakan **Room (SQLite)** atau cukup **SharedPreferences** untuk menyimpan history meeting. Struktur minimal:
- Table `meetings`:
  - `id` (INTEGER PRIMARY KEY)
  - `title` (TEXT) – default “Meeting {date}”
  - `transcript` (TEXT)
  - `summary` (TEXT)
  - `created_at` (TIMESTAMP)

Tidak perlu tabel terpisah karena ringkasan tunggal per meeting.

### 3.4 Struktur Backend (Node.js)
```
Sumryfen-backend/
├── package.json
├── server.js         # Inisialisasi WebSocket server, handle koneksi
├── stt.js            # Modul untuk kirim audio chunk ke Groq API
├── summarizer.js     # Modul untuk panggil LLM Groq dan akumulasi teks
├── config.js         # API keys dari env
└── .env              # GROQ_API_KEY
```
*Catatan:* Untuk STT, karena Groq tidak menyediakan WebSocket streaming, kita akan kirim chunk audio setiap 1 detik via HTTP POST ke endpoint `https://api.groq.com/openai/v1/audio/transcriptions` dengan model `whisper-large-v3-turbo`. Backend akan menggabungkan hasil transkrip setiap chunk (final). Hasil transkrip dikirim ke client.

*Optimasi:* Untuk mengurangi delay, backend dapat mengirim audio chunk setiap 1 detik, tapi Groq STT hanya menghasilkan teks final jika chunk tersebut berisi jeda. Ini masih bisa dipakai untuk live karena tetap muncul teks secara bertahap.

*Untuk LLM:* Panggil `https://api.groq.com/openai/v1/chat/completions` dengan model `llama-3-8b-instant` setiap 1 menit.

### 3.5 Pembatasan & Keamanan
- Backend akan menolak sesi WebSocket baru jika jumlah sesi yang aktif melebihi 5 (untuk mencegah abuse di free tier). Untuk penggunaan pribadi, 1 sesi cukup.
- Setiap 1 jam, backend melacak total detik audio yang diproses. Jika melebihi 7200 detik, kirim peringatan dan tutup koneksi.