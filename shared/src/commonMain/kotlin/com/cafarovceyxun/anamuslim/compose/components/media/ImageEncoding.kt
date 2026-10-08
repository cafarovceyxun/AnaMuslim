package com.cafarovceyxun.anamuslim.compose.components.media

import androidx.compose.ui.graphics.ImageBitmap

/** Redaktorun çəkdiyi kadrı JPEG-ə yazır ([quality] 0..100); alınmasa `null`. */
internal expect fun ImageBitmap.encodeJpeg(quality: Int): ByteArray?
