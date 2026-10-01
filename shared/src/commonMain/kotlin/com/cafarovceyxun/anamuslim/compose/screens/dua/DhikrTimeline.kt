package com.cafarovceyxun.anamuslim.compose.screens.dua

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafarovceyxun.anamuslim.compose.screens.hadith.withScriptDirection
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.theme.arabicFontFamily
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_check
import com.cafarovceyxun.anamuslim.resources.dr_icon_menu
import com.cafarovceyxun.anamuslim.resources.dr_icon_open
import com.cafarovceyxun.anamuslim.resources.dr_icon_refresh
import com.cafarovceyxun.anamuslim.resources.duaCounterDone
import com.cafarovceyxun.anamuslim.resources.duaCounterProgress
import com.cafarovceyxun.anamuslim.resources.duaCounterReset
import com.cafarovceyxun.anamuslim.resources.duaOpenSource
import com.cafarovceyxun.anamuslim.resources.duaPartActions
import com.cafarovceyxun.anamuslim.resources.duaPartsSummary
import com.cafarovceyxun.anamuslim.resources.strLabelDelete
import com.cafarovceyxun.anamuslim.resources.strLabelEdit
import com.cafarovceyxun.anamuslim.utils.supabase.Dua
import com.cafarovceyxun.anamuslim.utils.text.withSearchHighlight
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Zikr sayğacı — hər hissənin neçə dəfə deyildiyi.
 *
 * Açar hissənin **sətir id-sidir**: çoxhissəli duada hər hissə öz `dua` sətridir. Vəziyyət
 * vərəqləyici ekranı ilə yaşayır (bax [rememberDhikrCounts]) — səhifəni sürüşdürüb qayıdanda say
 * itmir, ekrandan çıxanda isə sıfırlanır. Bazaya yazılmır: bu, oxuyanın anlıq işidir.
 */
@Stable
internal class DhikrCounts {
    private val counts = mutableStateMapOf<Long, Int>()

    fun countOf(part: Dua): Int = part.id?.let { counts[it] } ?: 0

    /** Sayı bir artırır; hissə **məhz bu toxunuşla** tamamlandısa `true`. */
    fun increment(part: Dua): Boolean {
        val id = part.id ?: return false
        val target = part.repeat_count?.takeIf { it > 0 } ?: return false
        val current = counts[id] ?: 0
        if (current >= target) return false

        counts[id] = current + 1
        return current + 1 == target
    }

    fun reset(parts: List<Dua>) {
        parts.forEach { part -> part.id?.let(counts::remove) }
    }

    /**
     * Növbəti deyiləcək hissə — sayı olan və hələ bitməyən ilk hissə; hamısı bitibsə və ya heç
     * birinin sayı yoxdursa -1.
     */
    fun activeIndex(parts: List<Dua>): Int = parts.indexOfFirst { part ->
        val target = part.repeat_count?.takeIf { it > 0 }
        target != null && countOf(part) < target
    }

    fun total(parts: List<Dua>): Int = parts.sumOf { countOf(it) }
}

/** Ekran ömürlü sayğac — vərəqləyicinin səviyyəsində yaradılır ki, səhifə dəyişəndə itməsin. */
@Composable
internal fun rememberDhikrCounts(): DhikrCounts = remember { DhikrCounts() }

