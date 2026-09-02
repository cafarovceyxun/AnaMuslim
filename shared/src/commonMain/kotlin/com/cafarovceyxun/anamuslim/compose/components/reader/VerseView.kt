package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.compose.components.dialogs.SimpleTooltip
import com.cafarovceyxun.anamuslim.compose.components.dialogs.SimpleTooltipPosition
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_edit
import com.cafarovceyxun.anamuslim.resources.dr_icon_menu
import com.cafarovceyxun.anamuslim.resources.dr_icon_share
import com.cafarovceyxun.anamuslim.resources.ic_pause
import com.cafarovceyxun.anamuslim.compose.theme.LegacyColors
import com.cafarovceyxun.anamuslim.resources.ic_bookmark
import com.cafarovceyxun.anamuslim.resources.ic_bookmark_added
import com.cafarovceyxun.anamuslim.resources.strLabelBookmark
import com.cafarovceyxun.anamuslim.resources.ic_play
import com.cafarovceyxun.anamuslim.resources.strDescVerseNoWithChapter
import com.cafarovceyxun.anamuslim.resources.strLabelEdit
import com.cafarovceyxun.anamuslim.resources.strLabelShare
import com.cafarovceyxun.anamuslim.resources.strLabelVerseSerial
import com.cafarovceyxun.anamuslim.resources.strLabelVerseSerialWithChapter
import com.cafarovceyxun.anamuslim.resources.strTitleVerseOptions
import com.cafarovceyxun.anamuslim.resources.strTitleVerseRecitation
import com.cafarovceyxun.anamuslim.db.entities.quran.AyahWordEntity
import com.cafarovceyxun.anamuslim.db.relations.VerseWithDetails
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationPlayer
import com.cafarovceyxun.anamuslim.utils.reader.LocalVerseActions
import com.cafarovceyxun.anamuslim.viewModels.AuthViewModel
import com.cafarovceyxun.anamuslim.viewModels.DailyContentViewModel
import com.cafarovceyxun.anamuslim.viewModels.containsVerse
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun VerseView(
    verseUi: ReaderLayoutItem.VerseUI,
    isBookmarked: Boolean,
    showDivider: Boolean = false,
    onWordClick: ((AyahWordEntity) -> Unit)? = null,
) {
    val verse = verseUi.verse

    val dailyContentViewModel = viewModel { DailyContentViewModel() }
    val todayItems by dailyContentViewModel.todayItems.collectAsStateWithLifecycle()

    // Gündə beş element ola bilər, ona görə nişan siyahının hamısına baxır; çoxayəli element
    // aralığının **hər** ayəsində görünür.
    val isTodayVotd = remember(verse, todayItems) {
        todayItems.containsVerse(verse.chapterNo, verse.verseNo)
    }

    val recState = LocalRecitation.current
    val isVersePlaying = recState.isAnyPlaying && recState.playingVerse.doesEqual(verse)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isVersePlaying) colorScheme.primary.alpha(0.08f)
            else Color.Transparent
        ),
        border = if (isVersePlaying) BorderStroke(1.dp, colorScheme.primary.alpha(0.5f))
        else BorderStroke(0.5.dp, colorScheme.outlineVariant.alpha(0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (isTodayVotd) {
                IsVotd()
            }
            VerseActionBar(
                verse = verse,
                isVersePlaying = isVersePlaying,
                isBookmarked = isBookmarked,
            )

            QuranTextWbw(
                verseUi = verseUi,
                onWordClick = onWordClick
            )

            TranslationText(verseUi = verseUi)
        }
    }
}

/**
 * Sətirdəki əməllər: seçimlər, səsləndirmə, redaktə (admin), paylaş və yadda saxla.
 * "Günün ayəsi" seçimlər vərəqindədir (VerseOptionsSheet).
 */
