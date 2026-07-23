package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strLabelApply
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strTitleNote
import com.cafarovceyxun.anamuslim.resources.strTitleTranslations
import com.cafarovceyxun.anamuslim.utils.univ.StringUtils
import org.jetbrains.compose.resources.stringResource


@Composable
fun TranslationText(
    verseUi: ReaderLayoutItem.VerseUI,
) {
    val texts = verseUi.parsedTranslationTexts
    if (texts.isEmpty()) {
        return
    }

    val isDark = ThemeUtils.observeDarkTheme()
    val viewModel = LocalReaderViewModel.current
    val editingVerse by viewModel.editingVerse.collectAsStateWithLifecycle()
    val isEditing = editingVerse?.doesEqual(verseUi.verse) == true

    SelectionContainer {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            texts.forEach {
                val slug = it.slug
                val langCode = it.langCode
                val text = it.text
                val rawText = it.rawText
                val note = it.note

                if (isEditing) {
                    var textFieldValue by remember {
                        mutableStateOf(
                            TextFieldValue(
                                text = rawText,
                                selection = TextRange(0, rawText.length)
                            )
                        )
                    }

                    var noteFieldValue by remember {
                        mutableStateOf(
                            TextFieldValue(
                                text = note ?: "",
                                selection = TextRange(0, note?.length ?: 0)
                            )
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = textFieldValue,
                            onValueChange = { textFieldValue = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(Res.string.strTitleTranslations)) },
                            textStyle = TextStyle(
                                textDirection = if (StringUtils.isRtlLanguage(langCode)) TextDirection.Rtl else TextDirection.Ltr,
                                color = colorScheme.onBackground
                            )
                        )

                        OutlinedTextField(
                            value = noteFieldValue,
                            onValueChange = { noteFieldValue = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(Res.string.strTitleNote)) },
                            textStyle = TextStyle(
                                textDirection = if (StringUtils.isRtlLanguage(langCode)) TextDirection.Rtl else TextDirection.Ltr,
                                color = colorScheme.onBackground
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { viewModel.toggleEditing(null) }) {
                                Text(stringResource(Res.string.strLabelCancel))
                            }
                            TextButton(onClick = {
                                viewModel.saveTranslation(
                                    slug,
                                    verseUi.verse.chapterNo,
                                    verseUi.verse.verseNo,
                                    textFieldValue.text,
                                    noteFieldValue.text
                                )
                            }) {
                                Text(stringResource(Res.string.strLabelApply))
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text,
                            modifier = Modifier.fillMaxWidth(),
                            color = if (isDark) colorScheme.onBackground.alpha(0.8f) else colorScheme.onBackground,
                            style = TextStyle(
                                textDirection = if (StringUtils.isRtlLanguage(langCode)) TextDirection.Rtl else TextDirection.Ltr,
                            )
                        )

                            if (!note.isNullOrEmpty()) {
                                val noteFontSize = if (it.fontSize.isSpecified) {
                                    (it.fontSize.value - 2).sp
                                } else {
                                    MaterialTheme.typography.bodySmall.fontSize
                                }

                                val noteLabelColor = colorScheme.primaryContainer
                                val noteContentColor = colorScheme.primary
                                val noteLabel = stringResource(Res.string.strTitleNote)
                                val styledNote = buildAnnotatedString {
                                    withStyle(SpanStyle(color = noteLabelColor)) {
                                        append("$noteLabel: ")
                                    }
                                    withStyle(SpanStyle(color = noteContentColor)) {
                                        append(note)
                                    }
                                }

                                Text(
                                    styledNote,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = TextStyle(
                                        fontSize = noteFontSize,
                                        textDirection = if (StringUtils.isRtlLanguage(langCode)) TextDirection.Rtl else TextDirection.Ltr,
                                    )
                                )
                            }
                    }
                }
            }
        }
    }
}
