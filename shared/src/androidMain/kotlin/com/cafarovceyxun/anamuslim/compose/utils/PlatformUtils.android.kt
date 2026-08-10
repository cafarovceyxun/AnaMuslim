package com.cafarovceyxun.anamuslim.compose.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import com.cafarovceyxun.anamuslim.utils.AndroidPlatformContext
import com.cafarovceyxun.anamuslim.utils.AppLogger
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference

actual object PlatformUtils {
    actual fun copyToClipboard(text: String) {
        val clipboard = AndroidPlatformContext.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("anamuslim", text)
        clipboard.setPrimaryClip(clip)
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

    private var toast: WeakReference<Toast>? = null

    actual fun showToast(text: String) = show(text, Toast.LENGTH_SHORT)

    actual fun showLongToast(text: String) = show(text, Toast.LENGTH_LONG)

    /** Replaces any toast still on screen, as the app's `MessageUtils.showRemovableToast` did. */
    private fun show(text: String, duration: Int) {
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
