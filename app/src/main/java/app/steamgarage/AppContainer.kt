package app.steamgarage

import android.app.Application
import android.content.Context
import app.steamgarage.data.GarageRepository
import app.steamgarage.data.Settings
import app.steamgarage.data.SettingsRepository
import app.steamgarage.data.db.AppDatabase
import app.steamgarage.data.sync.SyncException
import app.steamgarage.data.sync.SyncManager
import app.steamgarage.data.sync.SyncResult
import app.steamgarage.data.sync.SyncWorker
import app.steamgarage.notify.Channels
import app.steamgarage.notify.CheckScheduler
import app.steamgarage.sound.SoundPlayer
import app.steamgarage.sound.Voice
import app.steamgarage.sound.VoiceEvent
import app.steamgarage.ui.components.Sfx
import app.steamgarage.ui.navigation.NavRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Ручной контейнер зависимостей (без Hilt, как в SPEC.md): всё создаётся один раз на процесс. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val database: AppDatabase = AppDatabase.create(appContext)
    val settings: SettingsRepository = SettingsRepository(appContext)
    val repository: GarageRepository = GarageRepository(database, settings)

    /** Фоновые дела на всё время жизни процесса. */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Переход, о котором попросило уведомление; экран навигации забирает его и обнуляет. */
    val navRequests = MutableStateFlow<NavRequest?>(null)

    /** Текущие настройки без ожидания: звуку и голосу они нужны мгновенно, в момент нажатия. */
    val settingsState: StateFlow<Settings> = settings.settings.stateIn(appScope, SharingStarted.Eagerly, Settings())

    /** Звуки механизмов. Создаются при первом обращении — то есть только когда открыт интерфейс. */
    val sound: SoundPlayer by lazy { SoundPlayer(appContext) { settingsState.value } }

    /** Голос механика. */
    val voice: Voice by lazy { Voice(appContext) { settingsState.value } }

    /** Синхронизация общего гаража через Firebase. */
    val sync: SyncManager by lazy {
        SyncManager(appContext, database, settings, appScope) { CheckScheduler.checkSoon(appContext) }
    }

    /** Сверка по рычагу: раскрутка шестерён в начале, двойной звонок или гудок в конце и фраза механика. */
    suspend fun syncWithFeedback(): Result<SyncResult> {
        sound.play(Sfx.SYNC_START)
        return try {
            val result = sync.syncNow()
            sound.play(Sfx.SYNC_OK)
            voice.say(VoiceEvent.SYNC_OK)
            Result.success(result)
        } catch (e: SyncException) {
            sound.play(Sfx.SYNC_ERROR)
            voice.say(VoiceEvent.SYNC_ERROR)
            Result.failure(e)
        }
    }
}

@OptIn(FlowPreview::class)
class SteamGarageApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Channels.create(this)
        container.appScope.launch {
            val settings = container.settings.current()
            CheckScheduler.scheduleDaily(this@SteamGarageApplication, settings.checkHour, settings.checkMinute)
        }
        // Пробег, замена, расходник — после любого изменения данных внеочередная проверка сроков.
        container.appScope.launch {
            container.repository.changes.collect { CheckScheduler.checkSoon(this@SteamGarageApplication) }
        }
        // Изменения общих машин уходят в гараж сразу; серия правок подряд — одной отправкой.
        container.appScope.launch {
            container.repository.changes
                .filter { it.garageId != null }
                .debounce(PUSH_DEBOUNCE_MS)
                .collect { container.sync.pushPending() }
        }
        if (container.sync.configured) SyncWorker.schedule(this)
    }

    private companion object {
        const val PUSH_DEBOUNCE_MS = 800L
    }
}

/** Контейнер приложения из любого Context. */
val Context.appContainer: AppContainer
    get() = (applicationContext as SteamGarageApplication).container
