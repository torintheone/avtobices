package app.steamgarage.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import app.steamgarage.domain.ItemStatus
import app.steamgarage.ui.theme.Steam
import app.steamgarage.ui.theme.SteamPalette
import kotlin.math.cos
import kotlin.math.sin

/** Показания манометра расходника. */
@Immutable
data class GaugeReading(
    val status: ItemStatus,
    /** Износ: 0 — только что заменили, 1 — срок наступил, больше 1 — просрочено; null — нет данных. */
    val wear: Float?,
    /** Доля интервала, с которой начинается зона «Скоро» (порог ÷ интервал). */
    val warnFraction: Float = 0.1f,
    /** Остаток цифрами одометра, например «1 200» или «350»; null — без цифр. */
    val readout: String? = null,
    /** Единица под цифрами — коротко, чтобы уместилась между упорами: «км», «дн.», «%». */
    val unit: String? = null,
    /** Срок превышен: цифры на барабанах красной эмали. */
    val alarm: Boolean = false,
)

/** Шкала размечена до 125 % интервала: последняя четверть — красная зона. */
private const val SCALE_MAX = 1.25f
private const val START_ANGLE = 135f
private const val SWEEP = 270f

/** Положение стрелки «на упоре» — когда данных нет. */
private const val REST = -0.035f

/** Верх окошка с цифрами — доля размера манометра: чуть ниже латунной оси стрелки (её край — 0,55). */
private const val READOUT_TOP = 0.565f

/**
 * Манометр: круглая шкала с рисками и тремя цветными зонами, стрелка на пружине.
 * В центре — остаток цифрами одометра. [compact] — мини-манометр без цифр и мелких рисок.
 */
