# Sumryfen — Panduan untuk AI Agent

## 1. Identitas Proyek

**Sumryfen** adalah aplikasi Android untuk merekam meeting dengan transkripsi
real-time dan ringkasan otomatis. Semua pemrosesan dilakukan via Groq API
(dapat diganti ke provider OpenAI-compatible lain via pengaturan).

| Komponen  | Stack                                                    |
|-----------|----------------------------------------------------------|
| Mobile    | Kotlin, OkHttp, AudioRecord, Room (SQLite)               |
| Eksternal | Groq API — STT: whisper-large-v3-turbo, LLM: llama-3-8b-instant |

Tidak ada backend terpisah. Aplikasi memanggil Groq API langsung dari HP.

## 2. Struktur Repo

```
Sumryfen/                    # Android app (single project)
├── app/
│   ├── src/main/java/com/sumryfen/
│   │   ├── MainActivity.kt
│   │   ├── ui/home/         # HomeScreen — history list
│   │   ├── ui/meeting/      # Live meeting — record, transcript, summary
│   │   ├── ui/detail/       # Past meeting detail
│   │   ├── ui/settings/     # Settings — konfigurasi model, simpan audio
│   │   ├── data/local/      # Room DB (entity Meeting, DAO, database)
│   │   ├── data/remote/     # HTTP client untuk Groq STT & LLM (OkHttp)
│   │   ├── data/repository/ # Repository layer
│   │   └── audio/           # AudioRecord + WAV encoder & penyusun file
│   ├── src/main/res/
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/

_archive/                    # Kode lama yang tidak dipakai
└── Sumryfen-backend/        # Backend Node.js — TIDAK DIGUNAKAN LAGI

*.md                         # Dokumentasi desain (PRD, SRS, SDD, UI-UX, Task)
```

## 3. Setup Environment

- **Target SDK:** 34, **Min SDK:** 26, **Kotlin:** 2.0.x, **AGP:** 8.x, **Gradle:** 8.x
- **Dependensi utama:** OkHttp 4.x (HTTP ke API), Room 2.6.x (ktx, runtime, ksp-compiler), Gson
- **Server backend tidak diperlukan** — semua request langsung dari HP ke Groq API

### Build APK

Prasyarat di laptop/PC:
1. **Java 17** (atau lebih baru) — download dari oracle.com atau pakai OpenJDK
2. **Android Studio** (atau Android SDK command-line tools)
3. Setelah clone repo, buat file `local.properties`:

   ```
   # Windows:
   sdk.dir=C:\\Users\\<USERNAME>\\AppData\\Local\\Android\\Sdk
   # macOS / Linux:
   # sdk.dir=/Users/<USERNAME>/Library/Android/sdk
   ```

   Atau cukup buka project dengan Android Studio — `local.properties` dibuat otomatis.

Build APK debug:
```bash
cd Sumryfen
./gradlew assembleDebug
```

APK output: `Sumryfen/app/build/outputs/apk/debug/app-debug.apk`

Install ke HP:
- Copy `app-debug.apk` ke HP, buka file manager, tap untuk install
- Atau via USB: `adb install app-debug.apk`

## 4. Arsitektur — Direct API Calls (No Backend)

Aplikasi memanggil Groq API langsung dari Android tanpa perantara:

```
[Android App]
  ├── AudioRecord → PCM chunk (16kHz mono 16-bit)
  ├── Encode WAV → HTTP POST → Groq Whisper STT
  │                    ↕
  │              Transkrip teks
  ├── Akumulasi teks → HTTP POST → Groq Llama LLM
  │                    ↕
  │              Ringkasan teks
  └── Simpan ke SQLite lokal (Room)
```

### Alur perekaman:

1. `AudioRecorder.start()` → Flow PCM chunks
2. Tiap ~1 detik: kumpulkan chunk → encode ke WAV → `SttApiClient.transcribe()`
3. Hasil transkrip ditampilkan di UI dan diakumulasi
4. Tiap 60 detik ATAU ≥200 kata baru: `LlmApiClient.summarize()`
5. Hasil ringkasan diperbarui di panel UI
6. Saat [Stop]: ringkasan final → simpan ke Room DB
7. Jika pengaturan simpan audio aktif: file WAV utuh disimpan ke `filesDir/audio/{id}.wav`

## 5. API Clients (OkHttp)

### SttApiClient

