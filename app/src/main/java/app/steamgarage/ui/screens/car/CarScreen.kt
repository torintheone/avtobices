package app.steamgarage.ui.screens.car

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import app.steamgarage.data.CarSummary
import app.steamgarage.data.ItemSummary
import app.steamgarage.data.db.ServiceEntity
import app.steamgarage.data.summarize
import app.steamgarage.ui.Format
import app.steamgarage.ui.components.BrassButton
import app.steamgarage.ui.components.BrassPlate
import app.steamgarage.ui.components.CarBadge
import app.steamgarage.ui.components.CategoryPlate
import app.steamgarage.ui.components.EmptyState
import app.steamgarage.ui.components.EngravedText
import app.steamgarage.ui.components.OdometerDisplay
import app.steamgarage.ui.components.PipeDivider
import app.steamgarage.ui.components.SteamDialog
import app.steamgarage.ui.components.SteamGauge
import app.steamgarage.ui.components.SteamPanel
import app.steamgarage.ui.components.SteamTextField
import app.steamgarage.ui.components.StatusBadge
import app.steamgarage.ui.components.ValveButton
import app.steamgarage.ui.components.statusLabel
import app.steamgarage.ui.navigation.CarAction
import app.steamgarage.ui.screens.SteamScreen
import app.steamgarage.ui.screens.garage.carSubtitle
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

data class CarState(val loaded: Boolean = false, val summary: CarSummary? = null)

class CarViewModel(private val container: AppContainer, private val carId: String) : ViewModel() {
    private val repo = container.repository

    val state: StateFlow<CarState> =
        combine(repo.snapshot, container.settings.settings) { snapshot, settings ->
            val car = snapshot.copy(cars = snapshot.cars.filter { it.id == carId })
                .summarize(LocalDate.now(), settings.thresholds)
                .firstOrNull()
            CarState(loaded = true, summary = car)
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CarState())

    fun addMileage(km: Int, correctPast: Boolean) {
        viewModelScope.launch { repo.addMileage(carId, km, LocalDate.now(), correctPast) }
    }

    fun recordService(form: ServiceForm) {
        viewModelScope.launch { repo.recordService(carId, form.lines, form.date, form.km, form.place, form.note) }
    }

    fun updateService(service: ServiceEntity, form: ServiceForm) {
        val line = form.lines.firstOrNull() ?: return
        viewModelScope.launch {
            repo.updateService(service.id, form.date, form.km, line.costKopecks, line.part, form.place, form.note)
        }
    }

    fun deleteService(service: ServiceEntity) {
        viewModelScope.launch { repo.deleteService(service.id) }
    }

    fun saveTemplate(name: String) {
        viewModelScope.launch { repo.saveTemplate(name, carId) }
    }
}

@Composable
fun CarHost(
    carId: String,
    highlightItemId: String?,
    action: String?,
    actionItems: String?,
    chromePadding: PaddingValues,
    onBack: () -> Unit,
    onEditCar: () -> Unit,
    onOpenItem: (String) -> Unit,
    onAddItem: () -> Unit,
    onAddFromTemplate: () -> Unit,
) {
    val vm = steamViewModel(key = "car-$carId") { CarViewModel(it, carId) }
    val state by vm.state.collectAsStateWithLifecycle()
    var mileageOpen by rememberSaveable { mutableStateOf(false) }
    var serviceRequest by remember { mutableStateOf<ServiceRequest?>(null) }
    var actionHandled by rememberSaveable { mutableStateOf(false) }
    var templateDialog by rememberSaveable { mutableStateOf(false) }

    val summary = state.summary
    LaunchedEffect(state.loaded, summary == null) {
        if (state.loaded && summary == null) onBack()
    }
    LaunchedEffect(summary != null, action) {
        if (summary != null && !actionHandled && action != null) {
            actionHandled = true
            when (action) {
                CarAction.MILEAGE -> mileageOpen = true
                CarAction.SERVICE -> {
                    val ids = actionItems.orEmpty().split(',').filter { id -> summary.items.any { it.item.id == id } }
                    if (ids.isNotEmpty()) serviceRequest = ServiceRequest.New(ids, batch = ids.size > 1)
                }
            }
        }
    }
    if (summary == null) return

    CarScreen(
        summary = summary,
        highlightItemId = highlightItemId,
        chromePadding = chromePadding,
        onBack = onBack,
        onEditCar = onEditCar,
        onOpenItem = onOpenItem,
        onAddItem = onAddItem,
        onAddFromTemplate = onAddFromTemplate,
        onUpdateMileage = { mileageOpen = true },
        onBatchService = {
            serviceRequest = ServiceRequest.New(emptyList(), batch = true)
        },
        onSaveTemplate = { templateDialog = true },
    )

    if (mileageOpen) {
        MileageDialog(
            currentKm = summary.currentKm,
            onDismiss = { mileageOpen = false },
            onSave = { km, correct -> vm.addMileage(km, correct) },
        )
    }
    serviceRequest?.let { request ->
        ServiceSheet(
            request = request,
            items = summary.items,
            currentKm = summary.currentKm,
            onDismiss = { serviceRequest = null },
            onSubmit = { form -> vm.recordService(form) },
        )
    }
    if (templateDialog) {
        SaveTemplateDialog(
            defaultName = summary.car.name,
            onDismiss = { templateDialog = false },
            onSave = { vm.saveTemplate(it) },
        )
    }
}

