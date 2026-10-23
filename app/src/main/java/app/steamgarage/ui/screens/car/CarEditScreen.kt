package app.steamgarage.ui.screens.car

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.steamgarage.AppContainer
import app.steamgarage.R
import app.steamgarage.data.CarDraft
import app.steamgarage.data.db.CarEntity
import app.steamgarage.ui.Format
import app.steamgarage.ui.components.BrassButton
import app.steamgarage.ui.components.CarBadge
import app.steamgarage.ui.components.CarColors
import app.steamgarage.ui.components.ConfirmDialog
import app.steamgarage.ui.components.NumberField
import app.steamgarage.ui.components.SectionTitle
import app.steamgarage.ui.components.SteamLever
import app.steamgarage.ui.components.SteamTextField
import app.steamgarage.ui.screens.SteamScreen
import app.steamgarage.ui.screens.steamViewModel
import app.steamgarage.ui.theme.Steam
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** null внутри — машина не найдена; сам StateFlow null — ещё грузится. */
data class CarEditInitial(val car: CarEntity?)

class CarEditViewModel(private val container: AppContainer, private val carId: String?) : ViewModel() {
    private val repo = container.repository
    private val _initial = MutableStateFlow<CarEditInitial?>(null)
    val initial: StateFlow<CarEditInitial?> = _initial

    init {
        viewModelScope.launch {
            val car = carId?.let { container.database.cars().get(it) }?.takeIf { !it.deleted }
            _initial.value = CarEditInitial(car)
        }
    }

    fun save(draft: CarDraft, done: (carId: String, created: Boolean) -> Unit) {
        viewModelScope.launch {
            if (carId == null) {
                done(repo.createCar(draft), true)
            } else {
                repo.updateCar(carId, draft)
                done(carId, false)
            }
        }
    }

    fun delete(done: () -> Unit) {
        val id = carId ?: return
        viewModelScope.launch {
            repo.deleteCar(id)
            done()
        }
    }
}

@Composable
fun CarEditHost(
    carId: String?,
    chromePadding: PaddingValues,
    onBack: () -> Unit,
    onCreated: (carId: String) -> Unit,
    onDeleted: () -> Unit,
) {
    val vm = steamViewModel(key = "car-edit-$carId") { CarEditViewModel(it, carId) }
    val initial by vm.initial.collectAsStateWithLifecycle()
    val init = initial ?: return
    LaunchedEffect(init) { if (carId != null && init.car == null) onBack() }
    CarEditScreen(
        car = init.car,
        chromePadding = chromePadding,
        onBack = onBack,
        onSave = { draft -> vm.save(draft) { id, created -> if (created) onCreated(id) else onBack() } },
        onDelete = { vm.delete(onDeleted) },
    )
}

@Composable
fun CarEditScreen(
    car: CarEntity?,
    chromePadding: PaddingValues,
    onBack: () -> Unit,
    onSave: (CarDraft) -> Unit,
    onDelete: () -> Unit,
) {
    val palette = Steam.palette
    var name by rememberSaveable { mutableStateOf(car?.name.orEmpty()) }
    var make by rememberSaveable { mutableStateOf(car?.make.orEmpty()) }
    var model by rememberSaveable { mutableStateOf(car?.model.orEmpty()) }
    var year by rememberSaveable { mutableStateOf(car?.year?.toString().orEmpty()) }
    var plate by rememberSaveable { mutableStateOf(car?.plate.orEmpty()) }
    var vin by rememberSaveable { mutableStateOf(car?.vin.orEmpty()) }
    var note by rememberSaveable { mutableStateOf(car?.note.orEmpty()) }
    var color by rememberSaveable { mutableIntStateOf(car?.color ?: 0) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val yearValue = Format.parseInt(year)
    val nameError = if (showErrors && name.isBlank()) stringResource(R.string.error_required) else null
    val yearError = if (year.isNotBlank() && (yearValue == null || yearValue !in 1886..2100)) stringResource(R.string.car_error_year) else null

    fun submit() {
        showErrors = true
        if (name.isBlank() || yearError != null) return
        onSave(CarDraft(name, make, model, yearValue, plate.uppercase(), vin.uppercase(), note, color))
    }

    SteamScreen(
        title = stringResource(if (car == null) R.string.car_new_title else R.string.car_edit_title),
        chromePadding = chromePadding,
        onBack = onBack,
        hasBottomBar = false,
    ) {
        SteamTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(R.string.car_name),
            supportingText = stringResource(R.string.car_name_hint),
            errorText = nameError,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SteamTextField(value = make, onValueChange = { make = it }, label = stringResource(R.string.car_make), modifier = Modifier.weight(1f))
            SteamTextField(value = model, onValueChange = { model = it }, label = stringResource(R.string.car_model), modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField(
                value = year,
                onValueChange = { year = it },
                label = stringResource(R.string.car_year),
                maxDigits = 4,
                errorText = yearError,
                modifier = Modifier.weight(1f),
            )
            SteamTextField(
                value = plate,
                onValueChange = { plate = it },
                label = stringResource(R.string.car_plate),
                capitalization = KeyboardCapitalization.Characters,
                modifier = Modifier.weight(1.4f),
            )
        }
        SteamTextField(
            value = vin,
            onValueChange = { if (it.length <= 17) vin = it },
            label = stringResource(R.string.car_vin),
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = KeyboardType.Ascii,
        )
        SteamTextField(value = note, onValueChange = { note = it }, label = stringResource(R.string.car_note), singleLine = false, minLines = 2)

        SectionTitle(stringResource(R.string.car_color))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CarColors.indices.forEach { index ->
                val selected = index == color
                val description = stringResource(R.string.car_color_option, index + 1)
                CarBadge(
                    color = index,
                    size = 36.dp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .border(if (selected) 3.dp else 0.dp, if (selected) palette.copperLight else palette.background.copy(alpha = 0f), CircleShape)
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { color = index })
                        .padding(4.dp)
                        .semantics { contentDescription = description },
                )
            }
        }

        SteamLever(
            text = stringResource(if (car == null) R.string.car_create else R.string.action_save),
            onPull = { submit() },
            modifier = Modifier.fillMaxWidth(),
        )
        if (car != null) {
            BrassButton(
                text = stringResource(R.string.car_delete),
                danger = true,
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.car_delete_title),
            text = stringResource(R.string.car_delete_text, name),
            confirmText = stringResource(R.string.action_delete),
            danger = true,
            onConfirm = onDelete,
            onDismiss = { confirmDelete = false },
        )
    }
}
