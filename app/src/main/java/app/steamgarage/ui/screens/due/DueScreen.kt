package app.steamgarage.ui.screens.due

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.steamgarage.AppContainer
import app.steamgarage.R
import app.steamgarage.data.CarSummary
import app.steamgarage.data.ItemSummary
import app.steamgarage.data.summarize
import app.steamgarage.domain.ItemStatus
import app.steamgarage.ui.components.EmptyState
import app.steamgarage.ui.components.SectionTitle
import app.steamgarage.ui.components.StatusBadge
import app.steamgarage.ui.components.SteamGauge
import app.steamgarage.ui.components.SteamPanel
import app.steamgarage.ui.components.statusLabel
import app.steamgarage.ui.screens.SteamScreen
import app.steamgarage.ui.screens.car.gaugeReadingFor
import app.steamgarage.ui.screens.car.itemShortLine
import app.steamgarage.ui.screens.steamViewModel
import app.steamgarage.ui.theme.Steam
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

class DueViewModel(container: AppContainer) : ViewModel() {
    val cars: StateFlow<List<CarSummary>?> =
        combine(container.repository.snapshot, container.settings.settings) { snapshot, settings ->
            snapshot.summarize(LocalDate.now(), settings.thresholds)
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** Порядок групп на экране «Сроки». */
private val DueGroups = listOf(ItemStatus.OVERDUE, ItemStatus.SOON, ItemStatus.NO_DATA, ItemStatus.OK)

@Composable
fun DueHost(chromePadding: PaddingValues, onOpenItem: (String) -> Unit, onAddCar: () -> Unit) {
    val vm = steamViewModel { DueViewModel(it) }
    val cars by vm.cars.collectAsStateWithLifecycle()
    var carFilter by rememberSaveable { mutableStateOf<String?>(null) }
    DueScreen(
        cars = cars,
        carFilter = carFilter,
        chromePadding = chromePadding,
        onCarFilter = { carFilter = it },
        onOpenItem = onOpenItem,
        onAddCar = onAddCar,
    )
}

@Composable
fun DueScreen(
    cars: List<CarSummary>?,
    carFilter: String?,
    chromePadding: PaddingValues,
    onCarFilter: (String?) -> Unit,
    onOpenItem: (String) -> Unit,
    onAddCar: () -> Unit,
) {
    SteamScreen(title = stringResource(R.string.tab_due), chromePadding = chromePadding) {
        if (cars == null) return@SteamScreen
        if (cars.isEmpty()) {
            EmptyState(
                text = stringResource(R.string.due_empty_no_cars),
                actionText = stringResource(R.string.garage_add_car),
                onAction = onAddCar,
            )
            return@SteamScreen
        }
        if (cars.size > 1) {
            CarFilterRow(cars = cars, selected = carFilter, onSelect = onCarFilter)
        }
        val rows = cars
            .filter { carFilter == null || it.car.id == carFilter }
            .flatMap { car -> car.activeItems.map { car to it } }
        if (rows.isEmpty()) {
            SteamPanel(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.due_empty_no_items), style = MaterialTheme.typography.bodyMedium)
            }
            return@SteamScreen
        }
        DueGroups.forEach { status ->
            val group = rows.filter { it.second.state.status == status }
                .sortedByDescending { it.second.state.wear ?: 0f }
            if (group.isEmpty()) return@forEach
            SectionTitle(stringResource(R.string.due_group, statusLabel(status), group.size))
            group.forEach { (car, item) ->
                DueRow(carName = car.car.name, item = item, showCar = cars.size > 1, onClick = { onOpenItem(item.item.id) })
            }
        }
    }
}

@Composable
private fun CarFilterRow(cars: List<CarSummary>, selected: String?, onSelect: (String?) -> Unit) {
    val palette = Steam.palette
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val options = listOf<Pair<String?, String>>(null to stringResource(R.string.journal_all_cars)) +
            cars.map { it.car.id to it.car.name }
        options.forEach { (id, label) ->
            val isSelected = id == selected
            val shape = CutCornerShape(6.dp)
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = palette.text,
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .background(if (isSelected) palette.surfaceDeep else palette.surface, shape)
                    .border(if (isSelected) 2.dp else 1.dp, if (isSelected) palette.copperLight else palette.brassShadow, shape)
                    .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(id) }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
            )
        }
    }
}

@Composable
private fun DueRow(carName: String, item: ItemSummary, showCar: Boolean, onClick: () -> Unit) {
    val palette = Steam.palette
    SteamPanel(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        onClickLabel = stringResource(R.string.item_open),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SteamGauge(
                reading = gaugeReadingFor(item.state),
                modifier = Modifier.size(52.dp),
                compact = true,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (showCar) Text(carName, style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                Text(item.item.name, style = MaterialTheme.typography.titleSmall)
                Text(itemShortLine(item.state), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(8.dp))
            StatusBadge(item.state.status)
        }
    }
}
