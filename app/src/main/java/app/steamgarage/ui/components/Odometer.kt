package app.steamgarage.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import app.steamgarage.ui.Format
import app.steamgarage.ui.theme.Steam

/**
 * Цифры в окошках барабанов, как на механическом одометре.
 * Пробел в [value] даёт промежуток между группами разрядов: «85 247».
 * [alarm] — барабаны красной эмали: так показывается превышение срока.
 */
@Composable
fun OdometerDisplay(
    value: String,
    modifier: Modifier = Modifier,
    style: TextStyle = Steam.text.odometerSmall,
    contentDescription: String = value,
    alarm: Boolean = false,
) {
    val palette = Steam.palette
    val window = if (alarm) palette.overdue else palette.odometerWindow
    val digit = if (alarm) palette.onOverdue else palette.odometerDigit
    val drum = Brush.verticalGradient(
        0.00f to Color.Black,
        0.18f to window,
        0.50f to lerp(window, Color.White, 0.10f),
        0.82f to window,
        1.00f to Color.Black,
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Brush.verticalGradient(listOf(palette.brassLight, palette.brassShadow)))
            .padding(2.dp)
            .clearAndSetSemantics { this.contentDescription = contentDescription },
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        value.forEach { ch ->
            if (ch == ' ' || ch == Format.GROUP_SPACE) {
                Spacer(Modifier.width(3.dp))
            } else {
                Box(
                    modifier = Modifier
                        .background(drum, RoundedCornerShape(2.dp))
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = ch.toString(), style = style, color = digit)
                }
            }
        }
    }
}
