package com.cafarovceyxun.anamuslim.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.titleExportData
import com.cafarovceyxun.anamuslim.resources.labelImportExportEverything
import com.cafarovceyxun.anamuslim.resources.msgExportFailed
import com.cafarovceyxun.anamuslim.resources.msgExportSuccess
import com.cafarovceyxun.anamuslim.resources.msgImportFailed
import com.cafarovceyxun.anamuslim.resources.msgImportNothing
import com.cafarovceyxun.anamuslim.resources.msgImportSuccess
import com.cafarovceyxun.anamuslim.resources.warnImportSettings
import com.cafarovceyxun.anamuslim.resources.labelImportExportSettings
import com.cafarovceyxun.anamuslim.resources.labelImportExportBookmarks
import com.cafarovceyxun.anamuslim.resources.labelImportExportHistory
import com.cafarovceyxun.anamuslim.resources.msgExportImportBookmarks
import com.cafarovceyxun.anamuslim.resources.msgExportImportEverything
import com.cafarovceyxun.anamuslim.resources.msgExportImportHistory
import com.cafarovceyxun.anamuslim.resources.labelImport
import com.cafarovceyxun.anamuslim.resources.labelExport
import org.jetbrains.compose.resources.stringResource
import com.cafarovceyxun.anamuslim.utils.univ.ExportImportManager
import com.cafarovceyxun.anamuslim.utils.univ.ExportKeys
import com.cafarovceyxun.anamuslim.utils.univ.rememberTextDocumentOpener
import com.cafarovceyxun.anamuslim.utils.univ.rememberTextDocumentSaver
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar

/**
 * Deliberately self-contained: the export and import work is platform-neutral
 * ([ExportImportManager]) and the only platform-bound part — the file picker — is reached through
 * its own seam. The screen used to take `exportCallback`/`importCallback` instead, which the
 * Android Activity supplied and the shared NavHost did not, so on iOS both buttons were wired to
 * the parameter defaults and did nothing at all, with no error anywhere.
 */
@Composable
fun ExportImportScreen() {
    // Resolved up front rather than with a suspending `getString` in the callbacks: an imported
    // locale recreates the Android Activity, and a lookup queued after that never reports back.
    val msgExportOk = stringResource(Res.string.msgExportSuccess)
    val msgExportFail = stringResource(Res.string.msgExportFailed)
    val msgImportOk = stringResource(Res.string.msgImportSuccess)
    val msgImportEmpty = stringResource(Res.string.msgImportNothing)
    val msgImportFail = stringResource(Res.string.msgImportFailed)

    // Which scopes the user last pressed a button for; the picker result arrives later.
    var pendingImportScopes by remember { mutableStateOf(emptyMap<String, Boolean>()) }

    val saver = rememberTextDocumentSaver { saved ->
        PlatformUtils.showToast(if (saved) msgExportOk else msgExportFail)
    }

    val opener = rememberTextDocumentOpener { content ->
        if (content == null) return@rememberTextDocumentOpener // cancelled

        ExportImportManager.import(content, pendingImportScopes) { result ->
            PlatformUtils.showLongToast(
                when {
                    result.failed -> msgImportFail
                    result.changedAnything -> msgImportOk
                    else -> msgImportEmpty
                }
            )
        }
    }

    fun export(scopes: Map<String, Boolean>) {
        ExportImportManager.export(scopes) { content ->
            saver.save(ExportImportManager.exportFileName(), content)
        }
    }

    fun import(scopes: Map<String, Boolean>) {
        pendingImportScopes = scopes
        opener.open()
    }

    Scaffold(
        topBar = {
            AppBar(
                stringResource(Res.string.titleExportData)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(it)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Birinci kart telefon dəyişən istifadəçi üçündür: üç əhatənin hamısı bir faylda.
            // Qalan üçü eyni faylın hissələrini ayrıca köçürmək istəyənlər üçün qalır.
            ExportImportCard(
                mapOf(
                    ExportKeys.SETTINGS to true,
                    ExportKeys.BOOKMARKS to true,
                    ExportKeys.HISTORY to true,
                ),
                Res.string.labelImportExportEverything,
                Res.string.msgExportImportEverything,
                ::import,
                ::export,
            )
            ExportImportCard(
                mapOf(
                    ExportKeys.SETTINGS to true,
                ),
                Res.string.labelImportExportSettings,
                Res.string.warnImportSettings,
                ::import,
                ::export,
            )
            ExportImportCard(
                mapOf(
                    ExportKeys.BOOKMARKS to true
                ),
                Res.string.labelImportExportBookmarks,
                Res.string.msgExportImportBookmarks,
                ::import,
                ::export,
            )
            ExportImportCard(
                mapOf(
                    ExportKeys.HISTORY to true
                ),
                Res.string.labelImportExportHistory,
                Res.string.msgExportImportHistory,
                ::import,
                ::export,
            )
        }
    }
}

@Composable
private fun ExportImportCard(
    scopes: Map<String, Boolean>,
    title: org.jetbrains.compose.resources.StringResource,
    description: org.jetbrains.compose.resources.StringResource,
    importCallback: (scopes: Map<String, Boolean>) -> Unit,
    exportCallback: (scopes: Map<String, Boolean>) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.labelLarge,
            )

            Text(
                text = stringResource(description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Button(onClick = { importCallback(scopes) }) {
                    Text(text = stringResource(Res.string.labelImport))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { exportCallback(scopes) }) {
                    Text(text = stringResource(Res.string.labelExport))
                }
            }
        }
    }
}
