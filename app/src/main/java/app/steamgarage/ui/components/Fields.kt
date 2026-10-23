package app.steamgarage.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import app.steamgarage.R
import app.steamgarage.ui.Format
import app.steamgarage.ui.theme.Steam
import java.time.LocalDate

private const val DAY_MILLIS = 86_400_000L

/** Цвета полей ввода: латунная рамка, текст темы, читаемая ошибка. */
@Composable
fun steamFieldColors(): TextFieldColors {
    val p = Steam.palette
    val focusedLabel = if (p.isDark) p.brassLight else p.text
    val border = if (p.isDark) p.brassDark else p.brassShadow
    val container = p.surfaceDeep.copy(alpha = if (p.isDark) 0.55f else 0.35f)
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = p.text,
        unfocusedTextColor = p.text,
        disabledTextColor = p.textSecondary,
        errorTextColor = p.text,
        focusedContainerColor = container,
        unfocusedContainerColor = container,
        disabledContainerColor = container,
        errorContainerColor = container,
        cursorColor = p.copperLight,
        errorCursorColor = p.errorText,
        focusedBorderColor = p.brassLight,
        unfocusedBorderColor = border,
        disabledBorderColor = border,
        errorBorderColor = p.errorText,
        focusedLabelColor = focusedLabel,
        unfocusedLabelColor = p.textSecondary,
        disabledLabelColor = p.textSecondary,
        errorLabelColor = p.errorText,
        focusedSupportingTextColor = p.textSecondary,
        unfocusedSupportingTextColor = p.textSecondary,
        errorSupportingTextColor = p.errorText,
        focusedSuffixColor = p.textSecondary,
        unfocusedSuffixColor = p.textSecondary,
        focusedPlaceholderColor = p.textSecondary,
        unfocusedPlaceholderColor = p.textSecondary,
    )
}

/** Поле ввода в стиле мастерской. */
@Composable
fun SteamTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    errorText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    singleLine: Boolean = true,
    minLines: Int = 1,
    suffix: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    enabled: Boolean = true,
) {
    val hint = errorText ?: supportingText
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = singleLine,
        minLines = minLines,
        isError = errorText != null,
        supportingText = hint?.let { { Text(it) } },
        suffix = suffix?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            keyboardType = keyboardType,
            imeAction = imeAction,
        ),
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = steamFieldColors(),
    )
}

/** Поле для целого числа (км, месяцы, год): пропускает только цифры и пробелы. */
@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    supportingText: String? = null,
    errorText: String? = null,
    maxDigits: Int = 7,
    imeAction: ImeAction = ImeAction.Next,
) {
    SteamTextField(
        value = value,
        onValueChange = { text ->
            val digits = text.filter { it.isDigit() }
            if (digits.length <= maxDigits) onValueChange(digits)
        },
        label = label,
        modifier = modifier,
        supportingText = supportingText,
        errorText = errorText,
        keyboardType = KeyboardType.Number,
        suffix = suffix,
        imeAction = imeAction,
    )
}

/** Поле суммы в рублях: цифры и одна запятая. */
@Composable
fun MoneyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorText: String? = null,
) {
    SteamTextField(
        value = value,
        onValueChange = { text ->
            val normalized = text.replace('.', ',').filter { it.isDigit() || it == ',' }
            if (normalized.count { it == ',' } <= 1 && normalized.length <= 12) onValueChange(normalized)
        },
        label = label,
        modifier = modifier,
        errorText = errorText,
        keyboardType = KeyboardType.Decimal,
        suffix = stringResource(R.string.unit_rub),
    )
}

/** Поле даты: показывает ДД.ММ.ГГГГ, по касанию открывает календарь. */
@Composable
fun DateField(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    maxDate: LocalDate? = LocalDate.now(),
) {
    var open by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    LaunchedEffect(interaction) {
        interaction.interactions.collect { if (it is PressInteraction.Release) open = true }
    }
    OutlinedTextField(
        value = Format.date(date),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        interactionSource = interaction,
        textStyle = Steam.text.number,
        colors = steamFieldColors(),
    )
    if (open) {
        SteamDatePickerDialog(
            initial = date,
            maxDate = maxDate,
            onDismiss = { open = false },
            onConfirm = onDateChange,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SteamDatePickerDialog(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    maxDate: LocalDate? = null,
) {
    val maxMillis = maxDate?.let { it.toEpochDay() * DAY_MILLIS + DAY_MILLIS - 1 }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.toEpochDay() * DAY_MILLIS,
        selectableDates = object : androidx.compose.material3.SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = maxMillis == null || utcTimeMillis <= maxMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { onConfirm(LocalDate.ofEpochDay(Math.floorDiv(it, DAY_MILLIS))) }
                    onDismiss()
                },
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    ) {
        DatePicker(state = state)
    }
}

/** Выбор времени — циферблат в латунной рамке, 24 часа. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SteamTimePickerDialog(
    title: String,
    hour: Int,
    minute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    SteamDialog(
        title = title,
        onDismiss = onDismiss,
        buttons = {
            ValveButton(text = stringResource(R.string.action_cancel), onClick = onDismiss)
            BrassButton(
                text = stringResource(R.string.action_ok),
                onClick = {
                    onConfirm(state.hour, state.minute)
                    onDismiss()
                },
            )
        },
    ) {
        TimePicker(state = state, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}
