package com.cafarovceyxun.anamuslim.compose.screens.hadith

import com.cafarovceyxun.anamuslim.resources.arabicLabel
import com.cafarovceyxun.anamuslim.resources.labelArabic
import com.cafarovceyxun.anamuslim.resources.labelTranslation
import com.cafarovceyxun.anamuslim.resources.allSettings
import com.cafarovceyxun.anamuslim.resources.strLabelContent
import com.cafarovceyxun.anamuslim.resources.strTitleReaderSettings
import com.cafarovceyxun.anamuslim.resources.textSizes

import com.cafarovceyxun.anamuslim.resources.defaultReadingMode
import com.cafarovceyxun.anamuslim.resources.defaultReadingModeDesc
import com.cafarovceyxun.anamuslim.resources.dr_icon_settings
import com.cafarovceyxun.anamuslim.resources.ic_mode_verse
import com.cafarovceyxun.anamuslim.resources.modeLastUsed
import com.cafarovceyxun.anamuslim.resources.strLabelMixed
import com.cafarovceyxun.anamuslim.resources.hadithBookMode
import com.cafarovceyxun.anamuslim.resources.hadithBookModeDesc
import com.cafarovceyxun.anamuslim.resources.ic_mode_book
import com.cafarovceyxun.anamuslim.resources.Res
import org.jetbrains.compose.resources.StringResource
import com.cafarovceyxun.anamuslim.resources.hadithShowArabic
import com.cafarovceyxun.anamuslim.resources.hadithShowAzerbaijani
import com.cafarovceyxun.anamuslim.resources.hadithShowSource
import com.cafarovceyxun.anamuslim.resources.strTitleTranslationDisplay
import com.cafarovceyxun.anamuslim.resources.translHighlightParentheses
import com.cafarovceyxun.anamuslim.resources.translShowParentheses
import com.cafarovceyxun.anamuslim.resources.strTitleTheme
import com.cafarovceyxun.anamuslim.resources.strTitleScripts
import com.cafarovceyxun.anamuslim.resources.dr_icon_theme
import com.cafarovceyxun.anamuslim.resources.dr_icon_quran_script
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafarovceyxun.anamuslim.compose.theme.hadithArabicFontFamily
import com.cafarovceyxun.anamuslim.compose.components.common.RadioItem
import com.cafarovceyxun.anamuslim.compose.components.common.SwitchItem
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheet
import com.cafarovceyxun.anamuslim.compose.components.settings.SettingsGroup
import com.cafarovceyxun.anamuslim.compose.components.settings.SettingsItem
import com.cafarovceyxun.anamuslim.compose.components.settings.ReaderSharedSettingsGroup
import com.cafarovceyxun.anamuslim.compose.components.settings.ThemeSelectorSheet
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.compose.utils.themeModeLabel
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HadithPreferences
import com.cafarovceyxun.anamuslim.utils.reader.getQuranScriptName
import com.cafarovceyxun.anamuslim.viewModels.HadithViewModel
import kotlinx.coroutines.launch

/**
 * Hədis oxucusunun ayarları — Quran tərəfindəki
 * [com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.ReaderSettingsSheet] ilə eyni
 * forma və eyni qrup dili.
 *
 * [onOpenAllSettings] **default-suzdur** və `null` ola bilər: vərəq elə ayarlar ekranından
 * açılıbsa «Bütün ayarlar» sətri göstərilmir. Default versək iOS-da səssizcə heç nə etməyən bir
 * düymə alınardı (layihə qaydası).
 */
