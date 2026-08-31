package com.cafarovceyxun.anamuslim.compose.components.homepage

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

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
