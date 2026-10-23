package app.steamgarage.ui.screens.car

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.steamgarage.R
import app.steamgarage.data.ItemSummary
import app.steamgarage.domain.ItemState
import app.steamgarage.domain.ItemStatus
import app.steamgarage.domain.Metric
import app.steamgarage.ui.Format
import app.steamgarage.ui.components.GaugeReading
import java.time.LocalDate

/** Показания манометра: остаток по интервалу, который наступит раньше. */
@Composable
fun gaugeReadingFor(state: ItemState): GaugeReading {
    if (state.status == ItemStatus.NO_DATA) return GaugeReading(ItemStatus.NO_DATA, null)
    return when (state.primary) {
        Metric.KM -> {
            val left = state.kmLeft ?: 0
            GaugeReading(
                status = state.status,
                wear = state.wear,
                warnFraction = state.warnFraction,
                readout = Format.kmAbs(left),
                unit = stringResource(R.string.unit_km),
                alarm = left <= 0,
            )
        }
        Metric.DAYS, null -> {
            val left = state.daysLeft ?: 0
            GaugeReading(
                status = state.status,
                wear = state.wear,
                warnFraction = state.warnFraction,
                readout = Format.kmAbs(left),
                unit = stringResource(R.string.unit_days),
                alarm = left <= 0,
            )
        }
    }
}

/** Строки пояснения под манометром: остатки по км и по времени, прогноз, последняя замена. */
@Composable
fun itemDetailLines(summary: ItemSummary, includeLast: Boolean = true): List<String> {
    val state = summary.state
    val item = summary.item
    val lines = mutableListOf<String>()
    if (state.lastService == null) {
        lines += stringResource(R.string.item_no_service_yet)
        return lines
    }
    if (item.intervalKm == null && item.intervalMonths == null) {
        lines += stringResource(R.string.item_no_interval)
    }
    state.kmLeft?.let { left ->
        val due = Format.km(state.dueKm ?: 0)
        lines += if (left > 0) {
            stringResource(R.string.item_km_left, Format.km(left), due)
        } else {
            stringResource(R.string.item_km_over, Format.kmAbs(left), due)
        }
    }
    state.daysLeft?.let { left ->
        val due = state.dueDate?.let { Format.date(it) }.orEmpty()
        lines += if (left > 0) {
            stringResource(R.string.item_days_left, left, due)
        } else {
            stringResource(R.string.item_days_over, -left, due)
        }
    }
    state.forecastDate?.let { lines += stringResource(R.string.item_forecast, Format.date(it)) }
    if (includeLast) {
        val last = state.lastService
        lines += stringResource(R.string.item_last_service, Format.date(last.date), Format.km(last.mileage))
    }
    return lines
}

/** Короткая строка для списков: самое срочное. */
@Composable
fun itemShortLine(state: ItemState): String = when {
    state.status == ItemStatus.NO_DATA && state.lastService == null -> stringResource(R.string.item_no_service_yet)
    state.status == ItemStatus.NO_DATA -> stringResource(R.string.item_no_interval)
    state.primary == Metric.KM -> {
        val left = state.kmLeft ?: 0
        if (left > 0) stringResource(R.string.item_short_km_left, Format.km(left))
        else stringResource(R.string.item_short_km_over, Format.kmAbs(left))
    }
    else -> {
        val left = state.daysLeft ?: 0
        if (left > 0) stringResource(R.string.item_short_days_left, left)
        else stringResource(R.string.item_short_days_over, -left)
    }
}

/** Интервал «10 000 км или 12 мес.» для экрана расходника. */
@Composable
fun intervalText(km: Int?, months: Int?): String = when {
    km != null && months != null -> stringResource(R.string.item_interval_both, Format.km(km), months)
    km != null -> stringResource(R.string.item_interval_km, Format.km(km))
    months != null -> stringResource(R.string.item_interval_months, months)
    else -> stringResource(R.string.item_interval_none)
}

fun LocalDate.isNotFuture(): Boolean = !isAfter(LocalDate.now())
