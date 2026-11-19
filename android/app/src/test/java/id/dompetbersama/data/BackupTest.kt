package id.dompetbersama.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupTest {
    private val sample = BackupData(
        createdAt = 1_763_600_000_000,
        transactions = listOf(
            MoneyTransaction(type = TxType.EXPENSE, amount = 100_000, category = "Transfer", note = "Top up ****1234", source = "Jago",
                occurredAt = 1, status = TxStatus.CONFIRMED, dedupKey = "k1", transferGroup = "g1"),
            MoneyTransaction(type = TxType.INCOME, amount = 100_000, category = "Lainnya", source = "GoPay",
                occurredAt = 2, status = TxStatus.PENDING, dedupKey = "k2", transferGroup = "g1"),
            MoneyTransaction(type = TxType.EXPENSE, amount = 25_000, category = "Makan", note = "baris \"kutip\"\nbaru", occurredAt = 3),
            MoneyTransaction(type = TxType.EXPENSE, amount = 5_000, category = "Lainnya", source = "OVO", occurredAt = 4,
                status = TxStatus.DISCARDED, dedupKey = "k3"),
        ),
        keywords = listOf(KeywordRule("com.jago.digitalBanking", "dana diterima", TxType.INCOME), KeywordRule(null, "qris", TxType.EXPENSE)),
        openings = mapOf("Jago" to Opening(4_000_000, 10), "GoPay" to Opening(50_000, 11)),
        mainWallet = "Jago",
        monthlyLimit = 3_000_000,
        sourcesOn = setOf("com.gojek.app"),
        sourcesOff = setOf("ovo.id"),
    )

    @Test
    fun everythingSurvivesTheRoundTrip() {
        assertEquals(sample, BackupCodec.decode(BackupCodec.encode(sample)))
    }

    @Test
    fun emptyPhoneBacksUpToo() {
        val empty = BackupData(1, emptyList(), emptyList(), emptyMap(), null, null, emptySet(), emptySet())
        assertEquals(empty, BackupCodec.decode(BackupCodec.encode(empty)))
    }

    @Test
    fun rejectsOtherFilesWithAReadableMessage() {
        assertMessage("Ini bukan file backup Dompet Bersama.", """{"hello":"world"}""")
        assertMessage("File backup rusak atau tidak lengkap.", "tanggal,tipe,nominal")
        val newer = BackupCodec.encode(sample).replace("\"version\":1", "\"version\":99")
        assertTrue(message(newer).contains("lebih baru"))
    }

    @Test
    fun oneBadRowRejectsTheWholeFile() {
        val bad = BackupCodec.encode(sample).replace("\"amount\":25000", "\"amount\":-25000")
        assertMessage("File backup berisi data yang tidak valid.", bad)
        val badType = BackupCodec.encode(sample).replace("\"status\":\"PENDING\"", "\"status\":\"HACKED\"")
        assertMessage("File backup berisi data yang tidak valid.", badType)
    }

    @Test
    fun mainWalletWithoutBalanceIsDropped() {
        val text = BackupCodec.encode(sample.copy(mainWallet = "DANA"))
        assertEquals(null, BackupCodec.decode(text).mainWallet)
    }

    @Test
    fun rejectsFilesThatWouldOnlyFailAfterConfirmOrSwitchOnJunk() {
        val dupes = sample.copy(transactions = sample.transactions.map { it.copy(dedupKey = it.dedupKey?.let { "same" }) })
        assertMessage("File backup berisi data yang tidak valid.", BackupCodec.encode(dupes))
        val junkSource = sample.copy(sourcesOn = setOf("not a package"))
        assertMessage("File backup berisi data yang tidak valid.", BackupCodec.encode(junkSource))
        val tooManyRules = sample.copy(keywords = (1..501).map { KeywordRule(null, "kata$it", TxType.INCOME) })
        assertMessage("File backup terlalu besar.", BackupCodec.encode(tooManyRules))
    }

    private fun message(text: String): String = assertThrows(BackupException::class.java) { BackupCodec.decode(text) }.message!!

    private fun assertMessage(expected: String, text: String) = assertEquals(expected, message(text))
}
