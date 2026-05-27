## 1. Product Requirement Document (PRD)

### 1.1 Tujuan Produk
Membantu profesional mencatat dan merangkum meeting secara otomatis **tanpa perlu menyimpan rekaman audio** – cukup merekam suara, lalu secara langsung (live) teks transkrip dan ringkasan muncul. Semua pemrosesan dilakukan di cloud (Groq & LLM) dengan tetap menjaga privasi karena audio hanya lewat untuk diproses, tidak disimpan permanen di server.

### 1.2 Target Pengguna
- **Profesional** yang sering mengikuti rapat, diskusi, atau interview.
- **Mahasiswa/peneliti** yang ingin mencatat seminar atau kuliah.
- **Pengguna Android** yang mencari solusi gratis dengan kualitas transkripsi tinggi.

### 1.3 Fitur Utama
- **Live Recording** – Merekam suara meeting secara langsung tanpa menyimpan file audio mentah secara persisten (hanya buffer sementara).
- **Transkrip Real-time** – Teks hasil transkripsi muncul bertahap (delay ~1-2 detik) seiring pembicaraan berlangsung.
- **Ringkasan Otomatis** – Setiap 1-2 menit, ringkasan poin penting diperbarui dan ditampilkan di panel terpisah. Ringkasan final akan disimpan setelah meeting berhenti.
- **Pembatasan Durasi** – Maksimal 2 jam per sesi meeting untuk menjaga kestabilan dan tidak kena rate limit API gratis.
- **History Meeting** – Menyimpan transkrip lengkap dan ringkasan final setiap meeting yang telah selesai. Dapat dibuka kembali untuk dibaca atau dibagikan.

### 1.4 User Flow (Alur Singkat)
1. Buka aplikasi → lihat daftar meeting sebelumnya.
2. Tekan tombol **"New Meeting"** untuk mulai.
3. Layar perekaman muncul:
   - Tombol mulai rekam.
   - Area transkrip yang terus bertambah.
   - Panel ringkasan yang diperbarui secara periodik.
4. Tekan tombol **"Stop"** untuk mengakhiri meeting → ringkasan final disimpan, semua data tersimpan di riwayat.
5. Pengguna dapat membuka detail meeting untuk melihat transkrip lengkap dan ringkasan.