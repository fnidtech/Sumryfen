## 4. UI/UX Flow

### 4.1 Layar Utama (Home)
```
+---------------------------------------+
|  Sumryfen               [Settings]  |
+---------------------------------------+
|  History Meeting:                     |
|  +----------------------------------+ |
|  | 📅 25 Mei 2026 - 09:30         > | |
|  | Durasi: 1j 20m                  | |
|  | Cuplikan ringkasan...           | |
|  +----------------------------------+ |
|  +----------------------------------+ |
|  | 📅 24 Mei 2026 - 14:00         > | |
|  | ...                              | |
|  +----------------------------------+ |
|                                       |
|  [ + New Meeting ]  (Floating Action) |
+---------------------------------------+
```
Ketika pengguna menekan item meeting → layar detail meeting (full transkrip & ringkasan).

### 4.2 Layar Meeting (Live)
```
+---------------------------------------+
|  ⚫ Rekaman berjalan...  [00:01:23]   |
+---------------------------------------+
|  [ Stop ]     [ Pause ]  (opsional)   |
+---------------------------------------+
|  --- Live Transkrip ---              |
|  "Halo semuanya, terima kasih..."     |
|  "Baik, kita mulai meeting pagi..."   |
|  ... (auto-scroll)                   |
+---------------------------------------+
|  --- Ringkasan Sementara ---         |
|  * Pembahasan poin A                  |
|  * Ada usulan budget...              |
|  (Update setiap 1 menit)             |
+---------------------------------------+
```
*Elemen:* Timer menunjukkan durasi berjalan. Tombol stop (warna merah). Teks transkrip akan bertambah setiap ~1 detik. Panel ringkasan bisa di-scroll terpisah.

### 4.3 Layar Detail Meeting (Setelah Selesai)
```
+---------------------------------------+
|  < Meeting 25 Mei 2026, 09:30        |
+---------------------------------------+
|  [ Share/Export ]                     |
+---------------------------------------+
|  Ringkasan:                           |
|  - Poin penting 1                     |
|  - Poin penting 2                     |
|  ...                                  |
+---------------------------------------+
|  Transkrip Lengkap:                   |
|  [00:00] Halo semuanya...            |
|  [00:15] Baik, kita akan bahas...    |
|  [00:45] ...                          |
|  (scrollable)                        |
+---------------------------------------+
```
*Fitur share:* Memungkinkan pengguna membagikan teks ringkasan + transkrip ke aplikasi lain (email, WhatsApp, dll).