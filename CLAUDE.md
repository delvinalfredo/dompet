# Dompet Bersama

Aplikasi pencatat keuangan Android, lokal di HP. Notifikasi bank/e-wallet dibaca, di-parse, lalu disimpan sebagai transaksi.

## Stack (keputusan)
- Android only: Kotlin + Jetpack Compose + Room + NotificationListenerService, folder `android/`
- Tanpa backend, tanpa login. Semua data lokal (offline).
- Sharing antar user / backend: ditunda, mungkin nanti (Firebase/Supabase atau server sendiri).

## Aturan
- Ikuti skill di `.claude/skills` (antislop-layoutmobile, antislop-ui, security-review, coding-standards).
- Satu notifikasi = satu transaksi (cegah duplikat dengan hash nominal+waktu+sumber).
- Parser notifikasi modular per aplikasi (bank/e-wallet).
- Jangan log data sensitif (nominal + rekening penuh).
