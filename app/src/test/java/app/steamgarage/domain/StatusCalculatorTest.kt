package app.steamgarage.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatusCalculatorTest {

    private val today = LocalDate.of(2026, 10, 23)

    private fun evaluate(
        rule: ItemRule,
        last: ServicePoint?,
        currentKm: Int? = null,
        avg: Double? = null,
        thresholds: ThresholdSettings = ThresholdSettings.DEFAULT,
    ) = StatusCalculator.evaluate(rule, listOfNotNull(last), currentKm, avg, today, thresholds)

    // ── Только км ──

    @Test
    fun kmOnly_ok() {
        val s = evaluate(ItemRule(10_000, null), ServicePoint(today.minusMonths(2), 50_000), currentKm = 53_000)
        assertEquals(ItemStatus.OK, s.status)
        assertEquals(60_000, s.dueKm)
        assertEquals(7_000, s.kmLeft)
        assertEquals(Metric.KM, s.primary)
        assertEquals(0.3f, s.wear!!, 0.0001f)
    }

    @Test
    fun kmOnly_soonWhenLeftWithinThreshold() {
        // Интервал 10 000 → порог 10 % = 1000 км.
        val s = evaluate(ItemRule(10_000, null), ServicePoint(today, 50_000), currentKm = 59_200)
        assertEquals(ItemStatus.SOON, s.status)
        assertEquals(800, s.kmLeft)
        assertEquals(1_000, s.warnKm)
    }

    @Test
    fun kmOnly_overdueShowsExcess() {
        val s = evaluate(ItemRule(10_000, null), ServicePoint(today, 50_000), currentKm = 60_350)
        assertEquals(ItemStatus.OVERDUE, s.status)
        assertEquals(-350, s.kmLeft)
    }

    @Test
    fun acceptanceCase_300LeftWithThreshold500IsSoon() {
        // Критерий приёмки этапа 2: остаток 300 км при пороге 500 даёт «Скоро».
        val s = evaluate(ItemRule(5_000, null, warnKm = 500), ServicePoint(today, 10_000), currentKm = 14_700)
        assertEquals(300, s.kmLeft)
        assertEquals(ItemStatus.SOON, s.status)
    }

    // ── Только время ──

    @Test
    fun timeOnly_okSoonOverdue() {
        val rule = ItemRule(null, 12)
        // Порог: 12 мес · 30 дн · 10 % = 36 → не больше 30 дней.
        val ok = evaluate(rule, ServicePoint(today.minusMonths(6), 0))
        assertEquals(ItemStatus.OK, ok.status)
        assertEquals(30, ok.warnDays)
        assertEquals(Metric.DAYS, ok.primary)

        val soon = evaluate(rule, ServicePoint(today.minusMonths(12).plusDays(20), 0))
        assertEquals(ItemStatus.SOON, soon.status)
        assertEquals(20, soon.daysLeft)

        val overdue = evaluate(rule, ServicePoint(today.minusMonths(12).minusDays(5), 0))
        assertEquals(ItemStatus.OVERDUE, overdue.status)
        assertEquals(-5, overdue.daysLeft)
    }

    // ── Оба интервала: срабатывает тот, что раньше ──

    @Test
    fun bothIntervals_earlierOneWins() {
        val rule = ItemRule(10_000, 12)
        // По км почти ничего не проехали, но прошёл год — срабатывает время.
        val byTime = evaluate(rule, ServicePoint(today.minusMonths(12).minusDays(1), 50_000), currentKm = 51_000)
        assertEquals(ItemStatus.OVERDUE, byTime.status)
        assertEquals(Metric.DAYS, byTime.primary)

        // Прошёл месяц, но проехали почти весь интервал — срабатывают км.
        val byKm = evaluate(rule, ServicePoint(today.minusMonths(1), 50_000), currentKm = 59_500)
        assertEquals(ItemStatus.SOON, byKm.status)
        assertEquals(Metric.KM, byKm.primary)
    }

    @Test
    fun bothIntervals_wearIsMaximumOfFractions() {
        val s = evaluate(ItemRule(10_000, 12), ServicePoint(today.minusMonths(6), 50_000), currentKm = 52_000)
        // Прошло ~50 % времени и 20 % пробега.
        assertEquals(0.5f, s.wear!!, 0.02f)
        assertEquals(Metric.DAYS, s.primary)
    }

    // ── Нет данных ──

    @Test
    fun noServices_isNoData() {
        val s = evaluate(ItemRule(10_000, 12), last = null, currentKm = 50_000)
        assertEquals(ItemStatus.NO_DATA, s.status)
        assertNull(s.wear)
    }

    @Test
    fun noIntervals_isNoData() {
        val s = evaluate(ItemRule(null, null), ServicePoint(today, 50_000), currentKm = 50_000)
        assertEquals(ItemStatus.NO_DATA, s.status)
    }

    // ── Граничные значения порогов ──

    @Test
    fun thresholdBoundaries() {
        val rule = ItemRule(10_000, null) // порог 1000 км
        val last = ServicePoint(today, 50_000)
        assertEquals(ItemStatus.OK, evaluate(rule, last, currentKm = 58_999).status) // осталось 1001
        assertEquals(ItemStatus.SOON, evaluate(rule, last, currentKm = 59_000).status) // ровно 1000
        assertEquals(ItemStatus.SOON, evaluate(rule, last, currentKm = 59_999).status) // остался 1 км
        assertEquals(ItemStatus.OVERDUE, evaluate(rule, last, currentKm = 60_000).status) // ровно 0
    }

    @Test
    fun defaultThresholdIsClamped() {
        val t = ThresholdSettings.DEFAULT
        assertEquals(500, t.kmFor(3_000)) // 10 % = 300 → не меньше 500
        assertEquals(1_500, t.kmFor(15_000))
        assertEquals(2_000, t.kmFor(60_000)) // 10 % = 6000 → не больше 2000
        assertEquals(7, t.daysFor(1)) // 3 дня → не меньше 7
        assertEquals(18, t.daysFor(6))
        assertEquals(30, t.daysFor(36)) // 108 → не больше 30
    }

    @Test
    fun perItemThresholdOverridesGlobal() {
        val s = evaluate(ItemRule(10_000, null, warnKm = 3_000), ServicePoint(today, 0), currentKm = 7_500)
        assertEquals(ItemStatus.SOON, s.status)
        assertEquals(3_000, s.warnKm)
    }

    @Test
    fun globalThresholdSettingsApply() {
        val custom = ThresholdSettings(percent = 20, kmMin = 100, kmMax = 5_000)
        val s = evaluate(ItemRule(10_000, null), ServicePoint(today, 0), currentKm = 8_100, thresholds = custom)
        assertEquals(2_000, s.warnKm)
        assertEquals(ItemStatus.SOON, s.status)
    }

    // ── Последняя замена ──

    @Test
    fun lastServiceIsMaxMileageThenLatestDate() {
        val a = ServicePoint(LocalDate.of(2026, 1, 10), 40_000)
        val b = ServicePoint(LocalDate.of(2025, 12, 1), 45_000)
        val c = ServicePoint(LocalDate.of(2026, 2, 1), 45_000)
        assertEquals(c, StatusCalculator.lastService(listOf(a, b, c)))
        assertEquals(b, StatusCalculator.lastService(listOf(a, b)))
    }

    // ── Прогноз ──

    @Test
    fun forecastDateUsesAverageDailyMileage() {
        val s = evaluate(ItemRule(10_000, null), ServicePoint(today, 50_000), currentKm = 55_000, avg = 50.0)
        // Осталось 5000 км при 50 км/день → 100 дней.
        assertEquals(today.plusDays(100), s.forecastDate)
    }

    @Test
    fun noForecastWithoutAverage() {
        val s = evaluate(ItemRule(10_000, null), ServicePoint(today, 50_000), currentKm = 55_000, avg = null)
        assertNull(s.forecastDate)
    }
}
