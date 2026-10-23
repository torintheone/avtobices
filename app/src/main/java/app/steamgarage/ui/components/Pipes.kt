package app.steamgarage.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import app.steamgarage.ui.theme.Steam

/** Разделитель секций: медная труба с латунными муфтами на болтах. */
@Composable
fun PipeDivider(modifier: Modifier = Modifier) {
    val palette = Steam.palette
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp),
    ) {
        val pipeHeight = 8.dp.toPx()
        val top = (size.height - pipeHeight) / 2f
        drawRect(
            brush = copperPipeBrush(palette, top, top + pipeHeight),
            topLeft = Offset(0f, top),
            size = Size(size.width, pipeHeight),
        )
        val couplingWidth = 12.dp.toPx()
        val step = 112.dp.toPx()
        val count = (size.width / step).toInt().coerceAtLeast(1)
        val gap = size.width / (count + 1)
        for (i in 1..count) {
            val x = gap * i - couplingWidth / 2f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(palette.brassLight, palette.brassDark, lerp(palette.brassShadow, Color.Black, 0.3f)),
                ),
                topLeft = Offset(x, 0f),
                size = Size(couplingWidth, size.height),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
            val bolt = 1.6.dp.toPx()
            drawCircle(palette.brassShadow, bolt, Offset(x + couplingWidth / 2f, 3.5.dp.toPx()))
            drawCircle(palette.brassShadow, bolt, Offset(x + couplingWidth / 2f, size.height - 3.5.dp.toPx()))
        }
    }
}
