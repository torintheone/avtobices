package app.steamgarage.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.steamgarage.ui.theme.Steam
import app.steamgarage.ui.theme.SteamPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Две сцепленные шестерни — индикатор загрузки и синхронизации.
 * Крутятся только пока [spinning]; при отключённых анимациях стоят на месте.
 */
@Composable
fun SteamGears(
    spinning: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Color? = null,
    contentDescription: String? = null,
) {
    val palette = Steam.palette
    val animate = spinning && !Steam.reduceMotion
    val angle = if (animate) {
        rememberInfiniteTransition(label = "gears").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 2400, easing = LinearEasing), RepeatMode.Restart),
            label = "gears",
        ).value
    } else {
        0f
    }
    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }
    Canvas(modifier.size(size).then(semanticsModifier)) {
        val unit = this.size.minDimension
        val bigR = unit * 0.36f
        val smallR = unit * 0.25f
        val bigCenter = Offset(unit * 0.38f, unit * 0.58f)
        val smallCenter = Offset(unit * 0.73f, unit * 0.30f)
        drawGear(gearPath(teeth = 10, radius = bigR, innerRatio = 0.78f), bigCenter, bigR, angle, palette, tint)
        // Малая шестерня крутится в обратную сторону быстрее — как в настоящей паре.
        drawGear(gearPath(teeth = 7, radius = smallR, innerRatio = 0.74f), smallCenter, smallR, -angle * 10f / 7f + 12f, palette, tint)
    }
}

/** Контур шестерни с центром в нуле: зубья трапецией. */
private fun gearPath(teeth: Int, radius: Float, innerRatio: Float): Path {
    val outer = radius
    val inner = radius * innerRatio
    val path = Path()
    val step = 2 * PI / teeth
    for (i in 0 until teeth) {
        val a0 = i * step
        val points = listOf(
            a0 to inner,
            a0 + step * 0.18 to outer,
            a0 + step * 0.50 to outer,
            a0 + step * 0.68 to inner,
        )
        points.forEachIndexed { j, (a, rr) ->
            val x = (cos(a) * rr).toFloat()
            val y = (sin(a) * rr).toFloat()
            if (i == 0 && j == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
    }
    path.close()
    return path
}

private fun DrawScope.drawGear(
    path: Path,
    center: Offset,
    radius: Float,
    angle: Float,
    palette: SteamPalette,
    tint: Color?,
) {
    translate(center.x, center.y) {
        rotate(angle, Offset.Zero) {
            if (tint != null) {
                drawPath(path, tint)
            } else {
                drawPath(
                    path,
                    Brush.linearGradient(
                        listOf(palette.brassLight, palette.brassDark, palette.brassShadow),
                        start = Offset(-radius, -radius),
                        end = Offset(radius, radius),
                    ),
                )
            }
            drawCircle(lerp(palette.brassShadow, Color.Black, 0.35f), radius * 0.30f, Offset.Zero)
            drawCircle(tint ?: palette.brassDark, radius * 0.14f, Offset.Zero)
        }
    }
}
