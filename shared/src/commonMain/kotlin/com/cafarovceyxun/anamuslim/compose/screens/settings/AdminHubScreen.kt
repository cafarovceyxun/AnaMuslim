package com.cafarovceyxun.anamuslim.compose.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.settings.SettingsGroup
import com.cafarovceyxun.anamuslim.compose.components.settings.SettingsItem
import com.cafarovceyxun.anamuslim.compose.navigation.SettingRoutes
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavigationOuterHeight
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.appLogs
import com.cafarovceyxun.anamuslim.resources.dailyContentManagementTitle
import com.cafarovceyxun.anamuslim.resources.dr_icon_bug
import com.cafarovceyxun.anamuslim.resources.dr_icon_download
import com.cafarovceyxun.anamuslim.resources.dr_icon_edit
import com.cafarovceyxun.anamuslim.resources.dr_icon_feature
import com.cafarovceyxun.anamuslim.resources.dr_icon_heart_filled
import com.cafarovceyxun.anamuslim.resources.dr_icon_report_problem
import com.cafarovceyxun.anamuslim.resources.dr_icon_translations
import com.cafarovceyxun.anamuslim.resources.dr_icon_update_app
import com.cafarovceyxun.anamuslim.resources.reports_management
import com.cafarovceyxun.anamuslim.resources.suggestionsManagementTitle
import com.cafarovceyxun.anamuslim.compose.utils.app.supportsAppLogs
import com.cafarovceyxun.anamuslim.viewModels.AdminBadgeViewModel
import com.cafarovceyxun.anamuslim.viewModels.ResourceAdminViewModel

/**
 * İdarəetmə paneli — bütün admin əməliyyatlarının bir yerdə toplandığı ekran.
 *
 * **Ayarlarda sətri yoxdur.** Yeganə giriş yolu Ayarlar başlığındakı **giriş edilmiş e-poçta
 * toxunmaqdır** (bax `SettingsMainScreen`), o da yalnız sessiya varsa çəkilir. Girişsiz istifadəçi
 * route-a birbaşa düşərsə [com.cafarovceyxun.anamuslim.compose.components.settings.AdminOnly] qapısı
 * onu saxlayır (qapı `SettingsNavHost`-dadır) — route naviqasiya qrafında qalır, sessiya isə başqa
 * cihazda bitirilə bilər.
 *
 * ⚠️ Bu görünüş qatıdır; əməliyyatların özü serverdə RLS ilə qorunur.
 */
@Composable
fun AdminHubScreen() {
    val navController = LocalSettingsNavController.current

    Scaffold(
        containerColor = colorScheme.background,
        topBar = { AppBar(title = "İdarəetmə paneli") },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .padding(bottom = mainBottomNavigationOuterHeight() + 24.dp),
            ) {
                AdminHubContent(onNavigate = { navController.navigate(it) })
            }
        }
    }
}

