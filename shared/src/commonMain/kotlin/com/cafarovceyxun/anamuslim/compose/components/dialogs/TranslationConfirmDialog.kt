package com.cafarovceyxun.anamuslim.compose.components.dialogs

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.cafarovceyxun.anamuslim.components.transls.TranslModel
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.labelDownload
import com.cafarovceyxun.anamuslim.resources.msgDeleteTranslation
import com.cafarovceyxun.anamuslim.resources.noContinueDownload
import com.cafarovceyxun.anamuslim.resources.re_download_az_transl_confirm
import com.cafarovceyxun.anamuslim.resources.strLabelApply
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelDelete
import com.cafarovceyxun.anamuslim.resources.strLabelUpdate
import com.cafarovceyxun.anamuslim.resources.strTitleDownloadTranslations
import com.cafarovceyxun.anamuslim.resources.strTitleTranslDelete
import com.cafarovceyxun.anamuslim.resources.titleCancelDownload
import com.cafarovceyxun.anamuslim.resources.yesCancelDownload
import org.jetbrains.compose.resources.stringResource

/**
 * A confirmation a translation row asked for. The row raises one of these as state; the screen
 * renders [TranslationConfirmDialog].
 */
sealed interface TranslationConfirm {
    val translation: TranslModel

    data class Download(override val translation: TranslModel) : TranslationConfirm
    data class CancelDownload(override val translation: TranslModel) : TranslationConfirm
    data class ForceUpdate(override val translation: TranslModel) : TranslationConfirm
    data class Delete(override val translation: TranslModel) : TranslationConfirm
}

/**
 * The per-translation confirmations, as one shared [AlertDialog]. These were `PeaceDialog` /
 * `MessageUtils.showConfirmationDialog` calls made imperatively from the rows (Android-only
 * views); the dialog is now state-driven Compose, so the translation screens work on both
 * platforms and match the confirmations the rest of the app already uses.
 */
@Composable
fun TranslationConfirmDialog(
    confirm: TranslationConfirm?,
    onClose: () -> Unit,
    onConfirmed: (TranslationConfirm) -> Unit,
) {
    val bookInfo = confirm?.translation?.bookInfo

    val title = when (confirm) {
        is TranslationConfirm.Download -> stringResource(Res.string.strTitleDownloadTranslations)
        is TranslationConfirm.CancelDownload -> stringResource(Res.string.titleCancelDownload)
        is TranslationConfirm.ForceUpdate -> stringResource(Res.string.strLabelUpdate)
        is TranslationConfirm.Delete -> stringResource(Res.string.strTitleTranslDelete)
        null -> ""
    }

    val message = when (confirm) {
        is TranslationConfirm.Download,
        is TranslationConfirm.CancelDownload -> "${bookInfo?.bookName}\n${bookInfo?.authorName}"

        is TranslationConfirm.ForceUpdate -> stringResource(Res.string.re_download_az_transl_confirm)
        is TranslationConfirm.Delete -> stringResource(
            Res.string.msgDeleteTranslation,
            bookInfo?.bookName ?: "",
            bookInfo?.authorName ?: "",
        )

        null -> ""
    }

    // Cancelling a download keeps its own wording ("continue downloading") as it did before.
    val cancelText = when (confirm) {
        is TranslationConfirm.CancelDownload -> stringResource(Res.string.noContinueDownload)
        else -> stringResource(Res.string.strLabelCancel)
    }

    val confirmText = when (confirm) {
        is TranslationConfirm.Download -> stringResource(Res.string.labelDownload)
        is TranslationConfirm.CancelDownload -> stringResource(Res.string.yesCancelDownload)
        is TranslationConfirm.ForceUpdate -> stringResource(Res.string.strLabelApply)
        is TranslationConfirm.Delete -> stringResource(Res.string.strLabelDelete)
        null -> ""
    }

    val confirmStyle = when (confirm) {
        is TranslationConfirm.CancelDownload,
        is TranslationConfirm.Delete -> AlertDialogActionStyle.Danger

        else -> AlertDialogActionStyle.Default
    }

    AlertDialog(
        isOpen = confirm != null,
        onClose = onClose,
        title = title,
        actions = listOf(
            AlertDialogAction(text = cancelText, onClick = onClose),
            AlertDialogAction(
                text = confirmText,
                style = confirmStyle,
                onClick = { confirm?.let(onConfirmed) },
            ),
        ),
    ) {
        Text(text = message)
    }
}
