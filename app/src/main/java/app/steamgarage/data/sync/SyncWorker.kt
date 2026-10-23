package app.steamgarage.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.steamgarage.appContainer
import app.steamgarage.domain.Defaults
import java.util.concurrent.TimeUnit

/** Фоновая сверка гаража раз в [Defaults.SYNC_PERIOD_HOURS] часов, когда есть сеть. */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val sync = applicationContext.appContainer.sync
        if (!sync.configured) return Result.success()
        return try {
            sync.syncNow()
            Result.success()
        } catch (e: SyncException) {
            // Нет сети — попробуем ещё; остальное (не в гараже, нет доступа) повтором не лечится.
            if (e.error == SyncError.OFFLINE) Result.retry() else Result.success()
        }
    }

    companion object {
        private const val NAME = "garage-sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(Defaults.SYNC_PERIOD_HOURS, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            try {
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
            } catch (_: IllegalStateException) {
                // WorkManager не запущен — так бывает только в тестах без полноценного приложения.
            }
        }
    }
}