@Composable
private fun AdminHubContent(onNavigate: (String) -> Unit) {
    val resourceAdminViewModel = viewModel { ResourceAdminViewModel() }
    val badgeViewModel = viewModel { AdminBadgeViewModel() }
    val adminStatus by resourceAdminViewModel.status.collectAsState()
    val isAdminLoading by resourceAdminViewModel.isLoading.collectAsState()
    val adminError by resourceAdminViewModel.error.collectAsState()
    val counts by badgeViewModel.counts.collectAsState()

    var showUpdateConfirmDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        resourceAdminViewModel.fetchStatus()
        badgeViewModel.refresh()
    }

    LaunchedEffect(adminError) {
        adminError?.let { PlatformUtils.showLongToast(it) }
    }

    SettingsGroup(title = "Moderasiya") {
        item {
            SettingsItem(
                titleStr = "Düzəlişləri İdarə Et",
                icon = Res.drawable.dr_icon_edit,
                subtitleStr = "Quran və Hədis düzəlişləri",
                badgeCount = counts.pendingEdits,
                flat = true,
            ) { onNavigate(SettingRoutes.EDITS_MANAGEMENT) }
        }

        item {
            SettingsItem(
                title = Res.string.reports_management,
                icon = Res.drawable.dr_icon_report_problem,
                subtitleStr = "İstifadəçilərin ayə bildirişləri",
                badgeCount = counts.pendingReports,
                flat = true,
            ) { onNavigate(SettingRoutes.REPORTS_MANAGEMENT) }
        }

        item {
            SettingsItem(
                title = Res.string.suggestionsManagementTitle,
                icon = Res.drawable.dr_icon_feature,
                subtitleStr = "İstifadəçi təklifləri və moderasiya",
                badgeCount = counts.pendingSuggestions,
                flat = true,
            ) { onNavigate(SettingRoutes.SUGGESTIONS_MANAGEMENT) }
        }
    }

    SettingsGroup(title = "Məzmun") {
        item {
            SettingsItem(
                titleStr = "Resurs Yenilənməsi",
                icon = Res.drawable.dr_icon_download,
                subtitleStr = when {
                    isAdminLoading -> "Yüklənir..."
                    adminStatus != null -> "Uzaqdakı Versiya: ${adminStatus?.version}\nSon yenilənmə: ${adminStatus?.updated_at?.substringBefore(".")?.replace("T", " ")}"
                    else -> "Məlumat yoxdur (Klikləyin)"
                },
                flat = true,
            ) {
                if (adminStatus == null) resourceAdminViewModel.fetchStatus()
                else showUpdateConfirmDialog = true
            }
        }

        item {
            SettingsItem(
                title = Res.string.dailyContentManagementTitle,
                icon = Res.drawable.dr_icon_heart_filled,
                subtitleStr = "Günün ayəsi/hədisi növbəsi və bildiriş sırası",
                flat = true,
            ) { onNavigate(SettingRoutes.DAILY_CONTENT_MANAGEMENT) }
        }

        item {
            SettingsItem(
                titleStr = "Tərcümələr",
                icon = Res.drawable.dr_icon_translations,
                subtitleStr = "Kitabları adi istifadəçilərə aç/bağla",
                flat = true,
            ) { onNavigate(SettingRoutes.TRANSLATION_BOOKS) }
        }

        item {
            SettingsItem(
                titleStr = "Tərcümə idxalı",
                icon = Res.drawable.dr_icon_download,
                subtitleStr = "Surə-surə mətn yüklə",
                flat = true,
            ) { onNavigate(SettingRoutes.TRANSLATION_IMPORT) }
        }

        item {
            SettingsItem(
                titleStr = "Buraxılış Bildirişi",
                icon = Res.drawable.dr_icon_update_app,
                subtitleStr = "Play Store / App Store yeniləmə elanı",
                flat = true,
            ) { onNavigate(SettingRoutes.APP_RELEASE_MANAGEMENT) }
        }

        // Route qrafda olmayan yerdə gizlənir (iOS) — bax [supportsAppLogs].
        if (supportsAppLogs) {
            item {
                SettingsItem(
                    title = Res.string.appLogs,
                    icon = Res.drawable.dr_icon_bug,
                    subtitleStr = "Local & Remote Logs",
                    flat = true,
                ) { onNavigate(SettingRoutes.APP_LOGS) }
            }
        }
    }

    if (showUpdateConfirmDialog) {
        AlertDialog(
            isOpen = showUpdateConfirmDialog,
            onClose = { showUpdateConfirmDialog = false },
            title = "Yenilənməni Başlat",
            actions = listOf(
                AlertDialogAction(
                    text = "Ləğv Et",
                    onClick = { showUpdateConfirmDialog = false }
                ),
                AlertDialogAction(
                    text = "Bəli, Başlat",
                    style = AlertDialogActionStyle.Primary,
                    onClick = {
                        val current = adminStatus?.version ?: 0
                        resourceAdminViewModel.updateVersion(current + 1)
                    }
                )
            ),
            content = {
                Text(
                    text = "Bütün istifadəçilər üçün hədis və tərcümə yenilənməsini başlatmaq istəyirsiniz?\n\nHazırkı Versiya: ${adminStatus?.version ?: 0}\nYeni Versiya: ${(adminStatus?.version ?: 0) + 1}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        )
    }
}
