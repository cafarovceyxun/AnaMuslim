package com.cafarovceyxun.anamuslim.compose.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.media.MediaScannerConnection
import android.os.Environment
import android.provider.MediaStore
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import com.cafarovceyxun.anamuslim.utils.AndroidPlatformContext
import com.cafarovceyxun.anamuslim.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference

actual object PlatformUtils {
    actual fun copyToClipboard(text: String) {
        val clipboard = AndroidPlatformContext.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("anamuslim", text)
        clipboard.setPrimaryClip(clip)
    }

    actual fun readFromClipboard(): String? {
        return try {
            val context = AndroidPlatformContext.context
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            // `coerceToText` rather than `text`: KDE Connect and several keyboards put the payload in
            // as a styled/HTML item, whose `text` is null while `coerceToText` still yields the plain
            // string.
            clipboard.primaryClip
                ?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)
                ?.coerceToText(context)
                ?.toString()
                ?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            AppLogger.saveError(e, "PlatformUtils.readFromClipboard")
            null
        }
    }

    actual fun browseLink(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            AndroidPlatformContext.context.startActivity(intent)
        } catch (ignored: Exception) {
        }
    }

    actual fun shareText(text: String, chooserTitle: String?) {
        try {
            val send = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, text)
                setTypeAndNormalize("text/plain")
            }
            val chooser = Intent.createChooser(send, chooserTitle)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            AndroidPlatformContext.context.startActivity(chooser)
        } catch (ignored: Exception) {
        }
    }

    actual fun shareImage(image: ImageBitmap, chooserTitle: String?): Boolean {
        return try {
            val context = AndroidPlatformContext.context
            val cacheDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
            val file = File(cacheDir, "share_${System.currentTimeMillis()}.png")

            FileOutputStream(file).use { stream ->
                image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

            val send = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_STREAM, uri)
                setTypeAndNormalize("image/png")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(send, chooserTitle)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            AppLogger.saveError(e, "PlatformUtils.shareImage")
            false
        }
    }

    /**
     * Android Q-dan (API 29) etibarən `MediaStore` **icazəsiz** yazmağa imkan verir və şəkil
     * birbaşa qalereyada görünür. Ondan aşağıda ictimai qovluğa yazmaq `WRITE_EXTERNAL_STORAGE`
     * tələb edərdi — yeni icazə əlavə etməmək üçün tətbiqin öz xarici `Pictures` qovluğuna yazılır
     * və `MediaScanner`-ə bildirilir; orada da qalereyada görünür, sadəcə tətbiq silinəndə gedir.
     */
    actual suspend fun saveImageToGallery(image: ImageBitmap, fileName: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val context = AndroidPlatformContext.context
                val bitmap = image.asAndroidBitmap()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.png")
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            "${Environment.DIRECTORY_PICTURES}/AnaMuslim",
                        )
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }

                    val resolver = context.contentResolver
                    val uri = resolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values,
                    ) ?: return@runCatching false

                    resolver.openOutputStream(uri)?.use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    } ?: return@runCatching false

                    // `IS_PENDING` sıfırlanmasa fayl qalereyada görünmür — yarımçıq sayılır.
                    resolver.update(
                        uri,
                        ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                        null,
                        null,
                    )
                    true
                } else {
                    val dir = File(
                        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                        "AnaMuslim",
                    ).apply { mkdirs() }
                    val file = File(dir, "$fileName.png")

                    FileOutputStream(file).use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    }

                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(file.absolutePath),
                        arrayOf("image/png"),
                        null,
                    )
                    true
                }
            }.onFailure {
                AppLogger.saveError(it, "PlatformUtils.saveImageToGallery")
            }.getOrDefault(false)
        }

    private var toast: WeakReference<Toast>? = null

    actual fun showToast(text: String) = show(text, Toast.LENGTH_SHORT)

    actual fun showLongToast(text: String) = show(text, Toast.LENGTH_LONG)

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Replaces any toast still on screen, as the app's `MessageUtils.showRemovableToast` did.
     *
     * Hər zaman əsas mötəbərdə göstərilir: `Toast` Looper tələb edir, paylaşılan çağırışların çoxu isə
     * view model-lərdən `Dispatchers.IO`-da gəlir (yadda saxlama/silmə nəticələri) və orada
     * `Can't toast on a thread that has not called Looper.prepare()` ilə **çökürdü**. iOS tərəfi
     * ([IosToast]) onsuz da `dispatch_get_main_queue`-ya keçirdi — bu, həmin asimmetriyanı bağlayır,
     * yəni `PlatformUtils.showToast` hər iki platformada istənilən mötəbərdən çağırıla bilər.
     */
    private fun show(text: String, duration: Int) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            showOnMain(text, duration)
        } else {
            mainHandler.post { showOnMain(text, duration) }
        }
    }

    private fun showOnMain(text: String, duration: Int) {
        try {
            toast?.get()?.cancel()
        } catch (ignored: Exception) {
        }
        val new = Toast.makeText(AndroidPlatformContext.context, text, duration)
        toast = WeakReference(new)
        new.show()
    }

    actual fun showClipboardMessage(text: String) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) showToast(text)
    }
}
