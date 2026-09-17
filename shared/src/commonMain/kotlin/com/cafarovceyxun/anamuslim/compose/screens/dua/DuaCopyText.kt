package com.cafarovceyxun.anamuslim.compose.screens.dua

import com.cafarovceyxun.anamuslim.utils.supabase.DuaSourceRef

/**
 * Paylaşılacaq/kopyalanacaq mətnə hansı blokların düşməsi.
 *
 * Ayrı tipdir, çünki eyni seçim həm vərəqdəki çekbokslardan, həm də uzun basma ilə edilən sürətli
 * kopyalamadan gəlir — ikincisi [visible] ilə qurulur.
 */
internal data class DuaShareParts(
    val arabic: Boolean = true,
    val transliteration: Boolean = true,
    val translation: Boolean = true,
    val note: Boolean = true,
    val source: Boolean = true,
) {
    val isEmpty: Boolean get() = !arabic && !transliteration && !translation && !note && !source

    companion object {
        /**
         * Ekranda **görünən** bloklara uyğun seçim.
         *
         * Uzun basıb kopyalayan adam ekranda gördüyünü gözləyir: «Ərəbcə» rejimində tərcüməni də
         * kopyalamaq «mən bunu istəməmişdim» hissi yaradır (hədisdəki `rememberHadithCopyAction`
         * ilə eyni qayda).
         */
        fun visible(visibility: DuaBlockVisibility): DuaShareParts = DuaShareParts(
            arabic = visibility.arabic,
            transliteration = visibility.transliteration,
            translation = visibility.translation,
            // Qeyd və mənbə tərcümənin davamıdır — bax `DuaPage`-dəki eyni qayda.
            note = visibility.translation,
            source = visibility.translation,
        )
    }
}

/**
 * Duanı/dəlili paylaşıla bilən mətnə çevirir.
 *
 * Bloklar **boş sətirlə** ayrılır, mənbə isə tire ilə başlayır — ekrandakı düzülüşün mətn qarşılığı.
 * Seçilmiş, amma boş olan blok buraxılır: əks halda paylaşılan mətndə izahsız boş sətirlər qalırdı.
 */
internal fun buildDuaShareText(
    ref: DuaSourceRef,
    parts: DuaShareParts = DuaShareParts(),
): String = buildList {
    if (parts.arabic) ref.text_ar.takeIf { it.isNotBlank() }?.let(::add)
    if (parts.transliteration) ref.transliteration?.takeIf { it.isNotBlank() }?.let(::add)
    if (parts.translation) ref.text_az.takeIf { it.isNotBlank() }?.let(::add)
    if (parts.note) ref.note?.takeIf { it.isNotBlank() }?.let(::add)
    if (parts.source) ref.source?.takeIf { it.isNotBlank() }?.let { add("— $it") }
}.joinToString("\n\n")
