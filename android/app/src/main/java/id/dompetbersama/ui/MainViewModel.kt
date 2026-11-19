package id.dompetbersama.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.dompetbersama.DompetApp
import id.dompetbersama.data.BackupCodec
import id.dompetbersama.data.BackupData
import id.dompetbersama.data.BackupException
import id.dompetbersama.data.CsvExporter
import id.dompetbersama.data.TxStatus
import id.dompetbersama.data.KeywordRule
import id.dompetbersama.data.KnownSources
import id.dompetbersama.data.MoneyTransaction
import id.dompetbersama.data.Opening
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SourceApp(val packageName: String, val label: String, val enabled: Boolean)

/** A backup file read and checked, waiting for the user to confirm it may replace [currentCount] rows. */
data class RestorePlan(val data: BackupData, val currentCount: Int)

data class MoneySettings(val openings: Map<String, Opening>, val main: String?, val monthlyLimit: Long?)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dompet = application as DompetApp
    private val repository = dompet.repository

    val confirmed: StateFlow<List<MoneyTransaction>> =
        repository.confirmed.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val pending: StateFlow<List<MoneyTransaction>> =
        repository.pending.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** null while loading. */
    private val _sources = MutableStateFlow<List<SourceApp>?>(null)
    val sources: StateFlow<List<SourceApp>?> = _sources.asStateFlow()

    private val _keywordRules = MutableStateFlow(dompet.keywordPrefs.all())
    val keywordRules: StateFlow<List<KeywordRule>> = _keywordRules.asStateFlow()

    private val _money = MutableStateFlow(readMoney())
    val money: StateFlow<MoneySettings> = _money.asStateFlow()

    private val _lastBackupAt = MutableStateFlow(dompet.backupPrefs.lastBackupAt())
    val lastBackupAt: StateFlow<Long?> = _lastBackupAt.asStateFlow()

    private val _restorePlan = MutableStateFlow<RestorePlan?>(null)
    val restorePlan: StateFlow<RestorePlan?> = _restorePlan.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun save(tx: MoneyTransaction, transferChanged: Boolean) {
        viewModelScope.launch { repository.save(tx, transferChanged) }
    }

    fun confirm(tx: MoneyTransaction) {
        viewModelScope.launch { repository.confirm(tx) }
    }

    fun confirmTransfer(group: String) {
        viewModelScope.launch { repository.confirmTransfer(group) }
    }

    fun splitTransfer(group: String) {
        viewModelScope.launch { repository.splitTransfer(group) }
    }

    fun discard(tx: MoneyTransaction) {
        viewModelScope.launch { repository.discard(tx) }
    }

    fun loadSources() {
        viewModelScope.launch {
            _sources.value = withContext(Dispatchers.IO) { installedApps() }
        }
    }

    fun setSourceEnabled(packageName: String, enabled: Boolean) {
        dompet.sourcePrefs.setEnabled(packageName, enabled)
        _sources.update { list ->
            list?.map { if (it.packageName == packageName) it.copy(enabled = enabled) else it }
        }
    }

    fun addKeywordRule(rule: KeywordRule) {
        dompet.keywordPrefs.add(rule)
        _keywordRules.value = dompet.keywordPrefs.all()
    }

    fun removeKeywordRule(rule: KeywordRule) {
        dompet.keywordPrefs.remove(rule)
        _keywordRules.value = dompet.keywordPrefs.all()
    }

    /** [amount]: what the bank app shows right now; only later transactions move it. Null keeps the balance. */
    fun setWallet(name: String, amount: Long?, isMain: Boolean) {
        val prefs = dompet.walletPrefs
        if (amount != null) prefs.setOpening(name, Opening(amount, System.currentTimeMillis()))
        when {
            isMain -> prefs.setMain(name)
            prefs.main() == name -> prefs.setMain(null)
        }
        _money.value = readMoney()
    }

    fun removeWallet(name: String) {
        dompet.walletPrefs.removeOpening(name)
        _money.value = readMoney()
    }

    fun setMonthlyLimit(limit: Long?) {
        dompet.walletPrefs.setMonthlyLimit(limit)
        _money.value = readMoney()
    }

    private fun readMoney(): MoneySettings = dompet.walletPrefs.let {
        MoneySettings(openings = it.openings(), main = it.main(), monthlyLimit = it.monthlyLimit())
    }

    fun backup(uri: Uri) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val (on, off) = dompet.sourcePrefs.userChanges()
            val wallets = dompet.walletPrefs
            val data = BackupData(
                createdAt = now,
                transactions = repository.everythingForBackup(),
                keywords = dompet.keywordPrefs.all(),
                openings = wallets.openings(),
                mainWallet = wallets.main(),
                monthlyLimit = wallets.monthlyLimit(),
                sourcesOn = on,
                sourcesOff = off,
            )
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val stream = dompet.contentResolver.openOutputStream(uri) ?: error("no stream")
                    stream.use { it.write(BackupCodec.encode(data).toByteArray(Charsets.UTF_8)) }
                }.isSuccess
            }
            if (ok) {
                dompet.backupPrefs.setLastBackupAt(now)
                _lastBackupAt.value = now
            }
            _message.value = if (ok) "Backup tersimpan: ${data.transactions.size} transaksi" else "Backup gagal. Coba pilih lokasi lain."
        }
    }

    /** Reads and checks the file; nothing changes until [applyRestore]. */
    fun readBackup(uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = dompet.contentResolver.openInputStream(uri)?.use { it.readNBytesCompat(BackupCodec.MAX_BYTES + 1) }
                        ?: throw BackupException("File tidak bisa dibuka.")
                    if (bytes.size > BackupCodec.MAX_BYTES) throw BackupException("File terlalu besar untuk backup Dompet Bersama.")
                    BackupCodec.decode(bytes.toString(Charsets.UTF_8))
                }
            }
            result
                .onSuccess { data ->
                    val current = repository.everythingForBackup().count { it.status != TxStatus.DISCARDED }
                    _restorePlan.value = RestorePlan(data, current)
                }
                .onFailure { _message.value = (it as? BackupException)?.message ?: "File tidak bisa dibaca." }
        }
    }

    fun cancelRestore() {
        _restorePlan.value = null
    }

    fun applyRestore() {
        val plan = _restorePlan.value ?: return
        _restorePlan.value = null
        viewModelScope.launch {
            val data = plan.data
            // Once started, the database and the preferences are replaced together even if the screen goes away.
            val ok = withContext(NonCancellable) {
                try {
                    repository.replaceAll(data.transactions)
                } catch (e: Exception) {
                    return@withContext false
                }
                dompet.keywordPrefs.replaceAll(data.keywords)
                dompet.walletPrefs.replaceAll(data.openings, data.mainWallet, data.monthlyLimit)
                dompet.sourcePrefs.replaceUserChanges(data.sourcesOn, data.sourcesOff)
                true
            }
            if (!ok) {
                _message.value = "Pemulihan gagal. Data di HP ini tidak berubah."
                return@launch
            }
            _keywordRules.value = dompet.keywordPrefs.all()
            _money.value = readMoney()
            if (_sources.value != null) loadSources()
            val shown = data.transactions.count { it.status != TxStatus.DISCARDED }
            _message.value = "Data dipulihkan: $shown transaksi"
        }
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            val items = repository.confirmedForExport()
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val stream = dompet.contentResolver.openOutputStream(uri) ?: error("no stream")
                    stream.use { CsvExporter.write(it, items) }
                }.isSuccess
            }
            _message.value = if (ok) "Ekspor selesai: ${items.size} transaksi" else "Ekspor gagal. Coba pilih lokasi lain."
        }
    }

    fun notify(text: String) {
        _message.value = text
    }

    fun consumeMessage() {
        _message.value = null
    }

    @Suppress("DEPRECATION") // the flags-object overload needs API 33
    private fun installedApps(): List<SourceApp> {
        val pm = dompet.packageManager
        val enabled = dompet.sourcePrefs.enabledPackages()
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(launcher, 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .distinctBy { it.first }
            .filter { it.first != dompet.packageName }
            .map { (pkg, label) -> SourceApp(pkg, label, pkg in enabled) }
            .sortedWith(compareByDescending<SourceApp> { it.packageName in KnownSources.packages }.thenBy { it.label.lowercase() })
    }
}

/** InputStream.readNBytes needs API 33; minSdk is 26. Stops after [limit] bytes so a huge file cannot fill memory. */
private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(64 * 1024)
    while (out.size() < limit) {
        val read = read(buffer, 0, minOf(buffer.size, limit - out.size()))
        if (read < 0) break
        out.write(buffer, 0, read)
    }
    return out.toByteArray()
}
