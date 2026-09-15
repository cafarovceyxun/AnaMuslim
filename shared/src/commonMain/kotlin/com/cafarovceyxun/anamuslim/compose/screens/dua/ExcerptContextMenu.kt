package com.cafarovceyxun.anamuslim.compose.screens.dua

import androidx.compose.foundation.text.contextmenu.builder.TextContextMenuBuilderScope
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession

/**
 * Seçim panelinə (Kopyala · Hamısını seç · Paylaş) **öz bəndimizi** əlavə edir.
 *
 * Niyə lazımdır: parçanı seçəndən sonra onu hədəfə yazmaq üçün istifadəçi barmağını qaldırıb
 * bloкun altındakı düyməyə uzanmalı olurdu — halbuki panel elə barmağın yanında açılır. Bənd
 * oradan basılanda seçim həmin anda hədəfə düşür.
 *
 * ⚠️ **expect/actual olmasının səbəbi:** `TextContextMenuBuilderScope.item` Compose-un **ortaq**
 * mənbəsində yoxdur — hər platformada ayrı imzası var (Android `@DrawableRes Int` ikon alır, skiko
 * isə `@Composable` ikon). Ortaq kodda birbaşa çağırmaq mümkün deyil, ona görə bu nazik örtük.
 */
expect fun TextContextMenuBuilderScope.excerptMenuItem(
    key: Any,
    label: String,
    onClick: TextContextMenuSession.() -> Unit,
)
