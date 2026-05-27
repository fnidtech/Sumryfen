# Sumryfen — Perekam & Peringkas Meeting Otomatis

Aplikasi Android untuk merekam meeting dengan **transkripsi real-time** dan **ringkasan otomatis**. Cukup satu APK — tidak perlu server atau backend terpisah.

## Fitur

- **Live Recording** — Rekam suara meeting langsung dari HP
- **Transkrip Real-time** — Teks transkrip muncul langsung saat Anda bicara
- **Ringkasan Otomatis** — Ringkasan poin penting diperbarui setiap 1 menit
- **Riwayat Meeting** — Semua transkrip dan ringkasan tersimpan di HP
- **Konfigurasi Fleksibel** — Ganti model STT/LLM dan API endpoint kapan saja
- **Simpan Audio** — Opsional: simpan rekaman sebagai file .wav

## Arsitektur

```
[Android App]
  ├── AudioRecord → PCM chunk (16kHz mono 16-bit)
  ├── Encode WAV → HTTP POST → Groq/OpenAI API (STT)
  ├── Akumulasi teks → HTTP POST → Groq/OpenAI API (LLM)
  └── Simpan ke SQLite lokal (Room)
```

**Tidak ada backend.** Semua pemrosesan via API langsung dari HP.

## Tech Stack

| Komponen | Teknologi |
|----------|-----------|
| Bahasa | Kotlin |
| UI | Material 3, Fragment + ViewModel |
| API Client | OkHttp 4.x |
| Database | Room (SQLite) |
| Speech-to-Text | Groq Whisper (`whisper-large-v3-turbo`) |
| Summarization | Groq Llama (`llama-3-8b-instant`) |

## Build APK

### Opsi 1: Android Studio (recommended)

1. Clone/download repo ini
2. Buka Android Studio, pilih **Open an existing project**
3. Pilih folder `Sumryfen/`
4. Android Studio otomatis download SDK + dependencies
5. Klik **Build → Build Bundle(s) / APK → Build APK**

### Opsi 2: SDK Command-line Tools (ringan, tanpa IDE)

**Step 1 — Download & extract Android SDK CLI tools:**
```powershell
# Download dari Google
# https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip

# Extract ke folder tetap
Expand-Archive -Path "$env:USERPROFILE\Downloads\commandlinetools-win-11076708_latest.zip" `
    -DestinationPath "C:\Android\Sdk\cmdline-tools" -Force

# Rename folder (wajib agar sdkmanager dikenali)
Rename-Item "C:\Android\Sdk\cmdline-tools\cmdline-tools" "C:\Android\Sdk\cmdline-tools\latest"
```

**Step 2 — Install platform Android 34:**
```powershell
cd "C:\Android\Sdk\cmdline-tools\latest\bin"
.\sdkmanager "platforms;android-34"
.\sdkmanager "build-tools;34.0.0"
.\sdkmanager --licenses
```

**Step 3 — Set environment variable (sementara):**
```powershell
$env:ANDROID_HOME = "C:\Android\Sdk"
```

**Step 4 — Build APK:**
```powershell
cd C:\path\ke\Sumryfen\Sumryfen
.\gradlew assembleDebug
```

> Agar tidak perlu set `ANDROID_HOME` tiap kali, buat file `Sumryfen/local.properties` berisi:
> ```
> sdk.dir=C:\\Android\\Sdk
> ```

**APK output:** `Sumryfen/app/build/outputs/apk/debug/app-debug.apk`

## Pengaturan Awal

Sebelum menggunakan, isi **API Key Groq** di menu Pengaturan:
1. Daftar gratis di [console.groq.com](https://console.groq.com) — dapatkan API key (Free Tier, tanpa kartu kredit)
2. Buka Pengaturan di aplikasi (ikon gear)
3. Masukkan API Key STT (dan LLM jika berbeda)
4. Sesuaikan model jika perlu (default: `whisper-large-v3-turbo` / `llama-3-8b-instant`)

> **Gratis.** Aplikasi ini berjalan penuh menggunakan Free Tier Groq API. Model default (`whisper-large-v3-turbo` untuk STT, `llama-3-8b-instant` untuk ringkasan) tersedia gratis tanpa perlu berlangganan. Batas rate limit Groq Free Tier lebih dari cukup untuk penggunaan meeting harian.

## Credits

Dibuat oleh **Fendi Novtiar** — [github.com/fnidtech](https://github.com/fnidtech)
