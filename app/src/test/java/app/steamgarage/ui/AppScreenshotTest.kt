package app.steamgarage.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import app.steamgarage.R
import app.steamgarage.data.CarSummary
import app.steamgarage.data.GarageSnapshot
import app.steamgarage.data.Settings
import app.steamgarage.data.ThemeMode
import app.steamgarage.data.db.CarEntity
import app.steamgarage.data.db.ItemEntity
import app.steamgarage.data.db.MileageEntity
import app.steamgarage.data.db.MileageSource
import app.steamgarage.data.db.Place
import app.steamgarage.data.db.ServiceEntity
import app.steamgarage.data.summarize
import app.steamgarage.domain.Category
import app.steamgarage.domain.ThresholdSettings
import app.steamgarage.ui.components.BrassBottomBar
import app.steamgarage.ui.components.NavTab
import app.steamgarage.ui.components.SteamBackground
import app.steamgarage.ui.components.SteamTimePickerDialog
import app.steamgarage.ui.components.TextureCache
import app.steamgarage.ui.screens.ShowcaseScreen
import app.steamgarage.ui.screens.car.CarScreen
import app.steamgarage.ui.screens.car.MileageDialog
import app.steamgarage.ui.screens.car.ServiceRequest
import app.steamgarage.ui.screens.car.ServiceSheet
import app.steamgarage.ui.screens.due.DueScreen
import app.steamgarage.ui.screens.garage.GarageScreen
import app.steamgarage.ui.screens.item.ItemScreen
import app.steamgarage.ui.screens.journal.JournalScreen
import app.steamgarage.ui.screens.journal.Period
import app.steamgarage.ui.screens.journal.journal
import app.steamgarage.ui.screens.onboarding.OnboardingScreen
import app.steamgarage.ui.screens.notify.NotificationPermissionDialog
import app.steamgarage.ui.screens.settings.NotificationSettings
import app.steamgarage.ui.screens.settings.SettingsScreen
import app.steamgarage.ui.screens.voice.VoiceMissingDialog
import app.steamgarage.ui.screens.sync.GarageActions
import app.steamgarage.ui.screens.sync.GaragePanel
import app.steamgarage.ui.screens.sync.GarageUiState
import app.steamgarage.ui.screens.sync.JoinGarageDialog
import app.steamgarage.data.db.GarageEntity
import app.steamgarage.data.sync.GarageMember
import app.steamgarage.data.sync.SyncError
import app.steamgarage.data.sync.SyncStatus
import app.steamgarage.ui.theme.SteamTheme
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Скриншоты экранов с тестовыми данными. Запуск: ./gradlew -Pscreenshots testDebugUnitTest.
 * PNG складываются в app/build/outputs/roborazzi — по ним проверяется внешний вид без телефона.
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w400dp-h860dp-hdpi")
class AppScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.now()

    private fun capture(name: String, dark: Boolean = true, screen: Boolean = false, content: @Composable () -> Unit) {
        val context: Context = RuntimeEnvironment.getApplication()
        runBlocking { TextureCache.load(context, dark) }
        compose.mainClock.autoAdvance = false
        compose.setContent {
            SteamTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
                SteamBackground { content() }
            }
        }
        compose.mainClock.advanceTimeBy(4_000)
        val path = "build/outputs/roborazzi/$name.png"
        if (screen) captureScreenRoboImage(path) else compose.onRoot().captureRoboImage(path)
    }

    /** Экран с нижней латунной панелью, как в приложении. */
    @Composable
    private fun WithTabs(selected: Int, content: @Composable (PaddingValues) -> Unit) {
        val context: Context = RuntimeEnvironment.getApplication()
        val tabs = listOf(
            NavTab(context.getString(R.string.tab_garage), R.drawable.ic_nav_garage),
            NavTab(context.getString(R.string.tab_due), R.drawable.ic_nav_due),
            NavTab(context.getString(R.string.tab_journal), R.drawable.ic_nav_journal),
            NavTab(context.getString(R.string.tab_settings), R.drawable.ic_nav_settings),
        )
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) { content(PaddingValues()) }
            BrassBottomBar(tabs = tabs, selectedIndex = selected, onSelect = {})
        }
    }

    private fun cars(): List<CarSummary> = Samples.snapshot(today).summarize(today, ThresholdSettings.DEFAULT)

    @Test
    fun garageDark() = capture("01_garage_dark") {
        WithTabs(0) { GarageScreen(cars(), it, onOpenCar = {}, onAddCar = {}) }
    }

    @Test
    fun garageEmptyLight() = capture("02_garage_empty_light", dark = false) {
        WithTabs(0) { GarageScreen(emptyList(), it, onOpenCar = {}, onAddCar = {}) }
    }

    @Test
    @Config(qualifiers = "w400dp-h2200dp-hdpi")
    fun carDark() = capture("03_car_dark") {
        CarScreen(
            summary = cars().first(),
            highlightItemId = null,
            chromePadding = PaddingValues(),
            onBack = {},
            onEditCar = {},
            onOpenItem = {},
            onAddItem = {},
            onAddFromTemplate = {},
            onUpdateMileage = {},
            onBatchService = {},
            onSaveTemplate = {},
            garageCode = "K7QM2XRA5BWDHJNP0TV9",
        )
    }

    @Test
    @Config(qualifiers = "w400dp-h1600dp-hdpi")
    fun itemLight() = capture("04_item_light", dark = false) {
        val car = cars().first()
        val item = car.items.first()
        ItemScreen(car = car, summary = item, chromePadding = PaddingValues(), onBack = {}, onEdit = {}, onReplaced = {}, onEditService = {})
    }

    @Test
    @Config(qualifiers = "w400dp-h1400dp-hdpi")
    fun dueDark() = capture("05_due_dark") {
        WithTabs(1) { DueScreen(cars(), null, it, onCarFilter = {}, onOpenItem = {}, onAddCar = {}) }
    }

    @Test
    fun journalLight() = capture("06_journal_light", dark = false) {
        val snapshot = Samples.snapshot(today)
        WithTabs(2) {
            JournalScreen(
                snapshot = snapshot,
                loaded = true,
                entries = snapshot.journal(null, null, Period.ALL, today),
                carId = null,
                category = null,
                period = Period.ALL,
                chromePadding = it,
                onCar = {},
                onCategory = {},
                onPeriod = {},
                onOpenItem = {},
                onAddCar = {},
            )
        }
    }

    @Test
    @Config(qualifiers = "w400dp-h3600dp-hdpi")
    fun settingsDark() = capture("07_settings_dark") {
        WithTabs(3) {
            SettingsScreen(
                settings = Settings(deviceName = "Папа", mutedCars = setOf("c2")),
                chromePadding = it,
                onDeviceName = {},
                onTheme = {},
                onThresholds = {},
                onAbout = {},
                notifications = NotificationSettings(
                    allowed = false,
                    cars = Samples.snapshot(today).cars,
                    showAutostart = true,
                ),
                garage = GarageUiState(configured = true, status = SyncStatus.NoGarage),
            )
        }
    }

    @Test
    fun onboardingDark() = capture("08_onboarding_dark") {
        OnboardingScreen(onContinue = { _, _ -> })
    }

    @Test
    fun mileageDialogDark() = capture("09_mileage_dialog_dark", screen = true) {
        MileageDialog(currentKm = 85_900, onDismiss = {}, onSave = { _, _ -> })
    }

    @Test
    @Config(qualifiers = "w400dp-h1100dp-hdpi")
    fun serviceSheetDark() = capture("10_service_sheet_dark", screen = true) {
        val car = cars().first()
        ServiceSheet(
            request = ServiceRequest.New(car.activeItems.take(2).map { it.item.id }, batch = true),
            items = car.items,
            currentKm = car.currentKm,
            onDismiss = {},
            onSubmit = {},
        )
    }

    @Test
    @Config(qualifiers = "w400dp-h2600dp-hdpi")
    fun showcaseLight() = capture("11_showcase_light", dark = false) {
        ShowcaseScreen(contentPadding = PaddingValues(), themeMode = ThemeMode.LIGHT, onThemeModeChange = {})
    }

    @Test
    fun permissionDialogDark() = capture("12_permission_dialog_dark", screen = true) {
        NotificationPermissionDialog(onAllow = {}, onLater = {})
    }

    @Test
    fun timePickerLight() = capture("13_time_picker_light", dark = false, screen = true) {
        SteamTimePickerDialog(title = "Время проверки", hour = 10, minute = 0, onDismiss = {}, onConfirm = { _, _ -> })
    }

    @Test
    fun voiceMissingDialogLight() = capture("14_voice_missing_light", dark = false, screen = true) {
        VoiceMissingDialog(onInstall = {}, onTurnOff = {}, onDismiss = {})
    }

    @Test
    @Config(qualifiers = "w400dp-h1300dp-hdpi")
    fun garageSettingsDark() = capture("15_garage_settings_dark") {
        Column(Modifier.padding(16.dp)) {
            GaragePanel(
                state = GarageUiState(
                    configured = true,
                    garage = GarageEntity(code = "K7QM2XRA5BWDHJNP0TV9", name = "", joinedAt = 0),
                    status = SyncStatus.Idle,
                    members = listOf(GarageMember("u1", "Папа", 1), GarageMember("u2", "Мама", 2), GarageMember("u3", "", 3)),
                    myUid = "u1",
                    lastSyncAt = 1_792_740_000_000,
                    lastReceived = 3,
                    lastSent = 1,
                ),
                actions = GarageActions(),
            )
        }
    }

    @Test
    fun joinDialogLight() = capture("16_join_dialog_light", dark = false, screen = true) {
        JoinGarageDialog(
            initialCode = "K7QM2XRA5BWDHJNP0TV9",
            busy = false,
            error = SyncError.OFFLINE,
            configured = true,
            alreadyInGarage = false,
            onJoin = {},
            onDismiss = {},
        )
    }
}

