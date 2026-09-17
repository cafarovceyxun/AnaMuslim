package com.cafarovceyxun.anamuslim.compose.screens.dua

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafarovceyxun.anamuslim.compose.components.common.RadioItem
import com.cafarovceyxun.anamuslim.compose.components.common.SwitchItem
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheet
import com.cafarovceyxun.anamuslim.compose.components.settings.ReaderSharedSettingsGroup
import com.cafarovceyxun.anamuslim.compose.components.settings.SettingsGroup
import com.cafarovceyxun.anamuslim.compose.components.settings.SettingsItem
import com.cafarovceyxun.anamuslim.compose.components.settings.ThemeSelectorSheet
import com.cafarovceyxun.anamuslim.compose.screens.hadith.withScriptDirection
import com.cafarovceyxun.anamuslim.compose.theme.arabicFontFamily
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.compose.utils.preferences.DuaPreferences
import com.cafarovceyxun.anamuslim.compose.utils.themeModeLabel
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.arabicLabel
import com.cafarovceyxun.anamuslim.resources.asmaAutoNote
import com.cafarovceyxun.anamuslim.resources.asmaAutoTitle
import com.cafarovceyxun.anamuslim.resources.defaultReadingMode
import com.cafarovceyxun.anamuslim.resources.defaultReadingModeDesc
import com.cafarovceyxun.anamuslim.resources.dr_icon_quran_script
import com.cafarovceyxun.anamuslim.resources.dr_icon_settings
import com.cafarovceyxun.anamuslim.resources.dr_icon_theme
import com.cafarovceyxun.anamuslim.resources.dr_icon_translations
import com.cafarovceyxun.anamuslim.resources.ic_mode_verse
import com.cafarovceyxun.anamuslim.resources.labelArabic
import com.cafarovceyxun.anamuslim.resources.labelTranslation
import com.cafarovceyxun.anamuslim.resources.labelTransliteration
import com.cafarovceyxun.anamuslim.resources.modeLastUsed
import com.cafarovceyxun.anamuslim.resources.strLabelContent
import com.cafarovceyxun.anamuslim.resources.strLabelMixed
import com.cafarovceyxun.anamuslim.resources.strTitleReaderSettings
import com.cafarovceyxun.anamuslim.resources.strTitleScripts
import com.cafarovceyxun.anamuslim.resources.strTitleTheme
import com.cafarovceyxun.anamuslim.resources.textSizes
import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptUtils
import com.cafarovceyxun.anamuslim.utils.reader.getQuranScriptName
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Dua və Əsmaül Hüsnə oxuma ayarları — `HadithSettingsSheet` ilə eyni forma və eyni qrup dili.
 *
 * Etiketlər mümkün qədər **mövcud ümumi sətirlərdən** qurulur (`labelArabic`,
 * `labelTransliteration`, `labelTranslation`, `textSizes`, …): hər yeni tərcümə sətri beş dil
 * faylına toxunmaq deməkdir, halbuki burada deyiləsi şeylərin çoxunun qarşılığı onsuz da var.
 * İstisna avtomatik uyğunlaşdırma açarıdır — onun mənasını («maşın təxminidir») mövcud sətirlərin
 * heç biri vermirdi, ona görə `asmaAutoTitle`/`asmaAutoNote` ayrıca əlavə olundu.
 *
 * [ReaderSharedSettingsGroup] ortaq oxucu ayarlarını (səhifə keçid animasiyası, iki barmaqla
 * ölçüləndirmə açarı, sürüşmə addımı) gətirir — onlar Quran və hədislə **eyni** açarları yazır,
 * yəni istifadəçi bir dəfə seçəndə hər üç oxucuda tətbiq olunur.
 */
