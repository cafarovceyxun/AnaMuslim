package com.cafarovceyxun.anamuslim.compose.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.cafarovceyxun.anamuslim.compose.utils.app.supportsAppLogs
import com.cafarovceyxun.anamuslim.compose.utils.app.supportsVolumeKeyNavigation
import com.cafarovceyxun.anamuslim.compose.utils.appLanguages
import com.cafarovceyxun.anamuslim.compose.utils.appLocale
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_heart_filled
import com.cafarovceyxun.anamuslim.resources.ic_lock_keyhole_closed
import com.cafarovceyxun.anamuslim.resources.ic_lock_open
import com.cafarovceyxun.anamuslim.resources.labelArabic
import com.cafarovceyxun.anamuslim.resources.labelTranslation
import com.cafarovceyxun.anamuslim.resources.strLabelDownloaded
import com.cafarovceyxun.anamuslim.resources.strLabelDownloads
import com.cafarovceyxun.anamuslim.resources.strLabelSystemDefault
import com.cafarovceyxun.anamuslim.resources.strTitleAppSettings
import com.cafarovceyxun.anamuslim.resources.strTitleQuran
import com.cafarovceyxun.anamuslim.resources.strTitleSettings
import com.cafarovceyxun.anamuslim.resources.dr_icon_info
import com.cafarovceyxun.anamuslim.resources.dr_icon_read_quran
import com.cafarovceyxun.anamuslim.resources.dr_icon_theme
import com.cafarovceyxun.anamuslim.resources.dr_icon_language
import com.cafarovceyxun.anamuslim.resources.dr_icon_quran_script
import com.cafarovceyxun.anamuslim.resources.dr_icon_download
import com.cafarovceyxun.anamuslim.resources.dr_icon_edit
import com.cafarovceyxun.anamuslim.resources.dr_icon_report_problem
import com.cafarovceyxun.anamuslim.resources.dr_icon_update_app
import com.cafarovceyxun.anamuslim.resources.reports_management
import com.cafarovceyxun.anamuslim.resources.dr_icon_history
import com.cafarovceyxun.anamuslim.resources.dr_icon_bug
import com.cafarovceyxun.anamuslim.resources.ic_verse_end
import com.cafarovceyxun.anamuslim.resources.icon_font_size
import com.cafarovceyxun.anamuslim.resources.strTitleAppLanguage
import com.cafarovceyxun.anamuslim.resources.strTitleTheme
import com.cafarovceyxun.anamuslim.resources.strTitleVOTD
import com.cafarovceyxun.anamuslim.resources.strLabelOn
import com.cafarovceyxun.anamuslim.resources.strLabelOff
import com.cafarovceyxun.anamuslim.resources.strTitleHadith
import com.cafarovceyxun.anamuslim.resources.strTitleReaderSettings
import com.cafarovceyxun.anamuslim.resources.strTitleScripts
import com.cafarovceyxun.anamuslim.resources.strTitleTranslationDisplay
import com.cafarovceyxun.anamuslim.resources.wordByWord
import com.cafarovceyxun.anamuslim.resources.textSizes
import com.cafarovceyxun.anamuslim.resources.strTitleTranslations
import com.cafarovceyxun.anamuslim.resources.downloadRecitations
import com.cafarovceyxun.anamuslim.resources.titleResourceDownloadSource
import com.cafarovceyxun.anamuslim.resources.appLogs
import com.cafarovceyxun.anamuslim.resources.msgArabicTextToggle
import com.cafarovceyxun.anamuslim.resources.strTitleVolumeKeyNavigation
import com.cafarovceyxun.anamuslim.resources.volumeKeyNavSubtitle
import com.cafarovceyxun.anamuslim.resources.titleArabicTextToggle
import com.cafarovceyxun.anamuslim.resources.titleTajweedColors
import com.cafarovceyxun.anamuslim.resources.msgTajweedColors
import com.cafarovceyxun.anamuslim.resources.translHighlightParentheses
import com.cafarovceyxun.anamuslim.resources.translShowParentheses
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.SwitchItem
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.compose.components.settings.DailyReminderSheet
import com.cafarovceyxun.anamuslim.compose.components.settings.SettingsGroup
import com.cafarovceyxun.anamuslim.compose.components.settings.LoginSheet
import com.cafarovceyxun.anamuslim.compose.components.settings.ResourceDownloadSrcSheet
import com.cafarovceyxun.anamuslim.compose.components.settings.ScrollStepSlider
import com.cafarovceyxun.anamuslim.compose.components.settings.SettingsItem
import com.cafarovceyxun.anamuslim.compose.components.settings.TextSizeSheet
import com.cafarovceyxun.anamuslim.compose.navigation.SettingRoutes
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.compose.utils.formatNumber
import com.cafarovceyxun.anamuslim.compose.utils.themeModeLabel
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.VersePreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import com.cafarovceyxun.anamuslim.utils.app.DownloadSourceUtils
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.reader.ReaderTextSizeUtils
import com.cafarovceyxun.anamuslim.utils.reader.getQuranScriptName
import com.cafarovceyxun.anamuslim.utils.reader.getQuranScriptVariantName
import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptUtils
import com.cafarovceyxun.anamuslim.viewModels.AuthViewModel
import com.cafarovceyxun.anamuslim.viewModels.HadithViewModel
import com.cafarovceyxun.anamuslim.viewModels.ResourceAdminViewModel
import com.cafarovceyxun.anamuslim.compose.screens.hadith.HadithSettingsSheet
import com.cafarovceyxun.anamuslim.resources.volumeKeyNavSubtitle
import kotlinx.coroutines.launch

