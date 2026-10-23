package app.steamgarage.notify

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.steamgarage.MainActivity
import app.steamgarage.R
import app.steamgarage.data.CarSummary
import app.steamgarage.data.ItemSummary
import app.steamgarage.domain.ItemState
import app.steamgarage.domain.ItemStatus
import app.steamgarage.domain.Metric
import app.steamgarage.ui.Format
import app.steamgarage.ui.navigation.CarAction
import app.steamgarage.ui.navigation.LaunchExtras

/** Сборка и показ уведомлений. Тексты — из strings.xml. */
object Notifier {
    private const val GROUP_DUE = "app.steamgarage.DUE"
    private const val SUMMARY_ID = 9_999

    fun canNotify(context: Context): Boolean {
        val permitted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return permitted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun dueNotificationId(carId: String): Int = 10_000 + (carId.hashCode() and 0xFFFF)

    private fun mileageNotificationId(carId: String): Int = 80_000 + (carId.hashCode() and 0xFFFF)

    /** Одно уведомление на машину: «Веста: 2 расходника пора менять» и строки по расходникам. */
    fun showDue(context: Context, car: CarSummary, items: List<ItemSummary>) {
        if (items.isEmpty() || !canNotify(context)) return
        val res = context.resources
        val overdue = items.count { it.state.status == ItemStatus.OVERDUE }
        val soon = items.size - overdue
        val title = when {
            soon == 0 -> res.getQuantityString(R.plurals.notify_due_overdue_title, overdue, car.car.name, overdue)
            overdue == 0 -> res.getQuantityString(R.plurals.notify_due_soon_title, soon, car.car.name, soon)
            else -> context.getString(R.string.notify_due_mixed_title, car.car.name, overdue, soon)
        }
        val lines = items.map { context.getString(R.string.notify_item_line, it.item.name, shortLine(context, it.state)) }
        val id = dueNotificationId(car.car.id)
        val itemIds = items.joinToString(",") { it.item.id }

        val open = activityIntent(context, id * 10 + 1, car.car.id, highlight = items.first().item.id)
        val replaced = activityIntent(
            context,
            id * 10 + 2,
            car.car.id,
            highlight = items.first().item.id,
            action = CarAction.SERVICE,
            actionItems = itemIds,
        )
        val snooze = PendingIntent.getBroadcast(
            context,
            id * 10 + 3,
            Intent(context, NotificationActionReceiver::class.java)
                .setAction(NotificationActionReceiver.ACTION_SNOOZE)
                .putExtra(LaunchExtras.EXTRA_ITEMS, itemIds)
                .putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val style = NotificationCompat.InboxStyle().setBigContentTitle(title)
        lines.forEach { style.addLine(it) }
        val notification = NotificationCompat.Builder(context, Channels.DUE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(lines.joinToString(context.getString(R.string.separator_dot)))
            .setStyle(style)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setGroup(GROUP_DUE)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(0, context.getString(R.string.notify_action_replaced), replaced)
            .addAction(0, context.getString(R.string.notify_action_snooze), snooze)
            .build()
        post(context, id, notification)
        postSummary(context)
    }

    /** «Обновите пробег»: показаний давно не было, прогноз устарел. */
    fun showMileageReminder(context: Context, car: CarSummary, days: Int) {
        if (!canNotify(context)) return
        val id = mileageNotificationId(car.car.id)
        val title = context.getString(R.string.notify_mileage_title, car.car.name)
        val text = context.resources.getQuantityString(R.plurals.notify_mileage_text, days, days)
        val open = activityIntent(context, id * 10 + 1, car.car.id, action = CarAction.MILEAGE)
        val notification = NotificationCompat.Builder(context, Channels.MILEAGE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(0, context.getString(R.string.notify_action_mileage), open)
            .build()
        post(context, id, notification)
    }

    fun cancel(context: Context, id: Int) {
        NotificationManagerCompat.from(context).cancel(id)
    }

    private fun postSummary(context: Context) {
        val summary = NotificationCompat.Builder(context, Channels.DUE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.channel_due))
            .setGroup(GROUP_DUE)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setAutoCancel(true)
            .build()
        post(context, SUMMARY_ID, summary)
    }

    private fun post(context: Context, id: Int, notification: android.app.Notification) {
        if (!canNotify(context)) return
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Разрешение отозвали между проверкой и показом — молча пропускаем.
        }
    }

    private fun activityIntent(
        context: Context,
        requestCode: Int,
        carId: String,
        highlight: String? = null,
        action: String? = null,
        actionItems: String? = null,
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(LaunchExtras.ACTION_OPEN_CAR)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(LaunchExtras.EXTRA_CAR, carId)
            .putExtra(LaunchExtras.EXTRA_NOTIFICATION_ID, requestCode / 10)
        highlight?.let { intent.putExtra(LaunchExtras.EXTRA_HIGHLIGHT, it) }
        action?.let { intent.putExtra(LaunchExtras.EXTRA_ACTION, it) }
        actionItems?.let { intent.putExtra(LaunchExtras.EXTRA_ITEMS, it) }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /** «превышено на 350 км» / «осталось 15 дн.» — для строки уведомления. */
    fun shortLine(context: Context, state: ItemState): String = when {
        state.primary == Metric.KM -> {
            val left = state.kmLeft ?: 0
            if (left > 0) context.getString(R.string.notify_short_km_left, Format.km(left))
            else context.getString(R.string.notify_short_km_over, Format.kmAbs(left))
        }
        else -> {
            val left = state.daysLeft ?: 0
            if (left > 0) context.getString(R.string.notify_short_days_left, left)
            else context.getString(R.string.notify_short_days_over, -left)
        }
    }
}
