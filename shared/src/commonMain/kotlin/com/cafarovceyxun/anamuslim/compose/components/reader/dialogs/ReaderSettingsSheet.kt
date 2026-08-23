package com.cafarovceyxun.anamuslim.compose.components.reader.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.SwitchItem
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheet
import com.cafarovceyxun.anamuslim.compose.components.settings.ReaderDefaultModeSheet
import com.cafarovceyxun.anamuslim.compose.components.settings.ReaderSharedSettingsGroup
import com.cafarovceyxun.anamuslim.compose.components.settings.SettingsGroup
import com.cafarovceyxun.anamuslim.compose.components.settings.SettingsItem
import com.cafarovceyxun.anamuslim.compose.components.settings.TextSizeSheet
import com.cafarovceyxun.anamuslim.compose.components.settings.ThemeSelectorSheet
import com.cafarovceyxun.anamuslim.compose.components.settings.readerDefaultModeLabel
import com.cafarovceyxun.anamuslim.compose.navigation.SettingRoutes
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.compose.utils.appLocale
import com.cafarovceyxun.anamuslim.compose.utils.formatNumber
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.compose.utils.themeModeLabel
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.allSettings
import com.cafarovceyxun.anamuslim.resources.defaultReadingMode
import com.cafarovceyxun.anamuslim.resources.dr_icon_quran_script
import com.cafarovceyxun.anamuslim.resources.dr_icon_read_quran
import com.cafarovceyxun.anamuslim.resources.dr_icon_settings
import com.cafarovceyxun.anamuslim.resources.dr_icon_info
import com.cafarovceyxun.anamuslim.resources.dr_icon_theme
import com.cafarovceyxun.anamuslim.resources.ic_mode_verse
import com.cafarovceyxun.anamuslim.resources.ic_verse_end
import com.cafarovceyxun.anamuslim.resources.icon_font_size
import com.cafarovceyxun.anamuslim.resources.labelArabic
import com.cafarovceyxun.anamuslim.resources.labelTranslation
import com.cafarovceyxun.anamuslim.resources.msgArabicTextToggle
import com.cafarovceyxun.anamuslim.resources.msgTajweedColors
import com.cafarovceyxun.anamuslim.resources.strLabelContent
import com.cafarovceyxun.anamuslim.resources.strTitleReaderSettings
import com.cafarovceyxun.anamuslim.resources.strTitleScripts
import com.cafarovceyxun.anamuslim.resources.strTitleTheme
import com.cafarovceyxun.anamuslim.resources.strTitleTranslationDisplay
import com.cafarovceyxun.anamuslim.resources.textSizes
import com.cafarovceyxun.anamuslim.resources.titleArabicTextToggle
import com.cafarovceyxun.anamuslim.resources.titleTajweedColors
import com.cafarovceyxun.anamuslim.resources.translHighlightParentheses
import com.cafarovceyxun.anamuslim.resources.translShowParentheses
import com.cafarovceyxun.anamuslim.resources.wordByWord
import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptUtils
import com.cafarovceyxun.anamuslim.utils.reader.ReaderTextSizeUtils
import com.cafarovceyxun.anamuslim.utils.reader.getQuranScriptName
import com.cafarovceyxun.anamuslim.utils.reader.getQuranScriptVariantName
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Quran oxucusunun ayarları — `HadithSettingsSheet`-in eyni forması, eyni qrup dili.
 *
 * Əvvəllər oxucudakı ⚙ düyməsi tam Ayarlar ekranını (Android-də ayrıca Activity) açırdı: oxucudan
 * çıxmaq lazım gəlirdi və qarşına vidcetlərdən idarəetmə panelinə qədər hər şey çıxırdı, hədis
 * oxucusunda isə eyni düymə oxumanın üstündə vərəq açırdı. İki oxucu üçün bir davranış qalsın deyə
 * bu vərəq həmin asimmetriyanı bağlayır; tam ekrana ehtiyacı olan səhifələr ([SettingRoutes.SCRIPT],
 * [SettingRoutes.WWB]) [onOpenRoute] ilə açılır.
 *
 * [onOpenRoute] və [onOpenAllSettings] **default-suzdur**: davranışı host verir, ona görə default
 * versək iOS-da səssizcə heç nə etməyən düymələr alınardı (layihə qaydası). [onOpenAllSettings]
 * `null` olanda «Bütün ayarlar» sətri göstərilmir — vərəq elə ayarlar ekranından açılıbsa oradan
 * yenidən ayarlara getmək mənasızdır.
 */