@Composable
fun DuaSettingsSheet(isOpen: Boolean, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var showThemeSelector by remember { mutableStateOf(false) }
    var showFontSelector by remember { mutableStateOf(false) }
    var showModeSelector by remember { mutableStateOf(false) }

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
                // Observe-lər qrup lambdasından kənarda: `SettingsGroup`-un content-i @Composable
                // deyil (bax [ReaderSharedSettingsGroup]-dakı eyni qeyd).
                val arabicEnabled = DuaPreferences.observeArabicEnabled()
                val translitEnabled = DuaPreferences.observeTransliterationEnabled()
                val translationEnabled = DuaPreferences.observeTranslationEnabled()
                val autoEvidenceEnabled = DuaPreferences.observeAutoEvidenceEnabled()
                val arabicSize = DuaPreferences.observeArabicSizeMultiplier()
                val translationSize = DuaPreferences.observeTranslationSizeMultiplier()
                val selectedFont = DuaPreferences.observeArabicFont()
                val themeLabel = stringResource(themeModeLabel(ThemeUtils.observeThemeMode()))
                val modeLabel = duaDefaultModeLabel(DuaPreferences.observeDefaultViewMode())

                SettingsGroup(title = stringResource(Res.string.strTitleReaderSettings)) {
                    item {
                        SettingsItem(
                            title = Res.string.defaultReadingMode,
                            subtitleStr = modeLabel,
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
                            subtitleStr = selectedFont.getQuranScriptName(),
                            icon = Res.drawable.dr_icon_quran_script,
                            flat = true,
                        ) { showFontSelector = true }
                    }
                }

                // Rejim zolağı bu üç açarı **əvəz etmir**, onlarla kəsişir: «Ərəbcə» rejimində
                // ərəbcə açarı bağlıdırsa səhifə boş qalır və bunu istifadəçiyə izahlı boş hal
                // deyir (bax `duaBlockVisibility`).
                SettingsGroup(title = stringResource(Res.string.strLabelContent)) {
                    item {
                        SwitchItem(
                            title = Res.string.labelArabic,
                            icon = Res.drawable.dr_icon_quran_script,
                            checked = arabicEnabled,
                            onCheckedChange = {
                                scope.launch { DuaPreferences.setArabicEnabled(it) }
                            },
                        )
                    }
                    item {
                        SwitchItem(
                            title = Res.string.labelTransliteration,
                            icon = Res.drawable.dr_icon_translations,
                            checked = translitEnabled,
                            onCheckedChange = {
                                scope.launch { DuaPreferences.setTransliterationEnabled(it) }
                            },
                        )
                    }
                    item {
                        SwitchItem(
                            title = Res.string.labelTranslation,
                            icon = Res.drawable.dr_icon_translations,
                            checked = translationEnabled,
                            onCheckedChange = {
                                scope.launch { DuaPreferences.setTranslationEnabled(it) }
                            },
                        )
                    }
                    item {
                        // Əsmaül Hüsnəyə aiddir, amma ayarlar vərəqi iki ekran üçün ortaqdır və
                        // «Məzmun» qrupu elə nəyin göstərilib-göstərilməyəcəyi haqqındadır.
                        SwitchItem(
                            title = Res.string.asmaAutoTitle,
                            subtitle = Res.string.asmaAutoNote,
                            icon = Res.drawable.dr_icon_quran_script,
                            checked = autoEvidenceEnabled,
                            onCheckedChange = {
                                scope.launch { DuaPreferences.setAutoEvidenceEnabled(it) }
                            },
                        )
                    }
                }

                SettingsGroup(title = stringResource(Res.string.textSizes)) {
                    item {
                        DuaTextSizeItem(
                            title = Res.string.labelArabic,
                            value = arabicSize,
                            onValueChange = {
                                scope.launch { DuaPreferences.setArabicSizeMultiplier(it) }
                            },
                        ) { mult ->
                            Text(
                                text = ARABIC_PREVIEW,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = 24.sp * mult,
                                    lineHeight = (24.sp * mult) * 1.95f,
                                    textAlign = TextAlign.Center,
                                ).withScriptDirection(
                                    arabic = true,
                                    arabicFontFamily = arabicFontFamily(),
                                ),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            )
                        }
                    }
                    item {
                        DuaTextSizeItem(
                            title = Res.string.labelTranslation,
                            value = translationSize,
                            onValueChange = {
                                scope.launch { DuaPreferences.setTranslationSizeMultiplier(it) }
                            },
                        ) { mult ->
                            Text(
                                text = stringResource(Res.string.labelTranslation),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 16.sp * mult,
                                    lineHeight = (16.sp * mult) * TRANSLATION_LINE_HEIGHT_RATIO,
                                ).withScriptDirection(arabic = false),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            )
                        }
                    }
                }

                ReaderSharedSettingsGroup()
            }
        }
    }

    ThemeSelectorSheet(isOpen = showThemeSelector, onDismiss = { showThemeSelector = false })
    DuaFontSelectorSheet(isOpen = showFontSelector, onDismiss = { showFontSelector = false })
    DuaDefaultModeSheet(isOpen = showModeSelector, onDismiss = { showModeSelector = false })
}

