## 5. Task Breakdown

### Fase 0: Persiapan & Fondasi (Setup)
**Tujuan:** Memastikan lingkungan pengembangan siap dan dua komponen utama (Android & backend) bisa berkomunikasi.

| ID | Tugas | Deskripsi Singkat | Output |
|----|-------|-------------------|--------|
| 0.1 | Setup project Android | Buat project Android Studio dengan Kotlin, pilih target SDK, inisialisasi git. | Project kosong yang bisa di-build. |
| 0.2 | Setup backend Node.js | Inisialisasi project Node.js, install `ws`, `express`, `dotenv`, buat server WebSocket sederhana dan health-check HTTP. | Server berjalan di localhost, bisa menerima koneksi WebSocket. |
| 0.3 | Uji koneksi | Dari Android, buat koneksi WebSocket ke backend lokal (gunakan library OkHttp), kirim pesan test. | Log berhasil connect dan kirim/terima pesan. |
| 0.4 | Siapkan Groq API key | Daftar ke Groq, dapatkan API key, simpan di `.env` backend. Uji panggil STT dan LLM via Postman/curl. | Berhasil dapat transkrip dari file audio pendek, dan ringkasan dari teks. |

---

### Fase 1: Core Loop – Rekam & Transkrip (STT)
**Tujuan:** Merekam audio, mengirim ke backend, dan menampilkan transkrip langsung di layar.

| ID | Tugas | Deskripsi Singkat | Output |
|----|-------|-------------------|--------|
| 1.1 | Implementasi perekaman audio di Android | Gunakan `AudioRecord` untuk stream audio mentah, format 16kHz mono 16-bit PCM. Simpan di buffer ringan, jangan simpan file. | Fungsi `startRecording()` dan `stopRecording()` mengembalikan byte array per detik. |
| 1.2 | Konversi ke format yang diminta Groq | Ubah buffer PCM menjadi WAV (tambah header) atau langsung kirim sebagai raw bytes? Groq API menerima file. Kita kirim chunk per 1 detik sebagai base64. | Fungsi `encodeToWavBase64(byte[])` yang siap dikirim. |
| 1.3 | Kirim audio chunk via WebSocket | Setiap 1 detik, kirim pesan `{ type: "audio", data: "<base64>" }` ke backend. Pastikan ada throttle agar tidak overload. | Backend menerima pesan audio. |
| 1.4 | Backend: terima audio dan panggil Groq STT | Setiap kali menerima pesan audio, kirim HTTP POST ke Groq Whisper. Tangkap respons teks. | Backend bisa mengembalikan teks transkrip per chunk. |
| 1.5 | Backend kirim hasil transkrip ke client | Kirim event `transcript` dengan teks ke client via WebSocket. | Client menerima dan menampilkan teks di log. |
| 1.6 | UI Live Transkrip | Buat layout dengan `RecyclerView` atau `TextView` yang terus bertambah. Auto-scroll saat teks baru masuk. | Layar meeting menampilkan teks yang terus bertambah saat bicara. |
| 1.7 | Timer dan kontrol rekam | Tampilkan timer berjalan (HH:MM:SS), tombol stop. Saat stop, tutup WebSocket dan hentikan perekaman. | Pengguna bisa melihat durasi dan menghentikan meeting. |

---

### Fase 2: Ringkasan Otomatis (LLM)
**Tujuan:** Menghasilkan ringkasan bertahap dan final menggunakan LLM.

| ID | Tugas | Deskripsi Singkat | Output |
|----|-------|-------------------|--------|
| 2.1 | Backend: akumulasi teks transkrip | Di backend, simpan semua teks transkrip yang masuk dalam satu variabel (per sesi). | Variabel `fullTranscript` yang terus bertambah. |
| 2.2 | Backend: timer periodik untuk ringkasan | Gunakan `setInterval` setiap 1 menit (atau cek 200 kata). Panggil fungsi `generateSummary()`. | Setiap 1 menit, backend memproses ringkasan. |
| 2.3 | Implementasi fungsi `generateSummary` | Ambil `fullTranscript` terbaru dan ringkasan sebelumnya (jika ada). Kirim ke Groq LLM (Llama 3) dengan prompt yang tepat. | Fungsi mengembalikan ringkasan baru. |
| 2.4 | Kirim ringkasan ke client | Kirim event `summary` ke client via WebSocket. | Client menampilkan di panel ringkasan. |
| 2.5 | UI Panel Ringkasan | Tambahkan area khusus di bawah transkrip untuk menampilkan teks ringkasan. Update otomatis saat event `summary` diterima. | Ringkasan muncul dan diperbarui setiap 1 menit. |
| 2.6 | Finalisasi ringkasan saat stop | Saat terima sinyal `stop`, jika ada teks tersisa yang belum diringkas, lakukan ringkasan terakhir. Kirim ringkasan final. | Ringkasan akhir komplit, simpan di objek meeting. |

