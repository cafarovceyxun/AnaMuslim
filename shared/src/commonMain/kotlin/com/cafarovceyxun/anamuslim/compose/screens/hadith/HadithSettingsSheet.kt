package com.cafarovceyxun.anamuslim.compose.screens.hadith

import com.cafarovceyxun.anamuslim.resources.labelArabic
import com.cafarovceyxun.anamuslim.resources.labelTranslation
import com.cafarovceyxun.anamuslim.resources.navigation
import com.cafarovceyxun.anamuslim.resources.strLabelAdditional
import com.cafarovceyxun.anamuslim.resources.strLabelContent
import com.cafarovceyxun.anamuslim.resources.strTitleReaderSettings
import com.cafarovceyxun.anamuslim.resources.textSizes

import com.cafarovceyxun.anamuslim.resources.dr_icon_settings
import com.cafarovceyxun.anamuslim.resources.Res
import org.jetbrains.compose.resources.StringResource
import com.cafarovceyxun.anamuslim.resources.hadithShowArabic
import com.cafarovceyxun.anamuslim.resources.hadithShowAzerbaijani
import com.cafarovceyxun.anamuslim.resources.hadithShowSource
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
import com.cafarovceyxun.anamuslim.compose.components.common.SwitchItem
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheet
import com.cafarovceyxun.anamuslim.compose.components.settings.SettingsGroup
import com.cafarovceyxun.anamuslim.compose.components.settings.SettingsItem
import com.cafarovceyxun.anamuslim.compose.components.settings.ScrollStepSlider
import com.cafarovceyxun.anamuslim.compose.components.settings.ThemeSelectorSheet
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.compose.utils.themeModeLabel
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HadithPreferences
import com.cafarovceyxun.anamuslim.utils.reader.getQuranScriptName
import com.cafarovceyxun.anamuslim.viewModels.HadithViewModel
import kotlinx.coroutines.launch

@Composable
fun HadithSettingsSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var showThemeSelector by remember { mutableStateOf(false) }
    var showFontSelector by remember { mutableStateOf(false) }

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

                // 4. Navigation
                SettingsGroup(title = stringResource(Res.string.navigation)) {
                    item { ScrollStepSlider() }
                }

                // 5. Additional (Translation Settings)
                SettingsGroup(title = stringResource(Res.string.strLabelAdditional)) {
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
            }
        }
    }

    ThemeSelectorSheet(isOpen = showThemeSelector) {
        showThemeSelector = false
    }

    HadithFontSelectorSheet(isOpen = showFontSelector) {
        showFontSelector = false
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
                valueRange = 50f..300f,
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
