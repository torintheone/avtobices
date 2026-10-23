package app.steamgarage.ui.screens.sync

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.steamgarage.R
import app.steamgarage.data.db.GarageEntity
import app.steamgarage.data.sync.GarageMember
import app.steamgarage.data.sync.SyncError
import app.steamgarage.data.sync.SyncStatus
import app.steamgarage.domain.GarageCode
import app.steamgarage.ui.Format
import app.steamgarage.ui.components.BrassButton
import app.steamgarage.ui.components.QrCode
import app.steamgarage.ui.components.SteamDialog
import app.steamgarage.ui.components.SteamPanel
import app.steamgarage.ui.components.SteamTextField
import app.steamgarage.ui.components.ValveButton
import app.steamgarage.ui.theme.Steam

/** Ссылка-приглашение: её содержит QR-код, по ней приложение открывается с кодом. */
fun joinLink(code: String): String = "steamgarage://join?code=$code"

@StringRes
fun syncErrorText(error: SyncError): Int = when (error) {
    SyncError.OFFLINE -> R.string.sync_error_offline
    SyncError.DENIED -> R.string.sync_error_denied
    SyncError.NO_DATABASE -> R.string.sync_error_no_database
    SyncError.AUTH -> R.string.sync_error_auth
    SyncError.BAD_CODE -> R.string.sync_error_bad_code
    SyncError.OTHER -> R.string.sync_error_other
}

/** «Поделиться кодом»: код и ссылка-приглашение через любой мессенджер. */
fun shareGarageCode(context: Context, code: String) {
    val text = context.getString(R.string.garage_share_text, GarageCode.format(code), joinLink(code))
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(
        Intent.createChooser(send, context.getString(R.string.garage_share_chooser)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

/** Всё, что показывает раздел «Общий гараж» в настройках. */
data class GarageUiState(
    val configured: Boolean = false,
    val garage: GarageEntity? = null,
    val status: SyncStatus = SyncStatus.NotConfigured,
    val members: List<GarageMember> = emptyList(),
    val myUid: String? = null,
    val lastSyncAt: Long = 0,
    val lastReceived: Int = 0,
    val lastSent: Int = 0,
)

class GarageActions(
    val onCreate: () -> Unit = {},
    val onJoin: (String) -> Unit = {},
    val onSync: () -> Unit = {},
    val onLeave: (keepData: Boolean) -> Unit = {},
    val onShare: (String) -> Unit = {},
)

/** Раздел «Общий гараж»: создать или вступить; в гараже — код, QR, участники, сверка, «Покинуть». */
@Composable
fun GaragePanel(state: GarageUiState, actions: GarageActions) {
    val palette = Steam.palette
    val error = (state.status as? SyncStatus.Failed)?.error
    SteamPanel(Modifier.fillMaxWidth()) {
        when {
            !state.configured -> {
                Text(stringResource(R.string.garage_not_configured), style = MaterialTheme.typography.bodyMedium)
            }
            state.garage == null -> {
                var code by rememberSaveable { mutableStateOf("") }
                val busy = state.status is SyncStatus.Syncing
                Text(stringResource(R.string.garage_intro), style = MaterialTheme.typography.bodyMedium)
                BrassButton(text = stringResource(R.string.garage_create), onClick = actions.onCreate, enabled = !busy)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.garage_join), style = MaterialTheme.typography.titleMedium)
                SteamTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = stringResource(R.string.garage_code_label),
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done,
                )
                ValveButton(
                    text = stringResource(R.string.garage_join_button),
                    onClick = { actions.onJoin(code) },
                    enabled = code.isNotBlank() && !busy,
                )
                if (error != null) ErrorLine(error)
            }
            else -> InGarage(state, state.garage, actions, error, palette.textSecondary)
        }
    }
}

