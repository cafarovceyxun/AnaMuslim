package com.cafarovceyxun.anamuslim.compose.components.reader.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheetHeader
import com.cafarovceyxun.anamuslim.compose.screens.hadith.FormTextField
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.db.relations.VerseWithDetails
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_info
import com.cafarovceyxun.anamuslim.resources.dr_icon_report_problem
import com.cafarovceyxun.anamuslim.resources.strHintReportMessage
import com.cafarovceyxun.anamuslim.resources.strLabelReportMessage
import com.cafarovceyxun.anamuslim.resources.strLabelSend
import com.cafarovceyxun.anamuslim.resources.strMsgReportContext
import com.cafarovceyxun.anamuslim.resources.strMsgReportFailed
import com.cafarovceyxun.anamuslim.resources.strMsgReportSent
import com.cafarovceyxun.anamuslim.resources.strTitleReportVerse
import com.cafarovceyxun.anamuslim.viewModels.VerseReportSubmitState
import com.cafarovceyxun.anamuslim.viewModels.VerseReportViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Ayə ilə bağlı səhv bildirişi. İstifadəçidən yalnız izah istənir — surə/ayə, tərcümə slug-ları və
 * app versiyası arxa fonda göndərilir, ona görə admin bildirişin hansı ayəyə aid olduğunu görür.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerseReportSheet(
    vwd: VerseWithDetails?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (vwd == null) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = colorScheme.scrim.alpha(0.5f),
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
    ) {
        SheetContent(verse = vwd, onDismiss = onDismiss)
    }
}

@Composable
private fun SheetContent(
    verse: VerseWithDetails,
    onDismiss: () -> Unit,
) {
    val viewModel = viewModel { VerseReportViewModel() }
    val submitState by viewModel.submitState.collectAsState()

    var message by remember { mutableStateOf("") }

    val sentMsg = stringResource(Res.string.strMsgReportSent)
    val failedMsg = stringResource(Res.string.strMsgReportFailed)

    LaunchedEffect(submitState) {
        when (val state = submitState) {
            is VerseReportSubmitState.Success -> {
                PlatformUtils.showLongToast(sentMsg)
                viewModel.resetSubmitState()
                onDismiss()
            }

            is VerseReportSubmitState.Error -> {
                PlatformUtils.showLongToast(state.message?.let { "$failedMsg\n$it" } ?: failedMsg)
                viewModel.resetSubmitState()
            }

            else -> Unit
        }
    }

    val isSubmitting = submitState is VerseReportSubmitState.Submitting
    val canSubmit = message.trim().length >= VerseReportViewModel.MIN_MESSAGE_LENGTH && !isSubmitting

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.ime)
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
    ) {
        BottomSheetHeader(
            icon = Res.drawable.dr_icon_report_problem,
            title = stringResource(Res.string.strTitleReportVerse),
            hasDragHandle = true,
        )

        VerseContextCard(verse)

        Spacer(Modifier.height(16.dp))

        FormTextField(
            value = message,
            onValueChange = {
                if (it.length <= VerseReportViewModel.MAX_MESSAGE_LENGTH) message = it
            },
            label = stringResource(Res.string.strLabelReportMessage),
            placeholder = stringResource(Res.string.strHintReportMessage),
            icon = Res.drawable.dr_icon_report_problem,
            minLines = 4,
            maxLines = 8,
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                viewModel.submit(
                    chapterNo = verse.chapterNo,
                    verseNo = verse.verseNo,
                    verseKey = "${verse.chapter.getCurrentName()} ${verse.chapterNo}:${verse.verseNo}",
                    message = message,
                    slugs = verse.translations.map { it.bookSlug }.toSet(),
                )
            },
            enabled = canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = colorScheme.onPrimary,
                )
            } else {
                Text(
                    text = stringResource(Res.string.strLabelSend),
                    style = typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** Bildirişlə birlikdə nəyin göndərildiyini istifadəçiyə açıq göstərir. */
@Composable
private fun VerseContextCard(verse: VerseWithDetails) {
    Surface(
        color = colorScheme.surfaceVariant.alpha(0.4f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_info),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = colorScheme.primary,
            )

            Spacer(Modifier.width(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "${verse.chapter.getCurrentName()} ${verse.chapterNo}:${verse.verseNo}",
                    style = typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(Res.string.strMsgReportContext),
                    style = typography.labelSmall,
                    color = colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
