package app.steamgarage.notify

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/** Переходы в системные настройки и оболочки, которые глушат фоновые задачи. */
object SystemSettings {
    /** Производители, у которых фоновые проверки часто не срабатывают без автозапуска и снятия ограничений батареи. */
    private val AGGRESSIVE_OEMS = setOf(
        "xiaomi", "redmi", "poco", "huawei", "honor", "oppo", "vivo", "realme",
        "oneplus", "meizu", "asus", "letv", "tecno", "infinix",
    )

    fun needsAutostartHint(): Boolean {
        val manufacturer = Build.MANUFACTURER.orEmpty().lowercase()
        val brand = Build.BRAND.orEmpty().lowercase()
        return AGGRESSIVE_OEMS.any { manufacturer.contains(it) || brand.contains(it) }
    }

    /** Название производителя для подсказки: «Xiaomi». */
    fun manufacturerName(): String =
        Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }.ifBlank { Build.BRAND.orEmpty() }

    /** Карточка приложения в настройках телефона: там автозапуск, батарея и разрешения. */
    fun openAppDetails(context: Context) {
        start(
            context,
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
        )
    }

    /** Настройки уведомлений приложения; если оболочка их не знает — карточка приложения. */
    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        if (!start(context, intent)) openAppDetails(context)
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
