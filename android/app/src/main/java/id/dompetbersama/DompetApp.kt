package id.dompetbersama

import android.app.Application
import androidx.room.withTransaction
import id.dompetbersama.data.AppDatabase
import id.dompetbersama.data.BackupPrefs
import id.dompetbersama.data.KeywordPrefs
import id.dompetbersama.data.SourcePrefs
import id.dompetbersama.data.TransactionRepository
import id.dompetbersama.data.WalletPrefs

class DompetApp : Application() {
    val database by lazy { AppDatabase.build(this) }
    val repository by lazy { TransactionRepository(database.transactionDao()) { block -> database.withTransaction { block() } } }
    val backupPrefs by lazy { BackupPrefs(this) }
    val sourcePrefs by lazy { SourcePrefs(this) }
    val keywordPrefs by lazy { KeywordPrefs(this) }
    val walletPrefs by lazy { WalletPrefs(this) }
}
