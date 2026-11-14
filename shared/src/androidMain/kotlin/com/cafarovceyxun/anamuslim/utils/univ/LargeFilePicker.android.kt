package com.cafarovceyxun.anamuslim.utils.univ

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.cafarovceyxun.anamuslim.utils.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Kopyalama bir neçə saniyə çəkir, seçicinin nəticəsi isə kompozisiya bərpa olunarkən gələ bilər —
 * `rememberCoroutineScope()` ləğv olunsa fayl yarımçıq qalardı. Ona görə proses ömürlü scope.
 */
private val copyScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

private const val CACHE_DIR = "apk-upload"
private const val FALLBACK_NAME = "upload.apk"

@Composable
actual fun rememberLargeFilePicker(onPicked: (PickedLargeFile?) -> Unit): LargeFilePicker {
    val context = LocalContext.current.applicationContext
    val currentOnPicked by rememberUpdatedState(onPicked)

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            currentOnPicked(null)
            return@rememberLauncherForActivityResult
        }

        copyScope.launch {
            val picked = copyToCache(context, uri)
            // Callback kompozisiyanın vəziyyətinə toxunur — Main-də çatdırılır.
            withContext(Dispatchers.Main) { currentOnPicked(picked) }
        }
    }

    return remember(launcher) {
        object : LargeFilePicker {
            // `*/*`: MIME süzgəci bəzi seçicilərdə faylı boz göstərir; `.apk` yoxlaması çağıranındır.
            override fun pick() = launcher.launch(arrayOf("*/*"))
        }
    }
}

private fun copyToCache(context: Context, uri: Uri): PickedLargeFile? {
    return try {
        val name = displayName(context, uri)?.takeIf { it.isNotBlank() } ?: FALLBACK_NAME
        val dir = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
        // Əvvəlki yükləmələrin 130 MB-lıq qalıqları keşdə yığılmasın.
        dir.listFiles()?.forEach { it.delete() }

        // Seçicinin verdiyi ad yol ayırıcısı daşıya bilər.
        val target = File(dir, name.substringAfterLast('/'))
        val input = context.contentResolver.openInputStream(uri) ?: return null
        input.use { source ->
            target.outputStream().use { sink -> source.copyTo(sink, bufferSize = 64 * 1024) }
        }
        PickedLargeFile(path = target.absolutePath, name = target.name, size = target.length())
    } catch (e: Exception) {
        AppLogger.saveError(e, "LargeFilePicker.copy")
        null
    }
}

private fun displayName(context: Context, uri: Uri): String? = try {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
} catch (e: Exception) {
    null
}
