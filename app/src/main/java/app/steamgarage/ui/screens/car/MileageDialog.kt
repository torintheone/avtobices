package app.steamgarage.ui.screens.car

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.steamgarage.R
import app.steamgarage.domain.Defaults
import app.steamgarage.ui.Format
import app.steamgarage.ui.components.BrassButton
import app.steamgarage.ui.components.ConfirmDialog
import app.steamgarage.ui.components.NumberField
import app.steamgarage.ui.components.OdometerInput
import app.steamgarage.ui.components.SteamDialog
import app.steamgarage.ui.components.ValveButton
import app.steamgarage.ui.theme.Steam

/**
 * Ввод пробега барабанами одометра с текущим значением для сравнения.
 * Меньше текущего максимума — спрашиваем, не ошибка ли в прошлых данных.
 */
@Composable
fun MileageDialog(
    currentKm: Int?,
    onDismiss: () -> Unit,
    onSave: (km: Int, correctPast: Boolean) -> Unit,
) {
    val palette = Steam.palette
    var value by rememberSaveable { mutableIntStateOf(currentKm ?: 0) }
    var keyboard by rememberSaveable { mutableStateOf(false) }
    var text by rememberSaveable { mutableStateOf((currentKm ?: 0).toString()) }
    var confirmLower by rememberSaveable { mutableStateOf(false) }

    fun submit() {
        val km = if (keyboard) Format.parseInt(text) ?: return else value
        if (km <= 0) return
        if (currentKm != null && km < currentKm) {
            confirmLower = true
        } else {
            onSave(km, false)
            onDismiss()
        }
    }

    SteamDialog(
        title = stringResource(R.string.mileage_title),
        onDismiss = onDismiss,
        buttons = {
            ValveButton(text = stringResource(R.string.action_cancel), onClick = onDismiss)
            BrassButton(text = stringResource(R.string.mileage_save), onClick = { submit() })
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (currentKm != null) {
                    stringResource(R.string.mileage_current, Format.km(currentKm))
                } else {
                    stringResource(R.string.mileage_current_unknown)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
            )
            if (keyboard) {
                NumberField(
                    value = text,
                    onValueChange = { text = it },
                    label = stringResource(R.string.mileage_field),
                    suffix = stringResource(R.string.unit_km),
                    imeAction = ImeAction.Done,
                )
            } else {
                OdometerInput(value = value, onValueChange = { value = it.coerceIn(0, Defaults.ODOMETER_MAX_KM) })
                Text(
                    text = stringResource(R.string.mileage_drums_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                )
            }
            Spacer(Modifier.height(2.dp))
            TextButton(
                onClick = {
                    if (keyboard) {
                        value = Format.parseInt(text)?.coerceIn(0, Defaults.ODOMETER_MAX_KM) ?: value
                    } else {
                        text = value.toString()
                    }
                    keyboard = !keyboard
                },
            ) {
                Text(
                    text = stringResource(if (keyboard) R.string.mileage_use_drums else R.string.mileage_use_keyboard),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (palette.isDark) palette.brassLight else palette.text,
                )
            }
        }
    }

    if (confirmLower && currentKm != null) {
        val km = if (keyboard) Format.parseInt(text) ?: 0 else value
        ConfirmDialog(
            title = stringResource(R.string.mileage_lower_title),
            text = stringResource(R.string.mileage_lower_text, Format.km(km), Format.km(currentKm)),
            confirmText = stringResource(R.string.mileage_lower_confirm),
            onConfirm = {
                onSave(km, true)
                onDismiss()
            },
            onDismiss = { confirmLower = false },
        )
    }
}
