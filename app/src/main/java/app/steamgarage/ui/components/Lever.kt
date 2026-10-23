package app.steamgarage.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.steamgarage.ui.theme.Steam
import app.steamgarage.ui.theme.SteamPalette
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

/**
 * Рычаг — главная кнопка действия («Заменено», «Синхронизировать»).
 * Срабатывает по нажатию или свайпу вниз: рукоять проходит ход вниз, щёлкает, даёт короткую вибрацию
 * и пружинит обратно.
 */
@Composable
fun SteamLever(
    text: String,
    onPull: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    sfx: Sfx = Sfx.LEVER,
) {
    val palette = Steam.palette
    val scope = rememberCoroutineScope()
    val travel = remember { Animatable(0f) }
    var dragging by remember { mutableStateOf(false) }
    var drag by remember { mutableFloatStateOf(0f) }
    var busy by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val player = LocalSfx.current
    val currentOnPull by rememberUpdatedState(onPull)
    val travelPx = with(LocalDensity.current) { 56.dp.toPx() }
    val reduceMotion = Steam.reduceMotion
    var puffs by remember { mutableIntStateOf(0) }

    suspend fun pullFrom(start: Float) {
        busy = true
        player.play(sfx)
        if (reduceMotion) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            currentOnPull()
            busy = false
            return
        }
        travel.snapTo(start)
        travel.animateTo(1f, tween(durationMillis = 150, easing = FastOutSlowInEasing))
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        puffs++
        currentOnPull()
        travel.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMediumLow))
        busy = false
    }

    val shape = CutCornerShape(8.dp)
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .heightIn(min = 64.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(lerp(palette.surface, palette.copper, 0.10f), palette.surfaceDeep),
                ),
            )
            .border(2.dp, Brush.linearGradient(listOf(palette.copperLight, palette.copper)), shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
            ) {
                if (!busy) scope.launch { pullFrom(0f) }
            }
            .draggable(
                state = rememberDraggableState { delta ->
                    if (!busy) drag = (drag + delta / travelPx).coerceIn(0f, 1f)
                },
                orientation = Orientation.Vertical,
                enabled = enabled,
                onDragStarted = {
                    dragging = true
                    drag = 0f
                },
                onDragStopped = {
                    dragging = false
                    val reached = drag
                    drag = 0f
                    if (busy) return@draggable
                    if (reached > 0.6f) {
                        pullFrom(reached)
                    } else {
                        travel.snapTo(reached)
                        travel.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium))
                    }
                },
            )
            .padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val progress = if (dragging) drag else travel.value
        Box {
            Canvas(modifier = Modifier.width(56.dp).height(64.dp)) {
                drawLever(progress, palette)
            }
            // Пар из-под рычага, когда он дошёл до упора.
            SteamPuff(trigger = puffs, modifier = Modifier.width(56.dp).height(64.dp))
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = palette.text,
        )
    }
}

/** Рукоять на оси: [progress] 0 — поднята, 1 — опущена до упора. */
private fun DrawScope.drawLever(progress: Float, palette: SteamPalette) {
    val pivot = Offset(16.dp.toPx(), size.height / 2f)
    val arm = (size.height / 2f - 12.dp.toPx()).coerceAtMost(26.dp.toPx())
    val angle = Math.toRadians((-55.0 + 110.0 * progress))
    val end = pivot + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * arm

    // Паз, по которому ходит рукоять.
    drawArc(
        color = palette.surfaceDeep,
        startAngle = -60f,
        sweepAngle = 120f,
        useCenter = false,
        topLeft = pivot - Offset(arm, arm),
        size = Size(arm * 2, arm * 2),
        style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round),
    )
    drawArc(
        color = Color.Black.copy(alpha = 0.45f),
        startAngle = -60f,
        sweepAngle = 120f,
        useCenter = false,
        topLeft = pivot - Offset(arm, arm),
        size = Size(arm * 2, arm * 2),
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
    )
    // Стержень.
    drawLine(palette.iron, pivot, end, strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
    drawLine(
        color = lerp(palette.iron, Color.White, 0.35f),
        start = pivot,
        end = end,
        strokeWidth = 1.5.dp.toPx(),
        cap = StrokeCap.Round,
    )
    // Медный шар рукояти с бликом.
    val knob = 8.dp.toPx()
    drawCircle(Color.Black.copy(alpha = 0.35f), knob, end + Offset(1.5.dp.toPx(), 2.dp.toPx()))
    drawCircle(
        brush = Brush.radialGradient(
            0f to lerp(palette.copperLight, Color.White, 0.5f),
            0.4f to palette.copper,
            1f to lerp(palette.copper, Color.Black, 0.6f),
            center = end - Offset(knob * 0.35f, knob * 0.4f),
            radius = knob * 1.5f,
        ),
        radius = knob,
        center = end,
    )
    // Латунная ось с болтом.
    val hub = 9.dp.toPx()
    drawCircle(
        brush = Brush.radialGradient(
            0f to palette.brassLight,
            0.7f to palette.brassDark,
            1f to palette.brassShadow,
            center = pivot - Offset(hub * 0.3f, hub * 0.3f),
            radius = hub * 1.4f,
        ),
        radius = hub,
        center = pivot,
    )
    drawLine(
        color = palette.brassShadow,
        start = pivot - Offset(hub * 0.45f, -hub * 0.2f),
        end = pivot + Offset(hub * 0.45f, -hub * 0.2f),
        strokeWidth = 1.5.dp.toPx(),
        cap = StrokeCap.Round,
    )
}
