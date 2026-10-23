package app.steamgarage.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import app.steamgarage.AppContainer
import app.steamgarage.appContainer
import app.steamgarage.ui.components.SteamTopBar

/** ViewModel экрана с доступом к контейнеру зависимостей. */
@Composable
inline fun <reified VM : ViewModel> steamViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = LocalContext.current.appContainer
    return viewModel(key = key) { create(container) }
}

/** Кнопки верхней панели всех вкладок — рычаг и шестерня синхронизации. Задаёт навигация приложения. */
val LocalTabBarActions = staticCompositionLocalOf<@Composable RowScope.() -> Unit> { {} }

/**
 * Каркас экрана: верхняя панель и прокручиваемое содержимое.
 * [chromePadding] — отступ снизу под нижнюю навигацию (если она видна на этом экране).
 * [hasBottomBar] = false — содержимое само отступает от системной панели навигации.
 */
@Composable
fun SteamScreen(
    title: String,
    chromePadding: PaddingValues,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    hasBottomBar: Boolean = onBack == null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        val tabActions = LocalTabBarActions.current
        SteamTopBar(
            title = title,
            onBack = onBack,
            actions = {
                if (hasBottomBar) tabActions()
                actions()
            },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = chromePadding.calculateBottomPadding())
                .then(if (hasBottomBar) Modifier else Modifier.navigationBarsPadding())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
            Spacer(Modifier.height(12.dp))
        }
    }
}