```kotlin
suspend fun transcribe(
    baseUrl: String,      // misal https://api.groq.com/openai/v1
    apiKey: String,
    model: String,        // misal whisper-large-v3-turbo
    audioWav: ByteArray   // WAV binary (header + PCM)
): String                 // teks transkrip
```

HTTP: `POST {baseUrl}/audio/transcriptions` (multipart/form-data)

### LlmApiClient

```kotlin
suspend fun summarize(
    baseUrl: String,
    apiKey: String,
    model: String,          // misal llama-3-8b-instant
    transcript: String,
    previousSummary: String?
): String                   // teks ringkasan
```

HTTP: `POST {baseUrl}/chat/completions` (application/json)

Error: HTTP 429 → throws `RateLimitException(retryAfterSeconds)`

## 6. Database (Room — SQLite Lokal)

Table `meetings`:

| Kolom            | Tipe    | Keterangan                                     |
|------------------|---------|-------------------------------------------------|
| id               | INTEGER | PRIMARY KEY, auto-generate                      |
| title            | TEXT    | Default "Meeting {date}"                        |
| transcript       | TEXT    | Transkrip lengkap                               |
| summary          | TEXT    | Ringkasan final                                 |
| audio_file_path  | TEXT    | Nullable — path file .wav jika simpan diaktifkan|
| created_at       | INTEGER | Timestamp (epoch second)                        |
| duration_seconds | INTEGER | Durasi rekaman                                  |

## 7. Pengaturan (Settings — SharedPreferences)

| Pengaturan         | Default                     | Keterangan                                |
|--------------------|-----------------------------|-------------------------------------------|
| Simpan Audio       | false                       | Simpan .wav di filesDir/audio/{id}.wav    |
| STT Base URL       | https://api.groq.com/openai/v1 | Endpoint OpenAI-compatible             |
| STT API Key        | (kosong)                    | Key untuk STT                             |
| STT Model          | whisper-large-v3-turbo      | Model transkripsi                         |
| LLM Base URL       | https://api.groq.com/openai/v1 | Endpoint OpenAI-compatible             |
| LLM API Key        | (kosong)                    | Key untuk LLM (fallback ke STT key)       |
| LLM Model          | llama-3-8b-instant          | Model ringkasan                           |

## 8. Urutan Pengerjaan (Task Breakdown)

1. **Fase 0 (Setup):** Init project Android + dependensi + arsitektur dasar
2. **Fase 1 (Core Loop):** Rekam audio → kirim ke Groq STT → tampilkan transkrip
3. **Fase 2 (Summarization):** Akumulasi teks → kirim ke Groq LLM tiap 1 menit/200 kata
4. **Fase 3 (Penyimpanan):** Room DB + history list + detail meeting
5. **Fase 3.5 (Settings):** UI pengaturan + konfigurasi model + opsi simpan audio
6. **Fase 4 (Error & Limits):** Rate limit 429, timeout, koneksi internet, notifikasi
7. **Fase 5 (Polish & Deploy):** Material Design, APK release

## 9. Aturan & Konvensi

- **Bahasa:** Indonesia untuk semua dokumentasi dan UI aplikasi
- **Audio:** WAV 16kHz mono 16-bit PCM, chunk ~1 detik, dikirim langsung ke Groq API
- **Penyimpanan audio opsional:** Jika diaktifkan, file WAV utuh disusun dari chunk setelah stop, disimpan di `context.filesDir/audio/{meeting_id}.wav`
- **Penyimpanan data:** Semua data meeting di SQLite lokal Android. Tidak ada server/cloud
- **Batas operasional:**
  - LLM dipanggil setiap 1 menit ATAU ≥200 kata baru (RPM < 30 aman)
  - Tidak ada batas sesi server — aplikasi berjalan penuh di HP
- **Error:** HTTP 429 → baca `retry-after`, tunda request; koneksi internet putus → status offline
- **API key:** User input di Settings, disimpan di SharedPreferences

## 10. Referensi

Dokumen desain — baca untuk konteks lebih dalam:
- `PRD.md` — tujuan produk, fitur utama, target pengguna
- `SRS.md` — batasan sistem, aturan aplikasi, perilaku
- `SDD.md` — **perlu diperbarui** (arsitektur sekarang tanpa backend)
- `Task Breakdown.md` — daftar task per fase
- `UI-UX Flow.md` — layout dan alur setiap layar
