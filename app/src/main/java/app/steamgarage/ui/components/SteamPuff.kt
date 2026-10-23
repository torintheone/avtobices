package app.steamgarage.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import app.steamgarage.ui.theme.Steam

/**
 * Клуб пара: облачка поднимаются, расплываются и тают за секунду. Каждое новое значение [trigger]
 * (кроме 0) выпускает новый клуб. В режиме без анимаций пара нет.
 */
@Composable
fun SteamPuff(trigger: Int, modifier: Modifier = Modifier) {
    if (Steam.reduceMotion) return
    val progress = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = PUFF_MS, easing = LinearOutSlowInEasing))
    }
    val steam = if (Steam.palette.isDark) STEAM_ON_DARK else STEAM_ON_LIGHT
    Canvas(modifier) {
        val p = progress.value
        if (p >= 1f) return@Canvas
        val base = Offset(size.width / 2f, size.height)
        for (i in 0 until CLOUDS) {
            val delay = i * 0.07f
            val t = ((p - delay) / (1f - delay)).coerceIn(0f, 1f)
            if (t <= 0f) continue
            val drift = (i - (CLOUDS - 1) / 2f) * size.width * 0.07f * t
            val rise = size.height * (0.1f + 0.8f * t)
            val radius = size.minDimension * (0.10f + 0.20f * t) * (0.8f + 0.12f * (i % 3))
            drawCircle(steam.copy(alpha = (1f - t) * 0.55f), radius, base + Offset(drift, -rise))
        }
    }
}

private const val PUFF_MS = 1_100
private const val CLOUDS = 6
private val STEAM_ON_DARK = Color(0xFFEDE6D6)
private val STEAM_ON_LIGHT = Color(0xFF7D7466)