@Composable
private fun InGarage(
    state: GarageUiState,
    garage: GarageEntity,
    actions: GarageActions,
    error: SyncError?,
    secondary: Color,
) {
    var leaving by rememberSaveable { mutableStateOf(false) }
    Text(stringResource(R.string.garage_code_title), style = MaterialTheme.typography.labelLarge)
    Text(GarageCode.format(garage.code), style = Steam.text.number.copy(fontSize = 18.sp))
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        QrCode(text = joinLink(garage.code), contentDescription = stringResource(R.string.garage_qr_description))
    }
    Text(stringResource(R.string.garage_qr_hint), style = MaterialTheme.typography.bodySmall, color = secondary)
    BrassButton(text = stringResource(R.string.garage_share), onClick = { actions.onShare(garage.code) })
    Text(stringResource(R.string.garage_share_cars_hint), style = MaterialTheme.typography.bodySmall, color = secondary)

    if (state.members.isNotEmpty()) {
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.garage_members), style = MaterialTheme.typography.titleMedium)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            state.members.forEach { member ->
                val name = member.name.ifBlank { stringResource(R.string.garage_member_unnamed) }
                Text(
                    text = if (member.uid == state.myUid) stringResource(R.string.garage_member_you, name) else name,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }

    Spacer(Modifier.height(4.dp))
    Text(
        text = if (state.lastSyncAt > 0) {
            stringResource(R.string.garage_last_sync, Format.dateTime(state.lastSyncAt), state.lastReceived, state.lastSent)
        } else {
            stringResource(R.string.garage_never_synced)
        },
        style = MaterialTheme.typography.bodySmall,
        color = secondary,
    )
    if (error != null) ErrorLine(error)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ValveButton(
            text = stringResource(R.string.garage_sync_now),
            onClick = actions.onSync,
            enabled = state.status !is SyncStatus.Syncing,
        )
        ValveButton(text = stringResource(R.string.garage_leave), onClick = { leaving = true })
    }
    if (leaving) {
        LeaveGarageDialog(
            onKeep = {
                leaving = false
                actions.onLeave(true)
            },
            onDelete = {
                leaving = false
                actions.onLeave(false)
            },
            onDismiss = { leaving = false },
        )
    }
}

@Composable
private fun ErrorLine(error: SyncError) {
    Text(stringResource(syncErrorText(error)), style = MaterialTheme.typography.bodyMedium, color = Steam.palette.errorText)
}

@Composable
fun LeaveGarageDialog(onKeep: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    SteamDialog(
        title = stringResource(R.string.garage_leave_title),
        onDismiss = onDismiss,
        buttons = {
            ValveButton(text = stringResource(R.string.garage_leave_delete), onClick = onDelete)
            BrassButton(text = stringResource(R.string.garage_leave_keep), onClick = onKeep)
        },
    ) {
        Text(stringResource(R.string.garage_leave_text), style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * «Вступить в гараж» из первого запуска или по ссылке-приглашению.
 * [alreadyInGarage] — телефон уже состоит в гараже: только объяснение, как перейти в другой.
 */
@Composable
fun JoinGarageDialog(
    initialCode: String,
    busy: Boolean,
    error: SyncError?,
    configured: Boolean,
    alreadyInGarage: Boolean,
    onJoin: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var code by rememberSaveable { mutableStateOf(GarageCode.normalize(initialCode)?.let(GarageCode::format) ?: initialCode) }
    val blocked = !configured || alreadyInGarage
    SteamDialog(
        title = stringResource(R.string.garage_join_title),
        onDismiss = onDismiss,
        buttons = {
            ValveButton(text = stringResource(if (blocked) R.string.action_ok else R.string.action_cancel), onClick = onDismiss)
            if (!blocked) {
                BrassButton(
                    text = stringResource(R.string.garage_join_button),
                    onClick = { onJoin(code) },
                    enabled = code.isNotBlank() && !busy,
                )
            }
        },
    ) {
        when {
            !configured -> Text(stringResource(R.string.garage_not_configured), style = MaterialTheme.typography.bodyMedium)
            alreadyInGarage -> Text(stringResource(R.string.garage_already_in), style = MaterialTheme.typography.bodyMedium)
            else -> {
                Text(
                    text = if (initialCode.isNotBlank()) {
                        stringResource(R.string.garage_join_link_text, code)
                    } else {
                        stringResource(R.string.garage_join_text)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                SteamTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = stringResource(R.string.garage_code_label),
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done,
                )
                if (error != null) {
                    Spacer(Modifier.height(6.dp))
                    ErrorLine(error)
                }
            }
        }
    }
}
