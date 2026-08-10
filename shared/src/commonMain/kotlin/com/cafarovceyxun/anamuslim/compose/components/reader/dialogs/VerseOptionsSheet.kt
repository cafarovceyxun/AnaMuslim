package com.cafarovceyxun.anamuslim.compose.components.reader.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_heart_filled
import com.cafarovceyxun.anamuslim.resources.dr_icon_report_problem
import com.cafarovceyxun.anamuslim.resources.ic_book_copy
import com.cafarovceyxun.anamuslim.resources.noSimilarVerses
import com.cafarovceyxun.anamuslim.resources.similarVerses
import com.cafarovceyxun.anamuslim.resources.strLabelReport
import com.cafarovceyxun.anamuslim.resources.strTitleReaderVerseInformation
import com.cafarovceyxun.anamuslim.resources.strTitleVOTD
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheetHeader
import com.cafarovceyxun.anamuslim.compose.components.reader.LocalReaderViewModel
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.db.relations.VerseWithDetails
import com.cafarovceyxun.anamuslim.utils.reader.LocalVerseActions
import com.cafarovceyxun.anamuslim.utils.supabase.DailyContent
import com.cafarovceyxun.anamuslim.viewModels.AuthViewModel
import com.cafarovceyxun.anamuslim.viewModels.DailyContentViewModel

data class VerseOptionsData(
    val verse: VerseWithDetails,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerseOptionsSheet(
    data: VerseOptionsData?,
    onClose: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(true)

    if (data == null) {
        return
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        scrimColor = colorScheme.scrim.alpha(0.5f),
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
    ) {
        VodSheetContent(
            verse = data.verse,
            onDismiss = onClose,
        )
    }
}

@Composable
private fun VodSheetContent(
    verse: VerseWithDetails,
    onDismiss: () -> Unit,
) {
    val repository = LocalReaderViewModel.current.repository
    val verseActions = LocalVerseActions.current

    val dailyContentViewModel = viewModel { DailyContentViewModel() }
    val authViewModel = viewModel { AuthViewModel() }
    val session by authViewModel.session.collectAsStateWithLifecycle()
    val isAuthorized = session != null

    val title = stringResource(
        Res.string.strTitleReaderVerseInformation,
        verse.chapter.getCurrentName(),
        verse.verseNo
    )

    val noSimilarVersesMsg = stringResource(Res.string.noSimilarVerses)

    val hasSimilarVerses by produceState(false, verse.id) {
        value = repository.extrasDao.countSimilarVerses(verse.id) > 0
    }

    Column(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        BottomSheetHeader(
            title = title,
            hasDragHandle = true
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VodOptionItem(
                iconRes = Res.drawable.ic_book_copy,
                labelRes = Res.string.similarVerses,
                enabled = hasSimilarVerses,
                onClick = {
                    if (hasSimilarVerses) {
                        onDismiss()
                        verseActions.onSimilarVerses?.invoke(verse)
                    } else {
                        PlatformUtils.showLongToast(noSimilarVersesMsg)
                    }
                }
            )

            if (isAuthorized) {
                VodOptionItem(
                    iconRes = Res.drawable.dr_icon_heart_filled,
                    labelRes = Res.string.strTitleVOTD,
                    tint = colorScheme.primary,
                    onClick = {
                        onDismiss()
                        dailyContentViewModel.setDailyContent(
                            DailyContent(
                                content_type = "verse",
                                chapter_no = verse.chapterNo,
                                verse_no = verse.verseNo,
                                text_ar = verse.words.joinToString(" ") { it.text },
                                text_az = verse.translations.firstOrNull()?.text ?: "",
                                source = "${verse.chapter.getCurrentName()} ${verse.verseNo}",
                            )
                        )
                    },
                )
            }

            VodOptionItem(
                iconRes = Res.drawable.dr_icon_report_problem,
                labelRes = Res.string.strLabelReport,
                onClick = {
                    onDismiss()
                    verseActions.onReportRequest?.invoke(verse)
                },
            )
        }
    }
}

@Composable
private fun VodOptionItem(
    iconRes: DrawableResource,
    labelRes: StringResource,
    enabled: Boolean = true,
    tint: Color? = null,
    onClick: () -> Unit,
) {
    val alpha = if (enabled) 1f else 0.5f
    val defaultTint = colorScheme.onBackground.alpha(0.7f)
    val iconTint = tint ?: defaultTint

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .alpha(alpha)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = stringResource(labelRes),
                modifier = Modifier.size(26.dp),
                tint = iconTint,
            )
        }
        Text(
            text = stringResource(labelRes),
            style = typography.labelMedium,
            color = defaultTint,
            modifier = Modifier.padding(top = 5.dp),
        )
    }
}
