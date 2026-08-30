package com.cafarovceyxun.anamuslim.utils.app

import androidx.compose.runtime.Composable

/** Qalereyadan seçilmiş şəkil — baytlar və MIME tipi (Storage-a yükləmək üçün ikisi də lazımdır). */
data class PickedImage(
    val bytes: ByteArray,
    val mimeType: String,
) {
    // data class ByteArray ilə referans müqayisəsi edir — siyahıda/state-də səhv «dəyişmədi»
    // nəticəsi çıxmasın deyə əl ilə yazılır.
    override fun equals(other: Any?): Boolean =
        this === other || (other is PickedImage && mimeType == other.mimeType && bytes.contentEquals(other.bytes))

    override fun hashCode(): Int = 31 * bytes.contentHashCode() + mimeType.hashCode()
}

/**
 * Sistem şəkil seçicisi. Qaytarılan lambda seçicini açır; **`null`** o deməkdir ki, bu platformada
 * seçici yoxdur — çağıran tərəf onda düyməni ümumiyyətlə göstərməməlidir.
 *
 * `?: error(...)` yerinə null qaytarılır, çünki bu, hər platformada olmaya bilən imkandır
 * (bax CLAUDE.md, «Provider/DI seam qaydası»): düymə basılıb heç nə etməkdənsə, görünməsin.
 */
@Composable
expect fun rememberImagePicker(onPicked: (PickedImage) -> Unit): (() -> Unit)?