@Composable
fun ReaderSettingsSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onOpenRoute: (route: String) -> Unit,
    onOpenAllSettings: (() -> Unit)?,
) {
    val scope = rememberCoroutineScope()
    var showThemeSelector by remember { mutableStateOf(false) }
    var showModeSelector by remember { mutableStateOf(false) }
    var showTextSizesSheet by remember { mutableStateOf(false) }

    // Observe-lər qrup lambdalarından kənarda: `SettingsGroup`-un content-i @Composable deyil.
    val selectedScript = ReaderPreferences.observeQuranScript()
    val selectedScriptVariant = ReaderPreferences.observeQuranScriptVariant()
    val defaultReaderMode = ReaderPreferences.observeDefaultReaderMode()
    val themeMode = ThemeUtils.observeThemeMode()
    val arabicTextEnabled = ReaderPreferences.observeArabicTextEnabled()
    val tajweedEnabled = ReaderPreferences.observeTajweedColorsEnabled()
    val arabicTextSizeMult = ReaderPreferences.observeArabicTextSizeMultiplier()
    val translationTextSizeMult = ReaderPreferences.observeTranlationTextSizeMultiplier()
    val highlightParentheses = ReaderPreferences.observeTranslHighlightParentheses()
    val showParentheses = ReaderPreferences.observeTranslShowParentheses()

    val defaultModeLabel = readerDefaultModeLabel(defaultReaderMode)
    val themeLabel = stringResource(themeModeLabel(themeMode))
    val scriptLabel = selectedScript.getQuranScriptName() +
            (selectedScriptVariant?.let { " | ${it.getQuranScriptVariantName()}" } ?: "")
    val textSizeLabel = appLocale().numeralSystem.run {
        "${stringResource(Res.string.labelArabic)}: " +
                "${formatNumber(ReaderTextSizeUtils.calculateProgressText(arabicTextSizeMult))}%, " +
                "${stringResource(Res.string.labelTranslation)}: " +
                "${formatNumber(ReaderTextSizeUtils.calculateProgressText(translationTextSizeMult))}%"
    }

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss,
        skipPartiallyExpanded = false,
        icon = Res.drawable.dr_icon_settings,
        title = stringResource(Res.string.strTitleReaderSettings),
    ) {
        Box(modifier = Modifier.fillMaxHeight(0.6f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // 1. Görünüş
                SettingsGroup(title = stringResource(Res.string.strTitleTheme)) {
                    item {
                        SettingsItem(
                            title = Res.string.defaultReadingMode,
                            subtitleStr = defaultModeLabel,
                            icon = Res.drawable.ic_mode_verse,
                            flat = true,
                        ) { showModeSelector = true }
                    }
                    item {
                        SettingsItem(
                            title = Res.string.strTitleTheme,
                            subtitleStr = themeLabel,
                            icon = Res.drawable.dr_icon_theme,
                            flat = true,
                        ) { showThemeSelector = true }
                    }
                    item {
                        SettingsItem(
                            title = Res.string.strTitleScripts,
                            subtitleStr = scriptLabel,
                            icon = Res.drawable.dr_icon_quran_script,
                            flat = true,
                        ) {
                            onDismiss()
                            onOpenRoute(SettingRoutes.SCRIPT)
                        }
                    }
                }

                // 2. Məzmun
                SettingsGroup(title = stringResource(Res.string.strLabelContent)) {
                    item {
                        SwitchItem(
                            title = Res.string.titleArabicTextToggle,
                            subtitle = Res.string.msgArabicTextToggle,
                            icon = Res.drawable.dr_icon_read_quran,
                            checked = arabicTextEnabled,
                            onCheckedChange = {
                                scope.launch { ReaderPreferences.setArabicTextEnabled(it) }
                            },
                        )
                    }

                    // Təcvid rəngləri yalnız Uthmani xəttində var.
                    if (selectedScript == QuranScriptUtils.SCRIPT_UTHMANI) {
                        item {
                            SwitchItem(
                                title = Res.string.titleTajweedColors,
                                subtitle = Res.string.msgTajweedColors,
                                icon = Res.drawable.dr_icon_theme,
                                checked = tajweedEnabled,
                                onCheckedChange = {
                                    scope.launch { ReaderPreferences.setTajweedColorsEnabled(it) }
                                },
                            )
                        }
                    }

                    item {
                        SettingsItem(
                            title = Res.string.wordByWord,
                            icon = Res.drawable.ic_verse_end,
                            flat = true,
                        ) {
                            onDismiss()
                            onOpenRoute(SettingRoutes.WWB)
                        }
                    }
                }

                // 3. Mətn ölçüləri
                SettingsGroup(title = stringResource(Res.string.textSizes)) {
                    item {
                        SettingsItem(
                            title = Res.string.textSizes,
                            subtitleStr = textSizeLabel,
                            icon = Res.drawable.icon_font_size,
                            flat = true,
                        ) { showTextSizesSheet = true }
                    }
                }

                // 4. Tərcümə görünüşü
                SettingsGroup(title = stringResource(Res.string.strTitleTranslationDisplay)) {
                    item {
                        SwitchItem(
                            title = Res.string.translShowParentheses,
                            icon = Res.drawable.dr_icon_info,
                            checked = showParentheses,
                            onCheckedChange = {
                                scope.launch { ReaderPreferences.setTranslShowParentheses(it) }
                            },
                        )
                    }
                    item {
                        SwitchItem(
                            title = Res.string.translHighlightParentheses,
                            icon = Res.drawable.dr_icon_theme,
                            checked = highlightParentheses,
                            onCheckedChange = {
                                scope.launch { ReaderPreferences.setTranslHighlightParentheses(it) }
                            },
                        )
                    }
                }

                // 5. Hər iki oxucu
                ReaderSharedSettingsGroup()

                // 6. Tam ayarlar ekranına keçid
                if (onOpenAllSettings != null) {
                    SettingsGroup {
                        item {
                            SettingsItem(
                                title = Res.string.allSettings,
                                icon = Res.drawable.dr_icon_settings,
                                flat = true,
                            ) {
                                onDismiss()
                                onOpenAllSettings()
                            }
                        }
                    }
                }
            }
        }
    }

    ThemeSelectorSheet(isOpen = showThemeSelector) { showThemeSelector = false }

    ReaderDefaultModeSheet(isOpen = showModeSelector) { showModeSelector = false }

    TextSizeSheet(isOpen = showTextSizesSheet) { showTextSizesSheet = false }
}
