package app.steamgarage.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.steamgarage.R
import app.steamgarage.ui.theme.Steam

/**
 * Латунная кнопка подтверждения для диалогов и форм: табличка с гравированной надписью.
 * [danger] — эмаль для опасных действий (удаление).
 */
@Composable
fun BrassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
) {
    val palette = Steam.palette
    val player = LocalSfx.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = CutCornerShape(6.dp)
    val background = if (danger) {
        Brush.verticalGradient(listOf(palette.overdue, palette.overdue))
    } else {
        brassBrush(palette)
    }
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .shadow(if (pressed) 1.dp else 4.dp, shape)
            .clip(shape)
            .background(background)
            .border(1.5.dp, Brush.linearGradient(listOf(palette.brassLight, palette.brassShadow)), shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
            ) {
                player.play(Sfx.CLICK)
                onClick()
            }
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (danger) {
            Text(text, style = MaterialTheme.typography.labelLarge, color = palette.onOverdue, textAlign = TextAlign.Center)
        } else {
            EngravedText(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
        if (pressed) Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.08f)))
    }
}

/** Диалог-панель мастерской: заголовок, содержимое, кнопки справа внизу. */
@Composable
fun SteamDialog(
    title: String,
    onDismiss: () -> Unit,
    buttons: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Ширину задаём сами: стандартная ширина диалога на разных телефонах то впритык к краям, то слишком узкая.
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        SteamPanel(
            Modifier
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                content()
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                content = buttons,
            )
        }
    }
}

/** Подтверждение действия: текст и две кнопки. */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = false,
    dismissText: String = stringResource(R.string.action_cancel),
) {
    SteamDialog(
        title = title,
        onDismiss = onDismiss,
        buttons = {
            ValveButton(text = dismissText, onClick = onDismiss)
            BrassButton(
                text = confirmText,
                danger = danger,
                onClick = {
                    onConfirm()
                    onDismiss()
                },
            )
        },
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Подсказка вместо пустого экрана: короткий текст и одна кнопка следующего шага. */
@Composable
fun EmptyState(
    text: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val palette = Steam.palette
    SteamPanel(modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SteamGears(spinning = false, size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = palette.text, modifier = Modifier.weight(1f))
        }
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(14.dp))
            SteamLever(text = actionText, onPull = onAction, modifier = Modifier.fillMaxWidth())
        }
    }
}