/** Тестовые данные: две машины, расходники во всех статусах, замены и показания пробега. */
object Samples {
    private fun day(date: LocalDate) = date.toEpochDay()

    fun snapshot(today: LocalDate): GarageSnapshot {
        val vesta = CarEntity(
            id = "c1", garageId = null, updatedAt = 0, updatedBy = "d",
            name = "Веста", make = "Lada", model = "Vesta", year = 2019, plate = "А123ВС 77", color = 0,
        )
        val duster = CarEntity(
            id = "c2", garageId = "GARAGE", updatedAt = 0, updatedBy = "d",
            name = "Папин Дастер", make = "Renault", model = "Duster", plate = "М456ОР 750", color = 4,
        )
        fun item(id: String, car: String, name: String, category: Category, km: Int?, months: Int?, part: String = "") =
            ItemEntity(id = id, garageId = null, updatedAt = 0, updatedBy = "d", carId = car, name = name, category = category.name,
                intervalKm = km, intervalMonths = months, defaultPart = part)
        val items = listOf(
            item("i1", "c1", "Моторное масло", Category.OILS, 10_000, 12, "Shell Helix 5W-40"),
            item("i2", "c1", "Салонный фильтр", Category.FILTERS, 15_000, 12),
            item("i3", "c1", "Тормозная жидкость", Category.FLUIDS, null, 24),
            item("i4", "c1", "Ремень ГРМ", Category.BELTS, null, null),
            item("i5", "c1", "Свечи зажигания", Category.IGNITION, 30_000, null),
            item("i6", "c1", "Воздушный фильтр", Category.FILTERS, 20_000, 24),
            item("i7", "c2", "Моторное масло", Category.OILS, 8_000, 12),
            item("i8", "c2", "Колодки передние", Category.BRAKES, 30_000, null),
        )
        fun service(id: String, car: String, item: String, date: LocalDate, km: Int, cost: Long?, author: String = "Папа") =
            ServiceEntity(id = id, garageId = null, updatedAt = 0, updatedBy = "d", carId = car, itemId = item,
                date = day(date), mileage = km, costKopecks = cost, place = Place.SERVICE.name, authorName = author)
        val services = listOf(
            service("s1", "c1", "i1", today.minusMonths(7), 76_500, 450_000),
            service("s0", "c1", "i1", today.minusMonths(19), 66_100, 380_000),
            service("s2", "c1", "i2", today.minusMonths(5), 80_000, 90_000),
            service("s3", "c1", "i3", today.minusMonths(25).minusDays(10), 52_000, 120_000),
            service("s4", "c1", "i5", today.minusMonths(16), 70_000, 260_000),
            service("s5", "c2", "i7", today.minusMonths(13), 120_000, 520_000, "Мама"),
            service("s6", "c2", "i8", today.minusMonths(10), 112_000, 340_000, "Мама"),
        )
        fun reading(id: String, car: String, date: LocalDate, km: Int) =
            MileageEntity(id = id, garageId = null, updatedAt = 0, updatedBy = "d", carId = car, date = day(date), km = km,
                source = MileageSource.MANUAL.name)
        val mileage = listOf(
            reading("m1", "c1", today.minusDays(80), 83_000),
            reading("m2", "c1", today.minusDays(30), 84_700),
            reading("m3", "c1", today.minusDays(2), 85_900),
            reading("m4", "c2", today.minusDays(20), 126_300),
        )
        return GarageSnapshot(cars = listOf(vesta, duster), items = items, services = services, mileage = mileage)
    }
}
