package com.cafarovceyxun.anamuslim.utils.supabase

import kotlinx.serialization.Serializable

/** `hajj_evidence.kind` dəyərləri — bazadakı CHECK ilə eyni sətirlər (bax [DuaSourceType]). */
object HajjEvidenceKind {
    /** Hökmün dəlili — kart kimi göstərilir. */
    const val EVIDENCE = "evidence"

    /** Deyiləcək zikr/dua (Təlbiyə, Səfa və Mərvə zikri) — Dua ekranının bloku kimi göstərilir. */
    const val DHIKR = "dhikr"
}

/** Namaz bələdçisinin əlavə növü — `salah_evidence.kind` (Həcc cədvəlində yoxdur). */
object SalahEvidenceKind {
    /** Qadınlara aid hədis — addımın içində ayrıca rəngli qeyd kimi göstərilir. */
    const val WOMEN = "women"
}

/**
 * Bələdçinin bir dəlili və ya zikri — **Həcc** (`hajj_evidence`) və **Namaz** (`salah_evidence`)
 * bələdçiləri eyni formanı işlədir.
 *
 * Forma [AsmaEvidence]-in eynisidir (eyni sütunlar, eyni CHECK), ona görə qaynaq vərəqi
 * (`DuaSourcePeekContent`) və seçim ekranı onu dəyişmədən işlədir. Fərq hədəfdədir: ad nömrəsi yerinə
 * [topic] — tətbiqdəki `HajjTopic`/`SalahTopic` açarı. Mövzu sətir kimi saxlanır, enum kimi yox:
 * gələcək buraxılışın əlavə etdiyi mövzu köhnə tətbiqi çökdürməsin, sadəcə görünməsin.
 */
@Serializable
data class GuideEvidence(
    val id: Long? = null,
    val topic: String,
    val kind: String = HajjEvidenceKind.EVIDENCE,
    override val source_type: String,
    override val hadith_id: Long? = null,
    override val chapter_no: Int? = null,
    override val verse_no: Int? = null,
    override val verse_end: Int? = null,
    override val text_ar: String,
    override val text_az: String,
    override val transliteration: String? = null,
    override val note: String? = null,
    override val source: String? = null,
    val sort_no: Int = 0,
    val created_at: String? = null,
    val updated_at: String? = null,
) : DuaSourceRef {
    val isDhikr: Boolean get() = kind == HajjEvidenceKind.DHIKR

    /** Qadınlara aid qeyd (yalnız Namaz bələdçisində). */
    val isWomenNote: Boolean get() = kind == SalahEvidenceKind.WOMEN
}

/** Həcc kodu köhnə adla qalır — forma eynidir. */
typealias HajjEvidence = GuideEvidence
