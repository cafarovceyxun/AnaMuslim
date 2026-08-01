package com.cafarovceyxun.anamuslim.utils.univ

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.cafarovceyxun.anamuslim.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun rememberTextDocumentSaver(onSaved: (Boolean) -> Unit): TextDocumentSaver {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnSaved by rememberUpdatedState(onSaved)

    // The content is produced before the picker opens but written only after the user picks a
    // destination, so it has to survive the round trip through the Activity result.
    val pending = remember { arrayOfNulls<String>(1) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(MIME_TYPE_JSON)
    ) { uri ->
        val content = pending[0]
        pending[0] = null

        if (uri == null || content == null) {
            currentOnSaved(false)
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            val written = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(content.encodeToByteArray())
                    } != null
                } catch (e: Exception) {
                    AppLogger.saveError(e, "TextDocumentSaver.write")
                    false
                }
            }
            currentOnSaved(written)
        }
    }

    return remember(launcher) {
        object : TextDocumentSaver {
            override fun save(suggestedFileName: String, content: String) {
                pending[0] = content
                launcher.launch(suggestedFileName)
            }
        }
    }
}

@Composable
actual fun rememberTextDocumentOpener(onOpened: (String?) -> Unit): TextDocumentOpener {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnOpened by rememberUpdatedState(onOpened)

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            currentOnOpened(null)
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            val text = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.readBytes().decodeToString()
                    }
                } catch (e: Exception) {
                    AppLogger.saveError(e, "TextDocumentOpener.read")
                    null
                }
            }
            currentOnOpened(text)
        }
    }

    return remember(launcher) {
        object : TextDocumentOpener {
            // Some file providers label a hand-edited export as text/plain rather than
            // application/json, so both are accepted; the parser is the real gate.
            override fun open() = launcher.launch(arrayOf(MIME_TYPE_JSON, "text/plain"))
        }
    }
}
