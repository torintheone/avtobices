package app.steamgarage.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.max

/** Запись замены в виде, нужном расчёту: дата и пробег. */
data class ServicePoint(val date: LocalDate, val mileage: Int)

/** Интервалы и пороги расходника. Порог null — считается по глобальному правилу. */
data class ItemRule(
    val intervalKm: Int?,
    val intervalMonths: Int?,
    val warnKm: Int? = null,
    val warnDays: Int? = null,
)

/** По какому интервалу показывать остаток на манометре. */
enum class Metric { KM, DAYS }

/** Результат расчёта для одного расходника. Все величины производные — в базе не хранятся. */
data class ItemState(
    val status: ItemStatus,
    val lastService: ServicePoint?,
    /** Срок по км и остаток (отрицательный — перепробег). */
    val dueKm: Int? = null,
    val kmLeft: Int? = null,
    /** Срок по времени и остаток в днях (отрицательный — просрочка). */
    val dueDate: LocalDate? = null,
    val daysLeft: Int? = null,
    /** Прогнозная дата по среднему суточному пробегу. */
    val forecastDate: LocalDate? = null,
    /** Износ для манометра: максимум из «пройдено км ÷ интервал» и «прошло дней ÷ интервал». */
    val wear: Float? = null,
    /** Действующие пороги. */
    val warnKm: Int? = null,
    val warnDays: Int? = null,
    /** Интервал, который наступит раньше, — его остаток показывается крупно. */
    val primary: Metric? = null,
    /** Доля шкалы, с которой начинается зона «Скоро», для основного интервала. */
    val warnFraction: Float = 0.1f,
)

/** Расчёт статусов (раздел «Статусы и уведомления» SPEC.md). Чистые функции. */
object StatusCalculator {

    /** Последняя замена: наибольший пробег, при равенстве — более поздняя дата. */
    fun lastService(services: List<ServicePoint>): ServicePoint? =
        services.maxWithOrNull(compareBy<ServicePoint> { it.mileage }.thenBy { it.date })

    fun evaluate(
        rule: ItemRule,
        services: List<ServicePoint>,
        currentKm: Int?,
        averageDailyKm: Double?,
        today: LocalDate,
        thresholds: ThresholdSettings = ThresholdSettings.DEFAULT,
    ): ItemState {
        val last = lastService(services) ?: return ItemState(ItemStatus.NO_DATA, null)
        val intervalKm = rule.intervalKm?.takeIf { it > 0 }
        val intervalMonths = rule.intervalMonths?.takeIf { it > 0 }
        if (intervalKm == null && intervalMonths == null) {
            // Интервал не задан (например, ремень ГРМ из шаблона) — расходник требует внимания.
            return ItemState(ItemStatus.NO_DATA, last)
        }

        var kmStatus: ItemStatus? = null
        var dueKm: Int? = null
        var kmLeft: Int? = null
        var kmWear: Float? = null
        var forecast: LocalDate? = null
        val warnKm = intervalKm?.let { rule.warnKm ?: thresholds.kmFor(it) }
        if (intervalKm != null && warnKm != null) {
            val current = max(currentKm ?: last.mileage, last.mileage)
            dueKm = last.mileage + intervalKm
            val left = dueKm - current
            kmLeft = left
            kmStatus = statusFor(left, warnKm)
            kmWear = (current - last.mileage).toFloat() / intervalKm
            if (averageDailyKm != null && averageDailyKm > 0 && left > 0) {
                forecast = today.plusDays(ceil(left / averageDailyKm).toLong())
            }
        }

        var timeStatus: ItemStatus? = null
        var dueDate: LocalDate? = null
        var daysLeft: Int? = null
        var timeWear: Float? = null
        var intervalDays = 0L
        val warnDays = intervalMonths?.let { rule.warnDays ?: thresholds.daysFor(it) }
        if (intervalMonths != null && warnDays != null) {
            val due = last.date.plusMonths(intervalMonths.toLong())
            dueDate = due
            intervalDays = ChronoUnit.DAYS.between(last.date, due).coerceAtLeast(1)
            val left = ChronoUnit.DAYS.between(today, due).toInt()
            daysLeft = left
            timeStatus = statusFor(left, warnDays)
            timeWear = ChronoUnit.DAYS.between(last.date, today).coerceAtLeast(0).toFloat() / intervalDays
        }

        val status = listOfNotNull(kmStatus, timeStatus).maxBy { it.urgency }
        val primary = when {
            kmStatus == null -> Metric.DAYS
            timeStatus == null -> Metric.KM
            kmStatus.urgency != timeStatus.urgency -> if (kmStatus.urgency > timeStatus.urgency) Metric.KM else Metric.DAYS
            else -> if ((kmWear ?: 0f) >= (timeWear ?: 0f)) Metric.KM else Metric.DAYS
        }
        val warnFraction = when (primary) {
            Metric.KM -> warnKm!!.toFloat() / intervalKm!!
            Metric.DAYS -> warnDays!!.toFloat() / intervalDays
        }
        return ItemState(
            status = status,
            lastService = last,
            dueKm = dueKm,
            kmLeft = kmLeft,
            dueDate = dueDate,
            daysLeft = daysLeft,
            forecastDate = forecast,
            wear = max(kmWear ?: 0f, timeWear ?: 0f),
            warnKm = warnKm,
            warnDays = warnDays,
            primary = primary,
            warnFraction = warnFraction.coerceIn(0f, 1f),
        )
    }

    private fun statusFor(left: Int, warn: Int): ItemStatus = when {
        left <= 0 -> ItemStatus.OVERDUE
        left <= warn -> ItemStatus.SOON
        else -> ItemStatus.OK
    }
}