/** Ölçü sürüşdürücüsü — 30–300%, altında canlı önbaxış. */
@Composable
private fun DuaTextSizeItem(
    title: StringResource,
    value: Float,
    onValueChange: (Float) -> Unit,
    preview: @Composable (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                modifier = Modifier.weight(1f),
                value = value * 100,
                onValueChange = { onValueChange(it / 100f) },
                valueRange = 30f..300f,
                steps = 25,
            )
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        preview(value)
    }
}

/**
 * Ərəbcə xətt seçicisi.
 *
 * `HadithFontSelectorSheet` təkrar istifadə olunmur, çünki o, birbaşa `HadithPreferences`-ə yazır —
 * dua öz açarını saxlayır ki, istifadəçi hədisdə bir, duada başqa xətt seçə bilsin.
 */
@Composable
private fun DuaFontSelectorSheet(isOpen: Boolean, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val selectedFont = DuaPreferences.observeArabicFont()

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss,
        icon = Res.drawable.dr_icon_quran_script,
        title = stringResource(Res.string.strTitleScripts),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            QuranScriptUtils.HADITH_ARABIC_FONTS.forEach { font ->
                RadioItem(
                    titleStr = font.getQuranScriptName(),
                    selected = selectedFont == font,
                    onClick = {
                        onDismiss()
                        scope.launch { DuaPreferences.setArabicFont(font) }
                    },
                )
            }
        }
    }
}

/**
 * Açılış rejimi seçimi.
 *
 * İndeks nömrələri [DuaPreferences.VIEW_MODE]-un dəyərləridir (0 qarışıq, 1 ərəbcə, 2 tərcümə),
 * ona görə siyahının sırası rejim zolağının sırasından ayrıla bilməz.
 */
private val duaDefaultModeOptions: List<Pair<Int, StringResource>> = listOf(
    0 to Res.string.strLabelMixed,
    1 to Res.string.arabicLabel,
    2 to Res.string.labelTranslation,
)

/** Seçilmiş açılış rejiminin ayarlar sətrində göstərilən adı. */
@Composable
fun duaDefaultModeLabel(mode: Int): String = stringResource(
    duaDefaultModeOptions.firstOrNull { it.first == mode }?.second ?: Res.string.modeLastUsed,
)

@Composable
private fun DuaDefaultModeSheet(isOpen: Boolean, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val selected = DuaPreferences.observeDefaultViewMode()

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss,
        icon = Res.drawable.ic_mode_verse,
        title = stringResource(Res.string.defaultReadingMode),
    ) {
        // Konkret rejimlər əvvəldədir; «Sonuncu istifadə olunan» sonda qalır — seçim deyil,
        // seçimdən imtinadır (hədis və Quran vərəqləri ilə eyni sıra).
        Column(modifier = Modifier.padding(12.dp)) {
            duaDefaultModeOptions.forEach { (mode, label) ->
                RadioItem(
                    title = label,
                    selected = selected == mode,
                    onClick = {
                        onDismiss()
                        scope.launch {
                            DuaPreferences.setDefaultViewMode(mode)
                            // Ayarda rejim seçən adam onu elə indi də görmək istəyir.
                            DuaPreferences.setViewMode(mode)
                        }
                    },
                )
            }

            RadioItem(
                title = Res.string.modeLastUsed,
                subtitle = Res.string.defaultReadingModeDesc,
                selected = selected == DuaPreferences.VIEW_MODE_LAST_USED,
                onClick = {
                    onDismiss()
                    scope.launch {
                        DuaPreferences.setDefaultViewMode(DuaPreferences.VIEW_MODE_LAST_USED)
                    }
                },
            )
        }
    }
}

/** «Bismillah» — ölçü önbaxışı üçün, hədis vərəqindəki ilə eyni mətn. */
private const val ARABIC_PREVIEW = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
