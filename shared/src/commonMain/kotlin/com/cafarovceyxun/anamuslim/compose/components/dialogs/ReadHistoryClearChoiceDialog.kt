package com.cafarovceyxun.anamuslim.compose.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_delete
import com.cafarovceyxun.anamuslim.resources.msgClearReadHistory
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelClearCompletionOnly
import com.cafarovceyxun.anamuslim.resources.strLabelClearHistoryOnly
import com.cafarovceyxun.anamuslim.resources.strLabelRemoveAll
import com.cafarovceyxun.anamuslim.resources.strMsgClearEverything
import com.cafarovceyxun.anamuslim.resources.strMsgClearHistoryOnly
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Oxu tarixçəsi ekranlarındakı zibil düyməsinin seçimləri.
 *
 * Oxuma izi **iki** cədvəldədir — «harada qaldım» siyahısı və ✓ «oxundu» nişanları — və istifadəçi
 * hansını silmək istədiyini özü seçir: birini saxlayıb o birini atmaq məqsədli haldır.
 */
enum class ReadHistoryClearChoice {
    /** Yalnız siyahı. */
    HistoryOnly,

    /** Yalnız ✓ nişanları. */
    CompletionOnly,

    /** Hər ikisi. */
    Everything,
}

/**
 * Quran və hədis tarixçə ekranları üçün ortaq seçim dialoqu.
 *
 * Seçimlər `actions` sırasında yox, **məzmunda** sətir kimi durur: üç dağıdıcı düymə + «Ləğv et»
 * bir sıraya sığmır ([AlertDialog] onları bərabər paylaşdırır), üstəlik hər seçimin nə silib nə
 * saxladığını yalnız yanındakı izah aydınlaşdırır.
 *
 * Yalnız [completionMessage] platformaya görə dəyişir — hədisdə «bablardakı», Quranda isə
 * «surə/cüz/hizb üzərindəki» nişanlardan söhbət gedir.
 */
@Composable
fun ReadHistoryClearChoiceDialog(
    isOpen: Boolean,
    completionMessage: StringResource,
    onDismiss: () -> Unit,
    onPick: (ReadHistoryClearChoice) -> Unit,
) {
    // Şərtsiz oxunur, sonra seçilir: `stringResource`-u budağın içində çağırmaq iOS-da
    // TranslationConfirmDialog-u çökdürmüşdü.
    val title = stringResource(Res.string.msgClearReadHistory)
    val cancelLabel = stringResource(Res.string.strLabelCancel)
    val historyOnlyLabel = stringResource(Res.string.strLabelClearHistoryOnly)
    val historyOnlyMessage = stringResource(Res.string.strMsgClearHistoryOnly)
    val completionOnlyLabel = stringResource(Res.string.strLabelClearCompletionOnly)
    val completionOnlyMessage = stringResource(completionMessage)
    val everythingLabel = stringResource(Res.string.strLabelRemoveAll)
    val everythingMessage = stringResource(Res.string.strMsgClearEverything)

    AlertDialog(
        isOpen = isOpen,
        onClose = onDismiss,
        title = title,
        actions = listOf(
            AlertDialogAction(text = cancelLabel, onClick = onDismiss),
        ),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ClearChoiceRow(
                icon = Res.drawable.dr_icon_delete,
                label = historyOnlyLabel,
                message = historyOnlyMessage,
                onClick = { onPick(ReadHistoryClearChoice.HistoryOnly) },
            )
            ClearChoiceRow(
                icon = Res.drawable.dr_icon_delete,
                label = completionOnlyLabel,
                message = completionOnlyMessage,
                onClick = { onPick(ReadHistoryClearChoice.CompletionOnly) },
            )
            ClearChoiceRow(
                icon = Res.drawable.dr_icon_delete,
                label = everythingLabel,
                message = everythingMessage,
                isDanger = true,
                onClick = { onPick(ReadHistoryClearChoice.Everything) },
            )
        }
    }
}

@Composable
private fun ClearChoiceRow(
    icon: DrawableResource,
    label: String,
    message: String,
    onClick: () -> Unit,
    isDanger: Boolean = false,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .clickable(onClick = onClick),
        color = if (isDanger) {
            colorScheme.errorContainer.alpha(0.55f)
        } else {
            colorScheme.surfaceContainerHighest.alpha(0.6f)
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isDanger) colorScheme.onErrorContainer else colorScheme.onSurfaceVariant,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDanger) colorScheme.onErrorContainer else colorScheme.onSurface,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDanger) {
                        colorScheme.onErrorContainer.alpha(0.8f)
                    } else {
                        colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
