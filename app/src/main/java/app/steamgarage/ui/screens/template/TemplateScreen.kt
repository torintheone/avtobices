package app.steamgarage.ui.screens.template

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import app.steamgarage.ui.components.Sfx
import app.steamgarage.data.ItemDraft
import app.steamgarage.data.LastReplacement
import app.steamgarage.data.UserTemplate
import app.steamgarage.domain.Category
import app.steamgarage.domain.Defaults
import app.steamgarage.ui.Format
import app.steamgarage.ui.components.BrassButton
import app.steamgarage.ui.components.CategoryPlate
import app.steamgarage.ui.components.DateField
import app.steamgarage.ui.components.NumberField
import app.steamgarage.ui.components.SectionTitle
import app.steamgarage.ui.components.SteamLever
import app.steamgarage.ui.components.SteamPanel
import app.steamgarage.ui.components.SteamSwitchRow
import app.steamgarage.ui.components.ValveButton
import app.steamgarage.ui.screens.SteamScreen
import app.steamgarage.ui.screens.steamViewModel
import app.steamgarage.ui.theme.Steam
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Строка шаблона на экране выбора. */
data class TemplateRow(
    val name: String,
    val category: Category,
    val km: String,
    val months: String,
    val checked: Boolean,
    val warnKm: Int? = null,
    val warnDays: Int? = null,
    val defaultPart: String = "",
    /** Ответ на «Когда меняли в последний раз?». */
    val knowLast: Boolean = false,
    val lastDate: LocalDate = LocalDate.now(),
    val lastKm: String = "",
) {
    val hasInterval: Boolean get() = (Format.parseInt(km) ?: 0) > 0 || (Format.parseInt(months) ?: 0) > 0
}

class TemplateViewModel(private val container: AppContainer, private val carId: String) : ViewModel() {
    private val repo = container.repository
    val userTemplates: StateFlow<List<UserTemplate>> =
        repo.userTemplates.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val rows = mutableStateListOf<TemplateRow>()
    var source by mutableStateOf<String?>(null)
        private set
    var step by mutableIntStateOf(1)
    var currentKm by mutableStateOf<Int?>(null)
        private set

    init {
        useDefault()
        viewModelScope.launch { currentKm = repo.currentKm(carId) }
    }

    fun useDefault() {
        source = null
        rows.clear()
        Defaults.TEMPLATE.forEach { entry ->
            rows += TemplateRow(
                name = container.appContext.getString(templateName(entry.key)),
                category = entry.category,
                km = entry.km?.toString().orEmpty(),
                months = entry.months?.toString().orEmpty(),
                checked = entry.km != null || entry.months != null,
            )
        }
    }

    fun useTemplate(template: UserTemplate) {
        source = template.id
        rows.clear()
        template.items.forEach { item ->
            rows += TemplateRow(
                name = item.name,
                category = Category.parse(item.category),
                km = item.intervalKm?.toString().orEmpty(),
                months = item.intervalMonths?.toString().orEmpty(),
                checked = true,
                warnKm = item.warnKm,
                warnDays = item.warnDays,
                defaultPart = item.defaultPart,
            )
        }
    }

    fun update(index: Int, transform: (TemplateRow) -> TemplateRow) {
        if (index in rows.indices) rows[index] = transform(rows[index])
    }

    /** Все отмеченные строки имеют хотя бы один интервал. */
    fun checkedValid(): Boolean = rows.filter { it.checked }.all { it.hasInterval }

    fun applyToAll(date: LocalDate, km: String) {
        rows.indices.forEach { i -> rows[i] = rows[i].copy(knowLast = true, lastDate = date, lastKm = km) }
    }

    fun finish(done: () -> Unit) {
        val entries = rows.filter { it.checked && it.hasInterval }.map { row ->
            val draft = ItemDraft(
                name = row.name,
                category = row.category.name,
                intervalKm = Format.parseInt(row.km)?.takeIf { it > 0 },
                intervalMonths = Format.parseInt(row.months)?.takeIf { it > 0 },
                warnKm = row.warnKm,
                warnDays = row.warnDays,
                defaultPart = row.defaultPart,
            )
            val last = Format.parseInt(row.lastKm)?.takeIf { row.knowLast }?.let { LastReplacement(row.lastDate, it) }
            draft to last
        }
        viewModelScope.launch {
            if (entries.isNotEmpty()) {
                repo.addItems(carId, entries)
                container.sound.play(Sfx.STEAM)
            }
            done()
        }
    }

    companion object {
        /** Названия расходников шаблона лежат в strings.xml. */
        fun templateName(key: String): Int = when (key) {
            "engine_oil" -> R.string.template_engine_oil
            "oil_filter" -> R.string.template_oil_filter
            "air_filter" -> R.string.template_air_filter
            "cabin_filter" -> R.string.template_cabin_filter
            "fuel_filter" -> R.string.template_fuel_filter
            "spark_plugs" -> R.string.template_spark_plugs
            "timing_belt" -> R.string.template_timing_belt
            "drive_belt" -> R.string.template_drive_belt
            "brake_fluid" -> R.string.template_brake_fluid
            "coolant" -> R.string.template_coolant
            "gearbox_oil" -> R.string.template_gearbox_oil
            "front_brake_pads" -> R.string.template_front_brake_pads
            "wiper_blades" -> R.string.template_wiper_blades
            else -> R.string.template_unknown
        }
    }
}

