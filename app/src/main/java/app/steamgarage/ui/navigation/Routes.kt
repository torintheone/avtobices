package app.steamgarage.ui.navigation

import kotlinx.serialization.Serializable

// Экраны приложения (типобезопасная навигация Navigation Compose).

@Serializable
data object OnboardingRoute

@Serializable
data object GarageRoute

@Serializable
data object DueRoute

@Serializable
data object JournalRoute

@Serializable
data object SettingsRoute

@Serializable
data object AboutRoute

@Serializable
data object ShowcaseRoute

/**
 * Машина. [highlightItemId] — подсветить расходник (из уведомления);
 * [action] — сразу открыть действие: [CarAction.MILEAGE] или [CarAction.SERVICE] с [actionItems].
 */
@Serializable
data class CarRoute(
    val carId: String,
    val highlightItemId: String? = null,
    val action: String? = null,
    val actionItems: String? = null,
)

object CarAction {
    const val MILEAGE = "mileage"
    const val SERVICE = "service"
}

/** Новая машина ([carId] = null) или правка существующей. */
@Serializable
data class CarEditRoute(val carId: String? = null)

/** Выбор расходников из шаблона и «Когда меняли в последний раз?». */
@Serializable
data class TemplateRoute(val carId: String)

@Serializable
data class ItemRoute(val itemId: String)

/** Новый расходник ([itemId] = null) или правка существующего. */
@Serializable
data class ItemEditRoute(val carId: String, val itemId: String? = null)
