package app.steamgarage.ui.screens.item

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.steamgarage.AppContainer
import app.steamgarage.R
import app.steamgarage.data.ItemDraft
import app.steamgarage.data.LastReplacement
import app.steamgarage.data.db.ItemEntity
import app.steamgarage.domain.Category
import app.steamgarage.domain.ThresholdSettings
import app.steamgarage.ui.Format
import app.steamgarage.ui.components.BrassButton
import app.steamgarage.ui.components.CategoryPlate
import app.steamgarage.ui.components.ConfirmDialog
import app.steamgarage.ui.components.DateField
import app.steamgarage.ui.components.NumberField
import app.steamgarage.ui.components.SectionTitle
import app.steamgarage.ui.components.SteamLever
import app.steamgarage.ui.components.SteamPanel
import app.steamgarage.ui.components.SteamSwitchRow
import app.steamgarage.ui.components.SteamTextField
import app.steamgarage.ui.components.label
import app.steamgarage.ui.screens.SteamScreen
import app.steamgarage.ui.screens.steamViewModel
import app.steamgarage.ui.theme.Steam
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ItemEditInitial(
    val item: ItemEntity?,
    val thresholds: ThresholdSettings,
    val currentKm: Int?,
)

class ItemEditViewModel(
    private val container: AppContainer,
    private val carId: String,
    private val itemId: String?,
) : ViewModel() {
    private val repo = container.repository
    private val _initial = MutableStateFlow<ItemEditInitial?>(null)
    val initial: StateFlow<ItemEditInitial?> = _initial

    init {
        viewModelScope.launch {
            val item = itemId?.let { container.database.items().get(it) }?.takeIf { !it.deleted }
            _initial.value = ItemEditInitial(
                item = item,
                thresholds = container.settings.current().thresholds,
                currentKm = repo.currentKm(carId),
            )
        }
    }

    fun save(draft: ItemDraft, last: LastReplacement?, done: () -> Unit) {
        viewModelScope.launch {
            if (itemId == null) repo.addItems(carId, listOf(draft to last)) else repo.updateItem(itemId, draft)
            done()
        }
    }

    fun delete(done: () -> Unit) {
        val id = itemId ?: return
        viewModelScope.launch {
            repo.deleteItem(id)
            done()
        }
    }
}

@Composable
fun ItemEditHost(
    carId: String,
    itemId: String?,
    chromePadding: PaddingValues,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
) {
    val vm = steamViewModel(key = "item-edit-$carId-$itemId") { ItemEditViewModel(it, carId, itemId) }
    val initial by vm.initial.collectAsStateWithLifecycle()
    val init = initial ?: return
    LaunchedEffect(init) { if (itemId != null && init.item == null) onBack() }
    ItemEditScreen(
        initial = init,
        isNew = itemId == null,
        chromePadding = chromePadding,
        onBack = onBack,
        onSave = { draft, last -> vm.save(draft, last, onBack) },
        onDelete = { vm.delete(onDeleted) },
    )
}

