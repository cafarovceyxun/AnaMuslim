package com.cafarovceyxun.anamuslim.utils.app

import androidx.compose.runtime.Composable

/** Qalereyadan seçilmiş fayl — baytlar, MIME tipi və (video üçün) uzunluq. */
data class PickedMedia(
    val bytes: ByteArray,
    val mimeType: String,
    val isVideo: Boolean,
) {
    // data class ByteArray-i referansla müqayisə edir — state-də səhv «dəyişmədi» nəticəsi
    // çıxmasın deyə əl ilə yazılır.
    override fun equals(other: Any?): Boolean =
        this === other || (
            other is PickedMedia &&
                isVideo == other.isVideo &&
                mimeType == other.mimeType &&
                bytes.contentEquals(other.bytes)
            )

    override fun hashCode(): Int =
        31 * (31 * bytes.contentHashCode() + mimeType.hashCode()) + isVideo.hashCode()
}

/** Seçicinin nəticəsi. İmtina ediləndə callback ümumiyyətlə çağırılmır. */
sealed interface MediaPickResult {
    data class Picked(val media: PickedMedia) : MediaPickResult

    /** Video [MediaPickLimits.MAX_VIDEO_MILLIS]-dən uzundur. */
    data object TooLong : MediaPickResult

    /** Seçilmiş fayl [MediaPickLimits.MAX_BYTES]-dan böyükdür. */
    data object TooLarge : MediaPickResult

    /** Video sıxışdırıldıqdan sonra da [MediaPickLimits.MAX_UPLOAD_BYTES]-dan böyükdür. */
    data object StillTooLarge : MediaPickResult

    data object Failed : MediaPickResult
}

object MediaPickLimits {
    /** İki dəqiqə — hekayə formatı üçün onsuz da yuxarı hədddir. */
    const val MAX_VIDEO_MILLIS = 120_000L

    /** Seçilə bilən mənbə faylın yuxarı həddi (sıxışdırmadan ƏVVƏL). */
    const val MAX_BYTES = 50L * 1024 * 1024

    /**
     * ⚠️ **Video yüklənməzdən əvvəl MÜTLƏQ sıxışdırılır** (`MediaPicker` actual-larında), çünki
     * hekayə videosu hər baxışda **tam** endirilir: 2026-09-18-də üç xam ekran yazısı (27.9, 21.2,
     * 19.6 MB) gündə **1.65 GB** egress yaradıb və Supabase-in pulsuz 5 GB-lıq «Cached Egress»
     * kvotası bir neçə günə dolub (160%). Nə kompilyator, nə test, nə də tətbiqin özü bunu göstərir
     * — yalnız hesabatda görünür.
     *
     * Ona görə üç qapı var: hədəf ölçü ([TARGET_VIDEO_BYTES], bitrate bundan hesablanır), sərt
     * klient həddi (bu sabit) və bucket-in öz `file_size_limit`-i (15 MB, server tərəf).
     */
    const val MAX_UPLOAD_BYTES = 12L * 1024 * 1024

    /** Sıxışdırmanın hədəfi — bitrate videonun uzunluğuna görə buradan hesablanır. */
    const val TARGET_VIDEO_BYTES = 8L * 1024 * 1024

    /** Uzun kənar. Hekayə portretdir, yəni nəticə ~405×720 olur. */
    const val TARGET_VIDEO_HEIGHT = 720
}

/**
 * Sistem media seçicisi (şəkil + video). Qaytarılan lambda seçicini açır; **`null`** o deməkdir ki,
 * bu platformada seçici yoxdur — çağıran tərəf onda düyməni ümumiyyətlə göstərməməlidir.
 *
 * `?: error(...)` yerinə null qaytarılır, çünki bu, hər platformada olmaya bilən imkandır
 * (bax CLAUDE.md, «Provider/DI seam qaydası»): düymə basılıb heç nə etməkdənsə, görünməsin.
 */
@Composable
expect fun rememberMediaPicker(onResult: (MediaPickResult) -> Unit): (() -> Unit)?
