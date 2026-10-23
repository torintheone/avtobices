package app.steamgarage.ui.navigation

import android.content.Intent

/** Переход, о котором попросили снаружи: уведомление или ссылка-приглашение. */
sealed interface NavRequest {
    data class OpenCar(
        val carId: String,
        val highlightItemId: String? = null,
        val action: String? = null,
        val actionItems: String? = null,
    ) : NavRequest

    data class JoinGarage(val code: String) : NavRequest
}

/** Ключи интентов, которыми уведомления открывают приложение. */
object LaunchExtras {
    const val ACTION_OPEN_CAR = "app.steamgarage.action.OPEN_CAR"
    const val EXTRA_CAR = "car"
    const val EXTRA_HIGHLIGHT = "highlight"
    const val EXTRA_ACTION = "action"
    const val EXTRA_ITEMS = "items"
    /** Уведомление, из которого открыли приложение: после перехода его можно убрать. */
    const val EXTRA_NOTIFICATION_ID = "notification_id"
}

/** Разбор интента запуска. null — обычный запуск без особого перехода. */
fun Intent.toNavRequest(): NavRequest? = when (action) {
    LaunchExtras.ACTION_OPEN_CAR -> getStringExtra(LaunchExtras.EXTRA_CAR)?.let { car ->
        NavRequest.OpenCar(
            carId = car,
            highlightItemId = getStringExtra(LaunchExtras.EXTRA_HIGHLIGHT),
            action = getStringExtra(LaunchExtras.EXTRA_ACTION),
            actionItems = getStringExtra(LaunchExtras.EXTRA_ITEMS),
        )
    }
    else -> null
}
