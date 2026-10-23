package app.steamgarage.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MileageForecastTest {

    private val today = LocalDate.of(2026, 10, 23)

    @Test
    fun averageOverLast90Days() {
        val readings = listOf(
            Reading(today.minusDays(60), 50_000),
            Reading(today.minusDays(30), 51_500),
            Reading(today, 53_000),
        )
        assertEquals(50.0, MileageForecast.averageDailyKm(readings, today)!!, 0.0001)
    }

    @Test
    fun singleReadingGivesNoAverage() {
        assertNull(MileageForecast.averageDailyKm(listOf(Reading(today, 50_000)), today))
    }

    @Test
    fun readingsOutsideWindowAreIgnored() {
        val readings = listOf(
            Reading(today.minusDays(200), 40_000),
            Reading(today.minusDays(10), 50_000),
        )
        // В окне 90 дней только одно показание.
        assertNull(MileageForecast.averageDailyKm(readings, today))
    }

    @Test
    fun sameDayReadingsGiveNoAverage() {
        val readings = listOf(Reading(today, 50_000), Reading(today, 50_100))
        assertNull(MileageForecast.averageDailyKm(readings, today))
    }

    @Test
    fun currentKmIsMaximum() {
        val readings = listOf(Reading(today.minusDays(5), 50_000), Reading(today.minusDays(9), 52_000))
        assertEquals(52_000, MileageForecast.currentKm(readings))
        assertNull(MileageForecast.currentKm(emptyList()))
    }

    @Test
    fun forecastWithSingleReadingIsAbsent() {
        val avg = MileageForecast.averageDailyKm(listOf(Reading(today, 50_000)), today)
        val state = StatusCalculator.evaluate(
            ItemRule(10_000, null),
            listOf(ServicePoint(today.minusDays(10), 45_000)),
            currentKm = 50_000,
            averageDailyKm = avg,
            today = today,
        )
        assertNull(state.forecastDate)
    }
}
