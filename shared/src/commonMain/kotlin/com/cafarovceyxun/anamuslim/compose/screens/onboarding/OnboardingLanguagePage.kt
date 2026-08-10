package com.cafarovceyxun.anamuslim.compose.screens.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.RadioItem
import com.cafarovceyxun.anamuslim.compose.screens.settings.NumeralSystemChipRow
import com.cafarovceyxun.anamuslim.compose.utils.NumeralSystem
import com.cafarovceyxun.anamuslim.compose.utils.APP_LOCALE_DEFAULT
import com.cafarovceyxun.anamuslim.compose.utils.appLanguages
import com.cafarovceyxun.anamuslim.compose.utils.appLocale
import com.cafarovceyxun.anamuslim.compose.utils.applyAppLanguage
import com.cafarovceyxun.anamuslim.compose.utils.languageSubtagOf
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strLabelSystemDefault
import org.jetbrains.compose.resources.stringResource
import com.cafarovceyxun.anamuslim.compose.utils.normalizedLanguageTag
import com.cafarovceyxun.anamuslim.compose.utils.numeralSystemsForLanguage
import com.cafarovceyxun.anamuslim.compose.extensions.verticalFadingEdge

@Composable
fun OnboardingLanguagePage() {
    val systemDefaultName = stringResource(Res.string.strLabelSystemDefault)
    var committed by remember {
        mutableStateOf(appLocale().rawLanguageTag to appLocale().numeralSystem)
    }
    val listState = rememberLazyListState()

    fun save(selectedTag: String, selectedNumeral: NumeralSystem?) {
        applyAppLanguage(selectedTag, selectedNumeral)
        // Re-read the applied value: the platform coerces the numeral system to the language.
        committed = appLocale().rawLanguageTag to appLocale().numeralSystem
    }

    val selectedTag = committed.first
    val selectedNumeral = committed.second

    Box(
        Modifier.verticalFadingEdge(listState)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 8.dp,
                end = 8.dp,
                top = 8.dp,
                bottom = 24.dp
            ),
        ) {
            itemsIndexed(appLanguages) { _, language ->
                val v = language.rawLanguageTag
                val isSelected = v == selectedTag
                val numeralItems = numeralSystemsForLanguage(languageSubtagOf(v))

                Column(Modifier.fillMaxWidth()) {
                    RadioItem(
                        titleStr = language.endonym ?: systemDefaultName,
                        selected = isSelected,
                        onClick = {
                            if (v != selectedTag) {
                                save(v, numeralItems.firstOrNull()?.first)
                            }
                        },
                    )

                    if (isSelected && numeralItems.isNotEmpty()) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                        ) {
                            NumeralSystemChipRow(
                                numeralItems = numeralItems,
                                selected = selectedNumeral,
                                onSelect = { save(selectedTag, it) },
                            )
                        }
                    }
                }
            }
        }
    }
}
