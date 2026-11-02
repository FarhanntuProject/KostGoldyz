# Pengingat Kos (Android)

Aplikasi Android untuk mengingatkan pembayaran sewa kos.

## Fitur

- Hitung mundur ke tanggal jatuh tempo, dengan status Aman, Segera bayar, dan Terlambat
- Notifikasi otomatis setiap hari sekitar jam 09.00, mulai H-1 sampai H-7 (bisa diatur) hingga sewa dicatat lunas. Aplikasi tidak perlu dibuka dan tidak butuh internet.
- Catat pembayaran: bulan, tanggal bayar, jumlah, dan keterangan
- Riwayat pembayaran dengan tanda "telat" dan total
- Tombol "Kabari pemilik" yang membuka WhatsApp dengan pesan konfirmasi transfer
- Salin nomor rekening, nomor WhatsApp, dan nominal sewa dengan sekali ketuk
- Tunggakan beberapa bulan dihitung otomatis
- Mendukung mode gelap

Data hanya disimpan di HP (tidak dikirim ke mana pun). Jika aplikasi dihapus, data ikut terhapus.

## Cara 1: Build lewat GitHub (tanpa Android Studio)

1. Buat repository baru di GitHub, lalu upload seluruh isi folder ini, termasuk folder `.github`.
2. Buka tab **Actions**. Workflow **Build APK** akan berjalan otomatis, sekitar 5–8 menit.
3. Setelah selesai (centang hijau), buka run tersebut. Di bagian **Artifacts**, download **PengingatKos-apk** (berupa zip).
4. Ekstrak zip-nya, lalu kirim file `app-debug.apk` ke HP.
5. Di HP, buka file APK tersebut. Izinkan "Instal dari sumber tidak dikenal" jika diminta, lalu pilih **Instal**.

## Cara 2: Build dengan Android Studio

1. Install Android Studio versi terbaru.
2. Pilih **File → Open**, lalu buka folder `PengingatKos`. Tunggu proses Gradle sync selesai.
3. Sambungkan HP dengan kabel USB dan aktifkan **USB debugging**, lalu klik **Run ▶**.
   Untuk membuat file APK, pilih **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.

## Setelah terpasang

- Saat pertama kali dibuka, izinkan **notifikasi**.
- Isi data kos: nama, nominal sewa, tanggal jatuh tempo, dan H-berapa ingin diingatkan.
- Ketuk **Coba kirim notifikasi** untuk memastikan notifikasi muncul.
- Beberapa merek HP (Xiaomi, Oppo, Vivo, Realme) suka mematikan aplikasi di latar belakang. Jika notifikasi tidak pernah muncul, buka **Pengaturan → Aplikasi → Pengingat Kos → Baterai**, lalu pilih **Tanpa batasan** dan aktifkan **Mulai otomatis**.

## Teknis

Kotlin, Jetpack Compose (Material 3), WorkManager untuk pengecekan harian, minSdk 26 (Android 8.0 ke atas).
