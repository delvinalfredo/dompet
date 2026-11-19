package id.dompetbersama.data

import id.dompetbersama.notification.NotificationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Synthetic notification texts only.
class KeywordRulesTest {
    private val bank = "id.example.bank"

    @Test
    fun userWordMakesUnknownWordingReadable() {
        val text = "Mutasi kredit rekening Rp500.000 dari BUDI"
        val rule = KeywordRule(bank, "mutasi kredit", TxType.INCOME)
        assertEquals(TxType.INCOME, NotificationParser.parse("Victoria", text, listOf(rule))!!.type)
    }

    @Test
    fun userWordWinsOverBuiltInWord() {
        // Built-in "pembayaran" says expense; the user taught that this bank's "pembayaran diterima" is income.
        val text = "Pembayaran diterima Rp75.000 dari pelanggan"
        assertEquals(TxType.EXPENSE, NotificationParser.parse("", text)!!.type)
        val rule = KeywordRule(null, "pembayaran diterima", TxType.INCOME)
        assertEquals(TxType.INCOME, NotificationParser.parse("", text, listOf(rule))!!.type)
    }

    @Test
    fun appRuleBeatsAllAppsRuleAndLongerWordWins() {
        val text = "Transfer ke BUDI Rp50.000"
        val allApps = KeywordRule(null, "transfer", TxType.INCOME)
        val forApp = KeywordRule(bank, "transfer", TxType.EXPENSE)
        assertEquals(TxType.EXPENSE, NotificationParser.parse("", text, listOf(allApps, forApp))!!.type)
        assertEquals(TxType.EXPENSE, NotificationParser.parse("", text, listOf(forApp, allApps))!!.type)

        val longer = KeywordRule(null, "transfer ke", TxType.EXPENSE)
        assertEquals(TxType.EXPENSE, NotificationParser.parse("", text, listOf(allApps, longer))!!.type)
    }

    @Test
    fun userWordMatchesWholeWordsOnly() {
        val rule = KeywordRule(null, "dana", TxType.INCOME)
        assertEquals(TxType.EXPENSE, NotificationParser.parse("", "Pembayaran ke Danamon Rp20.000", listOf(rule))!!.type)
        val card = KeywordRule(null, "kredit", TxType.INCOME)
        assertEquals(TxType.EXPENSE, NotificationParser.parse("", "Transaksi kartu kredit Rp20.000", listOf(card))!!.type)
    }

    @Test
    fun multiWordRuleMatchesAcrossLineBreak() {
        val rule = KeywordRule(null, "dana diterima", TxType.INCOME)
        assertEquals(TxType.INCOME, NotificationParser.parse("", "Pembayaran Dana\nditerima Rp100.000", listOf(rule))!!.type)
    }

    @Test
    fun taughtWordLetsPromoSuffixThroughButNeverOtp() {
        val rule = KeywordRule(null, "dana masuk", TxType.INCOME)
        assertNull(NotificationParser.parse("", "Dana masuk Rp100.000. Promo cashback menanti!"))
        assertEquals(TxType.INCOME, NotificationParser.parse("", "Dana masuk Rp100.000. Promo cashback menanti!", listOf(rule))!!.type)
        assertNull(NotificationParser.parse("", "Dana masuk Rp100.000, kode OTP 123456", listOf(rule)))
    }

    @Test
    fun ruleForOneAppDoesNotApplyToAnother() {
        val rule = KeywordRule(bank, "mutasi", TxType.INCOME)
        assertEquals(false, rule.appliesTo("id.other.bank"))
        assertEquals(true, KeywordRule(null, "mutasi", TxType.INCOME).appliesTo("id.other.bank"))
    }

    @Test
    fun wordsAreNormalisedAndTooShortOnesRejected() {
        assertEquals("dana diterima", KeywordRule.normalize("  Dana   DITERIMA "))
        assertNull(KeywordRule.normalize("ab"))
    }

    @Test
    fun codecRoundTripsAndSkipsCorruptEntries() {
        val rules = listOf(KeywordRule(bank, "mutasi kredit", TxType.INCOME), KeywordRule(null, "tarik", TxType.EXPENSE))
        rules.forEach { assertEquals(it, KeywordCodec.decode(KeywordCodec.encode(it))) }
        assertNull(KeywordCodec.decode("garbage"))
        assertNull(KeywordCodec.decode("SIDEWAYS\u001F*\u001Fmutasi"))
    }
}
