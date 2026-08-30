package com.cafarovceyxun.anamuslim.compose.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.cafarovceyxun.anamuslim.compose.components.common.ReadableMaxWidth
import com.cafarovceyxun.anamuslim.compose.utils.app.supportsAppLogs
import com.cafarovceyxun.anamuslim.compose.utils.appLanguages
import com.cafarovceyxun.anamuslim.compose.utils.appLocale
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_heart_filled
import com.cafarovceyxun.anamuslim.resources.ic_play
import com.cafarovceyxun.anamuslim.resources.msgAddWidgetToHomeScreen
import com.cafarovceyxun.anamuslim.resources.recitationPlayer
import com.cafarovceyxun.anamuslim.resources.strTitleWidgets
import com.cafarovceyxun.anamuslim.resources.ic_lock_keyhole_closed
import com.cafarovceyxun.anamuslim.resources.ic_lock_open
import com.cafarovceyxun.anamuslim.resources.strLabelDownloaded
import com.cafarovceyxun.anamuslim.resources.strLabelDownloads
import com.cafarovceyxun.anamuslim.resources.strLabelSystemDefault
import com.cafarovceyxun.anamuslim.resources.strTitleAppSettings
import com.cafarovceyxun.anamuslim.resources.strTitleQuran
import com.cafarovceyxun.anamuslim.resources.strTitleSettings
import com.cafarovceyxun.anamuslim.compose.components.LocalIndexMenuActions
import com.cafarovceyxun.anamuslim.resources.ic_bookmarks
import com.cafarovceyxun.anamuslim.resources.icon_clean
import com.cafarovceyxun.anamuslim.resources.icon_import_export
import com.cafarovceyxun.anamuslim.resources.dr_icon_rate
import com.cafarovceyxun.anamuslim.resources.dr_icon_share
import com.cafarovceyxun.anamuslim.resources.strTitleAboutUs
import com.cafarovceyxun.anamuslim.resources.strTitleBookmarks
import com.cafarovceyxun.anamuslim.resources.strTitleMenu
import com.cafarovceyxun.anamuslim.resources.strTitleRateApp
import com.cafarovceyxun.anamuslim.resources.strTitleShareApp
import com.cafarovceyxun.anamuslim.resources.strLabelUpdate
import com.cafarovceyxun.anamuslim.resources.titleExportData
import com.cafarovceyxun.anamuslim.resources.titleStorageCleanup
import com.cafarovceyxun.anamuslim.resources.suggestionsManagementTitle
import com.cafarovceyxun.anamuslim.resources.suggestionsSettingsSubtitle
import com.cafarovceyxun.anamuslim.resources.suggestionsTitle
import com.cafarovceyxun.anamuslim.resources.dr_icon_read_quran
import com.cafarovceyxun.anamuslim.resources.ic_book_copy
import com.cafarovceyxun.anamuslim.resources.strTitleReading
import com.cafarovceyxun.anamuslim.resources.dr_icon_theme
import com.cafarovceyxun.anamuslim.resources.dr_icon_language
import com.cafarovceyxun.anamuslim.resources.dr_icon_download
import com.cafarovceyxun.anamuslim.resources.dr_icon_edit
import com.cafarovceyxun.anamuslim.resources.dr_icon_feature
import com.cafarovceyxun.anamuslim.resources.dr_icon_info
import com.cafarovceyxun.anamuslim.resources.dr_icon_report_problem
import com.cafarovceyxun.anamuslim.resources.dr_icon_update_app
import com.cafarovceyxun.anamuslim.resources.reports_management
import com.cafarovceyxun.anamuslim.resources.dr_icon_history
import com.cafarovceyxun.anamuslim.resources.dr_icon_bug
import com.cafarovceyxun.anamuslim.resources.strTitleAppLanguage
import com.cafarovceyxun.anamuslim.resources.strTitleTheme
import com.cafarovceyxun.anamuslim.resources.strTitleVOTD
import com.cafarovceyxun.anamuslim.resources.strTitleVotdCard
import com.cafarovceyxun.anamuslim.resources.votdCardSubtitle
import com.cafarovceyxun.anamuslim.resources.strLabelOn
import com.cafarovceyxun.anamuslim.resources.strLabelOff
import com.cafarovceyxun.anamuslim.resources.strTitleHadith
import com.cafarovceyxun.anamuslim.resources.strTitleReaderSettings
import com.cafarovceyxun.anamuslim.resources.strTitleTranslations
import com.cafarovceyxun.anamuslim.resources.downloadRecitations
import com.cafarovceyxun.anamuslim.resources.titleResourceDownloadSource
import com.cafarovceyxun.anamuslim.resources.appLogs
import com.cafarovceyxun.anamuslim.resources.keepScreenOnSubtitle
import com.cafarovceyxun.anamuslim.resources.strTitleKeepScreenOn
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.SwitchItem
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.compose.components.settings.DailyReminderSheet
import com.cafarovceyxun.anamuslim.compose.components.settings.ReaderSharedSettingsGroup
import com.cafarovceyxun.anamuslim.compose.components.settings.SettingsGroup
import com.cafarovceyxun.anamuslim.compose.components.settings.LoginSheet
import com.cafarovceyxun.anamuslim.compose.components.settings.ResourceDownloadSrcSheet
import com.cafarovceyxun.anamuslim.compose.components.settings.SettingsItem
import com.cafarovceyxun.anamuslim.compose.components.settings.AppTextScaleSlider
import com.cafarovceyxun.anamuslim.compose.navigation.SettingRoutes
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.HomeWidgetKind
import com.cafarovceyxun.anamuslim.compose.utils.HomeWidgetPinProvider
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.compose.utils.themeModeLabel
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.VersePreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import com.cafarovceyxun.anamuslim.utils.app.DownloadSourceUtils
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.viewModels.AuthViewModel
import com.cafarovceyxun.anamuslim.viewModels.HadithViewModel
import com.cafarovceyxun.anamuslim.viewModels.ResourceAdminViewModel
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.ReaderSettingsSheet
import com.cafarovceyxun.anamuslim.compose.screens.hadith.HadithSettingsSheet
import kotlinx.coroutines.launch
import com.cafarovceyxun.anamuslim.compose.theme.LocalAppTextScale

