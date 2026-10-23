package app.steamgarage.ui.screens.notify

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import app.steamgarage.notify.CheckScheduler
import app.steamgarage.notify.Notifier
import app.steamgarage.notify.SystemSettings
import app.steamgarage.ui.components.BrassButton
import app.steamgarage.ui.components.SteamDialog
import app.steamgarage.ui.components.ValveButton
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Разрешение на уведомления (Android 13+) и подсказка про автозапуск — по одному разу,
 * когда у машин появился первый отслеживаемый расходник. Перед системным запросом — экран-объяснение.
 */
@Composable
fun NotificationOnboarding(container: AppContainer) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val hasItems by remember(container) {
        container.repository.snapshot.map { snapshot -> snapshot.items.any { it.active } }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)
    var requesting by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        requesting = false
        scope.launch { container.settings.setNotificationRationaleShown() }
        if (granted) CheckScheduler.checkSoon(context)
    }

    val loaded = settings ?: return
    if (!hasItems || requesting) return
    val needPermission = Build.VERSION.SDK_INT >= 33 && !loaded.notificationRationaleShown && !Notifier.canNotify(context)
    when {
        needPermission -> NotificationPermissionDialog(
            onAllow = {
                requesting = true
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onLater = { scope.launch { container.settings.setNotificationRationaleShown() } },
        )
        !loaded.oemHintShown && SystemSettings.needsAutostartHint() -> AutostartHintDialog(
            onOpen = {
                SystemSettings.openAppDetails(context)
                scope.launch { container.settings.setOemHintShown() }
            },
            onDismiss = { scope.launch { container.settings.setOemHintShown() } },
        )
    }
}

@Composable
fun NotificationPermissionDialog(onAllow: () -> Unit, onLater: () -> Unit) {
    SteamDialog(
        title = stringResource(R.string.permission_title),
        onDismiss = onLater,
        buttons = {
            ValveButton(text = stringResource(R.string.permission_later), onClick = onLater)
            BrassButton(text = stringResource(R.string.permission_allow), onClick = onAllow)
        },
    ) {
        Text(stringResource(R.string.permission_text), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun AutostartHintDialog(onOpen: () -> Unit, onDismiss: () -> Unit) {
    SteamDialog(
        title = stringResource(R.string.autostart_title),
        onDismiss = onDismiss,
        buttons = {
            ValveButton(text = stringResource(R.string.autostart_dismiss), onClick = onDismiss)
            BrassButton(text = stringResource(R.string.autostart_open), onClick = onOpen)
        },
    ) {
        Text(
            stringResource(R.string.autostart_text, SystemSettings.manufacturerName()),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** Разрешены ли уведомления сейчас; перепроверяется при возвращении в приложение из настроек телефона. */
@Composable
fun rememberNotificationsAllowed(): Boolean {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(Notifier.canNotify(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { allowed = Notifier.canNotify(context) }
    return allowed
}
