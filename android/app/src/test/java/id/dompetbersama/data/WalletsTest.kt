package id.dompetbersama.data

import id.dompetbersama.ui.walletCandidates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WalletsTest {
    private fun tx(type: TxType, amount: Long, source: String, at: Long, group: String? = null) =
        MoneyTransaction(type = type, amount = amount, category = "Lainnya", source = source, occurredAt = at, transferGroup = group)

    @Test
    fun balanceMovesOnlyFromTheMomentItWasSet() {
        val openings = mapOf("Jago" to Opening(1_000_000, since = 100), "GoPay" to Opening(0, since = 100))
        val history = listOf(
            tx(TxType.EXPENSE, 999_999, "Jago", at = 50), // before the balance was read: already inside it
            tx(TxType.INCOME, 200_000, "Jago", at = 150),
            tx(TxType.EXPENSE, 100_000, "Jago", at = 160, group = "g"), // top up GoPay: real money leaves Jago
            tx(TxType.INCOME, 100_000, "GoPay", at = 161, group = "g"),
            tx(TxType.EXPENSE, 30_000, "GoPay", at = 170),
        )
        val balances = walletBalances(openings, main = "GoPay", confirmed = history)
        assertEquals(listOf("GoPay", "Jago"), balances.map { it.name }) // main first
        assertTrue(balances.first().isMain)
        assertEquals(70_000L, balances.first { it.name == "GoPay" }.balance)
        assertEquals(1_100_000L, balances.first { it.name == "Jago" }.balance)
    }

    @Test
    fun limitCountsRemainingAndDaysIncludingToday() {
        val status = limitStatus(limit = 3_000_000, monthExpense = 1_800_000, today = LocalDate.of(2026, 11, 20))
        assertEquals(1_200_000L, status.remaining)
        assertEquals(11, status.daysLeft)
        assertFalse(status.over)

        val over = limitStatus(limit = 1_000_000, monthExpense = 1_200_000, today = LocalDate.of(2026, 2, 28))
        assertTrue(over.over)
        assertEquals(1f, over.usedShare)
        assertEquals(1, over.daysLeft)
    }

    @Test
    fun openingSurvivesStorageAndRejectsJunk() {
        val raw = OpeningCodec.encode("Bank Jago", Opening(4_150_000, 1_700_000_000_000))
        assertEquals("Bank Jago" to Opening(4_150_000, 1_700_000_000_000), OpeningCodec.decode(raw))
        assertNull(OpeningCodec.decode("only-a-name"))
        assertNull(OpeningCodec.decode("Jago\u001Fabc\u001F1"))
    }

    @Test
    fun candidatesPutConfiguredWalletsFirstAndIncludeManual() {
        val list = walletCandidates(
            enabledSourceLabels = listOf("OVO", "Jago"),
            confirmed = listOf(tx(TxType.EXPENSE, 1, "GoPay", at = 1)),
            openings = mapOf("Jago" to Opening(0, 0)),
        )
        assertEquals(listOf("Jago", "GoPay", "Manual", "OVO"), list)
    }
}
