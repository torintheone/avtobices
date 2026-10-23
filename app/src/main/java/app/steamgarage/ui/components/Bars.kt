package app.steamgarage.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.steamgarage.R
import app.steamgarage.ui.theme.Steam

/** Вкладка нижней навигации. */
data class NavTab(
    val label: String,
    @param:DrawableRes val icon: Int,
)

/** Нижняя навигация — латунная панель с гравированными иконками. */
@Composable
fun BrassBottomBar(
    tabs: List<NavTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = Steam.palette
    val player = LocalSfx.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(brassVerticalBrush(palette))
            .drawBehind {
                // Тёмная кромка сверху и заклёпки по краям.
                drawRect(palette.brassShadow, size = size.copy(height = 2.dp.toPx()))
                drawRect(
                    Color.White.copy(alpha = 0.35f),
                    topLeft = Offset(0f, 2.dp.toPx()),
                    size = size.copy(height = 1.dp.toPx()),
                )
                val r = 3.dp.toPx()
                drawRivet(Offset(10.dp.toPx(), 14.dp.toPx()), r, palette)
                drawRivet(Offset(size.width - 10.dp.toPx(), 14.dp.toPx()), r, palette)
            }
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 6.dp),
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .clip(CutCornerShape(6.dp))
                        .then(
                            if (selected) {
                                Modifier.background(
                                    Brush.verticalGradient(
                                        listOf(lerp(palette.navSelected, Color.Black, 0.35f), palette.navSelected),
                                    ),
                                )
                            } else {
                                Modifier
                            },
                        )
                        .selectable(
                            selected = selected,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                        ) {
                            if (!selected) player.play(Sfx.CLICK)
                            onSelect(index)
                        }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val ink = if (selected) palette.brassLight else palette.engraved
                    Box {
                        if (!selected) {
                            Icon(
                                painter = painterResource(tab.icon),
                                contentDescription = null,
                                tint = palette.engravedHighlight,
                                modifier = Modifier
                                    .size(26.dp)
                                    .offset(y = 1.dp),
                            )
                        }
                        Icon(
                            painter = painterResource(tab.icon),
                            contentDescription = null,
                            tint = ink,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    if (selected) {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.brassLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else {
                        EngravedText(
                            text = tab.label,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Верхняя панель: заголовок латунными буквами, слева — «назад», справа — действия
 * (рычаг синхронизации и шестерня-индикатор появятся на этапе синхронизации). Снизу — труба.
 */
@Composable
fun SteamTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val palette = Steam.palette
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(palette.background.copy(alpha = 0.95f), palette.background.copy(alpha = 0.75f)),
                ),
            )
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(onClickLabel = null, role = Role.Button, onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = androidx.compose.ui.res.stringResource(R.string.action_back),
                        tint = if (palette.isDark) palette.brassLight else palette.text,
                    )
                }
            } else {
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = if (palette.isDark) palette.brassLight else palette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            actions()
        }
        PipeDivider()
    }
}
