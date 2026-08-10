package com.cafarovceyxun.anamuslim.compose.components.reader.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheetHeader
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.ic_bookmark
import com.cafarovceyxun.anamuslim.resources.strHintBookmarkViewerNote
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelSaveBookmark
import com.cafarovceyxun.anamuslim.resources.strMsgBookmarkNoteOptional
import com.cafarovceyxun.anamuslim.resources.strTitleAddBookmark
import org.jetbrains.compose.resources.stringResource

/**
 * Yadda saxlamadan əvvəl açılan qeyd formu: istifadəçi istəsə qeyd yazır, istəməsə boş buraxır.
 * Qeyd məcburi deyil, ona görə təsdiq düyməsi həmişə aktivdir.
 *
 * Həm ayələr, həm hədislər üçün işlədilir — [subtitle] hansı elementin saxlanıldığını göstərir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkNoteSheet(
    subtitle: String?,
    onDismiss: () -> Unit,
    onSave: (note: String?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (subtitle == null) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = colorScheme.scrim.alpha(0.5f),
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
    ) {
        SheetContent(subtitle = subtitle, onDismiss = onDismiss, onSave = onSave)
    }
}

@Composable
private fun SheetContent(
    subtitle: String,
    onDismiss: () -> Unit,
    onSave: (note: String?) -> Unit,
) {
    var note by remember(subtitle) { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(subtitle) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.ime)
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
    ) {
        BottomSheetHeader(
            icon = Res.drawable.ic_bookmark,
            title = stringResource(Res.string.strTitleAddBookmark),
            hasDragHandle = true,
        )

        Text(
            text = subtitle,
            style = typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = colorScheme.primary,
        )

        Text(
            text = stringResource(Res.string.strMsgBookmarkNoteOptional),
            style = typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .padding(top = 12.dp)
                .focusRequester(focusRequester),
            placeholder = { Text(stringResource(Res.string.strHintBookmarkViewerNote)) },
            shape = RoundedCornerShape(12.dp),
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(Res.string.strLabelCancel))
            }

            Button(
                onClick = { onSave(note.trim().takeIf { it.isNotEmpty() }) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    text = stringResource(Res.string.strLabelSaveBookmark),
                    style = typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
