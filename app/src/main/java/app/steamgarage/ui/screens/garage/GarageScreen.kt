package app.steamgarage.ui.screens.garage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.steamgarage.AppContainer
import app.steamgarage.R
import app.steamgarage.data.CarSummary
import app.steamgarage.data.summarize
import app.steamgarage.ui.Format
import app.steamgarage.ui.components.BrassPlate
import app.steamgarage.ui.components.CarBadge
import app.steamgarage.ui.components.EmptyState
import app.steamgarage.ui.components.EngravedText
import app.steamgarage.ui.components.GaugeReading
import app.steamgarage.ui.components.OdometerDisplay
import app.steamgarage.ui.components.SteamGauge
import app.steamgarage.ui.components.SteamLever
import app.steamgarage.ui.components.statusLabel
import app.steamgarage.ui.screens.SteamScreen
import app.steamgarage.ui.screens.steamViewModel
import app.steamgarage.ui.theme.Steam
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

class GarageViewModel(container: AppContainer) : ViewModel() {
    val cars: StateFlow<List<CarSummary>?> =
        combine(container.repository.snapshot, container.settings.settings) { snapshot, settings ->
            snapshot.summarize(LocalDate.now(), settings.thresholds)
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun GarageHost(
    chromePadding: PaddingValues,
    onOpenCar: (String) -> Unit,
    onAddCar: () -> Unit,
) {
    val vm = steamViewModel { GarageViewModel(it) }
    val cars by vm.cars.collectAsStateWithLifecycle()
    GarageScreen(cars = cars, chromePadding = chromePadding, onOpenCar = onOpenCar, onAddCar = onAddCar)
}

@Composable
fun GarageScreen(
    cars: List<CarSummary>?,
    chromePadding: PaddingValues,
    onOpenCar: (String) -> Unit,
    onAddCar: () -> Unit,
) {
    SteamScreen(title = stringResource(R.string.app_name), chromePadding = chromePadding) {
        when {
            cars == null -> Unit
            cars.isEmpty() -> EmptyState(
                text = stringResource(R.string.garage_empty),
                actionText = stringResource(R.string.garage_add_car),
                onAction = onAddCar,
            )
            else -> {
                cars.forEach { summary -> CarPlate(summary, onClick = { onOpenCar(summary.car.id) }) }
                Spacer(Modifier.height(4.dp))
                SteamLever(
                    text = stringResource(R.string.garage_add_car),
                    onPull = onAddCar,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Латунная табличка машины: название, номер, пробег цифрами одометра и три самых срочных манометра. */
@Composable
fun CarPlate(summary: CarSummary, onClick: () -> Unit) {
    val car = summary.car
    BrassPlate(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        onClickLabel = stringResource(R.string.garage_open_car),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CarBadge(car.color)
            Spacer(Modifier.width(10.dp))
            EngravedText(car.name, style = Steam.text.plateTitle, maxLines = 1, modifier = Modifier.weight(1f))
            if (car.garageId != null) {
                Spacer(Modifier.width(8.dp))
                EngravedText(stringResource(R.string.car_shared_badge), style = Steam.text.plateCaption)
            }
        }
        val subtitle = carSubtitle(car.plate, car.make, car.model, stringResource(R.string.separator_dot))
        if (subtitle.isNotEmpty()) {
            EngravedText(subtitle, style = Steam.text.plateCaption, maxLines = 1)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val km = summary.currentKm
            if (km != null) {
                OdometerDisplay(Format.km(km), style = Steam.text.odometerSmall)
                Spacer(Modifier.width(6.dp))
                EngravedText(stringResource(R.string.unit_km), style = Steam.text.plateCaption)
            } else {
                EngravedText(stringResource(R.string.car_no_mileage), style = Steam.text.plateCaption)
            }
            Spacer(Modifier.weight(1f))
            val urgent = summary.activeItems.take(3)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                urgent.forEach { item ->
                    SteamGauge(
                        reading = GaugeReading(item.state.status, item.state.wear, item.state.warnFraction),
                        modifier = Modifier.size(42.dp),
                        compact = true,
                        contentDescription = stringResource(R.string.gauge_description, item.item.name, statusLabel(item.state.status)),
                    )
                }
            }
        }
        if (summary.items.isEmpty()) {
            Spacer(Modifier.height(6.dp))
            EngravedText(stringResource(R.string.car_no_items_short), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** «А123ВС 77 · Lada Vesta»: непустые части через разделитель. */
fun carSubtitle(plate: String, make: String, model: String, separator: String): String =
    listOf(plate, listOf(make, model).filter { it.isNotBlank() }.joinToString(" "))
        .filter { it.isNotBlank() }
        .joinToString(separator)