@Composable
fun CarScreen(
    summary: CarSummary,
    highlightItemId: String?,
    chromePadding: PaddingValues,
    onBack: () -> Unit,
    onEditCar: () -> Unit,
    onOpenItem: (String) -> Unit,
    onAddItem: () -> Unit,
    onAddFromTemplate: () -> Unit,
    onUpdateMileage: () -> Unit,
    onBatchService: () -> Unit,
    onSaveTemplate: () -> Unit,
) {
    val car = summary.car
    SteamScreen(
        title = car.name,
        chromePadding = chromePadding,
        onBack = onBack,
        hasBottomBar = false,
    ) {
        BrassPlate(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CarBadge(car.color)
                Spacer(Modifier.width(10.dp))
                EngravedText(car.name, style = Steam.text.plateTitle, maxLines = 1, modifier = Modifier.weight(1f))
            }
            val subtitle = carSubtitle(car.plate, car.make, car.model, stringResource(R.string.separator_dot))
            if (subtitle.isNotEmpty()) EngravedText(subtitle, style = Steam.text.plateCaption)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val km = summary.currentKm
                if (km != null) {
                    OdometerDisplay(Format.km(km), style = Steam.text.odometer)
                    Spacer(Modifier.width(8.dp))
                    EngravedText(stringResource(R.string.unit_km), style = Steam.text.plateCaption)
                } else {
                    EngravedText(stringResource(R.string.car_no_mileage), style = Steam.text.plateCaption)
                }
            }
            summary.averageDailyKm?.let {
                EngravedText(
                    stringResource(R.string.car_average_km, Format.km(it.toInt().coerceAtLeast(1))),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(10.dp))
            BrassButton(text = stringResource(R.string.car_update_mileage), onClick = onUpdateMileage, modifier = Modifier.fillMaxWidth())
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ValveButton(
                text = stringResource(R.string.car_batch_service),
                onClick = onBatchService,
                enabled = summary.activeItems.isNotEmpty(),
                modifier = Modifier.weight(1f),
            )
            ValveButton(text = stringResource(R.string.car_edit), onClick = onEditCar, modifier = Modifier.weight(1f))
        }

        PipeDivider()

        if (summary.items.isEmpty()) {
            EmptyState(
                text = stringResource(R.string.car_no_items),
                actionText = stringResource(R.string.car_add_from_template),
                onAction = onAddFromTemplate,
            )
        } else {
            summary.items.forEach { item ->
                ItemCard(item, highlighted = item.item.id == highlightItemId, onClick = { onOpenItem(item.item.id) })
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ValveButton(text = stringResource(R.string.car_add_item), onClick = onAddItem, modifier = Modifier.weight(1f))
            if (summary.items.isNotEmpty()) {
                ValveButton(text = stringResource(R.string.car_add_from_template_short), onClick = onAddFromTemplate, modifier = Modifier.weight(1f))
            }
        }
        if (summary.activeItems.isNotEmpty()) {
            ValveButton(text = stringResource(R.string.car_save_template), onClick = onSaveTemplate)
        }
    }
}

/** Карточка расходника: табличка категории, название, статус, манометр и остаток. */
@Composable
fun ItemCard(summary: ItemSummary, highlighted: Boolean, onClick: () -> Unit) {
    val palette = Steam.palette
    val inactive = !summary.item.active
    SteamPanel(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        onClickLabel = stringResource(R.string.item_open),
        highlighted = highlighted,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryPlate(summary.category, size = 32.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                text = summary.item.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                maxLines = 2,
            )
            Spacer(Modifier.width(8.dp))
            if (inactive) {
                Text(stringResource(R.string.item_inactive), style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
            } else {
                StatusBadge(summary.state.status)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SteamGauge(
                reading = gaugeReadingFor(summary.state),
                modifier = Modifier.size(116.dp),
                contentDescription = stringResource(R.string.gauge_description, summary.item.name, statusLabel(summary.state.status)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                itemDetailLines(summary).forEach { line ->
                    Text(line, style = MaterialTheme.typography.bodySmall, color = palette.text)
                }
            }
        }
    }
}

@Composable
fun SaveTemplateDialog(defaultName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(defaultName) }
    SteamDialog(
        title = stringResource(R.string.template_save_title),
        onDismiss = onDismiss,
        buttons = {
            ValveButton(text = stringResource(R.string.action_cancel), onClick = onDismiss)
            BrassButton(
                text = stringResource(R.string.action_save),
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(name)
                    onDismiss()
                },
            )
        },
    ) {
        Text(stringResource(R.string.template_save_text), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        SteamTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.template_name))
    }
}