@Composable
private fun VerseActionBar(
    verse: VerseWithDetails,
    isVersePlaying: Boolean,
    isBookmarked: Boolean,
) {
    val verseActions = LocalVerseActions.current
    val recitationState = LocalRecitation.current
    val controller = recitationState.controller
    val viewModel = LocalReaderViewModel.current
    val authViewModel = viewModel { AuthViewModel() }
    val session by authViewModel.session.collectAsStateWithLifecycle()
    val isAuthorized = session != null

    val iconTint = colorScheme.onBackground.alpha(0.7f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VerseActionIconButton(
                painter = painterResource(Res.drawable.dr_icon_menu),
                contentDescription = stringResource(Res.string.strTitleVerseOptions),
                tint = iconTint,
                iconModifier = Modifier.rotate(90f),
            ) {
                verseActions.onVerseOption?.invoke(verse)
            }

            PlayControlButton(controller, isVersePlaying, iconTint, verse)

            if (isAuthorized) {
                VerseActionIconButton(
                    painter = painterResource(Res.drawable.dr_icon_edit),
                    contentDescription = stringResource(Res.string.strLabelEdit),
                    tint = iconTint,
                ) {
                    viewModel.toggleEditing(ChapterVersePair(verse))
                }
            }

            VerseActionIconButton(
                painter = painterResource(Res.drawable.dr_icon_share),
                contentDescription = stringResource(Res.string.strLabelShare),
                tint = iconTint,
            ) {
                verseActions.onShareRequest?.invoke(verse)
            }

            VerseActionIconButton(
                painter = painterResource(
                    if (isBookmarked) Res.drawable.ic_bookmark_added else Res.drawable.ic_bookmark
                ),
                contentDescription = stringResource(Res.string.strLabelBookmark),
                tint = if (isBookmarked) LegacyColors.brandPrimary else iconTint,
            ) {
                verseActions.onBookmarkRequest?.invoke(
                    verse.chapterNo,
                    verse.verseNo..verse.verseNo
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        VerseSerial(verse = verse)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayControlButton(
    controller: RecitationPlayer,
    isVersePlaying: Boolean,
    iconTint: Color,
    verse: VerseWithDetails
) {
    val label = stringResource(Res.string.strTitleVerseRecitation)

    SimpleTooltip(
        text = label,
        position = SimpleTooltipPosition.Above
    ) {
        Box(
            modifier = Modifier
                .semantics {
                    this.contentDescription = label
                }
                .size(32.dp)
                .clip(CircleShape)
                .clickable {
                    controller.playControl(ChapterVersePair(verse))
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(if (isVersePlaying) Res.drawable.ic_pause else Res.drawable.ic_play),
                contentDescription = null,
                modifier = Modifier
                    .padding(6.dp),
                tint = iconTint,
            )
        }
    }
}

@Composable
private fun VerseActionIconButton(
    painter: Painter,
    contentDescription: String,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    tint: Color,
    onClick: () -> Unit,
) {
    SimpleTooltip(contentDescription, position = SimpleTooltipPosition.Above) {
        Box(
            modifier = modifier
                .semantics { this.contentDescription = contentDescription }
                .size(32.dp)
                .clip(CircleShape)
                .clickable(
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .padding(6.dp)
                    .then(iconModifier),
                tint = tint,
            )
        }
    }
}

@Composable
private fun VerseSerial(verse: VerseWithDetails) {
    val chapterNo = verse.chapterNo
    val verseNo = verse.verseNo


    val contentDescription = stringResource(
        Res.string.strDescVerseNoWithChapter,
        verse.chapter.getCurrentName(),
        verseNo
    )

    Text(
        text = if (verse.includeChapterNameInSerial) {
            stringResource(
                Res.string.strLabelVerseSerialWithChapter,
                verse.chapter.getCurrentName(),
                chapterNo,
                verseNo
            )
        } else stringResource(
            Res.string.strLabelVerseSerial,
            chapterNo,
            verseNo
        ),
        modifier = Modifier
            .semantics { this.contentDescription = contentDescription }
            .clip(RoundedCornerShape(8.dp))
            .background(colorScheme.surfaceContainer)
            .clickable(
                onClick = {
                    // `verse.id` bazadakı açardır (surahNo * 1000 + ayahNo) — panoya istinad formatı düşməlidir.
                    PlatformUtils.copyToClipboard("$chapterNo:$verseNo")
                },
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = colorScheme.primary,
        style = typography.labelLarge.copy(fontWeight = FontWeight.Bold),
    )
}
