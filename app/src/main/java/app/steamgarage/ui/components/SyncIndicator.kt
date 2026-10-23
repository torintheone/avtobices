package app.steamgarage.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.steamgarage.R
import app.steamgarage.ui.theme.Steam
import kotlin.math.cos
import kotlin.math.sin

/** Что показывает шестерня синхронизации в верхней панели. */
enum class SyncLamp { IDLE, SYNCING, ERROR }

/**
 * Шестерня-индикатор и рычаг синхронизации в верхней панели вкладок.
 * Шестерня крутится во время сверки и краснеет при ошибке; рычаг запускает сверку вручную.
 */
@Composable
fun SyncIndicator(lamp: SyncLamp, onSync: () -> Unit) {
    val palette = Steam.palette
    val gearDescription = stringResource(
        when (lamp) {
            SyncLamp.IDLE -> R.string.sync_gear_idle
            SyncLamp.SYNCING -> R.string.sync_gear_syncing
            SyncLamp.ERROR -> R.string.sync_gear_error
        },
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        SteamGears(
            spinning = lamp == SyncLamp.SYNCING,
            size = 30.dp,
            tint = if (lamp == SyncLamp.ERROR) palette.overdue else null,
            contentDescription = gearDescription,
        )
        val pulled by animateFloatAsState(
            targetValue = if (lamp == SyncLamp.SYNCING) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.5f),
            label = "sync-lever",
        )
        val leverDescription = stringResource(R.string.sync_lever_description)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(enabled = lamp != SyncLamp.SYNCING, role = Role.Button, onClick = onSync)
                .semantics { contentDescription = leverDescription },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(30.dp)) {
                val pivot = Offset(size.width * 0.5f, size.height * 0.82f)
                val arm = size.height * 0.62f
                val angle = Math.toRadians(-120.0 + 60.0 * pulled)
                val end = pivot + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * arm
                // Латунное основание.
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(palette.brassLight, palette.brassShadow)),
                    topLeft = Offset(size.width * 0.18f, size.height * 0.78f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.64f, size.height * 0.16f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                )
                drawLine(palette.iron, pivot, end, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                val knob = 4.5.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to lerp(palette.copperLight, Color.White, 0.5f),
                        0.45f to palette.copper,
                        1f to lerp(palette.copper, Color.Black, 0.55f),
                        center = end - Offset(knob * 0.35f, knob * 0.4f),
                        radius = knob * 1.6f,
                    ),
                    radius = knob,
                    center = end,
                )
                drawCircle(palette.brassDark, 2.5.dp.toPx(), pivot)
            }
        }
    }
}
