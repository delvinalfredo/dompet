package id.dompetbersama.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.dompetbersama.data.MoneyTransaction
import id.dompetbersama.data.Opening
import id.dompetbersama.ui.theme.Dompet
import id.dompetbersama.ui.theme.Figure

private const val MAX_AMOUNT_DIGITS = 13

/**
 * Wallets the user can set a balance for: switched-on source apps, sources already in the history,
 * wallets set before, and Manual for cash. Configured ones first.
 */
internal fun walletCandidates(
    enabledSourceLabels: List<String>,
    confirmed: List<MoneyTransaction>,
    openings: Map<String, Opening>,
): List<String> {
    val all = (openings.keys + enabledSourceLabels + confirmed.map { it.source } + MoneyTransaction.SOURCE_MANUAL).distinct()
    return all.sortedWith(compareByDescending<String> { it in openings }.thenBy { it.lowercase() })
}

@Composable
fun WalletScreen(
    candidates: List<String>,
    money: MoneySettings,
    /** Current computed balance per configured wallet; what the sheet starts from. */
    balances: Map<String, Long>,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    /** amount null: keep the balance as it is, only the main flag changed. */
    onSave: (name: String, amount: Long?, isMain: Boolean) -> Unit,
    onRemove: (String) -> Unit,
) {
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(onBack = onBack)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            TextButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Dompet.Ink)
                Text("Pengaturan", color = Dompet.Ink, modifier = Modifier.padding(start = 8.dp))
            }
            ScreenTitle(
                "Dompet",
                "Isi saldo yang tertera di aplikasi bank atau e-wallet sekarang. Setelah itu saldonya ikut berubah dari transaksi yang kamu simpan. Kalau mulai beda, isi lagi.",
            )
            SectionHeader("Pilih dompet")
        }
        items(candidates, key = { it }) { name ->
            WalletRow(name, money.openings[name], balances[name], isMain = name == money.main, onClick = { editing = name })
        }
    }

    editing?.let { name ->
        WalletSheet(
            name = name,
            opening = money.openings[name],
            balance = balances[name],
            isMain = name == money.main,
            onDismiss = { editing = null },
            onSave = { amount, isMain ->
                onSave(name, amount, isMain)
                editing = null
            },
            onRemove = {
                onRemove(name)
                editing = null
            },
        )
    }
}

@Composable
private fun WalletRow(name: String, opening: Opening?, balance: Long?, isMain: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 56.dp).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (isMain) "$name · Utama" else name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (opening != null && balance != null) {
                    "Saldo ${formatRupiah(balance)}, diisi ${formatDate(opening.since)}"
                } else {
                    "Saldo belum diisi"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Dompet.Muted,
            )
        }
    }
    HorizontalDivider(color = Dompet.Rule, modifier = Modifier.padding(horizontal = 20.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletSheet(
    name: String,
    opening: Opening?,
    balance: Long?,
    isMain: Boolean,
    onDismiss: () -> Unit,
    onSave: (Long?, Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Starts from today's computed balance, not the number typed last time, so saving unchanged loses nothing.
    val startText = balance?.takeIf { it >= 0 }?.toString().orEmpty()
    var amountText by rememberSaveable { mutableStateOf(startText) }
    var main by rememberSaveable { mutableStateOf(isMain) }
    val amount = amountText.toLongOrNull()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Dompet.Paper2) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        ) {
            Text(name, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter(Char::isDigit).take(MAX_AMOUNT_DIGITS) },
                label = { Text("Saldo sekarang (Rp)") },
                textStyle = Figure.copy(fontSize = 20.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                supportingText = { Text(amount?.let(::formatRupiah) ?: "Lihat di aplikasi $name, isi tanpa titik") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Dompet utama", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Tampil paling atas di Beranda dan jadi pilihan awal saat catat manual.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Dompet.Muted,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = main,
                    onCheckedChange = { main = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Dompet.Ink,
                        checkedTrackColor = Dompet.Accent,
                        checkedBorderColor = Dompet.AccentEdge,
                    ),
                )
            }
            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                "Simpan saldo",
                enabled = amount != null,
                onClick = {
                    val value = amount ?: return@PrimaryButton
                    onSave(if (opening != null && amountText == startText) null else value, main)
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (opening != null) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onRemove, modifier = Modifier.fillMaxWidth()) {
                    Text("Berhenti hitung saldo $name", color = Dompet.Danger)
                }
            }
        }
    }
}

/** One number for the whole month. Empty and saved = no limit. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LimitSheet(current: Long?, onDismiss: () -> Unit, onSave: (Long?) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by rememberSaveable { mutableStateOf(current?.toString().orEmpty()) }
    val amount = amountText.toLongOrNull()?.takeIf { it > 0 }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Dompet.Paper2) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        ) {
            Text("Batas pengeluaran bulanan", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "Beranda menampilkan sisanya untuk bulan ini. Pindah antar dompet sendiri tidak ikut dihitung.",
                style = MaterialTheme.typography.bodyMedium,
                color = Dompet.Muted,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter(Char::isDigit).take(MAX_AMOUNT_DIGITS) },
                label = { Text("Batas per bulan (Rp)") },
                textStyle = Figure.copy(fontSize = 20.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                supportingText = { Text(amount?.let(::formatRupiah) ?: "Contoh: 3000000") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
            PrimaryButton("Simpan batas", enabled = amount != null, onClick = { onSave(amount) }, modifier = Modifier.fillMaxWidth())
            if (current != null) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { onSave(null) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Hapus batas", color = Dompet.Danger)
                }
            }
        }
    }
}
