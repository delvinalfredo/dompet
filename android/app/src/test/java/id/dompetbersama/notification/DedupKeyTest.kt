package id.dompetbersama.notification

import id.dompetbersama.data.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DedupKeyTest {
    private val pkg = "id.example.bank"

    @Test
    fun repostOfSameNotificationGivesSameKey() {
        val first = DedupKey.of(pkg, "0|$pkg|7|null|10001", 1_000L, TxType.EXPENSE, 10_000)
        val again = DedupKey.of(pkg, "0|$pkg|7|null|10001", 1_000L, TxType.EXPENSE, 10_000)
        assertEquals(first, again)
    }

    @Test
    fun twoPaymentsOfSameAmountInSameMinuteStaySeparate() {
        val a = DedupKey.of(pkg, "0|$pkg|7|null|10001", 1_000L, TxType.EXPENSE, 10_000)
        val b = DedupKey.of(pkg, "0|$pkg|7|null|10001", 31_000L, TxType.EXPENSE, 10_000)
        val c = DedupKey.of(pkg, "0|$pkg|8|null|10001", 1_000L, TxType.EXPENSE, 10_000)
        assertNotEquals(a, b)
        assertNotEquals(a, c)
    }
}
