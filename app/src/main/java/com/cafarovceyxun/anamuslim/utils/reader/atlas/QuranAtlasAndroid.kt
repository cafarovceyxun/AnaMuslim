package com.cafarovceyxun.anamuslim.utils.reader.atlas

import android.graphics.Bitmap

/** Raw Android texture for a glyph — used by the widget rasterizer (Android-only render path). */
fun QuranAtlasBundle.androidTextureForGlyph(glyph: AtlasGlyphJson): Bitmap? =
    (textureSource as? QuranAtlasTextureStore)?.androidBitmapForIndex(glyph.textureIndex)

fun QuranAtlasBundle.singleAndroidTexture(): Bitmap? {
    val store = textureSource as? QuranAtlasTextureStore ?: return null
    return store.onlyIndex?.let { store.androidBitmapForIndex(it) }
}