@Composable
fun SettingsMainScreen() {
    val navController = LocalSettingsNavController.current
    val indexMenuActions = LocalIndexMenuActions.current
    val coroutineScope = rememberCoroutineScope()
    val authViewModel = viewModel { AuthViewModel() }
    val resourceAdminViewModel = viewModel { ResourceAdminViewModel() }
    val hadithViewModel = viewModel { HadithViewModel() }
    val session by authViewModel.session.collectAsState()
    val isAdmin by authViewModel.isAdmin.collectAsState()
    val adminStatus by resourceAdminViewModel.status.collectAsState()
    val isAdminLoading by resourceAdminViewModel.isLoading.collectAsState()
    val adminError by resourceAdminViewModel.error.collectAsState()
    val volumes by hadithViewModel.volumes.collectAsState()
    val cachedVolumes by hadithViewModel.cachedVolumes.collectAsState()

    var showDailyReminderSheet by rememberSaveable { mutableStateOf(false) }
    var showResourceDownloadSrcSheet by rememberSaveable { mutableStateOf(false) }
    var showLoginSheet by rememberSaveable { mutableStateOf(false) }
    var showHadithSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showQuranSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showUpdateConfirmDialog by rememberSaveable { mutableStateOf(false) }
    
    val isHadithDownloaded = volumes.isNotEmpty() && cachedVolumes.isNotEmpty()

    var secretClickCount by remember { mutableStateOf(0) }
    var lastClickTime by remember { mutableStateOf(0L) }

    val systemDefaultName = stringResource(Res.string.strLabelSystemDefault)
    val selectedLanguage = remember(systemDefaultName) {
        appLanguages
            .firstOrNull { it.rawLanguageTag == appLocale().rawLanguageTag }
            ?.let { it.endonym ?: systemDefaultName }
            .orEmpty()
    }

    val votdEnabled = VersePreferences.observeVOTDReminderEnabled()
    val slugs = ReaderPreferences.observeTranslations()

    // Which widgets this device can pin does not change while Settings is open, so this is read once
    // rather than watched.
    var offerableWidgets by remember { mutableStateOf(emptyList<HomeWidgetKind>()) }

    LaunchedEffect(Unit) {
        offerableWidgets = if (HomeWidgetPinProvider.isAvailable) {
            HomeWidgetPinProvider.pinner.offerableWidgets()
        } else {
            emptyList()
        }
    }

    LaunchedEffect(adminError) {
        adminError?.let {
            PlatformUtils.showLongToast(it)
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { 
            AppBar(
                title = stringResource(Res.string.strTitleSettings),
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (session != null) {
                            Text(
                                text = session?.user?.email ?: "",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp * LocalAppTextScale.current),
                                color = colorScheme.onSurface.alpha(0.6f),
                                maxLines = 1
                            )
                        }

                        IconButton(onClick = {
                            if (session != null) {
                                authViewModel.logout()
                            } else {
                                val currentTime = currentEpochMillis()
                                if (currentTime - lastClickTime < 1000) {
                                    secretClickCount++
                                } else {
                                    secretClickCount = 1
                                }
                                lastClickTime = currentTime
                                
                                if (secretClickCount >= 5) {
                                    showLoginSheet = true
                                    secretClickCount = 0
                                }
                            }
                        }) {
                            Icon(
                                painter = painterResource(
                                    if (session != null) Res.drawable.ic_lock_open
                                    else Res.drawable.ic_lock_keyhole_closed
                                ),
                                contentDescription = "Login",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            ) 
        }
    ) { paddingValues ->
        // The scroll lives on the full-width box, not on the capped column: on a tablet a drag in
        // the margin beside the rows still scrolls the page. The column itself is capped at a
        // readable width — left to `fillMaxWidth`, a settings row on a 13" iPad puts its label at
        // the far left and its own switch some 900dp away, with nothing in between.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .widthIn(max = ReadableMaxWidth)
                    .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 150.dp),
            ) {
                // 1. Tətbiq ayarları
                SettingsGroup(title = stringResource(Res.string.strTitleAppSettings)) {
                    item {
                        SettingsItem(
                            title = Res.string.strTitleAppLanguage,
                            subtitleStr = selectedLanguage,
                            icon = Res.drawable.dr_icon_language,
                            flat = true,
                        ) { navController.navigate(SettingRoutes.LANGUAGE) }
                    }

                    item {
                        SettingsItem(
                            title = Res.string.strTitleTheme,
                            subtitleStr = stringResource(themeModeLabel(ThemeUtils.observeThemeMode())),
                            icon = Res.drawable.dr_icon_theme,
                            flat = true,
                        ) { navController.navigate(SettingRoutes.THEME) }
                    }

                    item { AppTextScaleSlider() }

                    item {
                        SettingsItem(
                            title = Res.string.strTitleVOTD,
                            subtitle = if (votdEnabled) Res.string.strLabelOn else Res.string.strLabelOff,
                            iconImage = {
                                Image(
                                    painter = painterResource(Res.drawable.dr_icon_heart_filled),
                                    contentDescription = null,
                                )
                            },
                            flat = true,
                        ) { showDailyReminderSheet = true }
                    }

                    item {
                        SwitchItem(
                            title = Res.string.strTitleVotdCard,
                            subtitle = Res.string.votdCardSubtitle,
                            checked = VersePreferences.observeVOTDCardEnabled(),
                            onCheckedChange = { coroutineScope.launch { VersePreferences.setVOTDCardEnabled(it) } }
                        )
                    }

                    item {
                        SwitchItem(
                            title = Res.string.strTitleKeepScreenOn,
                            subtitle = Res.string.keepScreenOnSubtitle,
                            checked = AppPreferences.observeKeepScreenOnEnabled(),
                            onCheckedChange = { coroutineScope.launch { AppPreferences.setKeepScreenOnEnabled(it) } }
                        )
                    }

                    item {
                        SettingsItem(
                            title = Res.string.suggestionsTitle,
                            subtitle = Res.string.suggestionsSettingsSubtitle,
                            icon = Res.drawable.dr_icon_feature,
                            flat = true,
                        ) { navController.navigate(SettingRoutes.SUGGESTIONS) }
                    }
                }

                // 1b. Home screen widgets. Absent on iOS, where widgets can only be added from the
                // OS gallery, and on launchers that refuse pin requests — `SettingsGroup` renders
                // nothing for an empty scope, so the header disappears with its rows.
                SettingsGroup(title = stringResource(Res.string.strTitleWidgets)) {
                    offerableWidgets.forEach { kind ->
                        item {
                            SettingsItem(
                                title = when (kind) {
                                    HomeWidgetKind.RecitationPlayer -> Res.string.recitationPlayer
                                    HomeWidgetKind.VerseOfTheDay -> Res.string.strTitleVOTD
                                },
                                subtitle = Res.string.msgAddWidgetToHomeScreen,
                                icon = when (kind) {
                                    HomeWidgetKind.RecitationPlayer -> Res.drawable.ic_play
                                    HomeWidgetKind.VerseOfTheDay -> Res.drawable.dr_icon_heart_filled
                                },
                                flat = true,
                            ) { HomeWidgetPinProvider.pinner.requestPin(kind) }
                        }
                    }
                }
            

                // 2. Oxuma — hər oxucunun öz ayar vərəqi, oxucudakı ⚙ ilə eyni vərəq.
                // Ayarların özü burada təkrarlanmır: bir dəfə vərəqdə yazılıb, iki yerdən açılır.
                SettingsGroup(title = stringResource(Res.string.strTitleReading)) {
                    item {
                        SettingsItem(
                            title = Res.string.strTitleQuran,
                            subtitle = Res.string.strTitleReaderSettings,
                            icon = Res.drawable.dr_icon_read_quran,
                            flat = true,
                        ) { showQuranSettingsSheet = true }
                    }

                    item {
                        SettingsItem(
                            title = Res.string.strTitleHadith,
                            subtitle = Res.string.strTitleReaderSettings,
                            icon = Res.drawable.ic_book_copy,
                            flat = true,
                        ) { showHadithSettingsSheet = true }
                    }
                }

                // 3. Hər iki oxucunun paylaşdığı ayarlar.
                ReaderSharedSettingsGroup()

                // 4. Endirmələr (hamısı bir yerdə)
                SettingsGroup(title = stringResource(Res.string.strLabelDownloads)) {
                    item {
                        SettingsItem(
                            title = Res.string.strTitleTranslations,
                            icon = Res.drawable.dr_icon_download,
                            subtitleStr = if (isHadithDownloaded && slugs.isNotEmpty()) stringResource(Res.string.strLabelDownloaded) else null,
                            flat = true,
                        ) { navController.navigate(SettingRoutes.TRANSLATIONS) }
                    }

                    item {
                        SettingsItem(
                            title = Res.string.downloadRecitations,
                            icon = Res.drawable.dr_icon_download,
                            flat = true,
                        ) { navController.navigate(SettingRoutes.RECITATION_DOWNLOAD) }
                    }

                    item {
                        SettingsItem(
                            title = Res.string.titleResourceDownloadSource,
                            icon = Res.drawable.dr_icon_download,
                            subtitleStr = DownloadSourceUtils.observeCurrentSourceName(),
                            flat = true,
                        ) { showResourceDownloadSrcSheet = true }
                    }
                }

                // 5. Köhnə üst-bar menyusunun sətirləri. Menyu düyməsi götürüldü (2026-08-30),
                // sətirlər isə itməsin deyə buraya köçdü. Mağaza ilə bağlı üçü (yenilə,
                // qiymətləndir, paylaş) platformada link olmayanda `null` gəlir və sətir də
                // görünmür — basılıb heç nə etməyən sətirdən yaxşıdır.
                SettingsGroup(title = stringResource(Res.string.strTitleMenu)) {
                    item {
                        SettingsItem(
                            title = Res.string.strTitleBookmarks,
                            icon = Res.drawable.ic_bookmarks,
                            flat = true,
                        ) { indexMenuActions.onOpenBookmarks() }
                    }

                    item {
                        SettingsItem(
                            title = Res.string.titleStorageCleanup,
                            icon = Res.drawable.icon_clean,
                            flat = true,
                        ) { indexMenuActions.onOpenStorageCleanup() }
                    }

                    item {
                        SettingsItem(
                            title = Res.string.titleExportData,
                            icon = Res.drawable.icon_import_export,
                            flat = true,
                        ) { indexMenuActions.onOpenExportImport() }
                    }

                    indexMenuActions.onOpenPlayStore?.let { openStore ->
                        item {
                            SettingsItem(
                                title = Res.string.strLabelUpdate,
                                icon = Res.drawable.dr_icon_update_app,
                                flat = true,
                            ) { openStore() }
                        }
                    }

                    indexMenuActions.onRateApp?.let { rate ->
                        item {
                            SettingsItem(
                                title = Res.string.strTitleRateApp,
                                icon = Res.drawable.dr_icon_rate,
                                flat = true,
                            ) { rate() }
                        }
                    }

                    indexMenuActions.onShareApp?.let { share ->
                        item {
                            SettingsItem(
                                title = Res.string.strTitleShareApp,
                                icon = Res.drawable.dr_icon_share,
                                flat = true,
                            ) { share() }
                        }
                    }

                    item {
                        SettingsItem(
                            title = Res.string.strTitleAboutUs,
                            icon = Res.drawable.dr_icon_info,
                            flat = true,
                        ) { indexMenuActions.onOpenAboutUs() }
                    }
                }

                // 5. İdarəetmə (yalnız admin)
                if (isAdmin) {
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        resourceAdminViewModel.fetchStatus()
                    }

                    SettingsGroup(title = "İdarəetmə") {
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
                                if (adminStatus == null) {
                                    resourceAdminViewModel.fetchStatus()
                                } else {
                                    showUpdateConfirmDialog = true
                                }
                            }
                        }

                        item {
                            SettingsItem(
                                titleStr = "Düzəlişləri İdarə Et",
                                icon = Res.drawable.dr_icon_edit,
                                subtitleStr = "Quran və Hədis düzəlişləri",
                                flat = true,
                            ) { navController.navigate(SettingRoutes.EDITS_MANAGEMENT) }
                        }

                        item {
                            SettingsItem(
                                titleStr = "Buraxılış Bildirişi",
                                icon = Res.drawable.dr_icon_update_app,
                                subtitleStr = "Play Store / App Store yeniləmə elanı",
                                flat = true,
                            ) { navController.navigate(SettingRoutes.APP_RELEASE_MANAGEMENT) }
                        }

                        item {
                            SettingsItem(
                                title = Res.string.reports_management,
                                icon = Res.drawable.dr_icon_report_problem,
                                subtitleStr = "İstifadəçilərin ayə bildirişləri",
                                flat = true,
                            ) { navController.navigate(SettingRoutes.REPORTS_MANAGEMENT) }
                        }

                        item {
                            SettingsItem(
                                title = Res.string.suggestionsManagementTitle,
                                icon = Res.drawable.dr_icon_feature,
                                subtitleStr = "İstifadəçi təklifləri və moderasiya",
                                flat = true,
                            ) { navController.navigate(SettingRoutes.SUGGESTIONS_MANAGEMENT) }
                        }

                        // Hidden where the route is not in the graph (iOS) — see [supportsAppLogs].
                        if (supportsAppLogs) {
                            item {
                                SettingsItem(
                                    title = Res.string.appLogs,
                                    icon = Res.drawable.dr_icon_bug,
                                    subtitleStr = "Local & Remote Logs",
                                    flat = true,
                                ) { navController.navigate(SettingRoutes.APP_LOGS) }
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
            }
        }
    }

    ResourceDownloadSrcSheet(isOpen = showResourceDownloadSrcSheet) {
        showResourceDownloadSrcSheet = false
    }

    DailyReminderSheet(
        isOpen = showDailyReminderSheet,
        onClose = {
            showDailyReminderSheet = false
        },
    )

    LoginSheet(
        isOpen = showLoginSheet,
        onDismiss = { showLoginSheet = false }
    )

    ReaderSettingsSheet(
        isOpen = showQuranSettingsSheet,
        onDismiss = { showQuranSettingsSheet = false },
        onOpenRoute = { route -> navController.navigate(route) },
        // Vərəq elə ayarlar ekranından açılır — «Bütün ayarlar» sətri özünə aparardı.
        onOpenAllSettings = null,
    )

    HadithSettingsSheet(
        isOpen = showHadithSettingsSheet,
        onDismiss = { showHadithSettingsSheet = false },
        // Vərəq elə ayarlar ekranından açılır — «Bütün ayarlar» sətri özünə aparardı.
        onOpenAllSettings = null,
    )
}
