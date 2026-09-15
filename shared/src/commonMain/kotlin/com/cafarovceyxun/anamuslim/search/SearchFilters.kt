package com.cafarovceyxun.anamuslim.search

data class TranslationOption(
    val slug: String,
    val displayName: String,
)

data class SearchFilters(
    val selectedSlugs: Set<String>? = null,
    val searchQuran: Boolean = true,
    val searchHadith: Boolean = true,
    /**
     * Hədisin **mətni** axtarılsın.
     *
     * [searchHadith] mənbəni açır, bu ikisi isə onun içindəki əhatəni: hədis bazasında bir söz həm
     * mətndə, həm də onlarla bab adında keçir və başlıq uyğunluqları siyahının başını tutur. İkisi
     * də sönəndə mənbənin özü sönür ([searchHadith]) — yoxsa «Hədis» açıq görünər, nəticə isə boş
     * gələrdi.
     */
    val searchHadithText: Boolean = true,
    /**
     * Cild / kitab / bab / alt bab **başlıqları** axtarılsın.
     *
     * Defolt **sönülüdür**: başlıq uyğunluqları siyahının başını tuturdu (bir söz onlarla bab adında
     * keçir) və istifadəçi axtardığı hədisi tapmaq üçün ekranlarla sürüşməli olurdu. İstəyən çipdən
     * yandırır.
     */
    val searchHadithTitles: Boolean = false,
) {
    /**
     * Süzgəc nişanı (filtr ikonundakı nöqtə) üçün: heç nə **defoltdan** fərqlənmirmi.
     *
     * Sahələri bir-bir sadalamaq əvəzinə defolt nüsxə ilə müqayisə: yeni süzgəc əlavə olunanda bu
     * şərt özü düzgün qalır, sadalama isə səssizcə geridə qalardı.
     */
    val isEmpty: Boolean
        get() = selectedSlugs.isNullOrEmpty() && copy(selectedSlugs = null) == SearchFilters()
}
