package com.cafarovceyxun.anamuslim.compose.components.homepage

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cafarovceyxun.anamuslim.compose.components.common.IconButton
import com.cafarovceyxun.anamuslim.compose.components.settings.withContentDirection
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.repository.supabase.SuggestionRepository
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.dr_icon_feature
import com.cafarovceyxun.anamuslim.resources.strDescClose
import com.cafarovceyxun.anamuslim.resources.suggestionsWhatsNew
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.app.rememberRemoteImage
import com.cafarovceyxun.anamuslim.utils.supabase.Suggestion
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionLocalStore
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionStatus
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val STORY_DURATION_MILLIS = 6000

/**
 * «Yeniliklər» — əlavə olunmuş funksiyaların hekayə zolağı, ana səhifənin ən yuxarısında.
 *
 * Yalnız **şəkli olan tamamlanmış** təkliflər düşür: hekayənin bütün mənası «bu funksiya proqramda
 * buradadır» ekran görüntüsüdür, şəkilsiz dairə boş qalardı. Baxılmamışın ətrafında tətbiqin yaşıl
 * halqası olur, baxandan sonra halqa itir — baxılma vəziyyəti **cihazda** saxlanılır
 * ([SuggestionLocalStore]), serverdə istifadəçi kimliyi yoxdur.
 *
 * Şəbəkə çatmasa zolaq sadəcə görünmür.
 */
@Composable
fun FeatureStoriesRow() {
    val repository = remember { SuggestionRepository() }
    val scope = rememberCoroutineScope()

    var features by remember { mutableStateOf<List<Suggestion>>(emptyList()) }
    var seenIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var openIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        seenIds = SuggestionLocalStore.seenFeatureIds()
        features = runCatching {
            repository.fetchApproved()
                .filter { it.status == SuggestionStatus.DONE && !it.image_url.isNullOrBlank() }
        }.onFailure {
            AppLogger.d("FeatureStories", "Fetch failed: ${it.message}")
        }.getOrDefault(emptyList())
    }

    if (features.isEmpty()) return

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(features, key = { _, item -> item.id }) { index, feature ->
            StoryCircle(
                feature = feature,
                unseen = feature.id !in seenIds,
                onClick = { openIndex = index },
            )
        }
    }

    openIndex?.let { index ->
        FeatureStoryViewer(
            features = features,
            startIndex = index,
            onSeen = { id ->
                if (id !in seenIds) {
                    seenIds = seenIds + id
                    scope.launch { SuggestionLocalStore.markFeatureSeen(id) }
                }
            },
            onClose = { openIndex = null },
        )
    }
}

@Composable
private fun StoryCircle(
    feature: Suggestion,
    unseen: Boolean,
    onClick: () -> Unit,
) {
    val image = rememberRemoteImage(feature.image_url)

    // Baxılmayanda tətbiqin öz yaşılından halqa; baxandan sonra halqa itir və dairənin yalnız
    // nazik kənarı qalır.
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
                .border(width = if (unseen) 2.5.dp else 1.dp, brush = ringBrush, shape = CircleShape)
                .padding(if (unseen) 4.dp else 3.dp)
                .clip(CircleShape)
                .background(colorScheme.surfaceContainerHigh)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = feature.body,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_feature),
                    contentDescription = stringResource(Res.string.suggestionsWhatsNew),
                    tint = colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = feature.body,
            style = typography.labelSmall.withContentDirection(),
            color = colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Tam ekran hekayə baxışı. `Dialog` olaraq açılır (inline emit yox) — bax CLAUDE.md, «Tam ekran
 * səth `Dialog` olmalıdır»: ana səhifə gələcəkdə modal vərəqin altından da göstərilə bilər.
 */
@Composable
private fun FeatureStoryViewer(
    features: List<Suggestion>,
    startIndex: Int,
    onSeen: (Long) -> Unit,
    onClose: () -> Unit,
) {
    var index by remember { mutableStateOf(startIndex.coerceIn(0, features.lastIndex)) }
    val progress = remember { Animatable(0f) }
    val current = features.getOrNull(index) ?: return

    LaunchedEffect(index) {
        onSeen(current.id)
        progress.snapTo(0f)
        progress.animateTo(1f, tween(STORY_DURATION_MILLIS, easing = LinearEasing))

        if (index < features.lastIndex) index++ else onClose()
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(features.size) {
                    detectTapGestures { offset ->
                        // İnstaqram məntiqi: sağ tərəf növbəti, sol tərəf əvvəlki.
                        if (offset.x > size.width / 2) {
                            if (index < features.lastIndex) index++ else onClose()
                        } else {
                            if (index > 0) index--
                        }
                    }
                },
        ) {
            val image = rememberRemoteImage(current.image_url)

            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = current.body,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(28.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    features.forEachIndexed { i, _ ->
                        val fill = when {
                            i < index -> 1f
                            i == index -> progress.value
                            else -> 0f
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.alpha(0.35f)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fill)
                                    .height(3.dp)
                                    .background(Color.White),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(Res.string.suggestionsWhatsNew),
                        style = typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )

                    IconButton(
                        painter = painterResource(Res.drawable.dr_icon_close),
                        contentDescription = stringResource(Res.string.strDescClose),
                        tint = Color.White,
                        small = true,
                        onClick = onClose,
                    )
                }
            }

            Text(
                text = current.body,
                style = typography.bodyMedium.withContentDirection(),
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .background(Color.Black.alpha(0.55f))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }
    }
}
