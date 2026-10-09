package com.cafarovceyxun.anamuslim.compose.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.homepage.contentBackupSubtitle
import com.cafarovceyxun.anamuslim.compose.components.homepage.rememberContentBackup
import com.cafarovceyxun.anamuslim.compose.components.settings.SettingsGroup
import com.cafarovceyxun.anamuslim.compose.components.settings.SettingsItem
import com.cafarovceyxun.anamuslim.compose.navigation.SettingRoutes
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavigationOuterHeight
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.ic_bell_ring
import com.cafarovceyxun.anamuslim.resources.storyAnnouncementsTitle
import com.cafarovceyxun.anamuslim.resources.appLogs
import com.cafarovceyxun.anamuslim.resources.dailyContentManagementTitle
import com.cafarovceyxun.anamuslim.resources.dr_icon_bug
import com.cafarovceyxun.anamuslim.resources.dr_icon_download
import com.cafarovceyxun.anamuslim.resources.dr_icon_edit
import com.cafarovceyxun.anamuslim.resources.dr_icon_lunar
import com.cafarovceyxun.anamuslim.resources.lunarAnnouncementTitle
import com.cafarovceyxun.anamuslim.resources.dr_icon_feature
import com.cafarovceyxun.anamuslim.resources.dr_icon_heart_filled
import com.cafarovceyxun.anamuslim.resources.dr_icon_report_problem
import com.cafarovceyxun.anamuslim.resources.dr_icon_translations
import com.cafarovceyxun.anamuslim.resources.dr_icon_update_app
import com.cafarovceyxun.anamuslim.resources.reports_management
import com.cafarovceyxun.anamuslim.resources.suggestionsManagementTitle
import com.cafarovceyxun.anamuslim.compose.utils.app.supportsAppLogs
import com.cafarovceyxun.anamuslim.resources.dr_icon_refresh
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseFailover
import com.cafarovceyxun.anamuslim.viewModels.AdminBadgeViewModel
import com.cafarovceyxun.anamuslim.viewModels.BackendSwitchViewModel
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

    val backup = rememberContentBackup()

    ServerSwitchGroup()

    SettingsGroup(title = "Yedək") {
        item {
            SettingsItem(
                titleStr = "Məzmun yedəyi",
                icon = Res.drawable.dr_icon_download,
                subtitleStr = contentBackupSubtitle(backup),
                flat = true,
            ) { if (!backup.isRunning) backup.start() }
        }
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
                title = Res.string.lunarAnnouncementTitle,
                icon = Res.drawable.dr_icon_lunar,
                subtitleStr = "Ayın başlanğıcı, 29/30 və görünmə videosu",
                flat = true,
            ) { onNavigate(SettingRoutes.LUNAR_ANNOUNCEMENT_MANAGEMENT) }
        }

        item {
            SettingsItem(
                title = Res.string.storyAnnouncementsTitle,
                icon = Res.drawable.ic_bell_ring,
                subtitleStr = "Təklifə bağlı olmayan hekayə: şəkil/video, mətn, müddət",
                flat = true,
            ) { onNavigate(SettingRoutes.STORY_ANNOUNCEMENT_MANAGEMENT) }
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

/**
 * Əsas (Oracle) ↔ ehtiyat (Supabase Frankfurt) keçidi — bax [SupabaseFailover]. Düymə ehtiyatda saxlanır,
 * ona görə Oracle çökəndə də işləyir; dəyişmək üçün admin parolu yenidən istənir (ehtiyatın öz girişi).
 */
@Composable
private fun ServerSwitchGroup() {
    val viewModel = viewModel { BackendSwitchViewModel() }
    val state by viewModel.state.collectAsState()
    val route by SupabaseFailover.route.collectAsState()
    var showDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(state.error) {
        state.error?.let { PlatformUtils.showLongToast(it) }
    }

    val modeLine = when (state.forcedBackup) {
        null -> if (state.isLoading) "Yüklənir..." else "Ehtiyat server oxunmadı (klikləyin)"
        true -> "Rejim: EHTİYAT — bütün istifadəçilər Supabase-dən oxuyur"
        false -> "Rejim: Əsas (Oracle), çökəndə avtomatik ehtiyat"
    }
    val deviceLine = when (route) {
        SupabaseFailover.Route.Primary -> "Bu cihaz: əsas serverdə"
        SupabaseFailover.Route.BackupBySwitch -> "Bu cihaz: ehtiyatda (düymə ilə)"
        SupabaseFailover.Route.BackupByOutage -> "Bu cihaz: ehtiyatda (əsas cavab vermir)"
    }
    val syncLine = "Son köçürmə: " + (state.syncedAt?.toUtcMinute() ?: "—")

    SettingsGroup(title = "Server") {
        item {
            SettingsItem(
                titleStr = "Server keçidi",
                icon = Res.drawable.dr_icon_refresh,
                subtitleStr = "$modeLine\n$deviceLine\n$syncLine",
                flat = true,
            ) {
                if (state.forcedBackup == null) viewModel.refresh() else showDialog = true
            }
        }
    }

    if (showDialog) {
        val toBackup = state.forcedBackup != true
        var password by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            isOpen = showDialog,
            onClose = { if (!state.isSaving) showDialog = false },
            title = if (toBackup) "Ehtiyata keçir" else "Əsas serverə qaytar",
            actions = listOf(
                AlertDialogAction(
                    text = "Ləğv Et",
                    onClick = { showDialog = false }
                ),
                AlertDialogAction(
                    text = if (state.isSaving) "Gözləyin..." else if (toBackup) "Ehtiyata keçir" else "Qaytar",
                    style = if (toBackup) AlertDialogActionStyle.Danger else AlertDialogActionStyle.Primary,
                    // Sorğu bitənə qədər açıq qalsın — səhv parolda xəta görünsün, yenidən yazmaq olsun.
                    dismissOnClick = false,
                    onClick = {
                        if (password.isNotBlank() && !state.isSaving) {
                            viewModel.setForcedBackup(toBackup, password) { showDialog = false }
                        }
                    }
                )
            ),
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (toBackup) {
                            "Bütün istifadəçilər məzmunu ehtiyat Supabase-dən oxuyacaq (10 dəqiqə ərzində). " +
                                "Ehtiyatda yalnız son gecəki köçürmə var (${state.syncedAt?.toUtcMinute() ?: "—"}); " +
                                "şəkil/videolar görünməyəcək, yazma (təklif, düzəliş) yenə əsasa gedir."
                        } else {
                            "İstifadəçilər yenidən Oracle-dan oxuyacaq. Əsas cavab verməsə avtomatik keçid işləməyə davam edir."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Admin parolu") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                }
            }
        )
    }
}

/** `2026-10-07T06:40:58.96+00:00` → `2026-10-07 06:40 UTC`. */
private fun String.toUtcMinute(): String = replace("T", " ").take(16) + " UTC"
