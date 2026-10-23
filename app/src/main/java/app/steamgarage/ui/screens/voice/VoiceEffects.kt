package app.steamgarage.ui.screens.voice

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.steamgarage.AppContainer
import app.steamgarage.R
import app.steamgarage.data.Settings
import app.steamgarage.data.VoiceMode
import app.steamgarage.data.summarize
import app.steamgarage.domain.Defaults
import app.steamgarage.domain.ItemStatus
import app.steamgarage.notify.SystemSettings
import app.steamgarage.sound.VoiceStatus
import app.steamgarage.ui.components.BrassButton
import app.steamgarage.ui.components.SteamDialog
import app.steamgarage.ui.components.ValveButton
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Голос в жизни приложения: заранее будит синтезатор, при открытии раз в день предупреждает
 * о самом просроченном расходнике и предлагает установить русский голос, если его нет.
 */
@Composable
fun VoiceEffects(container: AppContainer, settings: Settings) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val voice = container.voice
    val status by voice.status.collectAsStateWithLifecycle()
    val mode = settings.voiceMode

    LaunchedEffect(mode) { if (mode != VoiceMode.OFF) voice.warmUp() }
    // Вернулись с экрана установки голоса — проверяем заново.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (voice.status.value == VoiceStatus.NO_RUSSIAN) voice.restart()
    }

    LaunchedEffect(Unit) {
        val s = container.settings.current()
        val today = LocalDate.now()
        if (s.voiceMode == VoiceMode.OFF) return@LaunchedEffect
        if (today.toEpochDay() - s.lastVoiceWarningDay < Defaults.VOICE_WARNING_INTERVAL_DAYS) return@LaunchedEffect
        val worst = container.repository.snapshot.first()
            .summarize(today, s.thresholds)
            .filter { it.car.id !in s.mutedCars }
            .flatMap { it.activeItems }
            .filter { it.state.status == ItemStatus.OVERDUE }
            .maxByOrNull { it.state.wear ?: 0f }
            ?: return@LaunchedEffect
        container.settings.setLastVoiceWarningDay(today.toEpochDay())
        voice.warnOverdue(worst.item.name)
    }

    var dismissed by rememberSaveable { mutableStateOf(false) }
    if (status == VoiceStatus.NO_RUSSIAN && mode != VoiceMode.OFF && !dismissed) {
        VoiceMissingDialog(
            onInstall = {
                if (!voice.openVoiceInstall()) SystemSettings.openTtsSettings(context)
                dismissed = true
            },
            onTurnOff = {
                scope.launch { container.settings.setVoiceMode(VoiceMode.OFF) }
                dismissed = true
            },
            onDismiss = { dismissed = true },
        )
    }
}

@Composable
fun VoiceMissingDialog(onInstall: () -> Unit, onTurnOff: () -> Unit, onDismiss: () -> Unit) {
    SteamDialog(
        title = stringResource(R.string.voice_missing_title),
        onDismiss = onDismiss,
        buttons = {
            ValveButton(text = stringResource(R.string.voice_missing_off), onClick = onTurnOff)
            BrassButton(text = stringResource(R.string.voice_missing_install), onClick = onInstall)
        },
    ) {
        Text(stringResource(R.string.voice_missing_text), style = MaterialTheme.typography.bodyMedium)
    }
}
