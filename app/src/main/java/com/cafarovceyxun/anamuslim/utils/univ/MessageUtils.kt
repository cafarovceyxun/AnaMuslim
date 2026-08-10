package com.cafarovceyxun.anamuslim.utils.univ

import android.content.Context
import android.os.Build
import android.widget.Toast
import com.cafarovceyxun.anamuslim.R
import java.lang.ref.WeakReference

/**
 * Toast helpers only. The dialog helpers that used to live here (`popMessage`,
 * `showConfirmationDialog`, `popNoInternetMessage`) were replaced by the shared Compose dialogs
 * ([com.cafarovceyxun.anamuslim.compose.components.dialogs.MessageDialog] and
 * `TranslationConfirmDialog`) and had no callers left.
 */
object MessageUtils {
    private var toast: WeakReference<Toast>? = null
    fun showRemovableToast(context: Context, msgRes: Int, duration: Int) {
        showRemovableToast(context, context.getString(msgRes), duration)
    }

    fun showRemovableToast(context: Context?, msg: CharSequence?, duration: Int) {
        try {
            toast?.get()?.cancel()
        } catch (ignored: Exception) {
        }
        toast = WeakReference(Toast.makeText(context, msg, duration))
        toast!!.get()!!.show()
    }

    fun popNoInternetToast(ctx: Context) {
        showRemovableToast(ctx, R.string.strMsgNoInternetLong, Toast.LENGTH_LONG)
    }

    fun showClipboardMessage(context: Context, text: String) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            showRemovableToast(context = context, msg = text, duration = Toast.LENGTH_SHORT)
        }
    }
}