@Composable
fun ItemEditScreen(
    initial: ItemEditInitial,
    isNew: Boolean,
    chromePadding: PaddingValues,
    onBack: () -> Unit,
    onSave: (ItemDraft, LastReplacement?) -> Unit,
    onDelete: () -> Unit,
) {
    val palette = Steam.palette
    val item = initial.item
    var name by rememberSaveable { mutableStateOf(item?.name.orEmpty()) }
    var category by rememberSaveable { mutableStateOf(item?.category ?: Category.OTHER.name) }
    var km by rememberSaveable { mutableStateOf(item?.intervalKm?.toString().orEmpty()) }
    var months by rememberSaveable { mutableStateOf(item?.intervalMonths?.toString().orEmpty()) }
    var warnKm by rememberSaveable { mutableStateOf(item?.warnKm?.toString().orEmpty()) }
    var warnDays by rememberSaveable { mutableStateOf(item?.warnDays?.toString().orEmpty()) }
    var part by rememberSaveable { mutableStateOf(item?.defaultPart.orEmpty()) }
    var note by rememberSaveable { mutableStateOf(item?.note.orEmpty()) }
    var active by rememberSaveable { mutableStateOf(item?.active ?: true) }
    var knowLast by rememberSaveable { mutableStateOf(false) }
    var lastDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var lastKm by rememberSaveable { mutableStateOf(initial.currentKm?.toString().orEmpty()) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val kmValue = Format.parseInt(km)?.takeIf { it > 0 }
    val monthsValue = Format.parseInt(months)?.takeIf { it > 0 }
    val nameError = if (showErrors && name.isBlank()) stringResource(R.string.error_required) else null
    val intervalError = if (showErrors && kmValue == null && monthsValue == null) stringResource(R.string.item_error_interval) else null
    val lastKmError = if (showErrors && knowLast && Format.parseInt(lastKm) == null) stringResource(R.string.service_error_km) else null

    fun submit() {
        showErrors = true
        if (name.isBlank() || (kmValue == null && monthsValue == null)) return
        val last = if (isNew && knowLast) {
            val kmLast = Format.parseInt(lastKm) ?: return
            LastReplacement(lastDate, kmLast)
        } else {
            null
        }
        onSave(
            ItemDraft(
                name = name,
                category = category,
                intervalKm = kmValue,
                intervalMonths = monthsValue,
                warnKm = Format.parseInt(warnKm)?.takeIf { it > 0 },
                warnDays = Format.parseInt(warnDays)?.takeIf { it > 0 },
                defaultPart = part,
                note = note,
                active = active,
            ),
            last,
        )
    }

    SteamScreen(
        title = stringResource(if (isNew) R.string.item_new_title else R.string.item_edit_title),
        chromePadding = chromePadding,
        onBack = onBack,
        hasBottomBar = false,
    ) {
        SteamTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.item_name), errorText = nameError)

        SectionTitle(stringResource(R.string.item_category))
        CategoryPicker(selected = Category.parse(category), onSelect = { category = it.name })

        SectionTitle(stringResource(R.string.item_interval))
        Text(stringResource(R.string.item_interval_hint), style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField(
                value = km,
                onValueChange = { km = it },
                label = stringResource(R.string.item_interval_km_label),
                suffix = stringResource(R.string.unit_km),
                modifier = Modifier.weight(1f),
                errorText = intervalError,
            )
            NumberField(
                value = months,
                onValueChange = { months = it },
                label = stringResource(R.string.item_interval_months_label),
                suffix = stringResource(R.string.unit_months),
                maxDigits = 3,
                modifier = Modifier.weight(1f),
            )
        }

        SectionTitle(stringResource(R.string.item_warn))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField(
                value = warnKm,
                onValueChange = { warnKm = it },
                label = stringResource(R.string.item_warn_km_label),
                suffix = stringResource(R.string.unit_km),
                supportingText = kmValue?.let { stringResource(R.string.item_warn_auto, Format.km(initial.thresholds.kmFor(it))) },
                modifier = Modifier.weight(1f),
            )
            NumberField(
                value = warnDays,
                onValueChange = { warnDays = it },
                label = stringResource(R.string.item_warn_days_label),
                suffix = stringResource(R.string.unit_days),
                maxDigits = 3,
                supportingText = monthsValue?.let { stringResource(R.string.item_warn_auto, initial.thresholds.daysFor(it).toString()) },
                modifier = Modifier.weight(1f),
            )
        }

        SteamTextField(
            value = part,
            onValueChange = { part = it },
            label = stringResource(R.string.item_default_part),
            capitalization = KeyboardCapitalization.Characters,
        )
        SteamTextField(value = note, onValueChange = { note = it }, label = stringResource(R.string.item_note), singleLine = false, minLines = 2)
        SteamPanel(Modifier.fillMaxWidth()) {
            SteamSwitchRow(
                text = stringResource(R.string.item_active),
                supportingText = stringResource(R.string.item_active_hint),
                checked = active,
                onCheckedChange = { active = it },
            )
        }

        if (isNew) {
            SectionTitle(stringResource(R.string.last_replaced_title))
            SteamPanel(Modifier.fillMaxWidth()) {
                SteamSwitchRow(
                    text = stringResource(R.string.last_replaced_known),
                    supportingText = stringResource(R.string.last_replaced_unknown_hint),
                    checked = knowLast,
                    onCheckedChange = { knowLast = it },
                )
                if (knowLast) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateField(date = lastDate, onDateChange = { lastDate = it }, label = stringResource(R.string.service_date))
                        NumberField(
                            value = lastKm,
                            onValueChange = { lastKm = it },
                            label = stringResource(R.string.service_km),
                            suffix = stringResource(R.string.unit_km),
                            errorText = lastKmError,
                        )
                    }
                }
            }
        }

        SteamLever(text = stringResource(R.string.action_save), onPull = { submit() }, modifier = Modifier.fillMaxWidth())
        if (!isNew) {
            BrassButton(
                text = stringResource(R.string.item_delete),
                danger = true,
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.item_delete_title),
            text = stringResource(R.string.item_delete_text, name),
            confirmText = stringResource(R.string.action_delete),
            danger = true,
            onConfirm = onDelete,
            onDismiss = { confirmDelete = false },
        )
    }
}

/** Выбор категории: таблички с подписями, по три в ряд. */
@Composable
fun CategoryPicker(selected: Category, onSelect: (Category) -> Unit) {
    val palette = Steam.palette
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Category.entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { category ->
                    val isSelected = category == selected
                    val shape = CutCornerShape(6.dp)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 76.dp)
                            .background(if (isSelected) palette.surfaceDeep else palette.surface.copy(alpha = 0.6f), shape)
                            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) palette.copperLight else palette.brassShadow, shape)
                            .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(category) })
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CategoryPlate(category, size = 32.dp)
                        Text(
                            stringResource(category.label),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
