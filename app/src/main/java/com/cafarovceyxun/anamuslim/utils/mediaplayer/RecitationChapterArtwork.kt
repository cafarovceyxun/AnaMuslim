package com.cafarovceyxun.anamuslim.utils.mediaplayer

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.core.graphics.ColorUtils
import com.cafarovceyxun.anamuslim.R
import com.cafarovceyxun.anamuslim.utils.quran.QuranGlyphs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

object RecitationChapterArtwork {
    const val ARTWORK_VERSION = 2

    suspend fun getChapterArtworkUri(context: Context, chapterNo: Int): Uri {
        val appContext = context.applicationContext

        return withContext(Dispatchers.IO) {
            try {
                val file =
                    File(appContext.cacheDir, "artwork_surah_v${ARTWORK_VERSION}_$chapterNo.png")

                val uri = FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.provider",
                    file,
                )

                grantGearheadAutoRead(appContext, uri)

                if (file.exists()) {
                    return@withContext uri
                }

                val size = 600
                val bitmap = createBitmap(size, size)
                val canvas = Canvas(bitmap)

                decodeSampledWallpaper(appContext, size)?.let { wallpaper ->
                    canvas.drawBitmap(
                        wallpaper,
                        null,
                        Rect(0, 0, size, size),
                        Paint(Paint.FILTER_BITMAP_FLAG),
                    )
                    wallpaper.recycle()
                }

                if (chapterNo > 0) {
                    val typeface = ResourcesCompat.getFont(appContext, R.font.suracon)

                    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        this.typeface = typeface
                        // 191 = 0.75 × 255, matching the removed peacedesign `createAlphaColor`.
                        this.color = ColorUtils.setAlphaComponent(Color.WHITE, 191)
                        textAlign = Paint.Align.CENTER
                    }

                    val chapterText = QuranGlyphs.Chapter.get(chapterNo)

                    val padding = size * 0.15f
                    val maxTextWidth = size - padding * 2

                    var textSize = size * 0.5f
                    paint.textSize = textSize
                    val textWidth = paint.measureText(chapterText)

                    if (textWidth > maxTextWidth) {
                        val scale = maxTextWidth / textWidth
                        textSize *= scale
                        paint.textSize = textSize
                    }

                    val textY = (size / 2f) - ((paint.descent() + paint.ascent()) / 2f)
                    canvas.drawText(chapterText, size / 2f, textY, paint)
                }

                ByteArrayOutputStream().use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    try {
                        file.writeBytes(outputStream.toByteArray())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                bitmap.recycle()

                uri
            } catch (_: Exception) {
                androidFallbackWallpaperUri(appContext)
            }
        }
    }

    private fun grantGearheadAutoRead(context: Context, uri: Uri) {
        try {
            context.grantUriPermission(
                "com.google.android.projection.gearhead",
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            context.grantUriPermission(
                "com.google.android.autosimulator",
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: Exception) {
        }
    }

    suspend fun getChapterArtworkBitmap(
        context: Context,
        chapterNo: Int,
        maxSidePx: Int,
    ): Bitmap {
        val app = context.applicationContext
        getChapterArtworkUri(app, chapterNo)

        return withContext(Dispatchers.IO) {
            val file = File(app.cacheDir, "artwork_surah_v${ARTWORK_VERSION}_$chapterNo.png")

            val raw = try {
                if (file.exists() && file.length() > 0L) {
                    BitmapFactory.decodeFile(file.absolutePath)
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            } ?: try {
                decodeSampledWallpaper(app, max(1, maxSidePx))
            } catch (_: Exception) {
                null
            } ?: createBitmap(1, 1)

            val w = raw.width
            val h = raw.height

            if (w <= 0 || h <= 0) return@withContext raw

            val cap = max(1, maxSidePx)
            if (w <= cap && h <= cap) return@withContext raw

            val scale = minOf(cap.toFloat() / w, cap.toFloat() / h)
            val nw = max(1, (w * scale).roundToInt())
            val nh = max(1, (h * scale).roundToInt())
            val scaled = raw.scale(nw, nh)

            if (scaled != raw) raw.recycle()

            return@withContext scaled
        }
    }

    /**
     * Decodes `quran_wallpaper` down to [maxSidePx], never up.
     *
     * The source art is 1672x941 and lives in `drawable-nodpi` for the same reason this helper
     * exists: a density-qualified decode treats it as mdpi and up-scales it to the screen bucket,
     * so a plain `decodeResource` on an xxhdpi phone allocates ~56 MB of ARGB_8888 for what is
     * drawn into a 600x600 square. `inScaled = false` keeps that scaling off even if the file ever
     * moves back to a qualified folder, and `inSampleSize` does the rest at decode time.
     */
    private fun decodeSampledWallpaper(context: Context, maxSidePx: Int): Bitmap? {
        val res = context.resources

        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            inScaled = false
        }
        BitmapFactory.decodeResource(res, R.drawable.quran_wallpaper, bounds)

        val srcW = bounds.outWidth
        val srcH = bounds.outHeight
        if (srcW <= 0 || srcH <= 0) return null

        val cap = max(1, maxSidePx)
        var sample = 1
        while (srcW / (sample * 2) >= cap && srcH / (sample * 2) >= cap) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply {
            inScaled = false
            inSampleSize = sample
        }

        return BitmapFactory.decodeResource(res, R.drawable.quran_wallpaper, options)
    }

    fun androidFallbackWallpaperUri(context: Context): Uri {
        val resId = R.drawable.quran_wallpaper
        return (
                ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                        context.resources.getResourcePackageName(resId) +
                        '/' +
                        context.resources.getResourceTypeName(resId) +
                        '/' +
                        context.resources.getResourceEntryName(resId)
                ).toUri()
    }
}
