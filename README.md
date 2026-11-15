# NextyLibrary

Aplikasi Android perpustakaan digital dan pembaca buku dari Nexty Studio.

## Arah produk
- Tidak menampilkan iklan dalam bentuk apa pun.
- Semua buku dan fitur membaca tersedia gratis; tidak ada bab terkunci atau kewajiban menonton iklan.
- Donasi bersifat sukarela, opsional, dan tidak memberikan keuntungan khusus atau membuka konten.
- Jangan menambahkan SDK iklan, iklan rewarded, interstitial, banner, atau mekanisme promosi paksa.
- Fitur donasi direncanakan menggunakan QRIS: halaman Donasi menampilkan gambar QRIS resmi milik penerima, nama penerima, dan petunjuk singkat untuk memindai melalui aplikasi pembayaran. Donasi bersifat sukarela; semua buku dan fitur tetap gratis.
- QRIS tidak boleh dibuat-buat atau memakai kode milik pihak lain. Integrasi menunggu gambar QRIS resmi dan nama penerima yang telah diverifikasi dari penyedia pembayaran/merchant yang sah. Jangan pernah menaruh PIN, kata sandi, OTP, atau data rahasia pembayaran di aplikasi.
- Pendaftaran merchant dan penerimaan dana harus dilakukan oleh pemilik akun yang berwenang; bila pengembang belum memenuhi syarat usia, minta orang tua/wali atau badan yang sah untuk mengurusnya.

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
