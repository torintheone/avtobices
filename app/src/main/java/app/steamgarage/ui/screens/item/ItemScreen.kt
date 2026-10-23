package app.steamgarage.ui.screens.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.steamgarage.AppContainer
import app.steamgarage.R
import app.steamgarage.sound.VoiceEvent
import app.steamgarage.ui.components.Sfx
import app.steamgarage.data.CarSummary
import app.steamgarage.data.ItemSummary
import app.steamgarage.data.db.Place
import app.steamgarage.data.db.ServiceEntity
import app.steamgarage.data.summarize
import app.steamgarage.ui.Format
import app.steamgarage.ui.components.CategoryPlate
import app.steamgarage.ui.components.PipeDivider
import app.steamgarage.ui.components.SectionTitle
import app.steamgarage.ui.components.StatusBadge
import app.steamgarage.ui.components.SteamGauge
import app.steamgarage.ui.components.SteamLever
import app.steamgarage.ui.components.SteamPanel
import app.steamgarage.ui.components.label
import app.steamgarage.ui.components.statusLabel
import app.steamgarage.ui.screens.SteamScreen
import app.steamgarage.ui.screens.car.ServiceForm
import app.steamgarage.ui.screens.car.ServiceRequest
import app.steamgarage.ui.screens.car.ServiceSheet
import app.steamgarage.ui.screens.car.gaugeReadingFor
import app.steamgarage.ui.screens.car.intervalText
import app.steamgarage.ui.screens.car.itemDetailLines
import app.steamgarage.ui.screens.steamViewModel
import app.steamgarage.ui.theme.Steam
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ItemScreenState(
    val loaded: Boolean = false,
    val car: CarSummary? = null,
    val item: ItemSummary? = null,
)

class ItemViewModel(private val container: AppContainer, private val itemId: String) : ViewModel() {
    private val repo = container.repository

    val state: StateFlow<ItemScreenState> =
        combine(repo.snapshot, container.settings.settings) { snapshot, settings ->
            val carId = snapshot.items.firstOrNull { it.id == itemId }?.carId
            val car = carId?.let { id ->
                snapshot.copy(cars = snapshot.cars.filter { it.id == id }).summarize(LocalDate.now(), settings.thresholds).firstOrNull()
            }
            ItemScreenState(loaded = true, car = car, item = car?.items?.firstOrNull { it.item.id == itemId })
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ItemScreenState())

    fun recordService(carId: String, form: ServiceForm) {
        viewModelScope.launch {
            repo.recordService(carId, form.lines, form.date, form.km, form.place, form.note)
            container.voice.say(VoiceEvent.SERVICE)
        }
    }

    fun updateService(service: ServiceEntity, form: ServiceForm) {
        val line = form.lines.firstOrNull() ?: return
        viewModelScope.launch {
            repo.updateService(service.id, form.date, form.km, line.costKopecks, line.part, form.place, form.note)
            container.sound.play(Sfx.STEAM)
        }
    }

    fun deleteService(service: ServiceEntity) {
        viewModelScope.launch {
            repo.deleteService(service.id)
            container.sound.play(Sfx.CLANG)
        }
    }
}

@Composable
fun ItemHost(
    itemId: String,
    chromePadding: PaddingValues,
    onBack: () -> Unit,
    onEdit: (carId: String) -> Unit,
) {
    val vm = steamViewModel(key = "item-$itemId") { ItemViewModel(it, itemId) }
    val state by vm.state.collectAsStateWithLifecycle()
    var request by remember { mutableStateOf<ServiceRequest?>(null) }
    LaunchedEffect(state.loaded, state.item == null) {
        if (state.loaded && state.item == null) onBack()
    }
    val car = state.car ?: return
    val item = state.item ?: return
    ItemScreen(
        car = car,
        summary = item,
        chromePadding = chromePadding,
        onBack = onBack,
        onEdit = { onEdit(car.car.id) },
        onReplaced = { request = ServiceRequest.New(listOf(item.item.id), batch = false) },
        onEditService = { request = ServiceRequest.Edit(it) },
    )
    request?.let { r ->
        ServiceSheet(
            request = r,
            items = car.items,
            currentKm = car.currentKm,
            onDismiss = { request = null },
            onSubmit = { form ->
                when (r) {
                    is ServiceRequest.New -> vm.recordService(car.car.id, form)
                    is ServiceRequest.Edit -> vm.updateService(r.service, form)
                }
            },
            onDelete = { vm.deleteService(it) },
        )
    }
}

