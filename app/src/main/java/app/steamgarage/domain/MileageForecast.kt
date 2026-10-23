package app.steamgarage.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Показание одометра для расчёта среднего пробега. */
data class Reading(val date: LocalDate, val km: Int)

/** Средний суточный пробег и прогноз. */
object MileageForecast {

    /** Текущий пробег — максимум по показаниям. */
    fun currentKm(readings: List<Reading>): Int? = readings.maxOfOrNull { it.km }

    /**
     * Средний суточный пробег по показаниям за последние [windowDays] дней (не меньше [minReadings] показаний).
     * null — данных мало: одно показание, все в один день или пробег не растёт.
     */
    fun averageDailyKm(
        readings: List<Reading>,
        today: LocalDate,
        windowDays: Int = Defaults.AVERAGE_WINDOW_DAYS,
        minReadings: Int = Defaults.AVERAGE_MIN_READINGS,
    ): Double? {
        val from = today.minusDays(windowDays.toLong())
        val recent = readings
            .filter { !it.date.isBefore(from) && !it.date.isAfter(today) }
            .sortedWith(compareBy<Reading> { it.date }.thenBy { it.km })
        if (recent.size < minReadings) return null
        val first = recent.first()
        val last = recent.last()
        val days = ChronoUnit.DAYS.between(first.date, last.date)
        if (days <= 0) return null
        val km = last.km - first.km
        if (km <= 0) return null
        return km.toDouble() / days
    }
}
