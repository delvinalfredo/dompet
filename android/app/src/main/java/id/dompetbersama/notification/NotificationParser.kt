package id.dompetbersama.notification

import id.dompetbersama.data.Categories
import id.dompetbersama.data.KeywordRule
import id.dompetbersama.data.TxType

data class ParsedNotification(
    val type: TxType,
    val amount: Long,
    val category: String,
    /** Masked title and text, safe to store. */
    val note: String,
)

/**
 * Best-effort reader for Indonesian bank and e-wallet notifications.
 * Results always land as PENDING, so a wrong guess costs one tap, not a wrong balance.
 */
object NotificationParser {
    // "Rp 1.500.000,00", "Rp50,000.00", "IDR 25000". Decimals after the group are dropped.
    // \b so "CORP 1234567890" is not read as an amount.
    private val amountPattern = Regex("""(?i)\b(?:rp\.?|idr)[\s:]*(\d{1,3}(?:[.,]\d{3})+|\d+)""")

    // Never a transaction, whatever the user taught.
    val alwaysIgnored = listOf("kode otp", "otp ", "jangan berikan")

    // Usually marketing; a user word that matches lets the notification through (a bank may append "Promo…").
    val ignoredUnlessTaught = listOf(
        "promo", "diskon", "voucher", "hingga rp", "s.d. rp", "jatuh tempo", "minimal transaksi", "min. transaksi",
    )

    private val whitespace = Regex("""\s+""")

    val incomeWords = listOf(
        "pemasukan", "dana masuk", "uang masuk", "transfer masuk", "saldo masuk", "telah masuk",
        "menerima", "diterima", "kredit", "credit", "received", "refund", "pengembalian dana", "cashback",
    )

    val expenseWords = listOf(
        "pengeluaran", "pembayaran", "membayar", "bayar", "debit", "transfer ke", "transfer keluar", "kirim uang",
        "mengirim", "tarik tunai", "penarikan", "pembelian", "membeli", "terpotong", "dipotong",
        "purchase", "payment", "paid", "transaksi kartu", "you've transferred", "you transferred", "you've sent", "you sent",
    )

    // "kredit" inside these names is a card product, not money coming in.
    private val cardNames = listOf("kartu kredit", "credit card")

    // Tabs, CR and other control characters would only travel on into the CSV export.
    private val controlChars = Regex("""[\p{Cntrl}&&[^\n]]""")

    /** [userRules]: the user's own words for this source app, checked before the built-in lists. */
    fun parse(title: String, text: String, userRules: List<KeywordRule> = emptyList()): ParsedNotification? {
        val raw = listOf(title, text).filter { it.isNotBlank() }.joinToString(" - ")
            .replace(controlChars, " ")
        // One space for any run of whitespace, so a rule "dana diterima" matches "Dana\nditerima".
        val lower = raw.lowercase().replace(whitespace, " ")
        if (alwaysIgnored.any { lower.contains(it) }) return null
        val userType = userTypeOf(lower, userRules)
        if (userType == null && ignoredUnlessTaught.any { lower.contains(it) }) return null

        val amount = amountPattern.find(raw)?.groupValues?.get(1)
            ?.filter { it.isDigit() }
            ?.toLongOrNull()
            ?.takeIf { it in 1..MAX_AMOUNT }
            ?: return null

        val type = userType ?: typeOf(lower) ?: return null
        return ParsedNotification(
            type = type,
            amount = amount,
            category = Categories.guess(lower, type),
            note = Masking.mask(raw).take(MAX_NOTE),
        )
    }

    /**
     * The user's word that appears first decides, matched as a whole word ("dana" does not hit "Danamon").
     * Ties at the same position: a rule for this app beats an all-apps rule, then the longer word wins
     * ("transfer ke" over "transfer"). Card names are blanked unless the rule itself names a card.
     */
    private fun userTypeOf(lower: String, rules: List<KeywordRule>): TxType? {
        if (rules.isEmpty()) return null
        val blanked = blankCardNames(lower)
        return rules
            .mapNotNull { rule ->
                val text = if (cardNames.any { it in rule.word }) lower else blanked
                wholeWord(rule.word).find(text)?.let { it.range.first to rule }
            }
            .minWithOrNull(
                compareBy<Pair<Int, KeywordRule>> { it.first }
                    .thenBy { if (it.second.packageName != null) 0 else 1 }
                    .thenByDescending { it.second.word.length },
            )
            ?.second?.type
    }

    private fun wholeWord(word: String) = Regex("""(?<![\p{L}\p{N}])""" + Regex.escape(word) + """(?![\p{L}\p{N}])""")

    // Blanking keeps every index in place, so positions stay comparable with the unblanked text.
    private fun blankCardNames(lower: String) = cardNames.fold(lower) { acc, name -> acc.replace(name, " ".repeat(name.length)) }

    /** The keyword that appears first decides: "pembayaran ... diterima" is an expense. */
    private fun typeOf(lower: String): TxType? {
        val incomeText = blankCardNames(lower)
        val inAt = firstIndex(incomeText, incomeWords)
        val outAt = firstIndex(lower, expenseWords)
        return when {
            inAt != null && (outAt == null || inAt < outAt) -> TxType.INCOME
            outAt != null -> TxType.EXPENSE
            else -> null
        }
    }

    private fun firstIndex(text: String, words: List<String>): Int? =
        words.mapNotNull { word -> text.indexOf(word).takeIf { it >= 0 } }.minOrNull()

    private const val MAX_NOTE = 240

    // Rp 10 trillion. Anything bigger is a parsing accident and would overflow monthly totals.
    private const val MAX_AMOUNT = 10_000_000_000_000L
}

object Masking {
    // Kept as written: an amount right after a whole-word Rp/IDR, and dates (18-11-2026, 2026-11-18).
    private val keep = Regex(
        """(?i)\b(?:rp\.?|idr)[\s:]*(?:\d{1,3}(?:[.,]\d{3})+|\d+)""" +
            """|\b\d{4}-\d{2}-\d{2}\b|\b\d{1,2}[-/.]\d{1,2}[-/.]\d{2,4}\b""",
    )

    // Any digit run joined by space, dot or dash, whatever the group sizes ("0123-01-012345-50-1").
    private val digitRun = Regex("""\d+(?:[ .-]\d+)*""")

    private const val MIN_SECRET_DIGITS = 6

    /** Account, card and phone numbers become ****1234. Amounts and dates stay readable. */
    fun mask(text: String): String {
        val out = StringBuilder()
        var last = 0
        for (span in keep.findAll(text)) {
            out.append(maskRuns(text.substring(last, span.range.first))).append(span.value)
            last = span.range.last + 1
        }
        return out.append(maskRuns(text.substring(last))).toString()
    }

    private fun maskRuns(part: String): String = digitRun.replace(part) { match ->
        val digits = match.value.filter { it.isDigit() }
        if (digits.length < MIN_SECRET_DIGITS) match.value else "****" + digits.takeLast(4)
    }
}
