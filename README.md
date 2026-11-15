# NextyLibrary

Aplikasi Android perpustakaan digital dan pembaca buku dari Nexty Studio.

## Arah produk
- Tidak menampilkan iklan dalam bentuk apa pun.
- Semua buku dan fitur membaca tersedia gratis; tidak ada bab terkunci atau kewajiban menonton iklan.
- Donasi bersifat sukarela, opsional, dan tidak memberikan keuntungan khusus atau membuka konten.
- Jangan menambahkan SDK iklan, iklan rewarded, interstitial, banner, atau mekanisme promosi paksa.
- Fitur donasi direncanakan sebagai dukungan sukarela. Penyedia pembayaran, ketentuan usia, perlindungan pengguna, dan kebijakan platform harus ditinjau sebelum integrasi. Jangan menyimpan data kartu atau kredensial pembayaran di aplikasi.

## Teknologi
- Kotlin + Jetpack Compose
- Supabase (Postgres, Auth, Storage)
- GitHub Actions untuk build APK

## Build
GitHub Actions akan menjalankan build saat ada push ke `main`, atau melalui tab Actions > Android APK > Run workflow.

Artifact build bernama `NextyLibrary-debug`.

## Status fondasi
Aplikasi memiliki katalog buku, autentikasi, dan pembaca konten. Fitur lanjutan seperti penyempurnaan pembaca, bookmark/progres tersinkron, panel admin, dan halaman donasi masih perlu dikembangkan serta diuji.

Distribusikan hanya buku yang public domain, berlisensi sesuai penggunaan, atau sudah mendapat izin distribusi. Cantumkan atribusi dan lisensi sesuai persyaratan masing-masing sumber.