@Composable
fun SettingsMainScreen(
    showReaderSettingsOnly: Boolean
) {
    val navController = LocalSettingsNavController.current
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
    var showTextSizesSheet by rememberSaveable { mutableStateOf(false) }
    var showResourceDownloadSrcSheet by rememberSaveable { mutableStateOf(false) }
    var showLoginSheet by rememberSaveable { mutableStateOf(false) }
    var showHadithSettingsSheet by rememberSaveable { mutableStateOf(false) }
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

    val arabicTextSizeMult = ReaderPreferences.observeArabicTextSizeMultiplier()
    val translationTextSizeMult = ReaderPreferences.observeTranlationTextSizeMultiplier()
    val selectedScript = ReaderPreferences.observeQuranScript()
    val selectedScriptVariant = ReaderPreferences.observeQuranScriptVariant()

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
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 150.dp),
            ) {
                // 1. App settings (full) / Appearance (reader-only)
                if (!showReaderSettingsOnly) {
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

                        // Hidden where the volume buttons belong to the system (iOS) — the preference
                        // exists on both platforms, but only one can act on it.
                        if (supportsVolumeKeyNavigation) {
                            item {
                                val keyNavEnabled = AppPreferences.observeVolumeKeyNavigationEnabled()
                                Column {
                                    SwitchItem(
                                        title = Res.string.strTitleVolumeKeyNavigation,
                                        subtitle = Res.string.volumeKeyNavSubtitle,
                                        checked = keyNavEnabled,
                                        onCheckedChange = { coroutineScope.launch { AppPreferences.setVolumeKeyNavigationEnabled(it) } }
                                    )
                                    // The step slider only matters once the keys are handed to the reader.
                                    if (keyNavEnabled) {
                                        ScrollStepSlider()
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // In Reader Only mode, show Theme under Appearance header
                    SettingsGroup(title = stringResource(Res.string.strTitleTheme)) {
                        item {
                            SettingsItem(
                                title = Res.string.strTitleTheme,
                                subtitleStr = stringResource(themeModeLabel(ThemeUtils.observeThemeMode())),
                                icon = Res.drawable.dr_icon_theme,
                                flat = true,
                            ) { navController.navigate(SettingRoutes.THEME) }
                        }
                    }
                }

                // 2. Quran (reading)
                SettingsGroup(title = stringResource(Res.string.strTitleQuran)) {
                    item {
                        SettingsItem(
                            title = Res.string.strTitleScripts,
                            icon = Res.drawable.dr_icon_quran_script,
                            subtitleStr = selectedScript.getQuranScriptName() +
                                    (selectedScriptVariant?.let { " | ${it.getQuranScriptVariantName()}" }
                                        ?: ""),
                            flat = true,
                        ) { navController.navigate(SettingRoutes.SCRIPT) }
                    }

                    item {
                        SwitchItem(
                            title = Res.string.titleArabicTextToggle,
                            subtitle = Res.string.msgArabicTextToggle,
                            icon = Res.drawable.dr_icon_read_quran,
                            checked = ReaderPreferences.observeArabicTextEnabled(),
                        ) {
                            coroutineScope.launch {
                                ReaderPreferences.setArabicTextEnabled(it)
                            }
                        }
                    }

                    if (selectedScript == QuranScriptUtils.SCRIPT_UTHMANI) {
                        item {
                            SwitchItem(
                                title = Res.string.titleTajweedColors,
                                subtitle = Res.string.msgTajweedColors,
                                icon = Res.drawable.dr_icon_theme,
                                checked = ReaderPreferences.observeTajweedColorsEnabled(),
                            ) {
                                coroutineScope.launch {
                                    ReaderPreferences.setTajweedColorsEnabled(it)
                                }
                            }
                        }
                    }

                    item {
                        SettingsItem(
                            title = Res.string.wordByWord,
                            icon = Res.drawable.ic_verse_end,
                            flat = true,
                        ) { navController.navigate(SettingRoutes.WWB) }
                    }

                    item {
                        SettingsItem(
                            title = Res.string.textSizes,
                            icon = Res.drawable.icon_font_size,
                            subtitleStr = appLocale().numeralSystem.run {
                                "${stringResource(Res.string.labelArabic)}: " +
                                        "${formatNumber(ReaderTextSizeUtils.calculateProgressText(arabicTextSizeMult))}%, " +
                                        "${stringResource(Res.string.labelTranslation)}: " +
                                        "${formatNumber(ReaderTextSizeUtils.calculateProgressText(translationTextSizeMult))}%"
                            },
                            flat = true,
                        ) { showTextSizesSheet = true }
                    }
                }

                // 3. Translation display
                SettingsGroup(title = stringResource(Res.string.strTitleTranslationDisplay)) {
                    item {
                        SwitchItem(
                            title = Res.string.translHighlightParentheses,
                            icon = Res.drawable.dr_icon_theme,
                            checked = ReaderPreferences.observeTranslHighlightParentheses(),
                        ) {
                            coroutineScope.launch {
                                ReaderPreferences.setTranslHighlightParentheses(it)
                            }
                        }
                    }

                    item {
                        SwitchItem(
                            title = Res.string.translShowParentheses,
                            icon = Res.drawable.dr_icon_info,
                            checked = ReaderPreferences.observeTranslShowParentheses(),
                        ) {
                            coroutineScope.launch {
                                ReaderPreferences.setTranslShowParentheses(it)
                            }
                        }
                    }
                }

                if (!showReaderSettingsOnly) {
                    // 4. Hadith
                    SettingsGroup(title = stringResource(Res.string.strTitleHadith)) {
                        item {
                            SettingsItem(
                                title = Res.string.strTitleHadith,
                                subtitle = Res.string.strTitleReaderSettings,
                                icon = Res.drawable.dr_icon_read_quran,
                                flat = true,
                            ) { showHadithSettingsSheet = true }
                        }
                    }

                    // 5. Downloads (all downloads live here together)
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

                    // 6. Management (admin only)
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
    }

    TextSizeSheet(isOpen = showTextSizesSheet) {
        showTextSizesSheet = false
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

    HadithSettingsSheet(
        isOpen = showHadithSettingsSheet,
        onDismiss = { showHadithSettingsSheet = false }
    )
}
