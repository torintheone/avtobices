package app.steamgarage.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.steamgarage.domain.Defaults
import app.steamgarage.domain.ThresholdSettings
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Выбор темы: тёмная мастерская, дневная («пергаментная») или как в системе. */
enum class ThemeMode { DARK, LIGHT, SYSTEM }

/** Режим голоса механика. */
enum class VoiceMode { OFF, IMPORTANT, ALL }

/** Все настройки устройства. Хранятся локально и не синхронизируются. */
data class Settings(
    val deviceId: String = "",
    val deviceName: String = "",
    val onboardingDone: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val thresholds: ThresholdSettings = ThresholdSettings.DEFAULT,
    val mileageReminderDays: Int = Defaults.MILEAGE_REMINDER_DAYS,
    val checkHour: Int = Defaults.CHECK_HOUR,
    val checkMinute: Int = Defaults.CHECK_MINUTE,
    /** Машины, по которым это устройство не присылает уведомлений. */
    val mutedCars: Set<String> = emptySet(),
    /** Общие машины, которые это устройство сделало личными: их копия из гаража не загружается. */
    val hiddenSharedCars: Set<String> = emptySet(),
    val soundEnabled: Boolean = true,
    val soundVolume: Float = Defaults.SOUND_VOLUME,
    val soundInSilent: Boolean = false,
    val voiceMode: VoiceMode = VoiceMode.IMPORTANT,
    val notificationRationaleShown: Boolean = false,
    val oemHintShown: Boolean = false,
    val lastVoiceWarningDay: Long = 0,
    val lastSyncAt: Long = 0,
    val lastSyncReceived: Int = 0,
    val lastSyncSent: Int = 0,
)

private val Context.settingsStore by preferencesDataStore(name = "settings")

