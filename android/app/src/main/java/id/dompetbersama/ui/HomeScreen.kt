package id.dompetbersama.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.dompetbersama.data.LimitStatus
import id.dompetbersama.data.MoneyTransaction
import id.dompetbersama.data.WalletBalance
import id.dompetbersama.data.TxType
import id.dompetbersama.data.isTransfer
import id.dompetbersama.data.inMonth
import id.dompetbersama.data.totals
import id.dompetbersama.ui.theme.Dompet
import id.dompetbersama.ui.theme.Figure
import id.dompetbersama.ui.theme.FigureSmall
import java.time.YearMonth

@Composable
fun HomeScreen(
    items: List<MoneyTransaction>,
    hasAccess: Boolean,
    contentPadding: PaddingValues,
    onOpen: (MoneyTransaction) -> Unit,
    onOpenAccess: () -> Unit,
    /** Empty when the user has not set any wallet balance yet. */
    wallets: List<WalletBalance>,
    limit: LimitStatus?,
) {
    val all = remember(items) { items.totals() }
    val month = remember(items) { items.inMonth(YearMonth.now()).totals() }
    val byDay = remember(items) { items.groupBy { toLocalDate(it.occurredAt) } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(bottom = 96.dp), // room for the Catat button
    ) {
        item { BalanceHeader(balance = all.net, wallets = wallets, monthIncome = month.income, monthExpense = month.expense) }
        if (limit != null) item { LimitBar(limit) }

        // Shown on the first screen so a new user learns why nothing is recorded automatically yet.
        if (!hasAccess) {
            item {
                NoticeCard(
                    title = "Transaksi belum tercatat otomatis",
                    body = "Izinkan app membaca notifikasi bank dan e-wallet. Setiap transaksi masuk ke Konfirmasi dulu sebelum dicatat.",
                    actionText = "Aktifkan akses notifikasi",
                    onAction = onOpenAccess,
                )
            }
        }

        if (items.isEmpty()) {
            item {
                EmptyState(
                    title = "Belum ada transaksi",
                    body = if (hasAccess) {
                        "Transaksi dari notifikasi akan muncul di Konfirmasi. Untuk catatan tunai, pakai tombol Catat."
                    } else {
                        "Catat manual lewat tombol Catat, atau aktifkan akses notifikasi di atas."
                    },
                )
            }
        }

        byDay.forEach { (date, dayItems) ->
            item(key = "day-$date") { SectionHeader(formatDay(date)) }
            items(dayItems, key = { it.id }) { tx -> TransactionRow(tx, onClick = { onOpen(tx) }) }
        }
    }
}

@Composable
private fun BalanceHeader(balance: Long, wallets: List<WalletBalance>, monthIncome: Long, monthExpense: Long) {
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 8.dp)) {
        // With wallet balances set, the real money across wallets is the headline, not income minus expense.
        val headline = if (wallets.isEmpty()) balance else wallets.sumOf { it.balance }
        Text(if (wallets.isEmpty()) "Saldo tercatat" else "Saldo dompet", style = MaterialTheme.typography.labelLarge, color = Dompet.Muted)
        Spacer(Modifier.height(8.dp))
        Text(formatRupiah(headline), style = MaterialTheme.typography.displayLarge, maxLines = 1)
        if (wallets.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            wallets.forEach { wallet ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (wallet.isMain) "${wallet.name} · Utama" else wallet.name,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(formatRupiah(wallet.balance), style = Figure, color = if (wallet.balance < 0) Dompet.Danger else Dompet.Ink)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        HorizontalDivider(thickness = 2.dp, color = Dompet.Ink)
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MonthFigure("Masuk bulan ini", "+" + formatRupiah(monthIncome), Dompet.Green, Modifier.weight(1f))
            MonthFigure("Keluar bulan ini", "−" + formatRupiah(monthExpense), Dompet.Ink, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MonthFigure(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Dompet.Muted)
        Spacer(Modifier.height(2.dp))
        Text(value, style = Figure, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun TransactionRow(tx: MoneyTransaction, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(tx.category, style = MaterialTheme.typography.titleMedium)
            Text(
                if (tx.isTransfer) "Pindah dana, ${tx.source}" else tx.note.ifBlank { tx.source },
                style = MaterialTheme.typography.bodyMedium,
                color = Dompet.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatSigned(tx.type, tx.amount),
                style = Figure,
                // A move between own wallets is neither earned nor spent.
                color = when {
                    tx.isTransfer -> Dompet.Muted
                    tx.type == TxType.INCOME -> Dompet.Green
                    else -> Dompet.Ink
                },
            )
            Text(formatTime(tx.occurredAt), style = FigureSmall, color = Dompet.Muted)
        }
    }
    HorizontalDivider(color = Dompet.Rule, modifier = Modifier.padding(horizontal = 20.dp))
}

@Composable
private fun LimitBar(status: LimitStatus) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Batas bulan ini", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text(
                if (status.over) "Lewat ${formatRupiah(-status.remaining)}" else "Sisa ${formatRupiah(status.remaining)}",
                style = Figure,
                color = if (status.over) Dompet.Danger else Dompet.Ink,
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { status.usedShare },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = if (status.over) Dompet.Danger else Dompet.Ink,
            trackColor = Dompet.Rule,
            drawStopIndicator = {},
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Terpakai ${formatRupiah(status.spent)} dari ${formatRupiah(status.limit)}, ${status.daysLeft} hari lagi",
            style = MaterialTheme.typography.bodySmall,
            color = Dompet.Muted,
        )
    }
}
