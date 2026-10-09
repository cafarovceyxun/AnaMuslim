package com.cafarovceyxun.anamuslim.compose.components.homepage

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.PressGestureScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Hekayə slaydının dolma sayğacı — həm günün məzmunu, həm də «Yeniliklər» hekayəsi bunu işlədir ki,
 * iki hekayə eyni cür dayanıb eyni cür davam etsin.
 *
 * İki `LaunchedEffect` qəsdəndir: sıfırlama **yalnız** slayd dəyişəndə olur, animasiya isə
 * dayandırma bayrağı ilə birlikdə yenidən qurulur. Bir effektdə birləşdirilsə barmaq qaldırılan
 * kimi zolaq başdan dolmağa başlayardı.
 *
 * [running] `false` olanda animasiya **dayandırılır** (sıfırlanmır), sonra qalan müddət qədər davam
 * edir: barmaq ekranda, ortadan toxunuşla pauza, açıq vərəq və ya video slaydı — hamısı bu bayraqdan
 * keçir.
 */
@Composable
internal fun LaunchedStoryProgress(
    key: Any,
    running: Boolean,
    durationMillis: Int,
    progress: Animatable<Float, AnimationVector1D>,
    onFinished: () -> Unit,
) {
    LaunchedEffect(key) {
        progress.snapTo(0f)
    }

    LaunchedEffect(key, running) {
        if (!running) {
            progress.stop()
            return@LaunchedEffect
        }

        val remaining = ((1f - progress.value) * durationMillis).toInt()
        progress.animateTo(1f, tween(remaining.coerceAtLeast(1), easing = LinearEasing))
        onFinished()
    }
}

/** Hekayəni bağlayan aşağı sürüşdürmənin həddi (piksel) — hər iki hekayədə eyni. */
internal const val STORY_DISMISS_DRAG_PX = 160f

/** Video slaydında kənarı basılı saxlayanda sarınma sürəti: sağ → `+2`, sol → `-2` (geri). */
internal const val STORY_SEEK_SPEED = 2f

/**
 * Hekayədə barmağı basılı saxlamaq. **Video** slaydında kənar üçdə birlərdə uzun basış sarınmadır —
 * sağ: 2× irəli, sol: 2× geri ([onSpeed] `+2`/`-2`, buraxanda `1`); qalan hallarda (orta hissə,
 * şəkil və mətn slaydı) əvvəlki kimi pauza.
 *
 * Sarınma dərhal yox, [longPressMillis]-dən sonra başlayır: kənara qısa toxunuş əvvəlki/növbəti
 * slayddır və bu müddətdə video sürüşsəydi keçiddən əvvəl bir anlıq «sıçrayış» görünərdi. Kənarda
 * barmaq ekranda olanda video **dayanmır** — sarınmanın mənası onun hərəkət etməsidir.
 */
internal suspend fun PressGestureScope.holdStory(
    offset: Offset,
    width: Int,
    isVideo: Boolean,
    longPressMillis: Long,
    onHoldPause: (Boolean) -> Unit,
    onSpeed: (Float) -> Unit,
) {
    val press = this
    val seekSpeed = when {
        !isVideo -> null
        offset.x > width * 2 / 3f -> STORY_SEEK_SPEED
        offset.x < width / 3f -> -STORY_SEEK_SPEED
        else -> null
    }

    if (seekSpeed != null) {
        coroutineScope {
            val seeking = launch {
                delay(longPressMillis)
                onSpeed(seekSpeed)
            }
            press.tryAwaitRelease()
            seeking.cancel()
            onSpeed(1f)
        }
    } else {
        onHoldPause(true)
        tryAwaitRelease()
        onHoldPause(false)
    }
}

/** «▶▶ 2×» / «◀◀ 2×» nişanı — sarınma gedərkən, zolaqların altında görünür. */
@Composable
internal fun SeekBadge(speed: Float, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (speed < 0f) Icons.Rounded.FastRewind else Icons.Rounded.FastForward,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "${abs(speed).toInt()}×",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
