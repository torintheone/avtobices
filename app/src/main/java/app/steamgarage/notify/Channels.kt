package app.steamgarage.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import app.steamgarage.R

/**
 * Каналы уведомлений: «Сроки замены» (паровой свисток), «Пробег» и «Синхронизация» (тихий).
 * Звук канала задаётся только при создании, поэтому у канала сроков — свой идентификатор с версией.
 */
object Channels {
    const val DUE = "due_whistle"
    const val MILEAGE = "mileage"
    const val SYNC = "sync"

    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val whistle = Uri.parse(
            ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.packageName + "/" + R.raw.notify_whistle,
        )
        val due = NotificationChannel(DUE, context.getString(R.string.channel_due), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = context.getString(R.string.channel_due_description)
            setSound(
                whistle,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
        val mileage = NotificationChannel(MILEAGE, context.getString(R.string.channel_mileage), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = context.getString(R.string.channel_mileage_description)
        }
        val sync = NotificationChannel(SYNC, context.getString(R.string.channel_sync), NotificationManager.IMPORTANCE_LOW).apply {
            description = context.getString(R.string.channel_sync_description)
            setSound(null, null)
        }
        manager.createNotificationChannels(listOf(due, mileage, sync))
    }
}
