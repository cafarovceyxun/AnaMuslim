package com.cafarovceyxun.anamuslim.utils.univ

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Qalereyadan bir şəkil seçdirir və onu [ImageBitmap] kimi qaytarır.
 *
 * [TextDocumentSaver]/[TextDocumentOpener] ilə eyni formadadır və eyni səbəbdən **kompozisiya
 * ömürlü**dir: Android nəticəni yalnız `ActivityResultLauncher` vasitəsilə verir, o isə
 * kompozisiya zamanı qeydiyyatdan keçməlidir. iOS-da qeydiyyat lazım deyil, amma seam ortaq
 * olduğu üçün forma saxlanılır.
 *
 * Şəkil kartın fonu kimi 1080px kətana çəkildiyi üçün hər iki platforma seçilmiş şəkli
 * yükləməzdən əvvəl **kiçildir** — tam ölçülü qalereya şəkli onlarla MB tutur və heç bir
 * fayda vermir.
 */
@Stable
interface ImagePicker {
    /**
     * Sistem seçicisini açır. Nəticə [rememberImagePicker]-ə verilən `onPicked` ilə gəlir —
     * heç vaxt sinxron qayıtmır. İstifadəçi imtina edəndə və ya şəkil oxunmayanda `null` gəlir.
     */
    fun pick()
}

/** Kətana çəkilən fon üçün kifayət edən yuxarı hədd — kart 1080px enlidir. */
internal const val PICKED_IMAGE_MAX_DIMENSION = 2160

@Composable
expect fun rememberImagePicker(onPicked: (ImageBitmap?) -> Unit): ImagePicker
