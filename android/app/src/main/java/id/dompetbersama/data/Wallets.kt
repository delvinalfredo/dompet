package id.dompetbersama.data

import android.content.Context
import java.time.LocalDate
import java.time.YearMonth

/**
 * The balance the user read off their bank app at [since]. Only transactions from then on move it,
 * so older history does not count twice. Setting it again recalibrates.
 */
data class Opening(val amount: Long, val since: Long)

data class WalletBalance(val name: String, val balance: Long, val isMain: Boolean)

/** Wallet = transaction source name ("Jago", "GoPay", "Manual"). Moves between own wallets do count here. */
fun walletBalances(openings: Map<String, Opening>, main: String?, confirmed: List<MoneyTransaction>): List<WalletBalance> =
    openings.map { (name, opening) ->
        val moved = confirmed
            .filter { it.source == name && it.occurredAt >= opening.since }
            .sumOf { if (it.type == TxType.INCOME) it.amount else -it.amount }
        WalletBalance(name, opening.amount + moved, isMain = name == main)
    }.sortedWith(compareByDescending<WalletBalance> { it.isMain }.thenBy { it.name.lowercase() })

/** [remaining] is negative once the limit is passed. [daysLeft] counts today. */
data class LimitStatus(val limit: Long, val spent: Long, val remaining: Long, val daysLeft: Int) {
    val over: Boolean get() = remaining < 0
    val usedShare: Float get() = if (limit == 0L) 1f else (spent.toFloat() / limit).coerceIn(0f, 1f)
}

fun limitStatus(limit: Long, monthExpense: Long, today: LocalDate): LimitStatus = LimitStatus(
    limit = limit,
    spent = monthExpense,
    remaining = limit - monthExpense,
    daysLeft = YearMonth.from(today).lengthOfMonth() - today.dayOfMonth + 1,
)

/** Small, hand-edited settings: per-wallet opening balance, the main wallet, the monthly spending limit. */
class WalletPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("wallets", Context.MODE_PRIVATE)

    fun openings(): Map<String, Opening> =
        prefs.getStringSet(KEY_OPENINGS, null).orEmpty().mapNotNull(OpeningCodec::decode).toMap()

    fun setOpening(name: String, opening: Opening) = saveOpenings(openings() + (name to opening))

    fun removeOpening(name: String) {
        saveOpenings(openings() - name)
        if (main() == name) setMain(null)
    }

    fun main(): String? = prefs.getString(KEY_MAIN, null)

    fun setMain(name: String?) = prefs.edit().putString(KEY_MAIN, name).apply()

    fun monthlyLimit(): Long? = prefs.getLong(KEY_LIMIT, 0L).takeIf { it > 0 }

    fun setMonthlyLimit(limit: Long?) = prefs.edit().putLong(KEY_LIMIT, limit ?: 0L).apply()

    fun replaceAll(openings: Map<String, Opening>, main: String?, limit: Long?) {
        saveOpenings(openings)
        setMain(main)
        setMonthlyLimit(limit)
    }

    private fun saveOpenings(map: Map<String, Opening>) {
        prefs.edit().putStringSet(KEY_OPENINGS, map.map { (name, o) -> OpeningCodec.encode(name, o) }.toSet()).apply()
    }

    private companion object {
        const val KEY_OPENINGS = "openings"
        const val KEY_MAIN = "main"
        const val KEY_LIMIT = "monthly_limit"
    }
}

internal object OpeningCodec {
    private const val SEP = '\u001F' // unit separator: never part of an app label

    fun encode(name: String, opening: Opening): String = listOf(name, opening.amount, opening.since).joinToString(SEP.toString())

    fun decode(raw: String): Pair<String, Opening>? {
        val parts = raw.split(SEP)
        if (parts.size != 3 || parts[0].isBlank()) return null
        val amount = parts[1].toLongOrNull() ?: return null
        val since = parts[2].toLongOrNull() ?: return null
        return parts[0] to Opening(amount, since)
    }
}
