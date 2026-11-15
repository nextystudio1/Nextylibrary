# NextyLibrary

Aplikasi Android katalog dan pembaca buku dari Nexty Studio.

## Teknologi
- Kotlin + Jetpack Compose
- Supabase (Postgres, Auth, Storage)
- GitHub Actions untuk build APK

## Build
GitHub Actions akan menjalankan build saat ada push ke `main`, atau melalui tab Actions > Android APK > Run workflow.

Artifact build bernama `NextyLibrary-debug`.

## Status fondasi
Saat ini tersedia kerangka UI, pencarian katalog, detail buku, dan koneksi baca-katalog dari tabel `books` di Supabase. Autentikasi, pembaca PDF, bookmark/progres, panel admin, AdMob, serta verifikasi server-side rewarded-ad belum diimplementasikan. Jangan gunakan client callback iklan sebagai bukti unlock; unlock perlu diverifikasi di server (AdMob SSV) terlebih dahulu.

Distribusikan hanya buku yang public domain, berlisensi, atau sudah mendapat izin distribusi.
