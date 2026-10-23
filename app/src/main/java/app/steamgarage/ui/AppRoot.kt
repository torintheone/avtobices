package app.steamgarage.ui

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.steamgarage.R
import app.steamgarage.ui.components.BrassBottomBar
import app.steamgarage.ui.components.NavTab
import app.steamgarage.ui.components.SectionTitle
import app.steamgarage.ui.components.SteamBackground
import app.steamgarage.ui.components.SteamGears
import app.steamgarage.ui.components.SteamPanel
import app.steamgarage.ui.components.SteamTopBar
import app.steamgarage.ui.screens.ShowcaseScreen
import app.steamgarage.ui.theme.Steam
import app.steamgarage.ui.theme.SteamTheme
import app.steamgarage.ui.theme.ThemeMode
import app.steamgarage.ui.theme.isDarkTheme

/** Корень интерфейса: тема, фон, нижняя навигация из четырёх вкладок. */
@Composable
fun AppRoot() {
    var themeMode by rememberSaveable { mutableStateOf(ThemeMode.DARK) }
    SteamTheme(themeMode) {
        SystemBarsEffect(isDarkTheme(themeMode))
        SteamBackground {
            var tab by rememberSaveable { mutableIntStateOf(0) }
            val tabs = listOf(
                NavTab(stringResource(R.string.tab_garage), R.drawable.ic_nav_garage),
                NavTab(stringResource(R.string.tab_due), R.drawable.ic_nav_due),
                NavTab(stringResource(R.string.tab_journal), R.drawable.ic_nav_journal),
                NavTab(stringResource(R.string.tab_settings), R.drawable.ic_nav_settings),
            )
            Scaffold(
                containerColor = Color.Transparent,
                contentColor = Steam.palette.text,
                topBar = {
                    SteamTopBar(
                        title = if (tab == 0) stringResource(R.string.showcase_title) else tabs[tab].label,
                    )
                },
                bottomBar = {
                    BrassBottomBar(tabs = tabs, selectedIndex = tab, onSelect = { tab = it })
                },
            ) { padding ->
                when (tab) {
                    0 -> ShowcaseScreen(padding, themeMode) { themeMode = it }
                    1 -> StubScreen(padding, stringResource(R.string.stub_due), stage = 2)
                    2 -> StubScreen(padding, stringResource(R.string.stub_journal), stage = 1)
                    else -> StageZeroSettings(padding, themeMode) { themeMode = it }
                }
            }
        }
    }
}

/** Значки системных панелей: светлые на тёмном дереве, тёмные на пергаменте и на латуни снизу. */
@Composable
private fun SystemBarsEffect(darkTheme: Boolean) {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    DisposableEffect(darkTheme) {
        val transparent = android.graphics.Color.TRANSPARENT
        activity.enableEdgeToEdge(
            statusBarStyle = if (darkTheme) {
                SystemBarStyle.dark(transparent)
            } else {
                SystemBarStyle.light(transparent, transparent)
            },
            navigationBarStyle = SystemBarStyle.light(transparent, transparent),
        )
        onDispose { }
    }
}

@Composable
private fun StubScreen(contentPadding: PaddingValues, text: String, stage: Int) {
    val palette = Steam.palette
    Column(
        modifier = Modifier
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SteamPanel(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SteamGears(spinning = false, size = 48.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.stub_stage, stage),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(text = text, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
        }
    }
}

/** Настройки этапа 0: только выбор темы. */
@Composable
private fun StageZeroSettings(
    contentPadding: PaddingValues,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    val palette = Steam.palette
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
                        .selectable(
                            selected = themeMode == mode,
                            role = Role.RadioButton,
                            onClick = { onThemeModeChange(mode) },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = themeMode == mode,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = palette.copperLight,
                            unselectedColor = palette.textSecondary,
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        Text(
            text = stringResource(R.string.stub_settings_hint),
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
        )
    }
}
