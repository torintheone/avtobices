package app.steamgarage.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.steamgarage.R
import app.steamgarage.ui.theme.Steam
import kotlinx.coroutines.launch

/**
 * Ввод пробега барабанами одометра: каждый разряд крутится свайпом вверх-вниз,
 * касание верхней половины барабана прибавляет единицу, нижней — убавляет. Каждый шаг щёлкает.
 */
@Composable
fun OdometerInput(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    digits: Int = 7,
) {
    val palette = Steam.palette
    val text = value.coerceAtLeast(0).toString().padStart(digits, '0').takeLast(digits)
    val currentValue by rememberUpdatedState(value)
    val currentOnChange by rememberUpdatedState(onValueChange)
    Row(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(palette.brassLight, palette.brassShadow)), RoundedCornerShape(6.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        text.forEachIndexed { index, ch ->
            val place = digits - 1 - index
            val weight = pow10(place)
            OdometerDrum(
                digit = ch - '0',
                position = index + 1,
                onStep = { delta ->
                    val digit = (currentValue / weight) % 10
                    val newDigit = (digit + delta + 10) % 10
                    currentOnChange(currentValue - digit * weight + newDigit * weight)
                },
            )
        }
    }
}

private fun pow10(n: Int): Int {
    var r = 1
    repeat(n) { r *= 10 }
    return r
}

@Composable
private fun OdometerDrum(digit: Int, position: Int, onStep: (Int) -> Unit) {
    val palette = Steam.palette
    val player = LocalSfx.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val stepPx = with(LocalDensity.current) { 26.dp.toPx() }
    var drag by remember { mutableFloatStateOf(0f) }
    val settle = remember { Animatable(0f) }
    val currentOnStep by rememberUpdatedState(onStep)

    fun step(delta: Int) {
        currentOnStep(delta)
        player.play(Sfx.DRUM_TICK)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val description = stringResource(R.string.odometer_drum_description, position, digit)
    val upLabel = stringResource(R.string.odometer_drum_up)
    val downLabel = stringResource(R.string.odometer_drum_down)
    val offset = drag + settle.value
    Box(
        modifier = Modifier
            .width(38.dp)
            .height(64.dp)
            .clipToBounds()
            .background(
                Brush.verticalGradient(
                    0f to Color.Black,
                    0.22f to palette.odometerWindow,
                    0.5f to lerp(palette.odometerWindow, Color.White, 0.12f),
                    0.78f to palette.odometerWindow,
                    1f to Color.Black,
                ),
                RoundedCornerShape(3.dp),
            )
            .border(1.dp, Color.Black.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
            .semantics {
                contentDescription = description
                customActions = listOf(
                    CustomAccessibilityAction(upLabel) { step(1); true },
                    CustomAccessibilityAction(downLabel) { step(-1); true },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { tap -> step(if (tap.y < size.height / 2f) 1 else -1) }
            }
            .draggable(
                state = rememberDraggableState { delta ->
                    drag += delta
                    // Свайп вниз — цифры едут вниз, сверху приходит следующая (больше).
                    while (drag > stepPx) {
                        drag -= stepPx
                        step(1)
                    }
                    while (drag < -stepPx) {
                        drag += stepPx
                        step(-1)
                    }
                },
                orientation = Orientation.Vertical,
                onDragStopped = {
                    val rest = drag
                    drag = 0f
                    scope.launch {
                        settle.snapTo(rest)
                        settle.animateTo(0f, spring(dampingRatio = 0.6f))
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Барабан: текущая цифра в окне, соседние — выше и ниже, частично видны.
        for (shift in -1..1) {
            val shown = (digit - shift + 10) % 10
            Text(
                text = shown.toString(),
                style = Steam.text.odometer.copy(fontSize = 28.sp),
                color = palette.odometerDigit.copy(alpha = if (shift == 0) 1f else 0.45f),
                modifier = Modifier.graphicsLayer { translationY = shift * stepPx + offset },
            )
        }
    }
}
