package app.steamgarage.ui

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.steamgarage.AppContainer
import app.steamgarage.R
import app.steamgarage.appContainer
import app.steamgarage.data.ThemeMode
import app.steamgarage.ui.components.BrassBottomBar
import app.steamgarage.ui.components.NavTab
import app.steamgarage.ui.components.SteamBackground
import app.steamgarage.ui.components.SteamTopBar
import app.steamgarage.ui.navigation.AboutRoute
import app.steamgarage.ui.navigation.CarEditRoute
import app.steamgarage.ui.navigation.CarRoute
import app.steamgarage.ui.navigation.DueRoute
import app.steamgarage.ui.navigation.GarageRoute
import app.steamgarage.ui.navigation.ItemEditRoute
import app.steamgarage.ui.navigation.ItemRoute
import app.steamgarage.ui.navigation.JournalRoute
import app.steamgarage.ui.navigation.NavRequest
import app.steamgarage.ui.navigation.OnboardingRoute
import app.steamgarage.ui.navigation.SettingsRoute
import app.steamgarage.ui.navigation.ShowcaseRoute
import app.steamgarage.ui.navigation.TemplateRoute
import app.steamgarage.ui.screens.ShowcaseScreen
import app.steamgarage.ui.screens.about.AboutScreen
import app.steamgarage.ui.screens.car.CarEditHost
import app.steamgarage.ui.screens.car.CarHost
import app.steamgarage.ui.screens.due.DueHost
import app.steamgarage.ui.screens.garage.GarageHost
import app.steamgarage.ui.screens.item.ItemEditHost
import app.steamgarage.ui.screens.item.ItemHost
import app.steamgarage.ui.screens.journal.JournalHost
import app.steamgarage.ui.screens.notify.NotificationOnboarding
import app.steamgarage.ui.screens.onboarding.OnboardingHost
import app.steamgarage.ui.screens.settings.SettingsHost
import app.steamgarage.ui.screens.template.TemplateHost
import app.steamgarage.ui.theme.SteamTheme
import app.steamgarage.ui.theme.isDarkTheme
import kotlinx.coroutines.launch

