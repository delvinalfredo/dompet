package id.dompetbersama.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.dompetbersama.data.MoneyTransaction
import id.dompetbersama.data.TxType
import id.dompetbersama.data.isTransfer
import id.dompetbersama.ui.theme.Dompet
import id.dompetbersama.ui.theme.Figure
import id.dompetbersama.ui.theme.FigureSmall

@Composable
fun PendingScreen(
    items: List<MoneyTransaction>,
    hasAccess: Boolean,
    contentPadding: PaddingValues,
    onConfirm: (MoneyTransaction) -> Unit,
    onConfirmTransfer: (String) -> Unit,
    onSplitTransfer: (String) -> Unit,
    onEdit: (MoneyTransaction) -> Unit,
    onDiscard: (MoneyTransaction) -> Unit,
    onOpenAccess: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item { ScreenTitle("Konfirmasi", "Transaksi yang terbaca dari notifikasi. Periksa dulu sebelum masuk catatan.") }

        if (!hasAccess) {
            item {
                NoticeCard(
                    title = "Akses notifikasi belum aktif",
                    body = "Tanpa izin ini, transaksi bank dan e-wallet tidak bisa tercatat otomatis.",
                    actionText = "Aktifkan akses notifikasi",
                    onAction = onOpenAccess,
                )
            }
        }

        if (items.isEmpty()) {
            item {
                EmptyState(
                    title = "Tidak ada yang perlu dikonfirmasi",
                    body = "Transaksi baru dari aplikasi sumber yang kamu nyalakan di Pengaturan akan muncul di sini.",
                )
            }
        }

        items(pendingEntries(items), key = { it.key }) { entry ->
            when (entry) {
                is PendingEntry.Single -> {
                    val tx = entry.tx
                    PendingCard(tx, onConfirm = { onConfirm(tx) }, onEdit = { onEdit(tx) }, onDiscard = { onDiscard(tx) })
                }
                is PendingEntry.Transfer -> TransferCard(
                    entry,
                    onConfirm = { onConfirmTransfer(entry.group) },
                    onSplit = { onSplitTransfer(entry.group) },
                )
            }
        }
    }
}

/** The Furrow notebook card: ink head with source and time, then the entry. */
@Composable
private fun PendingCard(tx: MoneyTransaction, onConfirm: () -> Unit, onEdit: () -> Unit, onDiscard: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        shape = EntryShape,
        color = Dompet.Paper2,
        border = BorderStroke(1.dp, Dompet.Ink),
        shadowElevation = 2.dp,
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().background(Dompet.Ink).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(tx.source, style = MaterialTheme.typography.labelLarge, color = Dompet.Paper2, modifier = Modifier.weight(1f))
                Text("${formatDate(tx.occurredAt)} ${formatTime(tx.occurredAt)}", style = FigureSmall, color = Dompet.Paper2)
            }
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            when {
                                tx.isTransfer -> "Pindah dana"
                                tx.type == TxType.INCOME -> "Masuk"
                                else -> "Keluar"
                            },
                            style = MaterialTheme.typography.labelMedium, color = Dompet.Muted)
                        Text(tx.category, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        formatSigned(tx.type, tx.amount),
                        style = Figure,
                        color = if (tx.type == TxType.INCOME) Dompet.Green else Dompet.Ink,
                    )
                }
                if (tx.note.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(tx.note, style = MaterialTheme.typography.bodySmall, color = Dompet.Muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    PrimaryButton("Simpan", onConfirm)
                    SecondaryButton("Ubah", onEdit)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDiscard) { Text("Buang", color = Dompet.Danger) }
                }
            }
        }
    }
}

internal sealed interface PendingEntry {
    val key: String

    data class Single(val tx: MoneyTransaction) : PendingEntry {
        override val key get() = "tx-${tx.id}"
    }

    /** Both sides of one move between own wallets, still waiting for confirmation. */
    data class Transfer(val group: String, val from: MoneyTransaction, val to: MoneyTransaction) : PendingEntry {
        override val key get() = "move-$group"
    }
}

/**
 * Pending rows as cards, newest first. A complete pair (one out, one in) becomes one card at the position of its
 * newest side. A lone side, whose partner was already saved or thrown away, stays an ordinary card.
 */
internal fun pendingEntries(items: List<MoneyTransaction>): List<PendingEntry> {
    val pairs = items.filter { it.transferGroup != null }.groupBy { it.transferGroup!! }
        .mapNotNull { (group, side) ->
            val from = side.singleOrNull { it.type == TxType.EXPENSE }
            val to = side.singleOrNull { it.type == TxType.INCOME }
            if (side.size == 2 && from != null && to != null) group to PendingEntry.Transfer(group, from, to) else null
        }
        .toMap()
    val shown = mutableSetOf<String>()
    return items.mapNotNull { tx ->
        val pair = tx.transferGroup?.let(pairs::get)
        when {
            pair == null -> PendingEntry.Single(tx)
            shown.add(pair.group) -> pair
            else -> null
        }
    }
}

@Composable
private fun TransferCard(entry: PendingEntry.Transfer, onConfirm: () -> Unit, onSplit: () -> Unit) {
    val at = maxOf(entry.from.occurredAt, entry.to.occurredAt)
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        shape = EntryShape,
        color = Dompet.Paper2,
        border = BorderStroke(1.dp, Dompet.Ink),
        shadowElevation = 2.dp,
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().background(Dompet.Ink).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Pindah dana", style = MaterialTheme.typography.labelLarge, color = Dompet.Paper2, modifier = Modifier.weight(1f))
                Text("${formatDate(at)} ${formatTime(at)}", style = FigureSmall, color = Dompet.Paper2)
            }
            Column(Modifier.padding(16.dp)) {
                TransferSide("Dari", entry.from)
                Spacer(Modifier.height(6.dp))
                TransferSide("Ke", entry.to)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Nominal sama keluar dan masuk di dua aplikasi. Kalau disetujui, tidak dihitung sebagai pemasukan atau pengeluaran.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Dompet.Muted,
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    PrimaryButton("Setujui", onConfirm)
                    SecondaryButton("Bukan, pisahkan", onSplit)
                }
            }
        }
    }
}

@Composable
private fun TransferSide(label: String, tx: MoneyTransaction) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Dompet.Muted, modifier = Modifier.width(40.dp))
        Text(tx.source, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(formatSigned(tx.type, tx.amount), style = Figure)
    }
}
