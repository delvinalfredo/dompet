package id.dompetbersama.notification

import id.dompetbersama.data.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Synthetic notification texts only; no real account data.
class NotificationParserTest {

    @Test
    fun incomingTransferIsIncome() {
        val result = NotificationParser.parse("Dana masuk", "Kamu menerima Rp 1.500.000,00 dari BUDI")!!
        assertEquals(TxType.INCOME, result.type)
        assertEquals(1_500_000L, result.amount)
        assertEquals("Transfer masuk", result.category)
    }

    @Test
    fun paymentIsExpenseWithCategory() {
        val result = NotificationParser.parse("Pembayaran berhasil", "Pembayaran GoFood Rp45.000 berhasil")!!
        assertEquals(TxType.EXPENSE, result.type)
        assertEquals(45_000L, result.amount)
        assertEquals("Makan", result.category)
    }

    @Test
    fun englishThousandsSeparatorAndDecimals() {
        val result = NotificationParser.parse("", "Payment of IDR 50,000.00 successful")!!
        assertEquals(50_000L, result.amount)
        assertEquals(TxType.EXPENSE, result.type)
    }

    @Test
    fun earliestKeywordDecidesType() {
        // "pembayaran" comes before "diterima": the user paid.
        val result = NotificationParser.parse("", "Pembayaran Rp20.000 telah diterima merchant")!!
        assertEquals(TxType.EXPENSE, result.type)
    }

    @Test
    fun promoIsIgnored() {
        assertNull(NotificationParser.parse("Promo!", "Diskon hingga Rp50.000 untuk pembayaran pakai QRIS"))
    }

    @Test
    fun otpIsIgnored() {
        assertNull(NotificationParser.parse("", "Kode OTP 123456 untuk transfer ke rekening Rp100.000"))
    }

    @Test
    fun noAmountMeansNoTransaction() {
        assertNull(NotificationParser.parse("Transfer masuk", "Cek saldo kamu sekarang"))
    }

    @Test
    fun noKeywordMeansNoTransaction() {
        assertNull(NotificationParser.parse("Info", "Saldo kamu Rp250.000"))
    }

    @Test
    fun accountNumbersAreMaskedButAmountStays() {
        val result = NotificationParser.parse("", "Transfer ke 1234567890 sebesar Rp1500000 berhasil")!!
        assertEquals(1_500_000L, result.amount)
        assertFalse(result.note.contains("1234567890"))
        assertTrue(result.note.contains("****7890"))
        assertTrue(result.note.contains("Rp1500000"))
    }

    @Test
    fun dashedAccountAndPhoneNumbersAreMasked() {
        val masked = Masking.mask("Transfer ke 123-456-7890 a.n. HP 0812 3456 7890")
        assertFalse(masked.contains("123-456-7890"))
        assertFalse(masked.contains("0812 3456"))
        assertEquals("Transfer ke ****7890 a.n. HP ****7890", masked)
    }

    @Test
    fun datesAndFormattedAmountsStayReadable() {
        val text = "Pada 18-11-2026 10:15 sebesar Rp 1.500.000,00"
        assertEquals(text, Masking.mask(text))
    }

    @Test
    fun mixedGroupAndDottedAccountFormatsKeepOnlyLastFourDigits() {
        assertEquals("ke rek ****5501", Masking.mask("ke rek 0123-01-012345-50-1"))
        assertEquals("ke rek ****5678", Masking.mask("ke rek 123-00-1234567-8"))
        assertEquals("HP ****7890", Masking.mask("HP 0812.3456.7890"))
    }

    @Test
    fun numberAfterAmountIsStillMasked() {
        assertEquals("Isi pulsa Rp 50000 ****7890 berhasil", Masking.mask("Isi pulsa Rp 50000 081234567890 berhasil"))
    }

    @Test
    fun corpIsNotMistakenForRupiah() {
        assertEquals("Transfer ke PT MAJU CORP ****7890", Masking.mask("Transfer ke PT MAJU CORP 1234567890"))
        assertNull(NotificationParser.parse("", "Transfer ke PT MAJU CORP 1234567890"))
    }

    @Test
    fun cardPurchasesAreExpenses() {
        assertEquals(TxType.EXPENSE, NotificationParser.parse("", "Transaksi kartu debit Rp50.000 di INDOMARET")!!.type)
        assertEquals(TxType.EXPENSE, NotificationParser.parse("", "Transaksi Kartu Kredit Rp150.000 di TOKOPEDIA")!!.type)
    }

    @Test
    fun refundToCreditCardIsIncome() {
        assertEquals(TxType.INCOME, NotificationParser.parse("", "Refund Rp75.000 ke kartu kredit Anda")!!.type)
    }

    @Test
    fun absurdAmountIsRejected() {
        assertNull(NotificationParser.parse("", "Dana masuk Rp 99999999999999999"))
    }

    @Test
    fun myBcaFinancialNoteFormat() {
        val income = NotificationParser.parse(
            "Catatan Finansial",
            "Pemasukan sebesar IDR 10,000.00 dari ***ANA **DI ***JAYA di kategori Transfer Rekening.",
        )!!
        assertEquals(TxType.INCOME, income.type)
        assertEquals(10_000L, income.amount)

        val expense = NotificationParser.parse(
            "Catatan Finansial",
            "Pengeluaran sebesar IDR 25,000.00 ke ***TOKO **MAJU di kategori Belanja.",
        )!!
        assertEquals(TxType.EXPENSE, expense.type)
        assertEquals(25_000L, expense.amount)
    }

    @Test
    fun jagoEnglishTransferOutIsExpense() {
        val result = NotificationParser.parse(
            "Jago",
            "You've transferred Rp10.000 to BUDI SANTOSO. Need help? Contact Tanya Jago at 1500 000",
        )!!
        assertEquals(TxType.EXPENSE, result.type)
        assertEquals(10_000L, result.amount)
        assertEquals("Transfer keluar", result.category)
    }

    @Test
    fun movingMoneyBetweenOwnPocketsIsNotATransaction() {
        assertNull(NotificationParser.parse("Jago", "Rp10.000 has been moved from your Main Pocket to your Savings Pocket."))
    }

    @Test
    fun creditCardProductNameIsNotIncome() {
        // Bill reminder: no transaction keyword once "kartu kredit" is set aside.
        assertNull(NotificationParser.parse("", "Tagihan kartu kredit Anda Rp2.500.000"))
        val paid = NotificationParser.parse("", "Pembayaran tagihan kartu kredit Rp2.500.000 berhasil")!!
        assertEquals(TxType.EXPENSE, paid.type)
    }
}
