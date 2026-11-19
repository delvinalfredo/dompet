package id.dompetbersama.ui

import id.dompetbersama.data.TxType
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val idLocale: Locale = Locale.forLanguageTag("id-ID")
private val dayFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", idLocale)
private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", idLocale)
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", idLocale)
private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", idLocale)

/** Rp1.500.000, or −Rp1.500.000 for a negative amount. */
fun formatRupiah(amount: Long): String {
    val digits = String.format(idLocale, "%,d", abs(amount))
    return if (amount < 0) "−Rp$digits" else "Rp$digits"
}

fun formatSigned(type: TxType, amount: Long): String =
    (if (type == TxType.INCOME) "+" else "−") + formatRupiah(amount)

fun toLocalDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

fun formatDay(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Hari ini"
        today.minusDays(1) -> "Kemarin"
        else -> date.format(dayFormat)
    }
}

fun formatDate(millis: Long): String = toLocalDate(millis).format(dateFormat)

fun formatTime(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(timeFormat)

fun formatMonth(month: YearMonth): String = month.format(monthFormat).replaceFirstChar { it.uppercase() }
