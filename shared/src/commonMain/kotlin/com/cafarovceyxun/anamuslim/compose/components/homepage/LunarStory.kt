package com.cafarovceyxun.anamuslim.compose.components.homepage

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cafarovceyxun.anamuslim.compose.components.common.IconButton
import com.cafarovceyxun.anamuslim.compose.components.common.StoryVideo
import com.cafarovceyxun.anamuslim.compose.components.prayer.PrayerUiFormat
import com.cafarovceyxun.anamuslim.compose.components.settings.withContentDirection
import com.cafarovceyxun.anamuslim.compose.theme.LocalAppTextScale
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.dr_icon_eye
import com.cafarovceyxun.anamuslim.resources.dr_icon_lunar
import com.cafarovceyxun.anamuslim.resources.lunarCalendarTitle
import com.cafarovceyxun.anamuslim.resources.lunarStoryLength
import com.cafarovceyxun.anamuslim.resources.lunarStorySighted
import com.cafarovceyxun.anamuslim.resources.lunarStoryStart
import com.cafarovceyxun.anamuslim.resources.strDescClose
import com.cafarovceyxun.anamuslim.resources.suggestionsViews
import com.cafarovceyxun.anamuslim.utils.IsoDate
import com.cafarovceyxun.anamuslim.utils.app.rememberRemoteImage
import com.cafarovceyxun.anamuslim.utils.supabase.LunarAnnouncement
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Şəkil/mətn slaydının müddəti — funksiya və günün ayəsi hekayələri ilə eyni ritm. */
private const val LUNAR_STORY_DURATION_MILLIS = 7000

/**
 * «Qəməri təqvim» hekayə dairəsi — zolaqda **günün ayəsinin yanında**.
 *
 * Dairədə son elanın şəkli göstərilir; elan yalnız videodursa hilal nişanı qalır (kadr çıxarmaq
 * ayrıca dekodlama tələb edərdi, «Yeniliklər» dairəsi ilə eyni qərar).
 */
@Composable
fun LunarStoryCircle(latest: LunarAnnouncement, itemCount: Int, unseen: Boolean, onClick: () -> Unit) {
    val image = rememberRemoteImage(latest.media.firstOrNull { !it.isVideo }?.url)

    val ringBrush = if (unseen) {
        Brush.linearGradient(listOf(colorScheme.primary, colorScheme.primary.alpha(0.45f)))
    } else {
        Brush.linearGradient(listOf(colorScheme.outlineVariant, colorScheme.outlineVariant))
    }

    val label = stringResource(Res.string.lunarCalendarTitle)

    Column(
        modifier = Modifier.width(StoryCircleWidth),
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
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_lunar),
                    contentDescription = label,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }

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
            text = label,
            style = typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Son 12 ayın elanları — tam ekran hekayə.
 *
 * `Dialog` olaraq açılır (inline emit yox) — bax CLAUDE.md, «Tam ekran səth `Dialog` olmalıdır».
 * Jestlər, zolaq və pauza qaydası «Yeniliklər» hekayəsinin eynisidir ki, istifadəçi iki fərqli
 * hekayə davranışı öyrənməsin.
 */
