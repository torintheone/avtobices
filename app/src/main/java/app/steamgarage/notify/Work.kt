package app.steamgarage.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.steamgarage.appContainer
import app.steamgarage.domain.Defaults
import app.steamgarage.ui.navigation.LaunchExtras
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Фоновая проверка сроков (WorkManager). */
class DueCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        DueChecker(applicationContext.appContainer).run()
        Result.success()
    } catch (_: Exception) {
        Result.retry()
    }
}

/** Расписание проверок: ежедневно в выбранное время (окно в час) и внеочередная после изменений. */
object CheckScheduler {
    private const val DAILY = "due-check-daily"
    private const val SOON = "due-check-soon"

    /**
     * Ежедневная проверка в [hour]:[minute]. [reschedule] = true — время поменяли в настройках,
     * старое расписание заменяется; иначе (при запуске приложения) существующее сохраняется.
     */
    fun scheduleDaily(context: Context, hour: Int, minute: Int, reschedule: Boolean = false) {
        val now = ZonedDateTime.now()
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delayMinutes = Duration.between(now, next).toMinutes()
        val request = PeriodicWorkRequestBuilder<DueCheckWorker>(
            1, TimeUnit.DAYS,
            Defaults.CHECK_FLEX_MINUTES, TimeUnit.MINUTES,
        )
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .build()
        workManager(context)?.enqueueUniquePeriodicWork(
            DAILY,
            if (reschedule) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /** Внеочередная проверка через пару секунд: после пробега, замены, синхронизации. */
    fun checkSoon(context: Context) {
        val request = OneTimeWorkRequestBuilder<DueCheckWorker>()
            .setInitialDelay(3, TimeUnit.SECONDS)
            .build()
        workManager(context)?.enqueueUniqueWork(SOON, ExistingWorkPolicy.REPLACE, request)
    }

    /** null — WorkManager не запущен (так бывает только в тестах без полноценного приложения). */
    private fun workManager(context: Context): WorkManager? = try {
        WorkManager.getInstance(context)
    } catch (_: IllegalStateException) {
        null
    }
}

/** Действие «Напомнить через неделю» из уведомления. */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SNOOZE) return
        val items = intent.getStringExtra(LaunchExtras.EXTRA_ITEMS).orEmpty().split(',').filter { it.isNotBlank() }
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val pending = goAsync()
        scope.launch {
            try {
                if (items.isNotEmpty()) DueChecker(context.appContainer).snooze(items)
            } finally {
                NotificationManagerCompat.from(context).cancel(notificationId)
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_SNOOZE = "app.steamgarage.action.SNOOZE"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
