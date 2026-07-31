package com.cafarovceyxun.anamuslim.compose.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavigationOuterHeight
import com.cafarovceyxun.anamuslim.api.ApiConfig
import com.cafarovceyxun.anamuslim.compose.components.common.AlertCard
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.Chip
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.LocalAppLocale
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.compose.utils.NumeralSystem
import com.cafarovceyxun.anamuslim.compose.utils.appLanguages
import com.cafarovceyxun.anamuslim.compose.utils.appLocale
import com.cafarovceyxun.anamuslim.compose.utils.applyAppLanguage
import com.cafarovceyxun.anamuslim.compose.utils.languageSubtagOf
import com.cafarovceyxun.anamuslim.compose.utils.localizedLanguageName
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.learnMore
import com.cafarovceyxun.anamuslim.resources.strLabelSystemDefault
import com.cafarovceyxun.anamuslim.resources.strTitleAppLanguage
import com.cafarovceyxun.anamuslim.resources.translationHelp
import org.jetbrains.compose.resources.stringResource
import com.cafarovceyxun.anamuslim.compose.utils.normalizedLanguageTag
import com.cafarovceyxun.anamuslim.compose.utils.numeralSystemsForLanguage

data class LanguageModel(
    val rawLanguageTag: String,
    val language: String,
    val localizedName: String,
    val nativeName: String,
)

@Composable
fun LanguageSelectionScreen() {
    val appLocaleTag = LocalAppLocale.current.languageTag
    val systemDefaultName = stringResource(Res.string.strLabelSystemDefault)

    val languages = remember(appLocaleTag, systemDefaultName) {
        appLanguages.map { language ->
            val nativeName = language.endonym ?: systemDefaultName
            val localizedName = localizedLanguageName(language.rawLanguageTag, appLocaleTag)
                ?.replaceFirstChar { it.uppercase() }
                ?: nativeName

            LanguageModel(
                language.rawLanguageTag,
                languageSubtagOf(language.rawLanguageTag),
                localizedName,
                nativeName,
            )
        }
    }

    var committed by remember {
        mutableStateOf(appLocale().rawLanguageTag to appLocale().numeralSystem)
    }

    var searchQuery by remember { mutableStateOf("") }

    val filteredLanguages by remember(searchQuery, languages) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                languages
            } else {
                val query = searchQuery.trim().lowercase()
                languages.filter {
                    it.localizedName.lowercase().contains(query) ||
                            it.nativeName.lowercase().contains(query) ||
                            it.rawLanguageTag.lowercase().contains(query)
                }
            }
        }
    }

    fun save(selectedTag: String, selectedNumeral: NumeralSystem?) {
        applyAppLanguage(selectedTag, selectedNumeral)
        // Re-read the applied value: the platform coerces the numeral system to the language.
        committed = appLocale().rawLanguageTag to appLocale().numeralSystem
    }

    val selectedTag = committed.first
    val selectedNumeral = committed.second

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            AppBar(
                stringResource(Res.string.strTitleAppLanguage),
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            contentPadding = PaddingValues(
                bottom = mainBottomNavigationOuterHeight() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                AlertCard(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.translationHelp),
                            style = typography.bodyMedium
                        )

                        Text(
                            stringResource(Res.string.learnMore),
                            modifier = Modifier.clickable {
                                PlatformUtils.browseLink(ApiConfig.GITHUB_REPOSITORY_URL)
                            },
                            style = typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = colorScheme.primary
                        )
                    }
                }
            }

            if (filteredLanguages.isNotEmpty()) {
                items(filteredLanguages, key = { it.rawLanguageTag }) { model ->
                    val isSelected = model.rawLanguageTag == selectedTag
                    val numeralItems = numeralSystemsForLanguage(model.language)

                    Column(Modifier.fillMaxWidth()) {
                        LanguageItem(
                            language = model,
                            isSelected = isSelected,
                            onSelect = {
                                save(model.rawLanguageTag, numeralItems.firstOrNull()?.first)
                            },
                        )

                        if (isSelected && numeralItems.isNotEmpty()) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 60.dp, end = 24.dp, bottom = 12.dp),
                            ) {
                                NumeralSystemChipRow(
                                    numeralItems = numeralItems,
                                    selected = selectedNumeral,
                                    onSelect = {
                                        save(selectedTag, it)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageItem(
    language: LanguageModel,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onSelect
            )
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = colorScheme.primary,
                unselectedColor = colorScheme.onSurfaceVariant.alpha(0.4f)
            ),
            modifier = Modifier.size(20.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = language.localizedName,
                style = typography.labelLarge,
                color = if (isSelected) colorScheme.primary else colorScheme.onSurface
            )

            if (language.nativeName != language.localizedName) {
                Text(
                    text = language.nativeName,
                    style = typography.bodyMedium,
                    color = (if (isSelected) colorScheme.primary else colorScheme.onSurface)
                        .alpha(0.7f),
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}


@Composable
fun NumeralSystemChipRow(
    numeralItems: List<Pair<NumeralSystem, String>>,
    selected: NumeralSystem?,
    onSelect: (NumeralSystem) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        numeralItems.forEach { (system, name) ->
            Chip(
                selected = selected == system,
                label = { Text(name) },
                onClick = { onSelect(system) },
            )
        }
    }
}
