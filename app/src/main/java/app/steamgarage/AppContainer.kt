package app.steamgarage

import android.app.Application
import android.content.Context
import app.steamgarage.data.GarageRepository
import app.steamgarage.data.SettingsRepository
import app.steamgarage.data.db.AppDatabase

/** Ручной контейнер зависимостей (без Hilt, как в SPEC.md): всё создаётся один раз на процесс. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val database: AppDatabase = AppDatabase.create(appContext)
    val settings: SettingsRepository = SettingsRepository(appContext)
    val repository: GarageRepository = GarageRepository(database, settings)
}

class SteamGarageApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Контейнер приложения из любого Context. */
val Context.appContainer: AppContainer
    get() = (applicationContext as SteamGarageApplication).container
