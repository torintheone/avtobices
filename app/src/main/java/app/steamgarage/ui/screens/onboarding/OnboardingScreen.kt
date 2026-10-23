package app.steamgarage.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.steamgarage.R
import app.steamgarage.appContainer
import app.steamgarage.ui.components.GaugeReading
import app.steamgarage.ui.components.PipeDivider
import app.steamgarage.ui.components.SteamGauge
import app.steamgarage.ui.components.SteamLever
import app.steamgarage.ui.components.SteamPanel
import app.steamgarage.ui.components.SteamTextField
import app.steamgarage.ui.components.ValveButton
import app.steamgarage.domain.ItemStatus
import app.steamgarage.ui.theme.Steam
import kotlinx.coroutines.launch

/** Первый запуск: имя на этом устройстве и первый шаг — своя машина или вступление в общий гараж. */
@Composable
fun OnboardingHost(canJoin: Boolean, onAddCar: () -> Unit, onSkip: () -> Unit, onJoin: () -> Unit) {
    val container = LocalContext.current.appContainer
    val scope = rememberCoroutineScope()
    fun finish(name: String, next: () -> Unit) {
        scope.launch {
            container.settings.setDeviceName(name)
            container.settings.setOnboardingDone()
            next()
        }
    }
    OnboardingScreen(
        onContinue = { name, addCar -> finish(name, if (addCar) onAddCar else onSkip) },
        canJoin = canJoin,
        onJoin = { name -> finish(name, onJoin) },
    )
}

@Composable
fun OnboardingScreen(
    onContinue: (name: String, addCar: Boolean) -> Unit,
    canJoin: Boolean = false,
    onJoin: (name: String) -> Unit = {},
) {
    val palette = Steam.palette
    var name by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SteamGauge(
            reading = GaugeReading(ItemStatus.OK, 0.62f),
            modifier = Modifier.size(150.dp),
            compact = true,
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            color = if (palette.isDark) palette.brassLight else palette.text,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.onboarding_tagline),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = palette.text,
        )
        PipeDivider()
        SteamPanel(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onboarding_name_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            SteamTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.settings_device_name_label),
                supportingText = stringResource(R.string.settings_device_name_hint),
                imeAction = ImeAction.Done,
            )
        }
        SteamLever(
            text = stringResource(R.string.onboarding_add_car),
            onPull = { onContinue(name, true) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (canJoin) {
            ValveButton(text = stringResource(R.string.onboarding_join), onClick = { onJoin(name) })
        }
        ValveButton(text = stringResource(R.string.onboarding_skip), onClick = { onContinue(name, false) })
    }
}
