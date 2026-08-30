package com.cafarovceyxun.anamuslim.compose.components.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.RadioItem
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheet
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderMode
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.defaultReadingMode
import com.cafarovceyxun.anamuslim.resources.defaultReadingModeDesc
import com.cafarovceyxun.anamuslim.resources.ic_mode_verse
import com.cafarovceyxun.anamuslim.resources.labelTranslation
import com.cafarovceyxun.anamuslim.resources.modeLastUsed
import com.cafarovceyxun.anamuslim.resources.modeMushaf
import com.cafarovceyxun.anamuslim.resources.modeVerseByVerse
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Oxucunun açılış rejimləri — app bar-dakı rejim tabları ilə eyni üçlük.
 *
 * [ReaderMode.TranslationVertical] qəsdən yoxdur: o, tərcümə rejiminin istiqaməti olduğu üçün
 * tablarda da ayrıca seçim deyil, [ReaderMode.Translation] seqmentini işıqlandırır.
 */
private val readerDefaultModeOptions: List<Pair<ReaderMode, StringResource>> = listOf(
    ReaderMode.VerseByVerse to Res.string.modeVerseByVerse,
    ReaderMode.Reading to Res.string.modeMushaf,
    ReaderMode.Translation to Res.string.labelTranslation,
)

/** Seçilmiş açılış rejiminin ayarlar sətrində göstərilən adı; `null` — «sonuncu istifadə olunan». */
@Composable
fun readerDefaultModeLabel(mode: ReaderMode?): String = stringResource(
    readerDefaultModeOptions.firstOrNull { it.first == mode }?.second ?: Res.string.modeLastUsed
)

/**
 * Quran oxucusunun açılış rejimini seçən vərəq.
 *
 * Yazdığı açar [ReaderPreferences.KEY_DEFAULT_READER_MODE]-dur — oxuyarkən rejim tabının yazdığı
 * canlı açar deyil. İkisi ayrıdır ki, müshəfə bir dəfə baxmaq növbəti dəfə əlfəcindən girişi də
 * müshəfə çevirməsin; «sonuncu istifadə olunan» seçimi məhz həmin köhnə davranışdır.
 */
@Composable
fun ReaderDefaultModeSheet(isOpen: Boolean, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val selected = ReaderPreferences.observeDefaultReaderMode()

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss,
        icon = Res.drawable.ic_mode_verse,
        title = stringResource(Res.string.defaultReadingMode),
    ) {
        // Konkret rejimlər əvvəldədir və siyahı ayə-ayə ilə başlayır — o, oxucunun susmaya görə
        // açılış rejimidir ([ReaderPreferences.KEY_DEFAULT_READER_MODE]). «Sonuncu istifadə olunan»
        // sonda qalır: seçim deyil, seçimdən imtinadır.
        Column(modifier = Modifier.padding(12.dp)) {
            readerDefaultModeOptions.forEach { (mode, label) ->
                RadioItem(
                    title = label,
                    selected = selected == mode,
                    onClick = {
                        onDismiss()
                        scope.launch { ReaderPreferences.setDefaultReaderMode(mode) }
                    },
                )
            }

            RadioItem(
                title = Res.string.modeLastUsed,
                subtitle = Res.string.defaultReadingModeDesc,
                selected = selected == null,
                onClick = {
                    onDismiss()
                    scope.launch { ReaderPreferences.setDefaultReaderMode(null) }
                },
            )
        }
    }
}
