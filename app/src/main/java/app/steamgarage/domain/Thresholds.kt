package app.steamgarage.domain

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Глобальное правило порога «Скоро»: процент интервала с нижней и верхней границей.
 * Меняется в настройках; у отдельного расходника порог можно задать числом.
 */
data class ThresholdSettings(
    val percent: Int = Defaults.WARN_PERCENT,
    val kmMin: Int = Defaults.WARN_KM_MIN,
    val kmMax: Int = Defaults.WARN_KM_MAX,
    val daysMin: Int = Defaults.WARN_DAYS_MIN,
    val daysMax: Int = Defaults.WARN_DAYS_MAX,
) {
    /** Порог в км для интервала [intervalKm]. */
    fun kmFor(intervalKm: Int): Int =
        (intervalKm * percent / 100.0).roundToInt().coerceIn(min(kmMin, kmMax), max(kmMin, kmMax))

    /** Порог в днях для интервала [intervalMonths] (месяц считается за [Defaults.DAYS_PER_MONTH] дней). */
    fun daysFor(intervalMonths: Int): Int =
        (intervalMonths * Defaults.DAYS_PER_MONTH * percent / 100.0).roundToInt()
            .coerceIn(min(daysMin, daysMax), max(daysMin, daysMax))

    companion object {
        val DEFAULT = ThresholdSettings()
    }
}