@Composable
fun ItemScreen(
    car: CarSummary,
    summary: ItemSummary,
    chromePadding: PaddingValues,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onReplaced: () -> Unit,
    onEditService: (ServiceEntity) -> Unit,
) {
    val palette = Steam.palette
    val item = summary.item
    SteamScreen(
        title = item.name,
        chromePadding = chromePadding,
        onBack = onBack,
        hasBottomBar = false,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryPlate(summary.category, size = 36.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(summary.category.label), style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                Text(car.car.name, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
            }
            StatusBadge(summary.state.status)
        }
        SteamGauge(
            reading = gaugeReadingFor(summary.state),
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.CenterHorizontally),
            readoutStyle = Steam.text.odometer,
            contentDescription = stringResource(R.string.gauge_description, item.name, statusLabel(summary.state.status)),
        )
        SteamPanel(Modifier.fillMaxWidth()) {
            itemDetailLines(summary).forEach { line ->
                Text(line, style = MaterialTheme.typography.bodyMedium)
            }
        }
        SteamLever(
            text = stringResource(R.string.item_replaced),
            onPull = onReplaced,
            modifier = Modifier.fillMaxWidth(),
        )

        SectionTitle(stringResource(R.string.item_rules))
        SteamPanel(Modifier.fillMaxWidth(), onClick = onEdit, onClickLabel = stringResource(R.string.item_edit)) {
            InfoRow(stringResource(R.string.item_interval), intervalText(item.intervalKm, item.intervalMonths))
            val warn = listOfNotNull(
                summary.state.warnKm?.let { stringResource(R.string.item_warn_km_value, Format.km(it)) },
                summary.state.warnDays?.let { stringResource(R.string.item_warn_days_value, it) },
            ).joinToString(stringResource(R.string.separator_dot))
            if (warn.isNotEmpty()) InfoRow(stringResource(R.string.item_warn), warn)
            if (item.defaultPart.isNotBlank()) InfoRow(stringResource(R.string.item_default_part), item.defaultPart)
            if (item.note.isNotBlank()) InfoRow(stringResource(R.string.item_note), item.note)
            Text(stringResource(R.string.item_tap_to_edit), style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
        }

        PipeDivider()
        SectionTitle(stringResource(R.string.item_history))
        if (summary.services.isEmpty()) {
            Text(stringResource(R.string.item_history_empty), style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
        } else {
            val yearAgo = LocalDate.now().minusYears(1).toEpochDay()
            val total = summary.services.sumOf { it.costKopecks ?: 0L }
            val lastYear = summary.services.filter { it.date > yearAgo }.sumOf { it.costKopecks ?: 0L }
            if (total > 0) {
                Text(
                    stringResource(R.string.costs_total_and_year, Format.money(total), Format.money(lastYear)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            summary.services.forEach { service ->
                ServiceRow(service, shared = car.car.garageId != null, onClick = { onEditService(service) })
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val palette = Steam.palette
    Column(Modifier.padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Строка истории замен: дата, пробег, стоимость, артикул, место, кто записал. */
@Composable
fun ServiceRow(service: ServiceEntity, shared: Boolean, onClick: () -> Unit, title: String? = null) {
    val palette = Steam.palette
    SteamPanel(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        onClickLabel = stringResource(R.string.service_open),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) {
        if (title != null) Text(title, style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Format.date(service.date), style = Steam.text.number, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.km_value, Format.km(service.mileage)), style = Steam.text.number)
        }
        val details = listOfNotNull(
            service.costKopecks?.let { stringResource(R.string.money_value, Format.money(it)) },
            service.part.takeIf { it.isNotBlank() },
            when (service.place) {
                Place.SERVICE.name -> stringResource(R.string.service_place_service)
                Place.SELF.name -> stringResource(R.string.service_place_self)
                else -> null
            },
        ).joinToString(stringResource(R.string.separator_dot))
        if (details.isNotEmpty()) Text(details, style = MaterialTheme.typography.bodyMedium)
        if (service.note.isNotBlank()) Text(service.note, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
        if (shared && service.authorName.isNotBlank()) {
            Text(stringResource(R.string.service_author, service.authorName), style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
        }
    }
}