/** Настройки в DataStore. */
class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsStore

    private object Keys {
        val deviceId = stringPreferencesKey("device_id")
        val deviceName = stringPreferencesKey("device_name")
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val themeMode = stringPreferencesKey("theme_mode")
        val warnPercent = intPreferencesKey("warn_percent")
        val warnKmMin = intPreferencesKey("warn_km_min")
        val warnKmMax = intPreferencesKey("warn_km_max")
        val warnDaysMin = intPreferencesKey("warn_days_min")
        val warnDaysMax = intPreferencesKey("warn_days_max")
        val mileageReminderDays = intPreferencesKey("mileage_reminder_days")
        val checkHour = intPreferencesKey("check_hour")
        val checkMinute = intPreferencesKey("check_minute")
        val mutedCars = stringSetPreferencesKey("muted_cars")
        val hiddenSharedCars = stringSetPreferencesKey("hidden_shared_cars")
        val soundEnabled = booleanPreferencesKey("sound_enabled")
        val soundVolume = floatPreferencesKey("sound_volume")
        val soundInSilent = booleanPreferencesKey("sound_in_silent")
        val voiceMode = stringPreferencesKey("voice_mode")
        val notificationRationaleShown = booleanPreferencesKey("notification_rationale_shown")
        val oemHintShown = booleanPreferencesKey("oem_hint_shown")
        val lastVoiceWarningDay = longPreferencesKey("last_voice_warning_day")
        val lastSyncAt = longPreferencesKey("last_sync_at")
        val lastSyncReceived = intPreferencesKey("last_sync_received")
        val lastSyncSent = intPreferencesKey("last_sync_sent")
    }

    val settings: Flow<Settings> = store.data
        .catch { emit(emptyPreferences()) }
        .map { it.toSettings() }

    suspend fun current(): Settings = settings.first()

    /** Идентификатор устройства: создаётся при первом обращении и больше не меняется. */
    suspend fun deviceId(): String {
        current().deviceId.takeIf { it.isNotEmpty() }?.let { return it }
        var id = ""
        store.edit { prefs ->
            id = prefs[Keys.deviceId] ?: UUID.randomUUID().toString().also { prefs[Keys.deviceId] = it }
        }
        return id
    }

    suspend fun setDeviceName(name: String) = edit { it[Keys.deviceName] = name.trim() }
    suspend fun setOnboardingDone() = edit { it[Keys.onboardingDone] = true }
    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.themeMode] = mode.name }

    suspend fun setThresholds(t: ThresholdSettings) = edit {
        it[Keys.warnPercent] = t.percent
        it[Keys.warnKmMin] = t.kmMin
        it[Keys.warnKmMax] = t.kmMax
        it[Keys.warnDaysMin] = t.daysMin
        it[Keys.warnDaysMax] = t.daysMax
    }

    suspend fun setMileageReminderDays(days: Int) = edit { it[Keys.mileageReminderDays] = days }

    suspend fun setCheckTime(hour: Int, minute: Int) = edit {
        it[Keys.checkHour] = hour
        it[Keys.checkMinute] = minute
    }

    suspend fun setCarMuted(carId: String, muted: Boolean) = edit {
        val set = it[Keys.mutedCars].orEmpty()
        it[Keys.mutedCars] = if (muted) set + carId else set - carId
    }

    suspend fun hideSharedCar(carId: String) = edit {
        it[Keys.hiddenSharedCars] = it[Keys.hiddenSharedCars].orEmpty() + carId
    }

    suspend fun unhideSharedCar(carId: String) = edit {
        it[Keys.hiddenSharedCars] = it[Keys.hiddenSharedCars].orEmpty() - carId
    }

    suspend fun clearHiddenSharedCars() = edit { it.remove(Keys.hiddenSharedCars) }

    suspend fun setSoundEnabled(enabled: Boolean) = edit { it[Keys.soundEnabled] = enabled }
    suspend fun setSoundVolume(volume: Float) = edit { it[Keys.soundVolume] = volume.coerceIn(0f, 1f) }
    suspend fun setSoundInSilent(allowed: Boolean) = edit { it[Keys.soundInSilent] = allowed }
    suspend fun setVoiceMode(mode: VoiceMode) = edit { it[Keys.voiceMode] = mode.name }
    suspend fun setNotificationRationaleShown() = edit { it[Keys.notificationRationaleShown] = true }
    suspend fun setOemHintShown() = edit { it[Keys.oemHintShown] = true }
    suspend fun setLastVoiceWarningDay(day: Long) = edit { it[Keys.lastVoiceWarningDay] = day }

    suspend fun setLastSync(at: Long, received: Int, sent: Int) = edit {
        it[Keys.lastSyncAt] = at
        it[Keys.lastSyncReceived] = received
        it[Keys.lastSyncSent] = sent
    }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        store.edit { block(it) }
    }

    private fun Preferences.toSettings() = Settings(
        deviceId = this[Keys.deviceId].orEmpty(),
        deviceName = this[Keys.deviceName].orEmpty(),
        onboardingDone = this[Keys.onboardingDone] ?: false,
        themeMode = this[Keys.themeMode]?.let { v -> ThemeMode.entries.firstOrNull { it.name == v } } ?: ThemeMode.DARK,
        thresholds = ThresholdSettings(
            percent = this[Keys.warnPercent] ?: Defaults.WARN_PERCENT,
            kmMin = this[Keys.warnKmMin] ?: Defaults.WARN_KM_MIN,
            kmMax = this[Keys.warnKmMax] ?: Defaults.WARN_KM_MAX,
            daysMin = this[Keys.warnDaysMin] ?: Defaults.WARN_DAYS_MIN,
            daysMax = this[Keys.warnDaysMax] ?: Defaults.WARN_DAYS_MAX,
        ),
        mileageReminderDays = this[Keys.mileageReminderDays] ?: Defaults.MILEAGE_REMINDER_DAYS,
        checkHour = this[Keys.checkHour] ?: Defaults.CHECK_HOUR,
        checkMinute = this[Keys.checkMinute] ?: Defaults.CHECK_MINUTE,
        mutedCars = this[Keys.mutedCars].orEmpty(),
        hiddenSharedCars = this[Keys.hiddenSharedCars].orEmpty(),
        soundEnabled = this[Keys.soundEnabled] ?: true,
        soundVolume = this[Keys.soundVolume] ?: Defaults.SOUND_VOLUME,
        soundInSilent = this[Keys.soundInSilent] ?: false,
        voiceMode = this[Keys.voiceMode]?.let { v -> VoiceMode.entries.firstOrNull { it.name == v } } ?: VoiceMode.IMPORTANT,
        notificationRationaleShown = this[Keys.notificationRationaleShown] ?: false,
        oemHintShown = this[Keys.oemHintShown] ?: false,
        lastVoiceWarningDay = this[Keys.lastVoiceWarningDay] ?: 0,
        lastSyncAt = this[Keys.lastSyncAt] ?: 0,
        lastSyncReceived = this[Keys.lastSyncReceived] ?: 0,
        lastSyncSent = this[Keys.lastSyncSent] ?: 0,
    )
}
