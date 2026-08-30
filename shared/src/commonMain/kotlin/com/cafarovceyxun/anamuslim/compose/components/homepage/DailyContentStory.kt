package com.cafarovceyxun.anamuslim.compose.components.homepage

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cafarovceyxun.anamuslim.compose.components.common.IconButton
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.QuranImageEditorScreen
import com.cafarovceyxun.anamuslim.compose.components.share.ShareImageSegment
import com.cafarovceyxun.anamuslim.compose.screens.hadith.HadithImageEditorScreen
import com.cafarovceyxun.anamuslim.compose.screens.hadith.withScriptDirection
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.theme.hadithArabicFontFamily
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HadithPreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.VersePreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dailyContentShareImage
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.dr_icon_heart_filled
import com.cafarovceyxun.anamuslim.resources.dr_icon_share
import com.cafarovceyxun.anamuslim.resources.ic_bookmark
import com.cafarovceyxun.anamuslim.resources.ic_bookmark_added
import com.cafarovceyxun.anamuslim.resources.strLabelBookmark
import com.cafarovceyxun.anamuslim.resources.strLabelRead
import com.cafarovceyxun.anamuslim.resources.strDescClose
import com.cafarovceyxun.anamuslim.resources.strTitleDailyHadith
import com.cafarovceyxun.anamuslim.resources.strTitleVOTD
import com.cafarovceyxun.anamuslim.utils.supabase.DailyContent
import com.cafarovceyxun.anamuslim.utils.verse.DailyContentDisplay
import com.cafarovceyxun.anamuslim.repository.supabase.DailyContentRepository
import com.cafarovceyxun.anamuslim.utils.verse.DailyContentBookmarks
import com.cafarovceyxun.anamuslim.utils.verse.DailyContentFactory
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Bir hekayənin ekranda qalma müddəti — funksiya hekayələri ilə eyni ritm. */
private const val DAILY_STORY_DURATION_MILLIS = 8000

/**
 * «Günün ayəsi» hekayə dairəsi — ana səhifədəki zolağın **birinci** elementi.
 *
 * Funksiya hekayələrindən fərqli olaraq şəkil yoxdur: dairə tətbiqin öz rənglərindədir, içindəki
 * səhifələr isə günün növbəsindən ([DailyContentStoryViewer]) qurulur. Gündə beş element ola bilər,
 * hekayə də bildirişlərlə **eyni sıra** ilə gedir.
 */
@Composable
fun DailyContentStoryCircle(itemCount: Int, unseen: Boolean, onClick: () -> Unit) {
    // Funksiya hekayələri ilə eyni qayda: baxılmayanda tətbiqin yaşıl halqası, baxandan sonra
    // halqa sönüb nazik kənara çevrilir. Vəziyyət **cihazda** saxlanılır.
    val ringBrush = if (unseen) {
        Brush.linearGradient(listOf(colorScheme.primary, colorScheme.primary.alpha(0.45f)))
    } else {
        Brush.linearGradient(listOf(colorScheme.outlineVariant, colorScheme.outlineVariant))
    }

    Column(
        modifier = Modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .border(width = 2.5.dp, brush = ringBrush, shape = CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(colorScheme.primaryContainer)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_heart_filled),
                contentDescription = stringResource(Res.string.strTitleVOTD),
                tint = colorScheme.primary,
                modifier = Modifier.size(26.dp),
            )

            // Birdən çox element varsa sayı dairənin küncündə görünür — istifadəçi hekayənin bir
            // səhifədən ibarət olmadığını açmadan bilir.
            if (itemCount > 1) {
                Text(
                    text = itemCount.toString(),
                    style = typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(colorScheme.primary)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = stringResource(Res.string.strTitleVOTD),
            style = typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