---

### Fase 3: Riwayat & Penyimpanan Lokal
**Tujuan:** Menyimpan meeting yang sudah selesai agar bisa dibuka kembali.

| ID | Tugas | Deskripsi Singkat | Output |
|----|-------|-------------------|--------|
| 3.1 | Setup Room database | Tambahkan dependensi Room di Android, buat entity `Meeting` dan DAO. | Database siap digunakan. |
| 3.2 | Simpan meeting saat stop | Setelah menerima ringkasan final dan transkrip lengkap, simpan objek `Meeting` ke database (judul default, transkrip, ringkasan, timestamp). | Data tersimpan dan bisa di-query. |
| 3.3 | UI Home (History) | Buat tampilan daftar meeting menggunakan `RecyclerView`. Item menampilkan tanggal, durasi, dan potongan ringkasan. | Layar utama menampilkan riwayat meeting. |
| 3.4 | UI Detail Meeting | Buat activity/fragment untuk melihat transkrip lengkap dan ringkasan dari meeting yang dipilih. Tambahkan fitur share teks. | Pengguna bisa membaca transkrip lama dan membagikannya. |

---

### Fase 4: Penanganan Batasan & Error
**Tujuan:** Menjaga stabilitas dengan mematuhi rate limit dan menangani error.

| ID | Tugas | Deskripsi Singkat | Output |
|----|-------|-------------------|--------|
| 4.1 | Backend: tracker durasi per sesi dan per jam | Lacak total detik audio yang diproses dalam 1 jam terakhir. Jika melebihi 7200 detik, tolak koneksi baru atau hentikan sesi. | Pembatasan 2 jam per jam berfungsi. |
| 4.2 | Backend: penanganan rate limit (429) | Saat panggil Groq STT atau LLM, tangkap error 429. Baca header `retry-after`, tunda request, atau beri tahu client bahwa layanan sibuk. | Tidak crash, client dapat notifikasi yang jelas. |
| 4.3 | Android: notifikasi error ke pengguna | Tampilkan snackbar atau dialog saat koneksi gagal, rate limit, atau format tidak didukung. | Pengguna mendapat umpan balik yang ramah. |
| 4.4 | Uji skenario | Simulasikan meeting 2 jam, cek apakah berhenti otomatis. Uji koneksi putus. Uji fast speaking. | Semua batasan terverifikasi. |

---

### Fase 5: Polish & Deployment
**Tujuan:** Memperbaiki tampilan dan menyiapkan rilis versi awal.

| ID | Tugas | Deskripsi Singkat | Output |
|----|-------|-------------------|--------|
| 5.1 | Desain visual & tema | Terapkan Material Design, warna, ikon, font. Pastikan tampilan clean dan mudah dibaca. | UI menarik dan konsisten. |
| 5.2 | Splash screen & icon app | Buat splash screen dan ikon aplikasi Sumryfen. | Identitas aplikasi siap. |
| 5.3 | Build APK & test di device nyata | Buat APK debug, install di HP Android, uji langsung dengan suara meeting asli. | Aplikasi berfungsi di perangkat fisik. |
| 5.4 | Deployment backend | Deploy backend ke platform gratis seperti `render.com` atau `fly.io`. Siapkan environment variable. | Backend online dan bisa diakses dari mana saja. |
| 5.5 | Update endpoint di Android | Ganti URL WebSocket dari `localhost` ke URL production. | Aplikasi siap digunakan secara publik (personal). |