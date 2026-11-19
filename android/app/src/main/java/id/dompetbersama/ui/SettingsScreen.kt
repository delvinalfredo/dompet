package id.dompetbersama.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import id.dompetbersama.data.KeywordRule
import id.dompetbersama.data.KnownSources
import id.dompetbersama.data.MoneyTransaction
import id.dompetbersama.data.TxStatus
import id.dompetbersama.data.walletBalances
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.dompetbersama.ui.theme.Dompet
import id.dompetbersama.ui.theme.FigureSmall
import java.time.LocalDate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    sources: List<SourceApp>?,
    hasAccess: Boolean,
    contentPadding: PaddingValues,
    onLoadSources: () -> Unit,
    onToggleSource: (String, Boolean) -> Unit,
    keywordRules: List<KeywordRule>,
    onAddRule: (KeywordRule) -> Unit,
    onRemoveRule: (KeywordRule) -> Unit,
    onExport: (android.net.Uri) -> Unit,
    onSettingUnavailable: () -> Unit,
    confirmed: List<MoneyTransaction>,
    money: MoneySettings,
    onSaveWallet: (String, Long?, Boolean) -> Unit,
    onRemoveWallet: (String) -> Unit,
    onSetLimit: (Long?) -> Unit,
    lastBackupAt: Long?,
    restorePlan: RestorePlan?,
    onBackup: (android.net.Uri) -> Unit,
    onPickedRestore: (android.net.Uri) -> Unit,
    onConfirmRestore: () -> Unit,
    onCancelRestore: () -> Unit,
) {
    val context = LocalContext.current
    var showingKeywords by rememberSaveable { mutableStateOf(false) }
    var showingWallets by rememberSaveable { mutableStateOf(false) }
    var editingLimit by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    // Keyed on the query: a new search starts again from the first page.
    var page by rememberSaveable(query) { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) onExport(uri)
    }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) onBackup(uri)
    }
    // */* as well: some file pickers label a .json file as octet-stream and would grey it out.
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onPickedRestore(uri)
    }
    LaunchedEffect(Unit) { onLoadSources() }

    // Above the sub-screens too: the plan can arrive while the user has moved on.
    restorePlan?.let { RestoreDialog(it, onConfirm = onConfirmRestore, onDismiss = onCancelRestore) }

    if (showingWallets) {
        val enabledLabels = sources.orEmpty().filter { it.enabled }.map { it.label }
        WalletScreen(
            candidates = remember(enabledLabels, confirmed, money.openings) { walletCandidates(enabledLabels, confirmed, money.openings) },
            money = money,
            balances = remember(money, confirmed) {
                walletBalances(money.openings, money.main, confirmed).associate { it.name to it.balance }
            },
            contentPadding = contentPadding,
            onBack = { showingWallets = false },
            onSave = onSaveWallet,
            onRemove = onRemoveWallet,
        )
        return
    }

    if (showingKeywords) {
        KeywordScreen(
            rules = keywordRules,
            sources = sources,
            contentPadding = contentPadding,
            onBack = { showingKeywords = false },
            onAddRule = onAddRule,
            onRemoveRule = onRemoveRule,
        )
        return
    }

    fun open(action: String) {
        if (!openSystemSetting(context, action)) onSettingUnavailable()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item { ScreenTitle("Pengaturan") }

        item {
            SectionHeader("Akses notifikasi")
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Status", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(
                        if (hasAccess) "Aktif" else "Belum aktif",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (hasAccess) Dompet.Green else Dompet.Danger,
                    )
                }
                Spacer(Modifier.height(12.dp))
                if (hasAccess) {
                    SecondaryButton("Buka pengaturan akses", onClick = { open(NOTIFICATION_ACCESS) })
                } else {
                    PrimaryButton("Aktifkan akses notifikasi", onClick = { open(NOTIFICATION_ACCESS) })
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Di beberapa HP (Xiaomi, Oppo, Vivo), sistem mematikan pembaca notifikasi di latar belakang. Matikan optimasi baterai untuk Dompet Bersama supaya tetap jalan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Dompet.Muted,
                )
                Spacer(Modifier.height(12.dp))
                SecondaryButton("Atur optimasi baterai", onClick = { open(BATTERY_OPTIMIZATION) })
            }
        }

        item {
            SectionHeader("Backup dan ekspor")
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(
                    lastBackupAt?.let { "Backup terakhir ${formatDate(it)}." }
                        ?: "Belum pernah backup. Data hanya ada di HP ini: kalau HP hilang, direset, atau app terhapus, catatan tidak bisa kembali.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (lastBackupAt == null) Dompet.Danger else Dompet.Ink,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Simpan file backup di luar HP, misalnya Google Drive. Isinya semua catatan keuangan, jadi jangan dibagikan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Dompet.Muted,
                )
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Simpan backup", onClick = { backupLauncher.launch("dompet-bersama-backup-${LocalDate.now()}.json") })
                Spacer(Modifier.height(8.dp))
                SecondaryButton("Pulihkan dari backup", onClick = { restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*")) })
                Spacer(Modifier.height(20.dp))
                Text(
                    "Untuk dibuka di Excel atau Google Sheets: transaksi yang sudah dikonfirmasi dalam file CSV. File CSV tidak bisa dipakai untuk memulihkan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Dompet.Muted,
                )
                Spacer(Modifier.height(12.dp))
                SecondaryButton("Ekspor CSV", onClick = { exportLauncher.launch("dompet-bersama-${LocalDate.now()}.csv") })
            }
        }

        item {
            SectionHeader("Dompet dan batas")
            EntryRow(
                title = "Saldo per dompet",
                subtitle = when (val n = money.openings.size) {
                    0 -> "Belum ada dompet yang dihitung"
                    else -> "$n dompet dihitung" + (money.main?.let { ", utama $it" } ?: "")
                },
                onClick = { showingWallets = true },
            )
            EntryRow(
                title = "Batas pengeluaran bulanan",
                subtitle = money.monthlyLimit?.let { formatRupiah(it) + " per bulan" } ?: "Belum diatur",
                onClick = { editingLimit = true },
            )
        }

        item {
            SectionHeader("Kata kunci")
            EntryRow(
                title = "Kelola kata kunci",
                subtitle = (if (keywordRules.isEmpty()) "Belum ada kata kamu" else "${keywordRules.size} kata kamu") + ", plus daftar kata bawaan",
                onClick = { showingKeywords = true },
            )
        }

        item {
            SectionHeader("Aplikasi sumber")
            Text(
                "Hanya notifikasi dari aplikasi yang dinyalakan yang dibaca. Aplikasi bank dan e-wallet umum sudah menyala dari awal.",
                style = MaterialTheme.typography.bodyMedium,
                color = Dompet.Muted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }

        if (sources == null) {
            item {
                Text(
                    "Memuat daftar aplikasi…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Dompet.Muted,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        } else {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(MAX_QUERY) },
                    label = { Text("Cari aplikasi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 8.dp),
                )
            }
            val shown = filterSources(sources, query)
            val current = pageOf(shown, page, SOURCES_PER_PAGE)
            if (shown.isEmpty()) {
                item {
                    Text(
                        "Tidak ada aplikasi yang cocok dengan “${query.trim()}”.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Dompet.Muted,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
            }
            items(current.items, key = { it.packageName }) { app ->
                SourceRow(app, onToggle = { onToggleSource(app.packageName, it) })
            }
            if (current.count > 1) {
                item {
                    Pager(current, onGo = { target ->
                        page = target
                        scope.launch { listState.scrollToItem(SOURCES_HEADER_INDEX) }
                    })
                }
            }
        }
    }

    if (editingLimit) {
        LimitSheet(
            current = money.monthlyLimit,
            onDismiss = { editingLimit = false },
            onSave = {
                onSetLimit(it)
                editingLimit = false
            },
        )
    }
}

private const val MAX_QUERY = 40
private const val SOURCES_PER_PAGE = 15

// LazyColumn position of the "Aplikasi sumber" header: title, akses, ekspor, dompet, kata kunci, then this.
private const val SOURCES_HEADER_INDEX = 5

/** One page of a list. [index] is clamped, so a list that shrank never leaves the user on an empty page. */
internal data class Page<T>(val items: List<T>, val index: Int, val count: Int)

internal fun <T> pageOf(all: List<T>, index: Int, size: Int): Page<T> {
    val count = maxOf(1, (all.size + size - 1) / size)
    val safe = index.coerceIn(0, count - 1)
    return Page(all.drop(safe * size).take(size), safe, count)
}

@Composable
private fun Pager(page: Page<*>, onGo: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = { onGo(page.index - 1) }, enabled = page.index > 0) { Text("Sebelumnya") }
        Text(
            "Halaman ${page.index + 1} dari ${page.count}",
            style = MaterialTheme.typography.bodyMedium,
            color = Dompet.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { onGo(page.index + 1) }, enabled = page.index < page.count - 1) { Text("Berikutnya") }
    }
}

@Composable
private fun EntryRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Dompet.Muted,
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Dompet.Muted)
    }
    HorizontalDivider(color = Dompet.Rule, modifier = Modifier.padding(horizontal = 20.dp))
}

