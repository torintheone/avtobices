package app.steamgarage.ui.screens.car

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.steamgarage.R
import app.steamgarage.data.ItemSummary
import app.steamgarage.data.ServiceLine
import app.steamgarage.data.db.Place
import app.steamgarage.data.db.ServiceEntity
import app.steamgarage.ui.Format
import app.steamgarage.ui.components.BrassButton
import app.steamgarage.ui.components.CategoryPlate
import app.steamgarage.ui.components.ConfirmDialog
import app.steamgarage.ui.components.DateField
import app.steamgarage.ui.components.MoneyField
import app.steamgarage.ui.components.NumberField
import app.steamgarage.ui.components.SteamLever
import app.steamgarage.ui.components.SteamTextField
import app.steamgarage.ui.theme.Steam
import java.time.LocalDate

/** Что открыть в форме замены. */
sealed interface ServiceRequest {
    /** Новая замена: один расходник или «Провести ТО» с отмеченными. */
    data class New(val itemIds: List<String>, val batch: Boolean) : ServiceRequest

    /** Правка существующей записи. */
    data class Edit(val service: ServiceEntity) : ServiceRequest
}

/** Данные, которые форма отдаёт наружу. */
data class ServiceForm(
    val lines: List<ServiceLine>,
    val date: LocalDate,
    val km: Int,
    val place: Place?,
    val note: String,
)

