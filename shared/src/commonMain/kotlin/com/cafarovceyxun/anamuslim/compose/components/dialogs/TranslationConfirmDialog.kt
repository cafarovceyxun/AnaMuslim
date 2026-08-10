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
import com.cafarovceyxun.anamuslim.resources.strMsgTranslCancelDownloadConfirm
import com.cafarovceyxun.anamuslim.resources.strMsgTranslDownloadConfirm
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

    // Every string is read unconditionally, and the `when`s below only pick between the already
    // resolved values. Do not move `stringResource` back inside a branch: the Compose compiler
    // wraps each branch that contains a composable call in its own replace-group, and on
    // Kotlin/Native that transform mis-selected the branch for `TranslationConfirm.Download` —
    // execution fell through every `is` check to the exhaustiveness fallback and the app died with
    // `kotlin.NoWhenBranchMatchedException` the moment this dialog opened (onboarding → download a
    // translation). Compilation and tests were both clean; only the simulator showed it.
    val downloadTitle = stringResource(Res.string.strTitleDownloadTranslations)
    val cancelDownloadTitle = stringResource(Res.string.titleCancelDownload)
    val updateTitle = stringResource(Res.string.strLabelUpdate)
    val deleteTitle = stringResource(Res.string.strTitleTranslDelete)
    val downloadMessage = stringResource(Res.string.strMsgTranslDownloadConfirm)
    val cancelDownloadMessage = stringResource(Res.string.strMsgTranslCancelDownloadConfirm)
    val forceUpdateMessage = stringResource(Res.string.re_download_az_transl_confirm)
    val deleteMessage = stringResource(
        Res.string.msgDeleteTranslation,
        bookInfo?.bookName ?: "",
        bookInfo?.authorName ?: "",
    )
    val continueDownloadLabel = stringResource(Res.string.noContinueDownload)
    val cancelLabel = stringResource(Res.string.strLabelCancel)
    val downloadLabel = stringResource(Res.string.labelDownload)
    val yesCancelDownloadLabel = stringResource(Res.string.yesCancelDownload)
    val applyLabel = stringResource(Res.string.strLabelApply)
    val deleteLabel = stringResource(Res.string.strLabelDelete)

    val title = when (confirm) {
        is TranslationConfirm.Download -> downloadTitle
        is TranslationConfirm.CancelDownload -> cancelDownloadTitle
        is TranslationConfirm.ForceUpdate -> updateTitle
        is TranslationConfirm.Delete -> deleteTitle
        null -> ""
    }

    // Book name and author on their own lines, then a blank line, then what the action will do.
    // The two download branches used to show only the book line, which said nothing about what
    // tapping "Download" or "Yes, cancel" would actually cause.
    val bookLine = "${bookInfo?.bookName}\n${bookInfo?.authorName}"

    val message = when (confirm) {
        is TranslationConfirm.Download -> "$bookLine\n\n$downloadMessage"
        is TranslationConfirm.CancelDownload -> "$bookLine\n\n$cancelDownloadMessage"
        is TranslationConfirm.ForceUpdate -> forceUpdateMessage
        is TranslationConfirm.Delete -> deleteMessage
        null -> ""
    }

    // Cancelling a download keeps its own wording ("continue downloading") as it did before.
    val cancelText = when (confirm) {
        is TranslationConfirm.CancelDownload -> continueDownloadLabel
        else -> cancelLabel
    }

    val confirmText = when (confirm) {
        is TranslationConfirm.Download -> downloadLabel
        is TranslationConfirm.CancelDownload -> yesCancelDownloadLabel
        is TranslationConfirm.ForceUpdate -> applyLabel
        is TranslationConfirm.Delete -> deleteLabel
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
