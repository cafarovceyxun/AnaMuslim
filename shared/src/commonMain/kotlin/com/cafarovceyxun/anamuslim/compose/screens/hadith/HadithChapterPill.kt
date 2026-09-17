package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cafarovceyxun.anamuslim.compose.components.common.FloatingTitlePill

/**
 * Axan siyahı rejimlərində (ərəbcə / tərcümə) ekranın üstündə üzən cari bab adı kutusu.
 *
 * Uzun siyahıda aşağı sürüşəndə app bar gizlənir və başlığı aparır — bu kutu həmin başlığı əvəz
 * edir ki, hansı babda olduğun görünsün. Görünmə app bar-ın çökməsindən yox, birbaşa siyahının
 * sürüşməsindən asılıdır ([visible]) — ona görə tam ekran rejimində də (app bar ümumiyyətlə yoxdur)
 * düzgün işləyir.
 *
 * Görkəm və davranış [FloatingTitlePill]-dədir: eyni kutu dua oxucusunda da işlənir, orada isə
 * **həmişə** görünür (duada app bar-da başlıq yoxdur). Ona görə görünmə şərti burada, çağıranda
 * qalır — komponentin özündə yox.
 *
 * @param title cari babın adı ([currentTitle]); boşdursa çəkilmir.
 * @param visible siyahı yuxarıdan sürüşübmü — `true` olanda kutu fade ilə görünür, `false`-da itir.
 */
@Composable
fun HadithChapterPill(
    title: String,
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingTitlePill(
        title = title,
        visible = visible,
        onClick = onClick,
        modifier = modifier,
    )
}
