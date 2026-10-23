package app.steamgarage.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/** Форматирование чисел и дат для интерфейса: км и ₽ с пробелами между разрядами, даты ДД.ММ.ГГГГ. */
object Format {
    private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    /** Неразрывный пробел между разрядами: число не переносится на две строки посередине. */
    const val GROUP_SPACE = '\u00A0'

    /** 85247 → «85 247» (одометр рисует по пробелам промежутки между разрядами). */
    fun km(value: Int): String = group(abs(value).toString()).let { if (value < 0) "−$it" else it }

    /** Модуль числа с разрядами — для «превышено на 350 км». */
    fun kmAbs(value: Int): String = group(abs(value).toString())

    fun date(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(dateFormat)

    fun date(date: LocalDate): String = date.format(dateFormat)

    /** Время ЧЧ:ММ: 9, 5 → «09:05». */
    fun time(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

    /** Момент времени по часам телефона: «23.10.2026 09:05». */
    fun dateTime(millis: Long): String {
        val t = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault())
        return "${date(t.toLocalDate())} ${time(t.hour, t.minute)}"
    }

    /** Копейки → «3 500» или «3 500,50» (без знака ₽ — его добавляет строка ресурса). */
    fun money(kopecks: Long): String {
        val rubles = kopecks / 100
        val rest = abs(kopecks % 100)
        val whole = group(abs(rubles).toString()).let { if (kopecks < 0) "−$it" else it }
        return if (rest == 0L) whole else "$whole,${rest.toString().padStart(2, '0')}"
    }

    /** Разбор суммы, введённой человеком: «3500», «3 500,50», «3500.5». null — не число. */
    fun parseMoney(text: String): Long? {
        val cleaned = text.trim().replace(" ", "").replace(" ", "").replace(',', '.')
        if (cleaned.isEmpty()) return null
        val value = cleaned.toBigDecimalOrNull() ?: return null
        if (value.signum() < 0) return null
        return value.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).toLong()
    }

    /** Разбор целого числа (км, месяцы): пробелы между разрядами допустимы. */
    fun parseInt(text: String): Int? =
        text.trim().replace(" ", "").replace(" ", "").takeIf { it.isNotEmpty() }?.toIntOrNull()

    /** Сумма для поля ввода: «3500» или «3500,5». */
    fun moneyInput(kopecks: Long?): String {
        if (kopecks == null) return ""
        val rubles = kopecks / 100
        val rest = kopecks % 100
        return if (rest == 0L) rubles.toString() else "$rubles,${rest.toString().padStart(2, '0')}"
    }

    private fun group(digits: String): String =
        digits.reversed().chunked(3).joinToString(GROUP_SPACE.toString()).reversed()
}