/**
 * Dua səhifəsinin gövdəsi — **ardıcıllıq xətti**, tək və çoxhissəli dua üçün eyni.
 *
 * Hər hissə bir addımdır: solda xəttin işarəsi, sağda **tam** mətn (ərəbcə, oxunuş, tərcümə, qeyd;
 * heç biri qısaldılmır). İşarə duanın növünü deyir:
 * - **saysız** hissə → kiçik nöqtə;
 * - **saylı** hissə → öz halqası («12/33», bitəndə ✓); addıma toxunmaq onu sayır.
 *
 * Saylı duada yuxarıda say nişanı və ↻ sıfırlama düyməsi var. Mətnin altındakı böyük sayğac
 * düyməsi istifadəçi istəyi ilə götürüldü (2026-10-01): sayma addımın özündədir.
 *
 * Mənbə **bir dəfə** yazılır və özü keçiddir («Qaynağa bax» düyməsinin yerinə); hissəyə aid
 * redaktə/silmə ⋮ menyusundadır. Uzun basmaq — səhifənin qalanında olduğu kimi — duanı kopyalayır.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DhikrTimeline(
    parts: List<Dua>,
    counts: DhikrCounts,
    isAuthorized: Boolean,
    visibility: DuaBlockVisibility,
    arabicSizeMult: Float,
    translationSizeMult: Float,
    query: String,
    /** Hissələr müxtəlif mənbədəndirsə «Qaynağa bax» hər hissənin menyusuna da düşür. */
    singleSource: Boolean,
    onOpenSource: (Dua) -> Unit,
    onEdit: (Dua) -> Unit,
    onDelete: (Dua) -> Unit,
    onLongPress: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val hasCounter = parts.any { (it.repeat_count ?: 0) > 0 }
    val multiPart = parts.size > 1
    val active = counts.activeIndex(parts)
    val total = counts.total(parts)
    val target = parts.sumOf { it.repeat_count?.takeIf { count -> count > 0 } ?: 0 }

    fun count(index: Int) {
        val completed = counts.increment(parts[index])
        haptics.performHapticFeedback(
            if (completed) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove,
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Say nişanı: çoxhissəli duada «4 hissə · cəmi 100» (sayılarkən «45/100»); tək hissəlidə
        // yalnız sayma başlayandan sonra — başlamamış halda halqanın özü «100» deyir. Yanında ↻.
        if (hasCounter && (multiPart || total > 0)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                // Sıfırlama düyməsinin yeri hər iki tərəfdə ayrılır ki, nişan mərkəzdən sürüşməsin.
                Spacer(Modifier.size(48.dp))

                Surface(
                    color = colorScheme.primaryContainer.alpha(0.45f),
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = when {
                            total == 0 -> stringResource(Res.string.duaPartsSummary, parts.size, target)
                            active < 0 -> stringResource(Res.string.duaCounterDone)
                            else -> stringResource(Res.string.duaCounterProgress, total, target)
                        },
                        style = typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            .withScriptDirection(arabic = false),
                        color = colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }

                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    if (total > 0) {
                        IconButton(onClick = { counts.reset(parts) }) {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_refresh),
                                contentDescription = stringResource(Res.string.duaCounterReset),
                                tint = colorScheme.onSurfaceVariant.alpha(0.7f),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }

        parts.forEachIndexed { index, part ->
            val partTarget = part.repeat_count?.takeIf { it > 0 }

            DhikrStep(
                part = part,
                count = counts.countOf(part),
                ringTarget = partTarget,
                // Vurğu fonu yalnız çoxhissəlidə — tək hissədə «növbəti» deyiləsi başqa hissə yoxdur.
                isActive = multiPart && index == active,
                isLast = index == parts.lastIndex,
                visibility = visibility,
                arabicSizeMult = arabicSizeMult,
                translationSizeMult = translationSizeMult,
                query = query,
                onTap = if (partTarget != null) ({ count(index) }) else null,
                onLongPress = onLongPress,
                onOpenSource = if (singleSource) null else ({ onOpenSource(part) }),
                onEdit = if (isAuthorized) ({ onEdit(part) }) else null,
                onDelete = if (isAuthorized) ({ onDelete(part) }) else null,
            )
        }

        SourceLinks(
            parts = parts,
            visible = visibility.translation,
            translationSizeMult = translationSizeMult,
            onOpenSource = onOpenSource,
        )
    }
}

/**
 * Mənbə sətirləri — eyni mətn **bir dəfə**, özü də «Qaynağa bax» keçidi. Mənbə sətri boşdursa
 * keçid «Qaynağa bax» yazısı ilə qalır, yoxsa qaynağı açmağın yolu itərdi.
 *
 * Tərcümə ilə birlikdə gizlənir — hədisdəki `showSource` qaydası: «Ərəbcə» rejimində ekranda yalnız
 * ərəbcə qalmalıdır. Keçid isə o rejimdə də lazımdır, ona görə orada yalnız «Qaynağa bax» qalır.
 */
@Composable
private fun SourceLinks(
    parts: List<Dua>,
    visible: Boolean,
    translationSizeMult: Float,
    onOpenSource: (Dua) -> Unit,
) {
    val fallback = stringResource(Res.string.duaOpenSource)

    // Mətn → həmin mətnli ilk hissə (keçid onun qaynağını açır).
    val links = parts
        .groupBy { it.source?.trim().orEmpty() }
        .map { (source, rows) -> (source.takeIf { visible && it.isNotEmpty() } ?: fallback) to rows.first() }
        .distinctBy { it.first }

    links.forEach { (text, part) ->
        Row(
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp, top = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onOpenSource(part) }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = if (text == fallback) text else "— $text",
                // Qeydlə **eyni** qayda: tərcümədən 3sp kiçik və onun çarpanı ilə böyüyüb-kiçilir.
                style = typography.labelMedium.copy(
                    fontSize = (typography.bodyLarge.fontSize.value - TRANSLATION_SUBTEXT_DROP_SP).sp *
                        translationSizeMult,
                ).withLineHeightRatio(TRANSLATION_LINE_HEIGHT_RATIO)
                    .withScriptDirection(arabic = false),
                // Keçid rəngi: əvvəl ayrıca böyük düymə idi, indi mətnin özü basılır.
                color = colorScheme.primary.alpha(0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f, fill = false),
            )

            Icon(
                painter = painterResource(Res.drawable.dr_icon_open),
                contentDescription = null,
                tint = colorScheme.primary.alpha(0.85f),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/** Xəttin bir addımı: işarə (nöqtə və ya halqa) + birləşdirici xətt, tam mətn və ⋮ menyusu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DhikrStep(
    part: Dua,
    count: Int,
    /** Halqanın hədəfi; `null` → işarə kiçik nöqtədir. */
    ringTarget: Int?,
    isActive: Boolean,
    isLast: Boolean,
    visibility: DuaBlockVisibility,
    arabicSizeMult: Float,
    translationSizeMult: Float,
    query: String,
    /** `null` → hissənin sayı yoxdur; toxunuş heç nə etmir, sətir basılan kimi görünmür. */
    onTap: (() -> Unit)?,
    onLongPress: () -> Unit,
    onOpenSource: (() -> Unit)?,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    val done = ringTarget != null && count >= ringTarget

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Xətt addımın **bütün** hündürlüyü boyunca uzanmalıdır, mətn neçə sətir olsa da.
            .height(IntrinsicSize.Min)
            // Növbəti deyiləcək hissə yüngül fonla seçilir — göz sayı axtarmasın.
            .then(
                if (isActive) {
                    Modifier.background(
                        color = colorScheme.primaryContainer.alpha(0.18f),
                        shape = RoundedCornerShape(14.dp),
                    )
                } else {
                    Modifier
                },
            )
            // Saysız hissədə basılma effekti olmasın: toxunuş heç nə etmir, uzun basmanı isə
            // səhifənin özü tutur.
            .then(
                if (onTap != null) {
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .combinedClickable(onClick = onTap, onLongClick = onLongPress)
                } else {
                    Modifier
                },
            )
            // Hər iki kənarda cəmi **2dp**: xəttin işarəsi ekranın sol kənarından, mətn sağ kənara
            // qədər — mətnə maksimum en qalsın (əvvəl 48dp-lik zolaq və 20dp səhifə boşluğu ekranı
            // daraldırdı; istifadəçi istəyi, 2026-10-01).
            .padding(start = 2.dp, end = 2.dp, top = 6.dp),
    ) {
        // Xəttin işarəsi **sol kənarda**, ekranla arası 2dp. Nöqtə üçün zolaq ensizdir (14dp),
        // halqa üçün isə halqanın özü qədər.
        Column(
            modifier = Modifier.width(if (ringTarget != null) 42.dp else 14.dp).fillMaxHeight(),
            horizontalAlignment = if (ringTarget != null) Alignment.CenterHorizontally else Alignment.Start,
        ) {
            if (ringTarget != null) {
                CounterRing(count = count, target = ringTarget, isActive = isActive, done = done)
            } else {
                // Nöqtə ərəbcənin ilk sətrinin ortasına düşsün.
                Box(
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .size(10.dp)
                        .background(colorScheme.primary.alpha(0.7f), CircleShape),
                )
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        // Nöqtənin mərkəzinin altına düşsün (nöqtə kənara yapışıqdır: 10dp → 4dp).
                        .then(if (ringTarget != null) Modifier else Modifier.padding(start = 4.dp))
                        .width(2.dp)
                        .weight(1f)
                        .background(
                            color = if (done) colorScheme.primary.alpha(0.6f)
                            else colorScheme.outlineVariant.alpha(0.6f),
                            shape = RoundedCornerShape(1.dp),
                        ),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, bottom = if (isLast) 8.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Mətnlər **tam** göstərilir — `maxLines` yoxdur: zikr qısaldılanda oxuyan adam onu
            // necə deyəcəyini bilmir.
            part.text_ar.takeIf { it.isNotBlank() && visibility.arabic }?.let { arabic ->
                Text(
                    text = arabic.withSearchHighlight(query),
                    style = typography.headlineSmall.copy(
                        fontSize = 22.sp * arabicSizeMult,
                        lineHeight = (22.sp * arabicSizeMult) * 1.85f,
                    ).withScriptDirection(
                        arabic = true,
                        arabicFontFamily = arabicFontFamily(),
                    ),
                    color = colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            part.transliteration?.takeIf { it.isNotBlank() && visibility.transliteration }
                ?.let { translit ->
                    Text(
                        text = translit.withSearchHighlight(query),
                        style = typography.bodyMedium.copy(
                            fontSize = typography.bodyMedium.fontSize * translationSizeMult,
                            fontStyle = FontStyle.Italic,
                        ).withLineHeightRatio(TRANSLATION_LINE_HEIGHT_RATIO)
                            .withScriptDirection(arabic = false),
                        // Oxunuş **vurğu rəngindədir**, tərcümə isə adi mətn rəngində: ikisi də
                        // latın hərflidir və eyni boz tonda bir-birinin davamı kimi oxunurdu.
                        color = colorScheme.primary.alpha(0.9f),
                    )
                }

            val showTranslation = part.text_az.isNotBlank() && visibility.translation
            val showTranslit = !part.transliteration.isNullOrBlank() && visibility.transliteration

            // Oxunuşla tərcümə arasında qısa xətt — iki blokun sərhədi rəngdən əlavə yerlə də
            // görünsün (yalnız ikisi də ekrandadırsa, yoxsa tək xətt boşluqda qalardı).
            if (showTranslit && showTranslation) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .width(32.dp)
                        .height(1.dp)
                        .background(colorScheme.outlineVariant),
                )
            }

            part.text_az.takeIf { showTranslation }?.let { translation ->
                Text(
                    text = translation.withSearchHighlight(query),
                    style = typography.bodyLarge.copy(
                        fontSize = typography.bodyLarge.fontSize * translationSizeMult,
                    ).withLineHeightRatio(TRANSLATION_LINE_HEIGHT_RATIO)
                        .withScriptDirection(arabic = false),
                    color = colorScheme.onSurface.alpha(0.92f),
                )
            }

            // Qeyd tərcümənin davamıdır — tərcümə gizlədiləndə o da getməlidir.
            part.note?.takeIf { it.isNotBlank() && visibility.translation }?.let { note ->
                Text(
                    text = note,
                    style = typography.bodySmall.copy(
                        fontSize = (typography.bodyLarge.fontSize.value - TRANSLATION_SUBTEXT_DROP_SP).sp *
                            translationSizeMult,
                    ).withLineHeightRatio(TRANSLATION_LINE_HEIGHT_RATIO)
                        .withScriptDirection(arabic = false),
                    color = colorScheme.onSurfaceVariant.alpha(0.85f),
                )
            }
        }

        if (onOpenSource != null || onEdit != null || onDelete != null) {
            PartMenu(onOpenSource = onOpenSource, onEdit = onEdit, onDelete = onDelete)
        }

    }
}

/**
 * Hissənin halqası: başlamamış hissədə hədəf («33»), sayılarkən «12/33» və dolan halqa, bitəndə ✓.
 */
@Composable
private fun CounterRing(count: Int, target: Int, isActive: Boolean, done: Boolean) {
    Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { (count.toFloat() / target).coerceIn(0f, 1f) },
            modifier = Modifier.size(42.dp),
            color = colorScheme.primary,
            trackColor = if (isActive) colorScheme.primary.alpha(0.25f) else colorScheme.outlineVariant.alpha(0.6f),
            strokeWidth = 3.dp,
        )

        when {
            done -> Icon(
                painter = painterResource(Res.drawable.dr_icon_check),
                contentDescription = stringResource(Res.string.duaCounterDone),
                tint = colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )

            count == 0 -> Text(
                text = target.toString(),
                style = typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (isActive) colorScheme.primary else colorScheme.onSurfaceVariant,
            )

            else -> Text(
                text = stringResource(Res.string.duaCounterProgress, count, target),
                style = typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                color = colorScheme.primary,
            )
        }
    }
}

/** Hissəyə aid əməliyyatlar — qaynaq (fərqli mənbədə), redaktə və silmə (admin). */
@Composable
private fun PartMenu(
    onOpenSource: (() -> Unit)?,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    var open by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_menu),
                contentDescription = stringResource(Res.string.duaPartActions),
                tint = colorScheme.onSurfaceVariant.alpha(0.6f),
                modifier = Modifier.size(18.dp),
            )
        }

        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            onOpenSource?.let { action ->
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.duaOpenSource)) },
                    onClick = {
                        open = false
                        action()
                    },
                )
            }
            onEdit?.let { action ->
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.strLabelEdit)) },
                    onClick = {
                        open = false
                        action()
                    },
                )
            }
            onDelete?.let { action ->
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.strLabelDelete), color = colorScheme.error) },
                    onClick = {
                        open = false
                        action()
                    },
                )
            }
        }
    }
}
