package com.cafarovceyxun.anamuslim.compose.screens.dua

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.contextmenu.builder.TextContextMenuBuilderScope
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession

/** Bax ortaq [excerptMenuItem]. Android-də bənd sistemin üzən seçim panelinə düşür. */
@OptIn(ExperimentalFoundationApi::class)
actual fun TextContextMenuBuilderScope.excerptMenuItem(
    key: Any,
    label: String,
    onClick: TextContextMenuSession.() -> Unit,
) = item(key = key, label = label, onClick = onClick)
