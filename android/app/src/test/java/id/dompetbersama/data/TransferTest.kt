package id.dompetbersama.data

import id.dompetbersama.ui.PendingEntry
import id.dompetbersama.ui.pendingEntries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.time.ZoneOffset

class TransferTest {
    private fun tx(id: Long, type: TxType, amount: Long, source: String, group: String? = null, at: Long = id) =
        MoneyTransaction(id = id, type = type, amount = amount, category = "Lainnya", source = source, occurredAt = at, transferGroup = group)

    @Test
    fun moveBetweenOwnWalletsIsNotIncomeOrExpense() {
        val items = listOf(
            tx(1, TxType.INCOME, 5_000_000, "Jago"),
            tx(2, TxType.EXPENSE, 100_000, "Jago", group = "g1"),
            tx(3, TxType.INCOME, 100_000, "GoPay", group = "g1"),
            tx(4, TxType.EXPENSE, 50_000, "GoPay"),
        )
        val totals = items.totals()
        assertEquals(5_000_000, totals.income)
        assertEquals(50_000, totals.expense)
        assertEquals("one move counted once", 100_000, totals.transferred)
        // Sides split across midnight at month end: only the month with the outgoing side shows the move.
        assertEquals(0, listOf(tx(3, TxType.INCOME, 100_000, "GoPay", group = "g1")).totals().transferred)
        assertEquals(listOf(50_000L), items.largestExpenses().map { it.amount })
        assertEquals(50_000L, items.expenseByCategory().single().second)
        assertEquals(50_000L, items.totalsBySource().first { it.first == "GoPay" }.second.expense)
    }

    @Test
    fun completePairBecomesOneCardLoneSideStaysOrdinary() {
        val entries = pendingEntries(
            listOf(
                tx(4, TxType.INCOME, 100_000, "GoPay", group = "g1"),
                tx(3, TxType.EXPENSE, 20_000, "Jago"),
                tx(2, TxType.EXPENSE, 100_000, "Jago", group = "g1"),
                tx(1, TxType.INCOME, 70_000, "OVO", group = "lonely"),
            ),
        )
        assertEquals(listOf("move-g1", "tx-3", "tx-1"), entries.map { it.key })
        val move = entries.first() as PendingEntry.Transfer
        assertEquals("Jago", move.from.source)
        assertEquals("GoPay", move.to.source)
        assertTrue(entries.last() is PendingEntry.Single)
    }

    @Test
    fun csvMarksMoves() {
        val out = ByteArrayOutputStream()
        CsvExporter.write(out, listOf(tx(1, TxType.EXPENSE, 100_000, "Jago", group = "g1")), ZoneOffset.UTC)
        assertTrue(out.toString(Charsets.UTF_8).lines()[1].contains(",pindah keluar,100000,"))
    }
}
