package com.cafarovceyxun.anamuslim.compose.screens.hadith

import com.cafarovceyxun.anamuslim.resources.dr_icon_quran_script
import com.cafarovceyxun.anamuslim.resources.Res
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.RadioItem
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheet
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HadithPreferences
import com.cafarovceyxun.anamuslim.resources.strTitleScripts
import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptUtils
import com.cafarovceyxun.anamuslim.utils.reader.getQuranScriptName
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun HadithFontSelectorSheet(isOpen: Boolean, onDismiss: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val selectedFont = HadithPreferences.observeArabicFont()

    val fonts = listOf(
        QuranScriptUtils.SCRIPT_UTHMANI,
        QuranScriptUtils.SCRIPT_PDMS_ISLAMIC,
    )

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss,
        icon = Res.drawable.dr_icon_quran_script,
        title = stringResource(Res.string.strTitleScripts),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            fonts.forEach { font ->
                RadioItem(
                    titleStr = font.getQuranScriptName(),
                    selected = selectedFont == font,
                    onClick = {
                        onDismiss()
                        coroutineScope.launch {
                            HadithPreferences.setArabicFont(font)
                        }
                    }
                )
            }
        }
    }
}
