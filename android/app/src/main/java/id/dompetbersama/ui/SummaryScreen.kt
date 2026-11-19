package id.dompetbersama.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.dompetbersama.data.MoneyTransaction
import id.dompetbersama.data.Totals
import id.dompetbersama.data.activeMonths
import id.dompetbersama.data.expenseByCategory
import id.dompetbersama.data.inMonth
import id.dompetbersama.data.largestExpenses
import id.dompetbersama.data.totals
import id.dompetbersama.data.totalsBySource
import id.dompetbersama.ui.theme.Dompet
import id.dompetbersama.ui.theme.Figure
import id.dompetbersama.ui.theme.FigureSmall
import java.time.YearMonth

/** Answers "where did my money go this month, and is it more than last month?". */
@Composable
fun SummaryScreen(items: List<MoneyTransaction>, contentPadding: PaddingValues, onOpen: (MoneyTransaction) -> Unit) {
    val thisMonth = remember { YearMonth.now() }
    var monthText by rememberSaveable { mutableStateOf(thisMonth.toString()) }
    val month = YearMonth.parse(monthText)
    val oldest = remember(items) { items.activeMonths().lastOrNull() ?: thisMonth }

    val monthItems = remember(items, month) { items.inMonth(month) }
    val totals = remember(monthItems) { monthItems.totals() }
    val previous = remember(items, month) { items.inMonth(month.minusMonths(1)).takeIf { it.isNotEmpty() }?.totals() }
    val categories = remember(monthItems) { monthItems.expenseByCategory() }
    val sources = remember(monthItems) { monthItems.totalsBySource() }
    val largest = remember(monthItems) { monthItems.largestExpenses() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item { ScreenTitle("Ringkasan") }
        item {
            MonthSwitcher(
                month = month,
                canGoBack = month > oldest,
                canGoForward = month < thisMonth,
                onChange = { monthText = it.toString() },
            )
        }
        item { MonthTotals(totals, previous, month.minusMonths(1)) }

        item { SectionHeader("Pengeluaran per kategori") }
        if (categories.isEmpty()) {
            item { EmptyState("Belum ada pengeluaran di bulan ini", "Pengeluaran yang sudah dikonfirmasi akan dikelompokkan per kategori di sini.") }
        }
        items(categories, key = { "cat-${it.first}" }) { (category, amount) ->
            ShareRow(category, amount, share = if (totals.expense > 0) amount.toFloat() / totals.expense else 0f)
        }

        if (sources.isNotEmpty()) {
            item { SectionHeader("Per sumber") }
            items(sources, key = { "src-${it.first}" }) { (source, sourceTotals) -> SourceTotalsRow(source, sourceTotals) }
        }

        if (largest.isNotEmpty()) {
            item { SectionHeader("Pengeluaran terbesar") }
            items(largest, key = { "big-${it.id}" }) { tx -> TransactionRow(tx, onClick = { onOpen(tx) }) }
        }
    }
}

@Composable
private fun MonthSwitcher(month: YearMonth, canGoBack: Boolean, canGoForward: Boolean, onChange: (YearMonth) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { onChange(month.minusMonths(1)) }, enabled = canGoBack) { Text("‹ Sebelumnya") }
        Text(formatMonth(month), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1)
        TextButton(onClick = { onChange(month.plusMonths(1)) }, enabled = canGoForward) { Text("Berikutnya ›") }
    }
}

@Composable
private fun MonthTotals(totals: Totals, previous: Totals?, previousMonth: YearMonth) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        HorizontalDivider(thickness = 2.dp, color = Dompet.Ink)
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TotalCell("Masuk", "+" + formatRupiah(totals.income), Dompet.Green, Modifier.weight(1f))
            TotalCell("Keluar", "−" + formatRupiah(totals.expense), Dompet.Ink, Modifier.weight(1f))
        }
        HorizontalDivider(color = Dompet.Rule)
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Selisih", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text(formatRupiah(totals.net), style = Figure, color = if (totals.net < 0) Dompet.Danger else Dompet.Ink)
        }
        if (totals.transferred > 0) {
            Text(
                "Pindah antar dompet sendiri ${formatRupiah(totals.transferred)}, tidak dihitung.",
                style = MaterialTheme.typography.bodyMedium,
                color = Dompet.Muted,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        // Only a real comparison: shown when the previous month has confirmed transactions.
        if (previous != null) {
            Text(
                expenseComparison(totals.expense, previous.expense, formatMonth(previousMonth)),
                style = MaterialTheme.typography.bodyMedium,
                color = Dompet.Muted,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

internal fun expenseComparison(current: Long, previous: Long, previousLabel: String): String {
    val diff = current - previous
    return when {
        diff > 0 -> "Pengeluaran ${formatRupiah(diff)} lebih banyak dari $previousLabel."
        diff < 0 -> "Pengeluaran ${formatRupiah(-diff)} lebih sedikit dari $previousLabel."
        else -> "Pengeluaran sama dengan $previousLabel."
    }
}

@Composable
private fun TotalCell(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Dompet.Muted)
        Text(value, style = Figure, color = color, maxLines = 1)
    }
}

@Composable
private fun ShareRow(label: String, amount: Long, share: Float) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${(share * 100).toInt()}%", style = FigureSmall, color = Dompet.Muted)
            Spacer(Modifier.width(12.dp))
            Text(formatRupiah(amount), style = Figure)
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(2.dp)).background(Dompet.Paper3)) {
            Box(Modifier.fillMaxWidth(share.coerceIn(0f, 1f)).fillMaxHeight().background(Dompet.Ink2))
        }
    }
    HorizontalDivider(color = Dompet.Rule, modifier = Modifier.padding(horizontal = 20.dp))
}

@Composable
private fun SourceTotalsRow(source: String, totals: Totals) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(source, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            if (totals.income > 0) Text("+" + formatRupiah(totals.income), style = Figure, color = Dompet.Green)
            if (totals.expense > 0) Text("−" + formatRupiah(totals.expense), style = Figure)
        }
    }
    HorizontalDivider(color = Dompet.Rule, modifier = Modifier.padding(horizontal = 20.dp))
}