@Composable
fun TemplateHost(
    carId: String,
    chromePadding: PaddingValues,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val vm = steamViewModel(key = "template-$carId") { TemplateViewModel(it, carId) }
    val templates by vm.userTemplates.collectAsStateWithLifecycle()
    var showErrors by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = vm.step == 2) { vm.step = 1 }

    if (vm.step == 1) {
        SteamScreen(
            title = stringResource(R.string.template_title),
            chromePadding = chromePadding,
            onBack = onBack,
            hasBottomBar = false,
        ) {
            Text(stringResource(R.string.template_hint), style = MaterialTheme.typography.bodyMedium)
            if (templates.isNotEmpty()) {
                SourcePicker(
                    templates = templates,
                    selected = vm.source,
                    onDefault = { vm.useDefault() },
                    onTemplate = { vm.useTemplate(it) },
                )
            }
            vm.rows.forEachIndexed { index, row ->
                TemplateRowCard(
                    row = row,
                    showErrors = showErrors,
                    onChange = { transform -> vm.update(index, transform) },
                )
            }
            SteamLever(
                text = stringResource(R.string.template_next),
                onPull = {
                    showErrors = true
                    if (vm.checkedValid()) {
                        showErrors = false
                        if (vm.rows.any { it.checked }) vm.step = 2 else vm.finish(onDone)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            ValveButton(text = stringResource(R.string.template_skip), onClick = { vm.finish(onDone) })
        }
    } else {
        LastReplacedStep(vm = vm, chromePadding = chromePadding, onBack = { vm.step = 1 }, onDone = onDone)
    }
}

@Composable
private fun SourcePicker(
    templates: List<UserTemplate>,
    selected: String?,
    onDefault: () -> Unit,
    onTemplate: (UserTemplate) -> Unit,
) {
    val palette = Steam.palette
    SectionTitle(stringResource(R.string.template_source))
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val options = listOf<Pair<String?, String>>(null to stringResource(R.string.template_default)) +
            templates.map { it.id to it.name }
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
                    .selectable(selected = isSelected, role = Role.RadioButton) {
                        if (id == null) onDefault() else templates.firstOrNull { it.id == id }?.let(onTemplate)
                    }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
            )
        }
    }
}

@Composable
private fun TemplateRowCard(
    row: TemplateRow,
    showErrors: Boolean,
    onChange: ((TemplateRow) -> TemplateRow) -> Unit,
) {
    val palette = Steam.palette
    SteamPanel(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(value = row.checked, role = Role.Checkbox) { checked -> onChange { it.copy(checked = checked) } },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = row.checked, onCheckedChange = null)
            Spacer(Modifier.width(6.dp))
            CategoryPlate(row.category, size = 30.dp)
            Spacer(Modifier.width(10.dp))
            Text(row.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        }
        if (row.checked) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField(
                    value = row.km,
                    onValueChange = { v -> onChange { it.copy(km = v) } },
                    label = stringResource(R.string.item_interval_km_label),
                    suffix = stringResource(R.string.unit_km),
                    modifier = Modifier.weight(1f),
                )
                NumberField(
                    value = row.months,
                    onValueChange = { v -> onChange { it.copy(months = v) } },
                    label = stringResource(R.string.item_interval_months_label),
                    suffix = stringResource(R.string.unit_months),
                    maxDigits = 3,
                    modifier = Modifier.weight(1f),
                )
            }
            if (!row.hasInterval) {
                Text(
                    text = stringResource(R.string.template_fill_by_manual),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (showErrors) palette.errorText else palette.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun LastReplacedStep(
    vm: TemplateViewModel,
    chromePadding: PaddingValues,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val palette = Steam.palette
    var allDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var allKm by rememberSaveable { mutableStateOf(vm.currentKm?.toString().orEmpty()) }
    SteamScreen(
        title = stringResource(R.string.last_replaced_title),
        chromePadding = chromePadding,
        onBack = onBack,
        hasBottomBar = false,
    ) {
        Text(stringResource(R.string.last_replaced_hint), style = MaterialTheme.typography.bodyMedium)
        SteamPanel(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.last_replaced_all_title), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.width(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField(date = allDate, onDateChange = { allDate = it }, label = stringResource(R.string.service_date))
                NumberField(
                    value = allKm,
                    onValueChange = { allKm = it },
                    label = stringResource(R.string.service_km),
                    suffix = stringResource(R.string.unit_km),
                )
                BrassButton(
                    text = stringResource(R.string.last_replaced_apply_all),
                    enabled = Format.parseInt(allKm) != null,
                    onClick = { vm.applyToAll(allDate, allKm) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        vm.rows.forEachIndexed { index, row ->
            if (!row.checked || !row.hasInterval) return@forEachIndexed
            SteamPanel(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryPlate(row.category, size = 30.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(row.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                }
                SteamSwitchRow(
                    text = stringResource(R.string.last_replaced_known),
                    checked = row.knowLast,
                    onCheckedChange = { known -> vm.update(index) { it.copy(knowLast = known) } },
                )
                if (row.knowLast) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                        DateField(
                            date = row.lastDate,
                            onDateChange = { d -> vm.update(index) { it.copy(lastDate = d) } },
                            label = stringResource(R.string.service_date),
                        )
                        NumberField(
                            value = row.lastKm,
                            onValueChange = { v -> vm.update(index) { it.copy(lastKm = v) } },
                            label = stringResource(R.string.service_km),
                            suffix = stringResource(R.string.unit_km),
                        )
                    }
                } else {
                    Text(stringResource(R.string.last_replaced_unknown_hint), style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                }
            }
        }
        SteamLever(text = stringResource(R.string.template_done), onPull = { vm.finish(onDone) }, modifier = Modifier.fillMaxWidth())
    }
}
