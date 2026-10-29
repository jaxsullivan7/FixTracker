package com.fixtracker.app.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Currency
import java.util.Locale

private val spanish = Locale("es", "ES")
private val dayMonth = DateTimeFormatter.ofPattern("d MMM", spanish)
private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", spanish)
private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", spanish)

fun formatKm(value: Int): String =
    NumberFormat.getIntegerInstance(spanish).format(value)

fun formatMoney(value: Double, currencyCode: String = "EUR"): String {
    val format = NumberFormat.getCurrencyInstance(spanish)
    format.currency = runCatching { Currency.getInstance(currencyCode) }.getOrNull() ?: Currency.getInstance("EUR")
    format.maximumFractionDigits = if (value % 1.0 == 0.0) 0 else 2
    format.minimumFractionDigits = if (value % 1.0 == 0.0) 0 else 2
    return format.format(value)
}

fun formatDate(date: LocalDate): String = date.format(dayMonthYear)

fun formatDateShort(date: LocalDate): String = date.format(dayMonth)

fun formatMonth(date: LocalDate): String =
    date.format(monthYear).replaceFirstChar { it.titlecase(spanish) }

fun formatHeaderDate(date: LocalDate = LocalDate.now()): String =
    date.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", spanish))
        .replaceFirstChar { it.titlecase(spanish) }

fun monthShort(month: YearMonth): String =
    month.month.getDisplayName(TextStyle.SHORT, spanish).removeSuffix(".")

fun daysUntil(date: LocalDate, today: LocalDate = LocalDate.now()): Long =
    ChronoUnit.DAYS.between(today, date)

fun dueDateLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String {
    val days = daysUntil(date, today)
    return when {
        days < -1 -> "Hace ${-days} días"
        days == -1L -> "Ayer"
        days == 0L -> "Hoy"
        days == 1L -> "Mañana"
        else -> "En $days días"
    }
}

fun relativePastLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String {    val days = ChronoUnit.DAYS.between(date, today)
    return when {
        days <= 0 -> "Hoy"
        days == 1L -> "Ayer"
        days < 30 -> "Hace $days días"
        days < 365 -> "Hace ${days / 30} ${if (days / 30 == 1L) "mes" else "meses"}"
        else -> "Hace ${days / 365} ${if (days / 365 == 1L) "año" else "años"}"
    }
}

fun initialOf(name: String): String = name.trim().take(1).uppercase(spanish)

val currencySymbols = mapOf("EUR" to "€", "USD" to "$", "GBP" to "£")
