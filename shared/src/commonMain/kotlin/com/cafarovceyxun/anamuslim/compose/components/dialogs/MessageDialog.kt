package com.cafarovceyxun.anamuslim.compose.components.dialogs

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strLabelClose
import org.jetbrains.compose.resources.stringResource

/**
 * A plain "here is what happened" dialog with a single Close button — the state-driven Compose
 * replacement for `MessageUtils.popMessage`, which ViewModels' `ShowMessage` events used to pop
 * imperatively through the Android-only `PeaceDialog`.
 *
 * Pass a null [title] to keep it closed.
 */
@Composable
fun MessageDialog(
    title: String?,
    message: String?,
    onClose: () -> Unit,
) {
    AlertDialog(
        isOpen = title != null,
        onClose = onClose,
        title = title.orEmpty(),
        actions = listOf(
            AlertDialogAction(
                text = stringResource(Res.string.strLabelClose),
                onClick = onClose,
            )
        )
    ) {
        message?.let { Text(text = it) }
    }
}
