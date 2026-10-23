package app.steamgarage.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.steamgarage.ui.theme.Steam
import app.steamgarage.ui.theme.SteamPalette
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Вентиль — второстепенная кнопка: маховик с надписью. По нажатию маховик проворачивается и щёлкает.
 */
@Composable
fun ValveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = Steam.palette
    val player = LocalSfx.current
    var turns by remember { mutableIntStateOf(0) }
    val angle by animateFloatAsState(
        targetValue = turns * 72f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow),
        label = "valve",
    )
    val shape = CutCornerShape(6.dp)
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .background(palette.surfaceDeep.copy(alpha = if (palette.isDark) 0.85f else 0.55f))
            .border(1.5.dp, Brush.linearGradient(listOf(palette.brassLight, palette.brassShadow)), shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                role = Role.Button,
            ) {
                turns++
                player.play(Sfx.CLICK)
                onClick()
            }
            .padding(start = 8.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(32.dp)) { drawHandwheel(angle, palette) }
        Spacer(Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = palette.text)
    }
}

/** Маховик вентиля: обод, пять спиц, ступица. */
fun DrawScope.drawHandwheel(angle: Float, palette: SteamPalette) {
    val r = size.minDimension / 2f
    val c = center
    rotate(angle, c) {
        val ringWidth = r * 0.22f
        val ringRadius = r - ringWidth / 2f
        repeat(5) { i ->
            val a = (i * 72.0 - 90.0) * PI / 180.0
            val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
            drawLine(palette.brassShadow, c, c + dir * ringRadius, strokeWidth = r * 0.16f, cap = StrokeCap.Round)
            drawLine(palette.brassDark, c, c + dir * ringRadius, strokeWidth = r * 0.08f, cap = StrokeCap.Round)
        }
        drawCircle(
            brush = Brush.sweepGradient(
                listOf(palette.brassLight, palette.brassShadow, palette.brassDark, palette.brassLight, palette.brassShadow, palette.brassLight),
                center = c,
            ),
            radius = ringRadius,
            center = c,
            style = Stroke(width = ringWidth),
        )
        drawCircle(palette.iron, r * 0.26f, c)
        drawCircle(palette.brassLight, r * 0.12f, c - Offset(r * 0.05f, r * 0.05f))
    }
}

/**
 * Тумблер: рукоять поднята — включено (горит лампа патины), опущена — выключено.
 * [onCheckedChange] = null — только показывает состояние (переключает вся строка, см. [SteamSwitchRow]).
 */
@Composable
fun SteamToggle(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = Steam.palette
    val player = LocalSfx.current
    val position by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "toggle",
    )
    val toggleModifier = if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ) {
            player.play(Sfx.CLICK)
            onCheckedChange(it)
        }
    } else {
        Modifier
    }
    Canvas(
        modifier = modifier
            .size(width = 64.dp, height = 48.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .then(toggleModifier),
    ) {
        drawToggle(position, palette)
    }
}

private fun DrawScope.drawToggle(position: Float, palette: SteamPalette) {
    val plateTop = 4.dp.toPx()
    val plateSize = Size(size.width, size.height - plateTop * 2)
    val corner = CornerRadius(6.dp.toPx())
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(lerp(palette.iron, Color.White, 0.08f), palette.iron)),
        topLeft = Offset(0f, plateTop),
        size = plateSize,
        cornerRadius = corner,
    )
    drawRoundRect(
        brush = Brush.linearGradient(listOf(palette.brassLight, palette.brassShadow)),
        topLeft = Offset(0f, plateTop),
        size = plateSize,
        cornerRadius = corner,
        style = Stroke(width = 1.5.dp.toPx()),
    )

    // Лампа-индикатор.
    val lamp = Offset(15.dp.toPx(), size.height / 2f)
    val lampR = 6.dp.toPx()
    if (position > 0.5f) {
        drawCircle(palette.ok.copy(alpha = 0.30f), lampR * 2f, lamp)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(lerp(palette.ok, Color.White, 0.6f), palette.ok, lerp(palette.ok, Color.Black, 0.4f)),
                center = lamp,
                radius = lampR,
            ),
            radius = lampR,
            center = lamp,
        )
    } else {
        drawCircle(Color(0xFF1A1714), lampR, lamp)
        drawCircle(Color.White.copy(alpha = 0.18f), lampR * 0.35f, lamp - Offset(lampR * 0.3f, lampR * 0.3f))
    }
    drawCircle(palette.brassShadow, lampR, lamp, style = Stroke(width = 1.5.dp.toPx()))

    // Гайка и рукоять. Рукоять «перелетает» через гайку: в середине хода видна только гайка.
    val nut = Offset(size.width - 22.dp.toPx(), size.height / 2f)
    val nutR = 8.dp.toPx()
    val hex = Path().apply {
        for (i in 0 until 6) {
            val a = (i * 60.0 + 30.0) * PI / 180.0
            val p = nut + Offset(cos(a).toFloat(), sin(a).toFloat()) * nutR
            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        close()
    }
    drawPath(hex, Brush.linearGradient(listOf(palette.brassLight, palette.brassShadow), nut - Offset(nutR, nutR), nut + Offset(nutR, nutR)))

    val reach = 17.dp.toPx() * abs(position * 2f - 1f)
    val dir = if (position >= 0.5f) -1f else 1f
    val tip = nut + Offset(0f, dir * reach)
    if (reach > 1f) {
        drawLine(Color.Black.copy(alpha = 0.35f), nut + Offset(2f, 2f), tip + Offset(2f, 2f), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
        drawLine(Color(0xFFCFC8BC), nut, tip, strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
        drawLine(Color.White.copy(alpha = 0.7f), nut, tip, strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(Color(0xFFE8E2D6), 4.5.dp.toPx(), tip)
    }
    drawCircle(palette.iron, 3.dp.toPx(), nut)
}

/** Строка настроек с тумблером: переключается касанием в любом месте строки. */
@Composable
fun SteamSwitchRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true,
) {
    val palette = Steam.palette
    val player = LocalSfx.current
    Row(
        modifier = modifier
            .heightIn(min = 56.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                player.play(Sfx.CLICK)
                onCheckedChange(it)
            }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text, style = MaterialTheme.typography.bodyLarge, color = palette.text)
            if (supportingText != null) {
                Text(supportingText, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
            }
        }
        Spacer(Modifier.width(12.dp))
        SteamToggle(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