/**
 * Форма замены — нижняя панель: дата (сегодня), пробег (текущий), стоимость, артикул (по умолчанию),
 * место, заметка. В режиме «Провести ТО» — список расходников с галочками и своей стоимостью.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceSheet(
    request: ServiceRequest,
    items: List<ItemSummary>,
    currentKm: Int?,
    onDismiss: () -> Unit,
    onSubmit: (ServiceForm) -> Unit,
    onDelete: (ServiceEntity) -> Unit = {},
) {
    val palette = Steam.palette
    val editing = (request as? ServiceRequest.Edit)?.service
    val batch = request is ServiceRequest.New && request.batch
    val initialIds = when (request) {
        is ServiceRequest.New -> request.itemIds
        is ServiceRequest.Edit -> listOf(request.service.itemId)
    }
    val singleItem = items.firstOrNull { it.item.id == initialIds.firstOrNull() }

    var date by rememberSaveable { mutableStateOf(editing?.let { LocalDate.ofEpochDay(it.date) } ?: LocalDate.now()) }
    var kmText by rememberSaveable { mutableStateOf((editing?.mileage ?: currentKm)?.toString().orEmpty()) }
    var costText by rememberSaveable { mutableStateOf(Format.moneyInput(editing?.costKopecks)) }
    var part by rememberSaveable { mutableStateOf(editing?.part ?: singleItem?.item?.defaultPart.orEmpty()) }
    var place by rememberSaveable { mutableStateOf(editing?.place?.takeIf { it.isNotEmpty() }) }
    var note by rememberSaveable { mutableStateOf(editing?.note.orEmpty()) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val checked = remember { mutableStateMapOf<String, Boolean>().apply { initialIds.forEach { put(it, true) } } }
    val costs = remember { mutableStateMapOf<String, String>() }

    val km = Format.parseInt(kmText)
    val kmError = if (showErrors && (km == null || km <= 0)) stringResource(R.string.service_error_km) else null
    val costError = if (costText.isNotBlank() && Format.parseMoney(costText) == null) stringResource(R.string.service_error_cost) else null
    val selected = if (batch) items.filter { checked[it.item.id] == true } else listOfNotNull(singleItem)
    val nothingSelected = showErrors && selected.isEmpty()
    val badCostText = stringResource(R.string.service_error_cost)
    fun lineCostError(itemId: String): String? =
        costs[itemId].orEmpty().takeIf { it.isNotBlank() && Format.parseMoney(it) == null }?.let { badCostText }
    val batchCostInvalid = batch && selected.any { lineCostError(it.item.id) != null }

    fun submit() {
        showErrors = true
        val kmValue = km ?: return
        if (kmValue <= 0 || selected.isEmpty() || costError != null || batchCostInvalid) return
        val lines = if (batch) {
            selected.map { s ->
                ServiceLine(s.item.id, costs[s.item.id]?.let { Format.parseMoney(it) }, s.item.defaultPart)
            }
        } else {
            listOf(ServiceLine(selected.first().item.id, Format.parseMoney(costText), part))
        }
        onSubmit(ServiceForm(lines, date, kmValue, place?.let { p -> Place.entries.firstOrNull { it.name == p } }, note))
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = palette.surface,
        contentColor = palette.text,
        dragHandle = { BottomSheetDefaults.DragHandle(color = palette.brassDark) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = when {
                    editing != null -> stringResource(R.string.service_edit_title, singleItem?.item?.name.orEmpty())
                    batch -> stringResource(R.string.service_batch_title)
                    else -> stringResource(R.string.service_title, singleItem?.item?.name.orEmpty())
                },
                style = MaterialTheme.typography.titleLarge,
            )

            if (batch) {
                Text(
                    text = stringResource(R.string.service_batch_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (nothingSelected) palette.errorText else palette.textSecondary,
                )
                items.filter { it.item.active }.forEach { s ->
                    val isChecked = checked[s.item.id] == true
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .toggleable(value = isChecked, role = Role.Checkbox) { checked[s.item.id] = it },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = isChecked, onCheckedChange = null)
                        Spacer(Modifier.width(6.dp))
                        CategoryPlate(s.category, size = 30.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(s.item.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    }
                    if (isChecked) {
                        MoneyField(
                            value = costs[s.item.id].orEmpty(),
                            onValueChange = { costs[s.item.id] = it },
                            label = stringResource(R.string.service_cost_for, s.item.name),
                            errorText = lineCostError(s.item.id),
                            modifier = Modifier.padding(start = 48.dp),
                        )
                    }
                }
            }

            DateField(date = date, onDateChange = { date = it }, label = stringResource(R.string.service_date))
            NumberField(
                value = kmText,
                onValueChange = { kmText = it },
                label = stringResource(R.string.service_km),
                suffix = stringResource(R.string.unit_km),
                errorText = kmError,
                supportingText = currentKm?.let { stringResource(R.string.service_km_current, Format.km(it)) },
            )
            if (!batch) {
                MoneyField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = stringResource(R.string.service_cost),
                    errorText = costError,
                )
                SteamTextField(
                    value = part,
                    onValueChange = { part = it },
                    label = stringResource(R.string.service_part),
                    capitalization = KeyboardCapitalization.Characters,
                )
            }

            Text(stringResource(R.string.service_place), style = MaterialTheme.typography.labelLarge)
            Row(Modifier.selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
                listOf(Place.SERVICE to R.string.service_place_service, Place.SELF to R.string.service_place_self).forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .selectable(
                                selected = place == value.name,
                                role = Role.RadioButton,
                                onClick = { place = if (place == value.name) null else value.name },
                            )
                            .padding(end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = place == value.name, onClick = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            SteamTextField(
                value = note,
                onValueChange = { note = it },
                label = stringResource(R.string.service_note),
                singleLine = false,
                minLines = 2,
            )

            if (editing != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    BrassButton(
                        text = stringResource(R.string.action_delete),
                        danger = true,
                        onClick = { confirmDelete = true },
                    )
                    Spacer(Modifier.weight(1f))
                    BrassButton(text = stringResource(R.string.action_save), onClick = { submit() })
                }
            } else {
                SteamLever(
                    text = stringResource(R.string.service_submit),
                    onPull = { submit() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.heightIn(min = 12.dp))
        }
    }

    if (confirmDelete && editing != null) {
        ConfirmDialog(
            title = stringResource(R.string.service_delete_title),
            text = stringResource(R.string.service_delete_text),
            confirmText = stringResource(R.string.action_delete),
            danger = true,
            onConfirm = {
                onDelete(editing)
                onDismiss()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
