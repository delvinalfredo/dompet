package id.dompetbersama.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.dompetbersama.data.Categories
import id.dompetbersama.data.MoneyTransaction
import id.dompetbersama.data.TxStatus
import id.dompetbersama.data.TxType
import id.dompetbersama.ui.theme.Dompet
import id.dompetbersama.ui.theme.Figure
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

private const val MAX_AMOUNT_DIGITS = 13 // up to Rp9,9 triliun
private const val MAX_NOTE = 240

/**
 * Add, edit or review a transaction. [initial] null = new manual entry.
 * Saving always produces a CONFIRMED transaction.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionSheet(
    initial: MoneyTransaction?,
    onDismiss: () -> Unit,
    /** Second value: whether the user flipped the "pindah dana" switch. */
    onSave: (MoneyTransaction, Boolean) -> Unit,
    onDelete: (() -> Unit)?,
    /** Wallets with a balance set; offered for manual entries only, a notification's source is fixed. */
    wallets: List<String>,
    defaultWallet: String,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var type by remember { mutableStateOf(initial?.type ?: TxType.EXPENSE) }
    var amountText by remember { mutableStateOf(initial?.amount?.toString().orEmpty()) }
    var category by remember { mutableStateOf(initial?.category ?: Categories.forType(type).first()) }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }
    var occurredAt by remember { mutableLongStateOf(initial?.occurredAt ?: System.currentTimeMillis()) }
    var pickingDate by remember { mutableStateOf(false) }
    var source by remember { mutableStateOf(initial?.source ?: defaultWallet) }
    val pickWallet = initial == null || initial.dedupKey == null
    var transfer by remember { mutableStateOf(initial?.transferGroup != null) }
    val amount = amountText.toLongOrNull()?.takeIf { it > 0 }

    val title = when {
        initial == null -> "Catat transaksi"
        initial?.status == TxStatus.PENDING -> "Periksa transaksi"
        else -> "Ubah transaksi"
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Dompet.Paper2) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(TxType.EXPENSE to "Keluar", TxType.INCOME to "Masuk").forEachIndexed { index, (option, label) ->
                    SegmentedButton(
                        selected = type == option,
                        onClick = {
                            type = option
                            if (category !in Categories.forType(option)) category = Categories.forType(option).first()
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, 2),
                        colors = SegmentedButtonDefaults.colors(activeContainerColor = Dompet.Accent, activeContentColor = Dompet.Ink),
                    ) { Text(label) }
                }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter(Char::isDigit).take(MAX_AMOUNT_DIGITS) },
                label = { Text("Nominal (Rp)") },
                textStyle = Figure.copy(fontSize = 20.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                supportingText = { Text(amount?.let(::formatRupiah) ?: "Isi nominal tanpa titik") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            if (pickWallet) {
                val options = (wallets + MoneyTransaction.SOURCE_MANUAL + source).distinct()
                if (options.size > 1) {
                    Text("Dompet", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.forEach { option ->
                            FilterChip(
                                selected = source == option,
                                onClick = { source = option },
                                label = { Text(option) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Dompet.Accent, selectedLabelColor = Dompet.Ink),
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            Text("Kategori", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Categories.forType(type).forEach { option ->
                    FilterChip(
                        selected = category == option,
                        onClick = { category = option },
                        label = { Text(option) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Dompet.Accent, selectedLabelColor = Dompet.Ink),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(MAX_NOTE) },
                label = { Text("Catatan") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Pindah antar dompet sendiri", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Contoh: top up GoPay dari Jago. Tidak dihitung sebagai masuk atau keluar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Dompet.Muted,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = transfer,
                    onCheckedChange = { transfer = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Dompet.Ink,
                        checkedTrackColor = Dompet.Accent,
                        checkedBorderColor = Dompet.AccentEdge,
                    ),
                )
            }
            Spacer(Modifier.height(12.dp))

            SecondaryButton("Tanggal: ${formatDate(occurredAt)}", onClick = { pickingDate = true })
            Spacer(Modifier.height(20.dp))

            PrimaryButton(
                text = if (initial?.status == TxStatus.PENDING) "Simpan ke catatan" else "Simpan",
                enabled = amount != null,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val value = amount ?: return@PrimaryButton
                    val base = initial ?: MoneyTransaction(type = type, amount = value, category = category, occurredAt = occurredAt)
                    onSave(
                        base.copy(
                            type = type,
                            amount = value,
                            category = category,
                            note = note.trim(),
                            occurredAt = occurredAt,
                            source = source,
                            status = TxStatus.CONFIRMED,
                            // Keep an existing pair's group so both sides stay linked.
                            transferGroup = if (transfer) initial?.transferGroup ?: UUID.randomUUID().toString() else null,
                        ),
                        transfer != (initial?.transferGroup != null),
                    )
                },
            )
            if (onDelete != null) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text("Hapus transaksi", color = Dompet.Danger)
                }
            }
        }
    }

    if (pickingDate) {
        DateDialog(
            millis = occurredAt,
            onPick = { occurredAt = it; pickingDate = false },
            onDismiss = { pickingDate = false },
        )
    }
}

/** Keeps the original time of day; DatePicker works in UTC dates. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(millis: Long, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val zone = ZoneId.systemDefault()
    val current = Instant.ofEpochMilli(millis).atZone(zone)
    val state = rememberDatePickerState(
        initialSelectedDateMillis = current.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val picked = state.selectedDateMillis
                if (picked == null) {
                    onDismiss()
                } else {
                    val date = Instant.ofEpochMilli(picked).atZone(ZoneOffset.UTC).toLocalDate()
                    onPick(date.atTime(current.toLocalTime()).atZone(zone).toInstant().toEpochMilli())
                }
            }) { Text("Pilih") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    ) { DatePicker(state = state) }
}