@Composable
fun SteamGauge(
    reading: GaugeReading,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    compact: Boolean = false,
    readoutStyle: TextStyle = Steam.text.odometerSmall,
) {
    val palette = Steam.palette
    val reduceMotion = Steam.reduceMotion
    val wear = reading.wear
    val target = if (reading.status == ItemStatus.NO_DATA || wear == null) REST else wear.coerceIn(0f, SCALE_MAX)
    val needle = remember { Animatable(REST) }
    LaunchedEffect(target, reduceMotion) {
        if (reduceMotion) {
            needle.snapTo(target)
        } else {
            needle.animateTo(target, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessVeryLow))
        }
    }
    val tremble = if (reading.status == ItemStatus.OVERDUE && !reduceMotion) {
        rememberInfiniteTransition(label = "tremble").animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 70, easing = LinearEasing), RepeatMode.Reverse),
            label = "tremble",
        ).value
    } else {
        0f
    }
    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .then(semanticsModifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawDial(reading.warnFraction, palette, compact)
            drawNeedle(needle.value + tremble * 0.008f, palette)
        }
        if (!compact) {
            // Окошко с цифрами — сразу под осью стрелки, подпись — в просвете между упорами.
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = maxWidth * READOUT_TOP),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (reading.status == ItemStatus.NO_DATA) {
                    Box(
                        modifier = Modifier
                            .background(brassBrush(palette), CutCornerShape(3.dp))
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        EngravedText(text = "?", style = readoutStyle)
                    }
                } else if (reading.readout != null) {
                    OdometerDisplay(reading.readout, style = readoutStyle, alarm = reading.alarm)
                }
                if (reading.unit != null && reading.status != ItemStatus.NO_DATA) {
                    Text(
                        text = reading.unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.gaugeInk,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun angleOf(value: Float): Float = START_ANGLE + SWEEP * value / SCALE_MAX

private fun polar(center: Offset, angleDeg: Float, radius: Float): Offset {
    val a = Math.toRadians(angleDeg.toDouble())
    return center + Offset(cos(a).toFloat(), sin(a).toFloat()) * radius
}

private fun DrawScope.drawDial(warnFraction: Float, palette: SteamPalette, compact: Boolean) {
    val r = size.minDimension / 2f
    val c = center

    // Тень и латунный ободок.
    drawCircle(Color.Black.copy(alpha = 0.35f), r * 0.97f, c + Offset(r * 0.02f, r * 0.05f))
    drawCircle(
        brush = Brush.sweepGradient(
            listOf(
                palette.brassLight, palette.brassShadow, palette.brassDark, palette.brassLight,
                palette.brassShadow, palette.brassDark, palette.brassLight,
            ),
            center = c,
        ),
        radius = r * 0.97f,
        center = c,
    )
    drawCircle(palette.brassShadow, r * 0.86f, c)

    // Циферблат.
    drawCircle(
        brush = Brush.radialGradient(
            listOf(lerp(palette.gaugeFace, Color.White, 0.35f), palette.gaugeFace, lerp(palette.gaugeFace, palette.brassShadow, 0.30f)),
            center = c - Offset(r * 0.15f, r * 0.2f),
            radius = r,
        ),
        radius = r * 0.84f,
        center = c,
    )

    // Цветные зоны: норма → скоро → пора менять.
    val zoneRadius = r * 0.64f
    val zoneWidth = r * if (compact) 0.20f else 0.11f
    val warnStart = (1f - warnFraction).coerceIn(0.05f, 0.95f)
    fun zone(from: Float, to: Float, color: Color) = drawArc(
        color = color,
        startAngle = angleOf(from),
        sweepAngle = angleOf(to) - angleOf(from),
        useCenter = false,
        topLeft = c - Offset(zoneRadius, zoneRadius),
        size = Size(zoneRadius * 2, zoneRadius * 2),
        style = Stroke(width = zoneWidth),
    )
    zone(0f, warnStart, palette.ok)
    zone(warnStart, 1f, palette.soon)
    zone(1f, SCALE_MAX, palette.overdue)

    // Риски: крупные через 25 %, мелкие через 5 %.
    val outer = r * 0.80f
    for (i in 0..25) {
        val major = i % 5 == 0
        if (compact && !major) continue
        val angle = angleOf(i * 0.05f)
        val length = if (major) r * 0.14f else r * 0.06f
        drawLine(
            color = palette.gaugeInk,
            start = polar(c, angle, outer - length),
            end = polar(c, angle, outer),
            strokeWidth = if (major) r * 0.028f else r * 0.013f,
            cap = StrokeCap.Round,
        )
    }

    // Упоры для стрелки.
    drawCircle(palette.iron, r * 0.04f, polar(c, angleOf(REST * 2.2f), r * 0.70f))
    drawCircle(palette.iron, r * 0.04f, polar(c, angleOf(SCALE_MAX + 0.035f), r * 0.70f))

    // Блик на стекле.
    drawArc(
        color = Color.White.copy(alpha = 0.16f),
        startAngle = 195f,
        sweepAngle = 75f,
        useCenter = false,
        topLeft = c - Offset(r * 0.74f, r * 0.74f),
        size = Size(r * 1.48f, r * 1.48f),
        style = Stroke(width = r * 0.07f, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawNeedle(value: Float, palette: SteamPalette) {
    val r = size.minDimension / 2f
    val c = center
    val length = r * 0.72f
    val tail = r * 0.20f
    val w = r * 0.05f
    val needle = Path().apply {
        moveTo(c.x - tail, c.y - w)
        lineTo(c.x + length, c.y - w * 0.12f)
        lineTo(c.x + length + w * 0.5f, c.y)
        lineTo(c.x + length, c.y + w * 0.12f)
        lineTo(c.x - tail, c.y + w)
        close()
    }
    rotate(angleOf(value), c) {
        translate(r * 0.02f, r * 0.04f) { drawPath(needle, Color.Black.copy(alpha = 0.30f)) }
        drawPath(needle, palette.iron)
    }
    drawCircle(
        brush = Brush.radialGradient(
            listOf(palette.brassLight, palette.brassDark, palette.brassShadow),
            center = c - Offset(r * 0.03f, r * 0.03f),
            radius = r * 0.12f,
        ),
        radius = r * 0.10f,
        center = c,
    )
    drawCircle(palette.iron, r * 0.035f, c)
}
