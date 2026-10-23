package app.steamgarage.ui.screens.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.steamgarage.AppContainer
import app.steamgarage.R
import app.steamgarage.data.GarageSnapshot
import app.steamgarage.data.db.CarEntity
import app.steamgarage.data.db.ItemEntity
import app.steamgarage.data.db.ServiceEntity
import app.steamgarage.domain.Category
import app.steamgarage.ui.Format
import app.steamgarage.ui.components.EmptyState
import app.steamgarage.ui.components.SteamPanel
import app.steamgarage.ui.components.SteamTopBar
import app.steamgarage.ui.components.label
import app.steamgarage.ui.screens.item.ServiceRow
import app.steamgarage.ui.screens.steamViewModel
import app.steamgarage.ui.theme.Steam
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Период для фильтра журнала. */
enum class Period { ALL, LAST_12_MONTHS, THIS_YEAR, LAST_YEAR }

class JournalViewModel(container: AppContainer) : ViewModel() {
    val snapshot: StateFlow<GarageSnapshot?> =
        container.repository.snapshot.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** Запись журнала с машиной и расходником. */
data class JournalEntry(val service: ServiceEntity, val car: CarEntity, val item: ItemEntity?)

fun GarageSnapshot.journal(carId: String?, category: Category?, period: Period, today: LocalDate = LocalDate.now()): List<JournalEntry> {
    val cars = cars.associateBy { it.id }
    val items = items.associateBy { it.id }
    val range: LongRange? = when (period) {
        Period.ALL -> null
        Period.LAST_12_MONTHS -> today.minusMonths(12).toEpochDay()..today.toEpochDay()
        Period.THIS_YEAR -> LocalDate.of(today.year, 1, 1).toEpochDay()..LocalDate.of(today.year, 12, 31).toEpochDay()
        Period.LAST_YEAR -> LocalDate.of(today.year - 1, 1, 1).toEpochDay()..LocalDate.of(today.year - 1, 12, 31).toEpochDay()
    }
    return services
        .mapNotNull { s -> cars[s.carId]?.let { JournalEntry(s, it, items[s.itemId]) } }
        .filter { carId == null || it.car.id == carId }
        .filter { category == null || it.item?.category == category.name }
        .filter { range == null || it.service.date in range }
        .sortedWith(compareByDescending<JournalEntry> { it.service.date }.thenByDescending { it.service.mileage })
}

@Composable
fun JournalHost(chromePadding: PaddingValues, onOpenItem: (String) -> Unit, onAddCar: () -> Unit) {
    val vm = steamViewModel { JournalViewModel(it) }
    val snapshot by vm.snapshot.collectAsStateWithLifecycle()
    var carId by rememberSaveable { mutableStateOf<String?>(null) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var period by rememberSaveable { mutableStateOf(Period.ALL.name) }
    val snap = snapshot ?: GarageSnapshot()
    val entries = remember(snap, carId, category, period) {
        snap.journal(carId, category?.let { Category.parse(it) }, Period.valueOf(period))
    }
    JournalScreen(
        snapshot = snap,
        loaded = snapshot != null,
        entries = entries,
        carId = carId,
        category = category?.let { Category.parse(it) },
        period = Period.valueOf(period),
        chromePadding = chromePadding,
        onCar = { carId = it },
        onCategory = { category = it?.name },
        onPeriod = { period = it.name },
        onOpenItem = onOpenItem,
        onAddCar = onAddCar,
    )
}

@Composable
fun JournalScreen(
    snapshot: GarageSnapshot,
    loaded: Boolean,
    entries: List<JournalEntry>,
    carId: String?,
    category: Category?,
    period: Period,
    chromePadding: PaddingValues,
    onCar: (String?) -> Unit,
    onCategory: (Category?) -> Unit,
    onPeriod: (Period) -> Unit,
    onOpenItem: (String) -> Unit,
    onAddCar: () -> Unit,
) {
    val palette = Steam.palette
    Column(Modifier.fillMaxSize()) {
        SteamTopBar(title = stringResource(R.string.tab_journal))
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 14.dp,
                bottom = 16.dp + chromePadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val allCars = stringResource(R.string.journal_all_cars)
                    FilterChip(
                        label = snapshot.cars.firstOrNull { it.id == carId }?.name ?: allCars,
                        options = listOf<Pair<String?, String>>(null to allCars) + snapshot.cars.map { it.id to it.name },
                        onSelect = onCar,
                    )
                    val allCategories = stringResource(R.string.journal_all_categories)
                    FilterChip(
                        label = category?.let { stringResource(it.label) } ?: allCategories,
                        options = listOf<Pair<Category?, String>>(null to allCategories) +
                            Category.entries.map { it to stringResource(it.label) },
                        onSelect = onCategory,
                    )
                    FilterChip(
                        label = stringResource(period.label()),
                        options = Period.entries.map { it to stringResource(it.label()) },
                        onSelect = onPeriod,
                    )
                }
            }
            if (loaded && snapshot.cars.isEmpty()) {
                item {
                    EmptyState(
                        text = stringResource(R.string.journal_empty_no_cars),
                        actionText = stringResource(R.string.garage_add_car),
                        onAction = onAddCar,
                    )
                }
            } else if (loaded && entries.isEmpty()) {
                item {
                    SteamPanel(Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.journal_empty), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                item {
                    val total = entries.sumOf { it.service.costKopecks ?: 0L }
                    Text(
                        text = stringResource(R.string.journal_summary, entries.size, Format.money(total)),
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.text,
                    )
                }
                items(entries, key = { it.service.id }) { entry ->
                    val title = listOf(entry.car.name, entry.item?.name.orEmpty())
                        .filter { it.isNotBlank() }
                        .joinToString(stringResource(R.string.separator_dot))
                    ServiceRow(
                        service = entry.service,
                        shared = entry.car.garageId != null,
                        onClick = { entry.item?.let { onOpenItem(it.id) } },
                        title = title,
                    )
                }
            }
        }
    }
}

private fun Period.label(): Int = when (this) {
    Period.ALL -> R.string.period_all
    Period.LAST_12_MONTHS -> R.string.period_last_12_months
    Period.THIS_YEAR -> R.string.period_this_year
    Period.LAST_YEAR -> R.string.period_last_year
}

/** Фильтр-табличка с выпадающим списком. */
@Composable
private fun <T> FilterChip(label: String, options: List<Pair<T, String>>, onSelect: (T) -> Unit) {
    val palette = Steam.palette
    var open by remember { mutableStateOf(false) }
    val shape = CutCornerShape(6.dp)
    Box {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = palette.text,
            modifier = Modifier
                .heightIn(min = 44.dp)
                .background(palette.surface, shape)
                .border(1.dp, palette.brassShadow, shape)
                .clickable(role = Role.DropdownList) { open = true }
                .padding(horizontal = 14.dp, vertical = 11.dp),
        )
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = palette.surface,
        ) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text, style = MaterialTheme.typography.bodyLarge, color = palette.text) },
                    onClick = {
                        open = false
                        onSelect(value)
                    },
                )
            }
        }
    }
}