/** Корень интерфейса: тема из настроек, фон, навигация с нижней панелью из четырёх вкладок. */
@Composable
fun AppRoot() {
    val container = LocalContext.current.appContainer
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val themeMode = settings?.themeMode ?: ThemeMode.DARK
    SteamTheme(themeMode) {
        SystemBarsEffect(isDarkTheme(themeMode))
        SteamBackground {
            val loaded = settings
            if (loaded != null) {
                AppNavigation(startOnboarding = !loaded.onboardingDone, container = container)
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

private val TabRoutes: List<Any> = listOf(GarageRoute, DueRoute, JournalRoute, SettingsRoute)

@Composable
private fun AppNavigation(startOnboarding: Boolean, container: AppContainer) {
    val nav = rememberNavController()
    val start: Any = remember { if (startOnboarding) OnboardingRoute else GarageRoute }
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val selectedTab = when {
        destination == null -> -1
        destination.hierarchy.any { it.hasRoute<GarageRoute>() } -> 0
        destination.hierarchy.any { it.hasRoute<DueRoute>() } -> 1
        destination.hierarchy.any { it.hasRoute<JournalRoute>() } -> 2
        destination.hierarchy.any { it.hasRoute<SettingsRoute>() } -> 3
        else -> -1
    }
    // Нажатие на уведомление: открыть машину с подсвеченным расходником (и, если нужно, форму).
    val request by container.navRequests.collectAsStateWithLifecycle()
    LaunchedEffect(request) {
        val open = request as? NavRequest.OpenCar ?: return@LaunchedEffect
        container.navRequests.value = null
        nav.navigate(CarRoute(open.carId, open.highlightItemId, open.action, open.actionItems)) {
            popUpTo(nav.graph.findStartDestination().id)
        }
    }
    val tabs = listOf(
        NavTab(stringResource(R.string.tab_garage), R.drawable.ic_nav_garage),
        NavTab(stringResource(R.string.tab_due), R.drawable.ic_nav_due),
        NavTab(stringResource(R.string.tab_journal), R.drawable.ic_nav_journal),
        NavTab(stringResource(R.string.tab_settings), R.drawable.ic_nav_settings),
    )
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (selectedTab >= 0) {
                BrassBottomBar(tabs = tabs, selectedIndex = selectedTab, onSelect = { nav.navigateTab(TabRoutes[it]) })
            }
        },
    ) { padding ->
        NavHost(navController = nav, startDestination = start, modifier = Modifier.fillMaxSize()) {
            composable<OnboardingRoute> {
                OnboardingHost(
                    onAddCar = {
                        nav.navigate(GarageRoute) { popUpTo<OnboardingRoute> { inclusive = true } }
                        nav.navigate(CarEditRoute())
                    },
                    onSkip = { nav.navigate(GarageRoute) { popUpTo<OnboardingRoute> { inclusive = true } } },
                )
            }
            composable<GarageRoute> {
                GarageHost(
                    chromePadding = padding,
                    onOpenCar = { nav.navigate(CarRoute(it)) },
                    onAddCar = { nav.navigate(CarEditRoute()) },
                )
            }
            composable<DueRoute> {
                DueHost(
                    chromePadding = padding,
                    onOpenItem = { nav.navigate(ItemRoute(it)) },
                    onAddCar = { nav.navigate(CarEditRoute()) },
                )
            }
            composable<JournalRoute> {
                JournalHost(
                    chromePadding = padding,
                    onOpenItem = { nav.navigate(ItemRoute(it)) },
                    onAddCar = { nav.navigate(CarEditRoute()) },
                )
            }
            composable<SettingsRoute> {
                SettingsHost(chromePadding = padding, onAbout = { nav.navigate(AboutRoute) })
            }
            composable<CarRoute> { entry ->
                val route = entry.toRoute<CarRoute>()
                CarHost(
                    carId = route.carId,
                    highlightItemId = route.highlightItemId,
                    action = route.action,
                    actionItems = route.actionItems,
                    chromePadding = padding,
                    onBack = { nav.popBackStack() },
                    onEditCar = { nav.navigate(CarEditRoute(route.carId)) },
                    onOpenItem = { nav.navigate(ItemRoute(it)) },
                    onAddItem = { nav.navigate(ItemEditRoute(route.carId)) },
                    onAddFromTemplate = { nav.navigate(TemplateRoute(route.carId)) },
                )
            }
            composable<CarEditRoute> { entry ->
                val route = entry.toRoute<CarEditRoute>()
                CarEditHost(
                    carId = route.carId,
                    chromePadding = padding,
                    onBack = { nav.popBackStack() },
                    onCreated = { id -> nav.navigate(TemplateRoute(id)) { popUpTo<CarEditRoute> { inclusive = true } } },
                    onDeleted = { nav.popBackStack(GarageRoute, inclusive = false) },
                )
            }
            composable<TemplateRoute> { entry ->
                val route = entry.toRoute<TemplateRoute>()
                val toCar = {
                    if (nav.previousBackStackEntry?.destination?.hasRoute<CarRoute>() == true) {
                        nav.popBackStack()
                    } else {
                        nav.navigate(CarRoute(route.carId)) { popUpTo<TemplateRoute> { inclusive = true } }
                    }
                    Unit
                }
                TemplateHost(carId = route.carId, chromePadding = padding, onBack = toCar, onDone = toCar)
            }
            composable<ItemRoute> { entry ->
                val route = entry.toRoute<ItemRoute>()
                ItemHost(
                    itemId = route.itemId,
                    chromePadding = padding,
                    onBack = { nav.popBackStack() },
                    onEdit = { carId -> nav.navigate(ItemEditRoute(carId, route.itemId)) },
                )
            }
            composable<ItemEditRoute> { entry ->
                val route = entry.toRoute<ItemEditRoute>()
                ItemEditHost(
                    carId = route.carId,
                    itemId = route.itemId,
                    chromePadding = padding,
                    onBack = { nav.popBackStack() },
                    onDeleted = { nav.navigate(CarRoute(route.carId)) { popUpTo<ItemRoute> { inclusive = true } } },
                )
            }
            composable<AboutRoute> {
                AboutScreen(chromePadding = padding, onBack = { nav.popBackStack() }, onShowcase = { nav.navigate(ShowcaseRoute) })
            }
            composable<ShowcaseRoute> {
                ShowcaseHost(container = container, onBack = { nav.popBackStack() })
            }
        }
    }
    NotificationOnboarding(container)
}

/** Переход на вкладку: одна копия каждой вкладки, состояние вкладок сохраняется. */
private fun NavHostController.navigateTab(route: Any) {
    navigate(route) {
        popUpTo<GarageRoute> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun ShowcaseHost(container: AppContainer, onBack: () -> Unit) {
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        SteamTopBar(title = stringResource(R.string.showcase_title), onBack = onBack)
        Box(Modifier.weight(1f)) {
            ShowcaseScreen(
                contentPadding = WindowInsets.navigationBars.asPaddingValues(),
                themeMode = settings?.themeMode ?: ThemeMode.DARK,
                onThemeModeChange = { mode -> scope.launch { container.settings.setThemeMode(mode) } },
            )
        }
    }
}