@Composable
fun HadithSettingsSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onOpenAllSettings: (() -> Unit)?,
) {
    val scope = rememberCoroutineScope()
    var showThemeSelector by remember { mutableStateOf(false) }
    var showFontSelector by remember { mutableStateOf(false) }
    var showModeSelector by remember { mutableStateOf(false) }

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss,
        skipPartiallyExpanded = false,
        icon = Res.drawable.dr_icon_settings,
        title = stringResource(Res.string.strTitleReaderSettings)
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
                // Grouped cards matching the main settings screen (SettingsGroup): each section is a
                // rounded card with hairline dividers, its category label sitting above.

                // 1. Appearance
                SettingsGroup(title = stringResource(Res.string.strTitleTheme)) {
                    item {
                        SettingsItem(
                            title = Res.string.defaultReadingMode,
                            subtitleStr = hadithDefaultModeLabel(
                                HadithPreferences.observeDefaultViewMode(),
                            ),
                            icon = Res.drawable.ic_mode_verse,
                            flat = true,
                        ) { showModeSelector = true }
                    }
                    item {
                        SwitchItem(
                            title = Res.string.hadithBookMode,
                            subtitle = Res.string.hadithBookModeDesc,
                            icon = Res.drawable.ic_mode_book,
                            checked = HadithPreferences.observeBookMode(),
                            onCheckedChange = {
                                scope.launch {
                                    HadithPreferences.setBookMode(it)
                                    // Ayarlardan tapan da izahı görməsin.
                                    HadithPreferences.setBookModeHintSeen(true)
                                }
                            }
                        )
                    }
                    item {
                        SettingsItem(
                            title = Res.string.strTitleTheme,
                            subtitleStr = stringResource(themeModeLabel(ThemeUtils.observeThemeMode())),
                            icon = Res.drawable.dr_icon_theme,
                            flat = true,
                        ) { showThemeSelector = true }
                    }
                    item {
                        val selectedFont = HadithPreferences.observeArabicFont()
                        SettingsItem(
                            title = Res.string.strTitleScripts,
                            subtitleStr = selectedFont.getQuranScriptName(),
                            icon = Res.drawable.dr_icon_quran_script,
                            flat = true,
                        ) { showFontSelector = true }
                    }
                }

                // 2. Content
                SettingsGroup(title = stringResource(Res.string.strLabelContent)) {
                    item {
                        SwitchItem(
                            title = Res.string.hadithShowArabic,
                            checked = HadithPreferences.observeArabicEnabled(),
                            onCheckedChange = { scope.launch { HadithPreferences.setArabicEnabled(it) } }
                        )
                    }
                    item {
                        SwitchItem(
                            title = Res.string.hadithShowAzerbaijani,
                            checked = HadithPreferences.observeAzerbaijaniEnabled(),
                            onCheckedChange = { scope.launch { HadithPreferences.setAzerbaijaniEnabled(it) } }
                        )
                    }
                    item {
                        SwitchItem(
                            title = Res.string.hadithShowSource,
                            checked = HadithPreferences.observeSourceEnabled(),
                            onCheckedChange = { scope.launch { HadithPreferences.setSourceEnabled(it) } }
                        )
                    }
                }

                // 3. Text Sizes
                SettingsGroup(title = stringResource(Res.string.textSizes)) {
                    item {
                        HadithTextSizeItem(
                            title = Res.string.labelArabic,
                            value = HadithPreferences.observeArabicSizeMultiplier(),
                            onValueChange = { scope.launch { HadithPreferences.setArabicSizeMultiplier(it) } }
                        ) { mult ->
                            ArabicBismillahPreview(mult)
                        }
                    }
                    item {
                        HadithTextSizeItem(
                            title = Res.string.labelTranslation,
                            value = HadithPreferences.observeAzerbaijaniSizeMultiplier(),
                            onValueChange = { scope.launch { HadithPreferences.setAzerbaijaniSizeMultiplier(it) } }
                        ) { mult ->
                            AzerbaijaniBismillahPreview(mult)
                        }
                    }
                }

                // 4. Tərcümə görünüşü
                SettingsGroup(title = stringResource(Res.string.strTitleTranslationDisplay)) {
                    item {
                        SwitchItem(
                            title = Res.string.translShowParentheses,
                            checked = HadithPreferences.observeShowParentheses(),
                            onCheckedChange = { scope.launch { HadithPreferences.setShowParentheses(it) } }
                        )
                    }
                    item {
                        SwitchItem(
                            title = Res.string.translHighlightParentheses,
                            checked = HadithPreferences.observeHighlightParentheses(),
                            onCheckedChange = { scope.launch { HadithPreferences.setHighlightParentheses(it) } }
                        )
                    }
                }

                // 5. Hər iki oxucu — pinch-zoom və sürüşmə addımı `AppPreferences`-dədir, yəni
                // buradan dəyişən Quran oxucusunu da dəyişir. Öz qrupunda ki, bu görünsün.
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

    ThemeSelectorSheet(isOpen = showThemeSelector) {
        showThemeSelector = false
    }

    HadithFontSelectorSheet(isOpen = showFontSelector) {
        showFontSelector = false
    }

    HadithDefaultModeSheet(isOpen = showModeSelector) {
        showModeSelector = false
    }
}

