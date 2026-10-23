package app.steamgarage.ui.screens.about

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.steamgarage.BuildConfig
import app.steamgarage.R
import app.steamgarage.ui.components.SectionTitle
import app.steamgarage.ui.components.SteamPanel
import app.steamgarage.ui.components.ValveButton
import app.steamgarage.ui.screens.SteamScreen
import app.steamgarage.ui.theme.Steam

/** «О приложении»: версия, лицензии ресурсов, витрина механизмов. */
@Composable
fun AboutScreen(chromePadding: PaddingValues, onBack: () -> Unit, onShowcase: () -> Unit) {
    val palette = Steam.palette
    SteamScreen(
        title = stringResource(R.string.about_title),
        chromePadding = chromePadding,
        onBack = onBack,
        hasBottomBar = false,
    ) {
        SteamPanel(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), style = Steam.text.number, color = palette.textSecondary)
            Text(stringResource(R.string.about_description), style = MaterialTheme.typography.bodyMedium)
        }
        SectionTitle(stringResource(R.string.about_licenses))
        SteamPanel(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.about_licenses_fonts), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.about_licenses_graphics), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.about_licenses_libraries), style = MaterialTheme.typography.bodyMedium)
        }
        ValveButton(text = stringResource(R.string.showcase_title), onClick = onShowcase)
    }
}
