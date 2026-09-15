package com.cafarovceyxun.anamuslim.utils.supabase

import kotlinx.serialization.Serializable

/**
 * `lunar_announcement` sətri — adminin «ayı gördük» elanı.
 *
 * Platformanın Ümmül-Qüra təqvimi **hesablanmış** təqvimdir, ölkələr isə ayı gözlə görməyə görə
 * elan edir; ona görə eyni gün bəzi yerlərdə bir-iki gün fərqli sayılır. Bu cədvəl həmin fərqi
 * mərkəzləşdirir: admin ayın **1-inin miladi tarixini** və uzunluğunu ([length_days], 29 və ya 30)
 * yazır, hər telefon isə ondan gün fərqi çıxarıb öz qəməri tarixini uyğunlaşdırır
 * (bax [com.cafarovceyxun.anamuslim.utils.prayer.LunarCalendar]).
 *
 * [media] **qəsdən** [SuggestionMedia] modelini bölüşür: bazada da forma eynidir
 * (`[{"url","type"}]`) və hekayə slaydını çəkən kod da eynidir. Ayrı tip yaratmaq iki eyni sinif
 * demək olardı, CLAUDE.md-dəki dublikat tələsinə açıq.
 *
 * Sxem: `docs/supabase/SCHEMA.md`.
 */
@Serializable
data class LunarAnnouncement(
    val id: Long,
    val hijri_year: Int,
    val hijri_month: Int,
    /** Ayın **1-i** hansı miladi gündür (`yyyy-MM-dd`). */
    val start_date: String,
    /** 29 və ya 30 — paylaşılan təqvim cədvəli də bu qədər sətir olur. */
    val length_days: Int,
    /** Ayın göründüyü an (ISO). Hekayədə videonun altında yazılır; məcburi deyil. */
    val sighted_at: String? = null,
    val media: List<SuggestionMedia> = emptyList(),
    /** Hekayədə görünən ictimai admin qeydi (≤300). */
    val note: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
) {
    /**
     * Hekayə dairəsinə düşürmü — göstəriləcək bir şey olmalıdır: ya media, ya da qeyd.
     * Funksiya hekayələri ilə eyni qayda ([Suggestion.hasStory]).
     */
    val hasStory: Boolean get() = media.isNotEmpty() || !note.isNullOrBlank()
}

/** `lunar_announcement.length_days` — bazadakı CHECK ilə eyni dəst. */
object LunarMonthLength {
    const val SHORT = 29
    const val LONG = 30

    val ALL = listOf(SHORT, LONG)
}