/**
 * Hədis oxucusunun açılış rejimləri — app bar-dakı tab zolağı ilə eyni üçlük, eyni sıra ilə.
 *
 * İndeks nömrələri [HadithPreferences.VIEW_MODE]-un dəyərləridir (0 qarışıq, 1 ərəbcə, 2 tərcümə),
 * ona görə siyahının sırası zolağın sırasından ayrıla bilməz.
 */
private val hadithDefaultModeOptions: List<Pair<Int, StringResource>> = listOf(
    0 to Res.string.strLabelMixed,
    1 to Res.string.arabicLabel,
    2 to Res.string.labelTranslation,
)

/** Seçilmiş açılış rejiminin ayarlar sətrində göstərilən adı. */
@Composable
fun hadithDefaultModeLabel(mode: Int): String = stringResource(
    hadithDefaultModeOptions.firstOrNull { it.first == mode }?.second ?: Res.string.modeLastUsed
)

/**
 * Hədis oxucusunun açılış rejimini seçən vərəq.
 *
 * Yazdığı açar [HadithPreferences.DEFAULT_VIEW_MODE]-dur — oxuyarkən tab zolağının yazdığı canlı
 * açar deyil. Əvvəllər indeksdən, əlfəcindən və oxuma tarixçəsindən giriş rejimi zorla qarışığa
 * qaytarırdı; «sonuncu istifadə olunan» seçimi cari tabı olduğu kimi saxlayır.
 */
@Composable
fun HadithDefaultModeSheet(isOpen: Boolean, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val selected = HadithPreferences.observeDefaultViewMode()

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss,
        icon = Res.drawable.ic_mode_verse,
        title = stringResource(Res.string.defaultReadingMode),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            RadioItem(
                title = Res.string.modeLastUsed,
                subtitle = Res.string.defaultReadingModeDesc,
                selected = selected == HadithPreferences.VIEW_MODE_LAST_USED,
                onClick = {
                    onDismiss()
                    scope.launch {
                        HadithPreferences.setDefaultViewMode(HadithPreferences.VIEW_MODE_LAST_USED)
                    }
                },
            )

            hadithDefaultModeOptions.forEach { (mode, label) ->
                RadioItem(
                    title = label,
                    selected = selected == mode,
                    onClick = {
                        onDismiss()
                        scope.launch {
                            HadithPreferences.setDefaultViewMode(mode)
                            // Ayarda rejim seçən adam onu elə indi də görmək istəyir.
                            HadithPreferences.setViewMode(mode)
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun ArabicBismillahPreview(arabicSizeMult: Float) {
    val selectedFont = HadithPreferences.observeArabicFont()

    val arabicFontFamily = hadithArabicFontFamily(selectedFont)

    Text(
        text = "بِسْمِ اللهِ الرَّحْمٰنِ الرَّحِيْمِ",
        style = MaterialTheme.typography.headlineMedium.copy(
            fontSize = 24.sp * arabicSizeMult,
            lineHeight = (24.sp * arabicSizeMult) * 1.6f,
            fontFamily = arabicFontFamily,
            textAlign = TextAlign.Right
        ),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    )
}

@Composable
fun AzerbaijaniBismillahPreview(azerbaijaniSizeMult: Float) {
    Text(
        text = "Mərhəmətli və Rəhmli Allahın adı ilə!",
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 16.sp * azerbaijaniSizeMult,
            lineHeight = (16.sp * azerbaijaniSizeMult) * 1.5f
        ),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    )
}

@Composable
private fun HadithTextSizeItem(
    title: StringResource,
    value: Float,
    onValueChange: (Float) -> Unit,
    preview: @Composable (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                modifier = Modifier.weight(1f),
                value = value * 100,
                onValueChange = { onValueChange(it / 100f) },
                valueRange = 30f..300f,
                steps = 25
            )
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        preview(value)
    }
}
