package com.cafarovceyxun.anamuslim.utils.univ

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * Seçilmiş faylın tətbiqin keş qovluğundakı **yerli nüsxəsi**. [path] həmişə birbaşa oxuna bilən
 * adi fayl yoludur (Android `content://`, iOS security-scoped URL deyil) — çağıran işi bitirəndə
 * onu özü silməlidir.
 */
data class PickedLargeFile(val path: String, val name: String, val size: Long)

/**
 * Böyük faylı (məs. ~130 MB APK) seçir və keşə **axınla** kopyalayır — heç vaxt `ByteArray` kimi
 * yaddaşa oxumur (bax [TextDocumentOpener]: o, kiçik mətn sənədləri üçündür).
 */
@Stable
interface LargeFilePicker {
    /** Seçicini açır. Nəticə yalnız [rememberLargeFilePicker]-ə verilən callback ilə gəlir. */
    fun pick()
}

/**
 * [onPicked] kopyalama bitəndə çağırılır; `null` = ləğv və ya fayl oxunmadı. Kopyalama bir neçə
 * saniyə çəkir və kompozisiyanın ömründən asılı olmamalıdır (fırlanma/bərpa scope-u ləğv edər).
 */
@Composable
expect fun rememberLargeFilePicker(onPicked: (PickedLargeFile?) -> Unit): LargeFilePicker
