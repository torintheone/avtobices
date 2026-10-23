package app.steamgarage.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.steamgarage.ui.theme.Steam
import app.steamgarage.ui.theme.SteamPalette

/** Полированная латунь: полосы света по диагонали, как на начищенной табличке. */
fun brassBrush(palette: SteamPalette, start: Offset = Offset.Zero, end: Offset = Offset.Infinite): Brush =
    Brush.linearGradient(
        0.00f to lerp(palette.brassDark, palette.brassShadow, 0.25f),
        0.30f to palette.brassLight,
        0.55f to palette.brassDark,
        0.80f to lerp(palette.brassLight, palette.brassDark, 0.35f),
        1.00f to lerp(palette.brassDark, palette.brassShadow, 0.35f),
        start = start,
        end = end,
    )

/** Латунь для горизонтальных панелей (нижняя навигация): свет сверху, тень снизу. */
fun brassVerticalBrush(palette: SteamPalette): Brush =
    Brush.verticalGradient(
        0.00f to palette.brassLight,
        0.12f to lerp(palette.brassLight, palette.brassDark, 0.3f),
        0.55f to palette.brassDark,
        1.00f to lerp(palette.brassDark, palette.brassShadow, 0.55f),
    )

/** Медная труба: блик посередине, тени по краям. */
fun copperPipeBrush(palette: SteamPalette, top: Float, bottom: Float): Brush =
    Brush.verticalGradient(
        0.00f to lerp(palette.copper, Color.Black, 0.45f),
        0.35f to palette.copperLight,
        0.55f to palette.copper,
        1.00f to lerp(palette.copper, Color.Black, 0.55f),
        startY = top,
        endY = bottom,
    )

/** Заклёпка с бликом в левом верхнем углу и тенью под шляпкой. */
fun DrawScope.drawRivet(center: Offset, radius: Float, palette: SteamPalette) {
    drawCircle(
        color = Color.Black.copy(alpha = 0.35f),
        radius = radius,
        center = center + Offset(radius * 0.25f, radius * 0.35f),
    )
    drawCircle(
        brush = Brush.radialGradient(
            0.0f to lerp(palette.brassLight, Color.White, 0.45f),
            0.45f to palette.brassDark,
            1.0f to palette.brassShadow,
            center = center - Offset(radius * 0.35f, radius * 0.35f),
            radius = radius * 1.6f,
        ),
        radius = radius,
        center = center,
    )
}

/** Текст, «выгравированный» на латуни: тёмная краска и светлая кромка снизу. */
@Composable
fun EngravedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Steam.palette.engraved,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    val highlight = Steam.palette.engravedHighlight
    val offset = with(LocalDensity.current) { 1.dp.toPx() }
    Text(
        text = text,
        modifier = modifier,
        style = style.copy(color = color, shadow = Shadow(highlight, Offset(0f, offset), 0f)),
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}