@Composable
fun LunarStoryViewer(
    announcements: List<LunarAnnouncement>,
    startIndex: Int,
    onSeen: (Long) -> Unit,
    onClose: () -> Unit,
) {
    var index by remember { mutableStateOf(startIndex.coerceIn(0, announcements.lastIndex)) }
    var slide by remember { mutableStateOf(0) }
    val progress = remember { Animatable(0f) }

    var isHeldPaused by remember { mutableStateOf(false) }
    var isTapPaused by remember { mutableStateOf(false) }
    val isPaused = isHeldPaused || isTapPaused

    val current = announcements.getOrNull(index) ?: return
    val media = current.media

    // Mediası olmayan elan **bir** slayd kimi göstərilir: qeyd mətn kartı olur.
    val slideCount = maxOf(media.size, 1)
    val currentMedia = media.getOrNull(slide)

    val goNext: () -> Unit = {
        isTapPaused = false
        when {
            slide < slideCount - 1 -> slide++
            index < announcements.lastIndex -> {
                index++
                slide = 0
            }

            else -> onClose()
        }
    }

    val goPrevious: () -> Unit = {
        isTapPaused = false
        when {
            slide > 0 -> slide--
            index > 0 -> {
                index--
                slide = (announcements[index].media.size - 1).coerceAtLeast(0)
            }
        }
    }

    var videoProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(index) { onSeen(announcements[index].id) }

    LaunchedEffect(index, slide) { videoProgress = 0f }

    LaunchedStoryProgress(
        key = index to slide,
        running = !isPaused && currentMedia?.isVideo != true,
        durationMillis = LUNAR_STORY_DURATION_MILLIS,
        progress = progress,
        onFinished = goNext,
    )

    val title = stringResource(Res.string.lunarCalendarTitle)
    val monthName = stringResource(PrayerUiFormat.hijriMonthName(current.hijri_month))
    val heading = "$monthName ${current.hijri_year}"

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(announcements.size) {
                    detectTapGestures(
                        onPress = {
                            isHeldPaused = true
                            tryAwaitRelease()
                            isHeldPaused = false
                        },
                        // `onLongPress` verilməsə uzun basışın buraxılışı da `onTap` sayılır və
                        // hekayə oxunan kimi növbəti slayda tullanır.
                        onLongPress = {},
                        onTap = { offset ->
                            when {
                                offset.x < size.width / 3f -> goPrevious()
                                offset.x > size.width * 2 / 3f -> goNext()
                                else -> isTapPaused = !isTapPaused
                            }
                        },
                    )
                }
                .pointerInput(Unit) {
                    var dragged = 0f

                    detectVerticalDragGestures(
                        onDragStart = { dragged = 0f },
                        onDragEnd = { if (dragged > STORY_DISMISS_DRAG_PX) onClose() },
                        onDragCancel = { dragged = 0f },
                        onVerticalDrag = { _, delta -> dragged += delta },
                    )
                },
        ) {
            if (currentMedia == null) {
                LunarTextSlide(heading = heading, note = current.note.orEmpty())
            } else if (currentMedia.isVideo) {
                StoryVideo(
                    url = currentMedia.url,
                    modifier = Modifier.fillMaxSize(),
                    paused = isPaused,
                    onProgress = { videoProgress = it },
                    onFinished = goNext,
                )
            } else {
                val image = rememberRemoteImage(currentMedia.url)

                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = heading,
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
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.alpha(0.55f), Color.Transparent),
                        ),
                    )
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 28.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(slideCount) { i ->
                        val fill = when {
                            i < slide -> 1f
                            i > slide -> 0f
                            media.getOrNull(i)?.isVideo == true -> videoProgress
                            else -> progress.value
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
                        text = "$title · $heading",
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

            val textScale = LocalAppTextScale.current

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.alpha(0.75f),
                                Color.Black.alpha(0.92f),
                            ),
                        ),
                    )
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 16.dp),
            ) {
                // Mətn slaydında qeyd onsuz da kadrın ortasındadır — altda təkrarlanmır.
                current.note?.takeIf { it.isNotBlank() && currentMedia != null }?.let { note ->
                    Text(
                        text = note,
                        style = typography.titleSmall.withContentDirection().copy(
                            fontSize = 17.sp * textScale,
                            lineHeight = 24.sp * textScale,
                        ),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )

                    Spacer(Modifier.height(6.dp))
                }

                Text(
                    text = lunarFactsLine(current),
                    style = typography.bodyMedium.withContentDirection().copy(
                        fontSize = 15.sp * textScale,
                        lineHeight = 22.sp * textScale,
                    ),
                    color = Color.White.alpha(0.92f),
                )

                // Baxış sayğacı — funksiya hekayəsindəki ilə eyni yer və eyni görünüş
                // (`FeatureStoryViewer`). Sıfır gizlədilmir: elan təzə yayımlananda «0» görmək
                // sayğacın işlədiyini göstərir, boşluq isə «yoxdur»la qarışardı.
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.weight(1f))

                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_eye),
                        contentDescription = stringResource(Res.string.suggestionsViews),
                        tint = Color.White.alpha(0.75f),
                        modifier = Modifier.size(16.dp),
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        text = current.view_count.toString(),
                        style = typography.labelMedium.copy(fontSize = 13.sp * textScale),
                        fontWeight = FontWeight.Bold,
                        color = Color.White.alpha(0.75f),
                    )
                }
            }
        }
    }
}

/**
 * «Ayın 1-i · 30 gün · Ay göründü» sətri — elanın **bütün faktı** bir yerdə.
 *
 * Boş sahə ayırıcısını da aparır: görünmə vaxtı yazılmayıbsa sətrin sonunda asılı qalan «·»
 * qalmasın.
 */
@Composable
private fun lunarFactsLine(announcement: LunarAnnouncement): String = listOfNotNull(
    stringResource(Res.string.lunarStoryStart, IsoDate.display(announcement.start_date)),
    stringResource(Res.string.lunarStoryLength, announcement.length_days),
    announcement.sighted_at
        ?.takeIf { it.isNotBlank() }
        ?.let { stringResource(Res.string.lunarStorySighted, displaySighting(it)) },
).joinToString(" · ")

/**
 * `2026-09-14T19:42:00+00:00` → `14.09.2026 19:42`.
 *
 * ⚠️ Qurşaq çevirməsi **qəsdən yoxdur**: admin ayı hansı saatda gördüyünü öz yerli saatı ilə yazır
 * və klient onu `+00:00` ilə göndərir, yəni baza dəyəri olduğu kimi geri qaytarır. Burada yerli
 * qurşağa çevirsəydik eyni elan Bakıda bir saat, İstanbulda başqa saat görünərdi — halbuki bu,
 * ölçülmüş bir an deyil, **elan olunmuş** bir vaxtdır.
 */
private fun displaySighting(iso: String): String {
    val date = IsoDate.display(iso)
    val time = iso.substringAfter('T', "").take(5).takeIf { it.length == 5 }

    return listOfNotNull(date, time).joinToString(" ")
}

/**
 * Mediası olmayan elanın slaydı — qeyd kadrın ortasında.
 *
 * Fon gecə səmasına yaxın qradiyentdir: hilal hekayəsi şəkilsiz də «ay» kimi oxunmalıdır, tətbiqin
 * adi yaşıl qradiyenti isə burada funksiya hekayəsi kimi görünürdü.
 */
@Composable
private fun LunarTextSlide(heading: String, note: String) {
    val textScale = LocalAppTextScale.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        colorScheme.primary.alpha(0.40f),
                        colorScheme.primary.alpha(0.12f),
                        Color.Black,
                    ),
                ),
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 28.dp, vertical = 80.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_lunar),
                contentDescription = null,
                tint = Color.White.alpha(0.9f),
                modifier = Modifier.size(56.dp),
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = heading,
                style = typography.headlineSmall.withContentDirection().copy(
                    fontSize = 24.sp * textScale,
                    lineHeight = 34.sp * textScale,
                ),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            if (note.isNotBlank()) {
                Spacer(Modifier.height(12.dp))

                Text(
                    text = note,
                    style = typography.bodyLarge.withContentDirection().copy(
                        fontSize = 16.sp * textScale,
                        lineHeight = 24.sp * textScale,
                    ),
                    color = Color.White.alpha(0.92f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
