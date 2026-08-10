package com.cafarovceyxun.anamuslim.compose.components.dialogs

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.labelDownload
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelUpdate
import com.cafarovceyxun.anamuslim.resources.strMsgHadithDownloadConfirm
import com.cafarovceyxun.anamuslim.resources.strTitleHadithDownload
import org.jetbrains.compose.resources.stringResource

/**
 * Confirmation before the hadith books are downloaded or re-synced.
 *
 * Shared by the two screens that own a hadith row — onboarding and settings — so both ask the same
 * question in the same words. Until this existed the hadith button started a multi-megabyte sync on
 * the first tap with no confirmation at all, while the translation row sitting directly below it
 * did ask; [isUpdate] only swaps the wording, since the same button re-syncs once the books are on
 * the device.
 */
@Composable
fun HadithSyncConfirmDialog(
    isOpen: Boolean,
    isUpdate: Boolean,
    onClose: () -> Unit,
    onConfirmed: () -> Unit,
) {
    // Read unconditionally, then pick — see the note in TranslationConfirmDialog: a `stringResource`
    // inside a conditional branch is what crashed that dialog on iOS.
    val updateLabel = stringResource(Res.string.strLabelUpdate)
    val downloadTitle = stringResource(Res.string.strTitleHadithDownload)
    val downloadLabel = stringResource(Res.string.labelDownload)
    val cancelLabel = stringResource(Res.string.strLabelCancel)
    val message = stringResource(Res.string.strMsgHadithDownloadConfirm)

    AlertDialog(
        isOpen = isOpen,
        onClose = onClose,
        title = if (isUpdate) updateLabel else downloadTitle,
        actions = listOf(
            AlertDialogAction(text = cancelLabel, onClick = onClose),
            // Default, not Primary: every other non-destructive confirm in the app (the translation
            // download sitting right below this one included) uses the plain style, and Danger is
            // reserved for delete. A green button here would make the two rows look unrelated.
            AlertDialogAction(
                text = if (isUpdate) updateLabel else downloadLabel,
                onClick = onConfirmed,
            ),
        ),
    ) {
        Text(text = message)
    }
}
