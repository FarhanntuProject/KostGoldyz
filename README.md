# Kost Goldyz (Android)

Aplikasi Kost Goldyz & Kontrakan untuk HP Android. **Tidak perlu login.**

- Semua halaman ada di dalam aplikasi (`app/src/main/assets/index.html`).
- Data dan foto bukti transfer/kwitansi disimpan di HP ini.
- Saat pertama dibuka, data per 2 November 2026 sudah terisi: 5 penghuni kos, pembayaran November, maintenance (WiFi, bersih-bersih, saldo Rp735.503,49), dan kontrakan Rino Poetra.
- Bisa dipakai tanpa internet. Internet hanya dibutuhkan untuk font dan untuk membuka WhatsApp/Google Calendar.

Catatan: kalau aplikasi dihapus atau "Hapus data" dilakukan di pengaturan HP, datanya ikut hilang.

## Build APK lewat GitHub
1. Upload semua isi folder ini ke repository GitHub.
2. Kalau folder `.github` tidak ikut ter-upload, buka tab **Actions**, pilih **set up a workflow yourself**, beri nama `build-apk.yml`, lalu paste isi `.github/workflows/build-apk.yml`.
3. Tunggu sampai muncul centang hijau, lalu download **KostGoldyz-apk** dari bagian **Artifacts**.
