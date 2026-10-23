package app.steamgarage.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.steamgarage.ui.theme.Steam

/**
 * Латунная табличка: фаска по углам, градиент латуни, четыре заклёпки и «гравированный» текст.
 * Содержимое по умолчанию рисуется цветом гравировки.
 */
@Composable
fun BrassPlate(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    chamfer: Dp = 10.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = Steam.palette
    val shape = CutCornerShape(chamfer)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interaction,
            indication = null,
            onClickLabel = onClickLabel,
            role = Role.Button,
            onClick = onClick,
        )
    } else {
        Modifier
    }
    Column(
        modifier = modifier
            .shadow(if (pressed) 2.dp else 6.dp, shape)
            .clip(shape)
            .background(brassBrush(palette), shape)
            .then(clickModifier)
            .drawWithContent {
                drawContent()
                val inset = chamfer.toPx() * 0.55f + 6.dp.toPx()
                val r = 3.5.dp.toPx()
                listOf(
                    Offset(inset, inset),
                    Offset(size.width - inset, inset),
                    Offset(inset, size.height - inset),
                    Offset(size.width - inset, size.height - inset),
                ).forEach { drawRivet(it, r, palette) }
                if (pressed) drawRect(Color.Black.copy(alpha = 0.10f))
            }
            .border(2.dp, Brush.linearGradient(listOf(palette.brassLight, palette.brassShadow)), shape)
            .padding(contentPadding),
    ) {
        CompositionLocalProvider(LocalContentColor provides palette.engraved) {
            content()
        }
    }
}

/**
 * Панель мастерской: поверхность темы в латунной рамке с маленькими заклёпками.
 * Используется для карточек расходников, форм и секций настроек.
 */
@Composable
fun SteamPanel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    highlighted: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = Steam.palette
    val shape = CutCornerShape(8.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interaction,
            indication = null,
            onClickLabel = onClickLabel,
            role = Role.Button,
            onClick = onClick,
        )
    } else {
        Modifier
    }
    val frame = if (highlighted) {
        Brush.linearGradient(listOf(palette.copperLight, palette.copper))
    } else {
        Brush.linearGradient(listOf(palette.brassLight, palette.brassShadow))
    }
    Column(
        modifier = modifier
            .shadow(4.dp, shape)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        lerp(palette.surface, palette.brassLight, if (palette.isDark) 0.04f else 0.10f),
                        palette.surface,
                    ),
                ),
                shape,
            )
            .then(clickModifier)
            .drawWithContent {
                drawContent()
                val inset = 7.dp.toPx()
                val r = 2.5.dp.toPx()
                listOf(
                    Offset(inset, inset),
                    Offset(size.width - inset, inset),
                    Offset(inset, size.height - inset),
                    Offset(size.width - inset, size.height - inset),
                ).forEach { drawRivet(it, r, palette) }
                if (pressed) drawRect(Color.Black.copy(alpha = 0.08f))
            }
            .border(if (highlighted) 3.dp else 2.dp, frame, shape)
            .padding(contentPadding),
    ) {
        CompositionLocalProvider(LocalContentColor provides palette.text) {
            content()
        }
    }
}
