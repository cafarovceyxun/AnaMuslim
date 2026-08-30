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

    /** Fayl [MediaPickLimits.MAX_BYTES]-dan böyükdür. */
    data object TooLarge : MediaPickResult

    data object Failed : MediaPickResult
}

object MediaPickLimits {
    /** İki dəqiqə — hekayə formatı üçün onsuz da yuxarı hədddir. */
    const val MAX_VIDEO_MILLIS = 120_000L

    /** Bucket-in öz limiti ilə eynidir (50 MB); ekran yazısı bu ölçüyə rahat sığır. */
    const val MAX_BYTES = 50L * 1024 * 1024
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