/** Matches the app name or package, ignoring case. A blank query shows everything. */
internal fun filterSources(sources: List<SourceApp>, query: String): List<SourceApp> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return sources
    return sources.filter { q in it.label.lowercase() || q in it.packageName.lowercase() }
}

@Composable
private fun SourceRow(app: SourceApp, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(app.packageName, style = FigureSmall, color = Dompet.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = app.enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Dompet.Ink,
                checkedTrackColor = Dompet.Accent,
                checkedBorderColor = Dompet.AccentEdge,
            ),
        )
    }
    HorizontalDivider(color = Dompet.Rule, modifier = Modifier.padding(horizontal = 20.dp))
}

/** Replacing is not undoable: name what goes and what comes, and keep the safe button on the right-hand default spot. */
@Composable
private fun RestoreDialog(plan: RestorePlan, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val incoming = plan.data.transactions.count { it.status != TxStatus.DISCARDED }
    // Apps switched on beyond the defaults are named: a file from elsewhere should not quietly start reading new apps.
    val switchedOn = plan.data.sourcesOn.map { KnownSources.labels[it] ?: it }.sorted()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Dompet.Paper2,
        title = { Text("Ganti data di HP ini?") },
        text = {
            Text(
                "${plan.currentCount} transaksi di HP ini akan diganti dengan $incoming transaksi dari backup ${formatDate(plan.data.createdAt)}. " +
                    "Kata kunci, saldo dompet, dan batas bulanan juga diganti. Ini tidak bisa dibatalkan." +
                    (if (switchedOn.isEmpty()) "" else "\n\nNotifikasi aplikasi ini akan ikut dibaca: ${switchedOn.joinToString(", ")}."),
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Batal", color = Dompet.Ink) } },
        dismissButton = { TextButton(onClick = onConfirm) { Text("Ganti dengan backup", color = Dompet.Danger) } },
    )
}
