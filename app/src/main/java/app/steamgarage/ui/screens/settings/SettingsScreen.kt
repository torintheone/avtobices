package app.steamgarage.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.steamgarage.AppContainer
import app.steamgarage.R
import app.steamgarage.data.Settings
import app.steamgarage.data.ThemeMode
import app.steamgarage.data.db.CarEntity
import app.steamgarage.domain.ThresholdSettings
import app.steamgarage.notify.CheckScheduler
import app.steamgarage.notify.SystemSettings
import app.steamgarage.ui.Format
import app.steamgarage.ui.components.BrassButton
import app.steamgarage.ui.components.NumberField
import app.steamgarage.ui.components.SectionTitle
import app.steamgarage.ui.components.SteamPanel
import app.steamgarage.ui.components.SteamSwitchRow
import app.steamgarage.ui.components.SteamTextField
import app.steamgarage.ui.components.SteamTimePickerDialog
import app.steamgarage.ui.components.ValveButton
import app.steamgarage.ui.screens.SteamScreen
import app.steamgarage.ui.screens.notify.rememberNotificationsAllowed
import app.steamgarage.ui.screens.steamViewModel
import app.steamgarage.ui.theme.Steam
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    private val settingsRepo = container.settings
    private val context = container.appContext

    val settings: StateFlow<Settings?> =
        settingsRepo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val cars: StateFlow<List<CarEntity>> = container.repository.snapshot
        .map { snapshot -> snapshot.cars.sortedBy { it.name.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setDeviceName(name: String) = launch { settingsRepo.setDeviceName(name) }
    fun setTheme(mode: ThemeMode) = launch { settingsRepo.setThemeMode(mode) }
    fun setThresholds(t: ThresholdSettings) = launch {
        settingsRepo.setThresholds(t)
        CheckScheduler.checkSoon(context)
    }

    fun setCheckTime(hour: Int, minute: Int) = launch {
        settingsRepo.setCheckTime(hour, minute)
        CheckScheduler.scheduleDaily(context, hour, minute, reschedule = true)
    }

    fun setMileageReminderDays(days: Int) = launch {
        settingsRepo.setMileageReminderDays(days)
        CheckScheduler.checkSoon(context)
    }

    fun setCarMuted(carId: String, muted: Boolean) = launch { settingsRepo.setCarMuted(carId, muted) }

    fun checkNow() = CheckScheduler.checkSoon(context)

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}

@Composable
fun SettingsHost(chromePadding: PaddingValues, onAbout: () -> Unit) {
    val vm = steamViewModel { SettingsViewModel(it) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val cars by vm.cars.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val s = settings ?: return
    SettingsScreen(
        settings = s,
        chromePadding = chromePadding,
        onDeviceName = vm::setDeviceName,
        onTheme = vm::setTheme,
        onThresholds = vm::setThresholds,
        onAbout = onAbout,
        notifications = NotificationSettings(
            allowed = rememberNotificationsAllowed(),
            cars = cars,
            showAutostart = SystemSettings.needsAutostartHint(),
            onCheckTime = vm::setCheckTime,
            onMileageReminderDays = vm::setMileageReminderDays,
            onCarMuted = vm::setCarMuted,
            onCheckNow = vm::checkNow,
            onOpenSystemNotifications = { SystemSettings.openNotificationSettings(context) },
            onOpenAppDetails = { SystemSettings.openAppDetails(context) },
        ),
    )
}

/** Всё для раздела «Уведомления»: состояние разрешения, машины и действия. */
class NotificationSettings(
    val allowed: Boolean = true,
    val cars: List<CarEntity> = emptyList(),
    val showAutostart: Boolean = false,
    val onCheckTime: (Int, Int) -> Unit = { _, _ -> },
    val onMileageReminderDays: (Int) -> Unit = {},
    val onCarMuted: (String, Boolean) -> Unit = { _, _ -> },
    val onCheckNow: () -> Unit = {},
    val onOpenSystemNotifications: () -> Unit = {},
    val onOpenAppDetails: () -> Unit = {},
)

@Composable
fun SettingsScreen(
    settings: Settings,
    chromePadding: PaddingValues,
    onDeviceName: (String) -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onThresholds: (ThresholdSettings) -> Unit,
    onAbout: () -> Unit,
    notifications: NotificationSettings = NotificationSettings(),
) {
    val palette = Steam.palette
    SteamScreen(title = stringResource(R.string.tab_settings), chromePadding = chromePadding) {
        // ── Имя на устройстве ──
        SectionTitle(stringResource(R.string.settings_device_name))
        SteamPanel(Modifier.fillMaxWidth()) {
            var name by rememberSaveable(settings.deviceName) { mutableStateOf(settings.deviceName) }
            SteamTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.settings_device_name_label),
                supportingText = stringResource(R.string.settings_device_name_hint),
                imeAction = ImeAction.Done,
            )
            if (name.trim() != settings.deviceName) {
                BrassButton(text = stringResource(R.string.action_save), onClick = { onDeviceName(name) })
            }
        }

        // ── Тема ──
        SectionTitle(stringResource(R.string.settings_theme))
        SteamPanel(Modifier.fillMaxWidth().selectableGroup()) {
            listOf(
                ThemeMode.DARK to R.string.settings_theme_dark,
                ThemeMode.LIGHT to R.string.settings_theme_light,
                ThemeMode.SYSTEM to R.string.settings_theme_system,
            ).forEach { (mode, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .selectable(selected = settings.themeMode == mode, role = Role.RadioButton, onClick = { onTheme(mode) }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = settings.themeMode == mode, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        // ── Уведомления ──
        SectionTitle(stringResource(R.string.settings_notifications))
        NotificationsPanel(settings, notifications)
        if (notifications.cars.isNotEmpty()) {
            SectionTitle(stringResource(R.string.settings_cars_notify))
            SteamPanel(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_cars_notify_hint), style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                notifications.cars.forEach { car ->
                    SteamSwitchRow(
                        text = car.name,
                        checked = car.id !in settings.mutedCars,
                        onCheckedChange = { on -> notifications.onCarMuted(car.id, !on) },
                    )
                }
            }
        }

        // ── Пороги ──
        SectionTitle(stringResource(R.string.settings_thresholds))
        ThresholdsPanel(settings.thresholds, onThresholds)

        // ── О приложении ──
        SectionTitle(stringResource(R.string.settings_about))
        ValveButton(text = stringResource(R.string.settings_about_open), onClick = onAbout)
        Text(stringResource(R.string.settings_more_soon), style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
    }
}

@Composable
private fun NotificationsPanel(settings: Settings, n: NotificationSettings) {
    val palette = Steam.palette
    var timeDialog by rememberSaveable { mutableStateOf(false) }
    var checkStarted by rememberSaveable { mutableStateOf(false) }
    var reminder by rememberSaveable(settings.mileageReminderDays) { mutableStateOf(settings.mileageReminderDays.toString()) }
    val reminderDays = Format.parseInt(reminder)?.takeIf { it in 1..MAX_REMINDER_DAYS }
    SteamPanel(Modifier.fillMaxWidth()) {
        if (!n.allowed) {
            Text(stringResource(R.string.settings_notifications_off), style = MaterialTheme.typography.bodyMedium, color = palette.errorText)
            BrassButton(text = stringResource(R.string.settings_notifications_allow), onClick = n.onOpenSystemNotifications)
            Spacer(Modifier.height(4.dp))
        }
        Text(
            text = stringResource(R.string.settings_check_time, Format.time(settings.checkHour, settings.checkMinute)),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(stringResource(R.string.settings_check_time_hint), style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
        ValveButton(text = stringResource(R.string.settings_check_time_change), onClick = { timeDialog = true })
        Spacer(Modifier.height(4.dp))
        NumberField(
            value = reminder,
            onValueChange = { reminder = it },
            label = stringResource(R.string.settings_mileage_reminder),
            suffix = stringResource(R.string.unit_days),
            supportingText = stringResource(R.string.settings_mileage_reminder_hint),
            maxDigits = 3,
            imeAction = ImeAction.Done,
        )
        if (reminderDays != null && reminderDays != settings.mileageReminderDays) {
            BrassButton(text = stringResource(R.string.action_save), onClick = { n.onMileageReminderDays(reminderDays) })
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ValveButton(
                text = stringResource(R.string.settings_check_now),
                onClick = {
                    n.onCheckNow()
                    checkStarted = true
                },
            )
            if (checkStarted) {
                Text(stringResource(R.string.settings_check_now_done), style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
            }
            ValveButton(text = stringResource(R.string.settings_system_notifications), onClick = n.onOpenSystemNotifications)
            if (n.showAutostart) {
                ValveButton(text = stringResource(R.string.settings_autostart), onClick = n.onOpenAppDetails)
            }
        }
    }
    if (timeDialog) {
        SteamTimePickerDialog(
            title = stringResource(R.string.settings_check_time_title),
            hour = settings.checkHour,
            minute = settings.checkMinute,
            onDismiss = { timeDialog = false },
            onConfirm = n.onCheckTime,
        )
    }
}

/** Больше года без показаний — напоминание теряет смысл. */
private const val MAX_REMINDER_DAYS = 365

@Composable
private fun ThresholdsPanel(current: ThresholdSettings, onSave: (ThresholdSettings) -> Unit) {
    val palette = Steam.palette
    var percent by rememberSaveable(current) { mutableStateOf(current.percent.toString()) }
    var kmMin by rememberSaveable(current) { mutableStateOf(current.kmMin.toString()) }
    var kmMax by rememberSaveable(current) { mutableStateOf(current.kmMax.toString()) }
    var daysMin by rememberSaveable(current) { mutableStateOf(current.daysMin.toString()) }
    var daysMax by rememberSaveable(current) { mutableStateOf(current.daysMax.toString()) }
    val edited = ThresholdSettings(
        percent = Format.parseInt(percent)?.coerceIn(1, 100) ?: current.percent,
        kmMin = Format.parseInt(kmMin) ?: current.kmMin,
        kmMax = Format.parseInt(kmMax) ?: current.kmMax,
        daysMin = Format.parseInt(daysMin) ?: current.daysMin,
        daysMax = Format.parseInt(daysMax) ?: current.daysMax,
    )
    SteamPanel(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.settings_thresholds_hint), style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
        Spacer(Modifier.width(4.dp))
        NumberField(
            value = percent,
            onValueChange = { percent = it },
            label = stringResource(R.string.settings_threshold_percent),
            suffix = stringResource(R.string.unit_percent_short),
            maxDigits = 3,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField(kmMin, { kmMin = it }, stringResource(R.string.settings_threshold_km_min), Modifier.weight(1f), suffix = stringResource(R.string.unit_km), maxDigits = 6)
            NumberField(kmMax, { kmMax = it }, stringResource(R.string.settings_threshold_km_max), Modifier.weight(1f), suffix = stringResource(R.string.unit_km), maxDigits = 6)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField(daysMin, { daysMin = it }, stringResource(R.string.settings_threshold_days_min), Modifier.weight(1f), suffix = stringResource(R.string.unit_days), maxDigits = 3)
            NumberField(daysMax, { daysMax = it }, stringResource(R.string.settings_threshold_days_max), Modifier.weight(1f), suffix = stringResource(R.string.unit_days), maxDigits = 3)
        }
        Text(
            stringResource(R.string.settings_threshold_example, Format.km(edited.kmFor(10_000)), edited.daysFor(12)),
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (edited != current) {
                BrassButton(text = stringResource(R.string.action_save), onClick = { onSave(edited) })
            }
            if (current != ThresholdSettings.DEFAULT) {
                ValveButton(text = stringResource(R.string.settings_threshold_reset), onClick = { onSave(ThresholdSettings.DEFAULT) })
            }
        }
    }
}
