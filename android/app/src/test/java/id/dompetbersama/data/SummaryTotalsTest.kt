package id.dompetbersama.data

import id.dompetbersama.ui.SourceApp
import id.dompetbersama.ui.expenseComparison
import id.dompetbersama.ui.filterSources
import id.dompetbersama.ui.pageOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth
import java.time.ZoneOffset

class SummaryTotalsTest {
    private fun tx(id: Long, type: TxType, amount: Long, source: String, at: Long = 0L) =
        MoneyTransaction(id = id, type = type, amount = amount, category = "Lainnya", source = source, occurredAt = at)

    @Test
    fun totalsPerSourceBusiestFirst() {
        val items = listOf(
            tx(1, TxType.EXPENSE, 10_000, "myBCA"),
            tx(2, TxType.INCOME, 500_000, "Jago"),
            tx(3, TxType.EXPENSE, 20_000, "myBCA"),
        )
        val bySource = items.totalsBySource()
        assertEquals(listOf("Jago", "myBCA"), bySource.map { it.first })
        assertEquals(Totals(income = 0, expense = 30_000), bySource[1].second)
    }

    @Test
    fun largestExpensesSkipIncome() {
        val items = listOf(
            tx(1, TxType.EXPENSE, 10_000, "a"),
            tx(2, TxType.INCOME, 900_000, "a"),
            tx(3, TxType.EXPENSE, 50_000, "a"),
        )
        assertEquals(listOf(3L, 1L), items.largestExpenses(limit = 5).map { it.id })
    }

    @Test
    fun activeMonthsNewestFirst() {
        val oct = YearMonth.of(2026, 10).atDay(5).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val nov = YearMonth.of(2026, 11).atDay(5).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val items = listOf(tx(1, TxType.EXPENSE, 1, "a", oct), tx(2, TxType.EXPENSE, 1, "a", nov), tx(3, TxType.EXPENSE, 1, "a", oct))
        assertEquals(listOf(YearMonth.of(2026, 11), YearMonth.of(2026, 10)), items.activeMonths(ZoneOffset.UTC))
    }

    @Test
    fun comparisonSaysMoreOrLess() {
        assertTrue(expenseComparison(150_000, 100_000, "Oktober 2026").contains("lebih banyak"))
        assertTrue(expenseComparison(50_000, 100_000, "Oktober 2026").contains("lebih sedikit"))
    }

    @Test
    fun sourceSearchMatchesNameOrPackage() {
        val apps = listOf(SourceApp("com.jago.digitalBanking", "Jago", true), SourceApp("com.bca", "BCA mobile", true))
        assertEquals(listOf("Jago"), filterSources(apps, " jaGo ").map { it.label })
        assertEquals(listOf("BCA mobile"), filterSources(apps, "com.bca").map { it.label })
        assertEquals(2, filterSources(apps, "").size)
    }

    @Test
    fun sourcePagesSplitAndClamp() {
        val apps = (1..32).toList()
        assertEquals((16..30).toList(), pageOf(apps, 1, 15).items)
        assertEquals(3, pageOf(apps, 0, 15).count)
        // The list shrank (a search) while the user was on page 3: land on the last real page, not an empty one.
        val shrunk = pageOf((1..5).toList(), 2, 15)
        assertEquals(0, shrunk.index)
        assertEquals(5, shrunk.items.size)
        assertEquals(1, pageOf(emptyList<Int>(), 0, 15).count)
    }
}
