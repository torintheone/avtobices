package app.steamgarage.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.steamgarage.R
import app.steamgarage.domain.ItemStatus
import app.steamgarage.ui.theme.Steam
import app.steamgarage.ui.theme.SteamPalette

/** Цвет зоны манометра и бейджа для статуса. */
fun SteamPalette.statusColor(status: ItemStatus): Color = when (status) {
    ItemStatus.NO_DATA -> noData
    ItemStatus.OK -> ok
    ItemStatus.SOON -> soon
    ItemStatus.OVERDUE -> overdue
}

/** Цвет текста поверх бейджа статуса (контраст не ниже 4,5:1). */
fun SteamPalette.onStatusColor(status: ItemStatus): Color = when (status) {
    ItemStatus.NO_DATA -> onNoData
    ItemStatus.OK -> onOk
    ItemStatus.SOON -> onSoon
    ItemStatus.OVERDUE -> onOverdue
}

@Composable
fun statusLabel(status: ItemStatus): String = stringResource(
    when (status) {
        ItemStatus.NO_DATA -> R.string.status_no_data
        ItemStatus.OK -> R.string.status_ok
        ItemStatus.SOON -> R.string.status_soon
        ItemStatus.OVERDUE -> R.string.status_overdue
    },
)

/** Эмалевый бейдж статуса: цветная плашка с надписью. */
@Composable
fun StatusBadge(status: ItemStatus, modifier: Modifier = Modifier) {
    val palette = Steam.palette
    Text(
        text = statusLabel(status),
        style = MaterialTheme.typography.labelMedium,
        color = palette.onStatusColor(status),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(palette.statusColor(status), CutCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** Заголовок секции на фоне экрана. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    val palette = Steam.palette
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = if (palette.isDark) palette.brassLight else palette.text,
        modifier = modifier,
    )
}
