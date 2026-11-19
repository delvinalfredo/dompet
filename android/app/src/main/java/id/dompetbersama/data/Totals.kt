package id.dompetbersama.data

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/** [transferred]: money moved between own wallets, shown for reference and kept out of income and expense. */
data class Totals(val income: Long, val expense: Long, val transferred: Long = 0) {
    val net: Long get() = income - expense
}

fun List<MoneyTransaction>.totals(): Totals {
    val (moves, counted) = partition { it.isTransfer }
    return Totals(
        income = counted.filter { it.type == TxType.INCOME }.sumOf { it.amount },
        expense = counted.filter { it.type == TxType.EXPENSE }.sumOf { it.amount },
        // Outgoing sides only: counts a move once, even when its two sides fall in different months.
        transferred = moves.filter { it.type == TxType.EXPENSE }.sumOf { it.amount },
    )
}

fun List<MoneyTransaction>.inMonth(
    month: YearMonth,
    zone: ZoneId = ZoneId.systemDefault(),
): List<MoneyTransaction> = filter { YearMonth.from(Instant.ofEpochMilli(it.occurredAt).atZone(zone)) == month }

/** Expense per category, largest first. */
fun List<MoneyTransaction>.expenseByCategory(): List<Pair<String, Long>> =
    filter { it.type == TxType.EXPENSE && !it.isTransfer }
        .groupBy { it.category }
        .map { (category, items) -> category to items.sumOf { it.amount } }
        .sortedByDescending { it.second }

/** Income and expense per source app (or "Manual"), busiest source first. */
fun List<MoneyTransaction>.totalsBySource(): List<Pair<String, Totals>> =
    filterNot { it.isTransfer }
        .groupBy { it.source }
        .map { (source, items) -> source to items.totals() }
        .sortedByDescending { (_, t) -> t.income + t.expense }

/** The [limit] largest expenses, largest first. */
fun List<MoneyTransaction>.largestExpenses(limit: Int = 5): List<MoneyTransaction> =
    filter { it.type == TxType.EXPENSE && !it.isTransfer }.sortedByDescending { it.amount }.take(limit)

/** Months that have at least one transaction, newest first. */
fun List<MoneyTransaction>.activeMonths(zone: ZoneId = ZoneId.systemDefault()): List<YearMonth> =
    map { YearMonth.from(Instant.ofEpochMilli(it.occurredAt).atZone(zone)) }.distinct().sortedDescending()
