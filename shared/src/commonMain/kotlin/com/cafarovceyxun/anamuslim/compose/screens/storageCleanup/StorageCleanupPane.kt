package com.cafarovceyxun.anamuslim.compose.screens.storageCleanup

import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strTitleRecitations
import com.cafarovceyxun.anamuslim.resources.strTitleScripts
import com.cafarovceyxun.anamuslim.resources.strTitleTranslations
import com.cafarovceyxun.anamuslim.resources.titleStorageCleanup
import org.jetbrains.compose.resources.StringResource

/**
 * The storage-cleanup panes. Declared next to the panes themselves rather than inside the root
 * screen, which still lives in `:app` — a shared screen may not depend on an app screen
 * (`EditorType`/`FilterField` precedent).
 */
enum class StorageCleanupPane(val titleRes: StringResource) {
    Hub(Res.string.titleStorageCleanup),
    Translations(Res.string.strTitleTranslations),
    Recitations(Res.string.strTitleRecitations),
    Scripts(Res.string.strTitleScripts),
}
