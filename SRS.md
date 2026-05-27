## 2. Software Requirements Specification (SRS)

### 2.1 Validasi dan Batasan Sistem
- **Durasi Maksimum**: Satu sesi meeting dibatasi **2 jam (7200 detik)**. Jika pengguna mencoba merekam lebih, sistem akan otomatis menghentikan perekaman dan menampilkan pesan “Batas maksimal 2 jam tercapai”.
- **Format Audio**: Backend menerima audio dalam format **WAV 16 kHz mono 16-bit** dari client. Konversi dilakukan di sisi Android sebelum dikirim.
- **Koneksi Internet**: Diperlukan koneksi stabil. Jika koneksi putus saat perekaman, aplikasi akan menampilkan status offline dan mencoba reconnect. Transkrip yang terlewat tidak akan diulang, kecuali jika buffer tersimpan (tidak diimplementasikan di versi awal).
- **API Key**: Backend menyimpan Groq API key sebagai environment variable. Client tidak menyimpan key.
- **Error Handling**:
  - HTTP 429 dari Groq/LLM → Backend akan menahan request dan mencoba lagi sesuai header `retry-after`, atau menampilkan notifikasi bahwa layanan sedang sibuk.
  - Input audio tidak valid (format salah) → Backend mengembalikan error “Format audio tidak didukung”.

### 2.2 Perilaku Aplikasi (Behavior)
- **Saat mulai rekam**: Client menginisialisasi WebSocket ke backend, lalu mulai mengirim chunk audio setiap **1 detik** (atau 2 detik) dalam format base64.
- **Saat menerima transkrip**: Backend mengirim teks baru ke client via WebSocket. Client menampilkan teks di layar dengan auto-scroll.
- **Ringkasan**: Backend mengakumulasi teks transkrip. Setiap **1 menit**, backend menjalankan summarization menggunakan LLM (Llama 3 8B via Groq) dengan konteks ringkasan sebelumnya. Hasil ringkasan baru dikirim ke client dan ditampilkan di panel ringkasan.
- **Saat stop**: Client mengirim sinyal “stop”. Backend melakukan finalisasi: ringkasan terakhir (bila ada teks tersisa), lalu menyimpan transkrip lengkap + ringkasan ke database/file.

### 2.3 Aturan Aplikasi
- **Interval Ringkasan**: Teks dikirim ke LLM setiap **1 menit** atau setelah minimal **200 kata** (mana yang lebih dulu). Ini untuk menjaga RPM di bawah 30.
- **Pembatasan RPM LLM**: Karena kita hanya memanggil LLM 1-2 kali per menit, otomatis aman (RPM rata-rata < 2).
- **Penyimpanan Lokal**: Riwayat meeting disimpan di SQLite lokal Android. Transkrip dan ringkasan disimpan sebagai teks. Tidak ada sinkronisasi cloud.
- **Privasi**: Audio tidak disimpan di server; hanya dikirim streaming ke Groq untuk diproses, hasil transkrip dikirim balik. File audio sementara dihapus dari memori setelah dikirim.