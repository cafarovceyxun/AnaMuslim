package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import androidx.compose.ui.text.font.FontFamily
import com.cafarovceyxun.anamuslim.compose.theme.hadithArabicFontFamily
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.add_sub_chapter
import com.cafarovceyxun.anamuslim.resources.arabic_text
import com.cafarovceyxun.anamuslim.resources.az_translation
import com.cafarovceyxun.anamuslim.resources.dr_icon_edit
import com.cafarovceyxun.anamuslim.resources.dr_icon_info
import com.cafarovceyxun.anamuslim.resources.dr_icon_quran_script
import com.cafarovceyxun.anamuslim.resources.dr_icon_read_quran
import com.cafarovceyxun.anamuslim.resources.dr_icon_refresh
import com.cafarovceyxun.anamuslim.resources.dr_icon_share
import com.cafarovceyxun.anamuslim.resources.dr_icon_translations
import com.cafarovceyxun.anamuslim.resources.hadith_number
import com.cafarovceyxun.anamuslim.resources.name_az
import com.cafarovceyxun.anamuslim.resources.order_no
import com.cafarovceyxun.anamuslim.resources.placeholder_book_name
import com.cafarovceyxun.anamuslim.resources.placeholder_hadith_ar
import com.cafarovceyxun.anamuslim.resources.placeholder_hadith_az
import com.cafarovceyxun.anamuslim.resources.placeholder_note
import com.cafarovceyxun.anamuslim.resources.placeholder_slug
import com.cafarovceyxun.anamuslim.resources.placeholder_source
import com.cafarovceyxun.anamuslim.resources.save
import com.cafarovceyxun.anamuslim.resources.slug_system_name
import com.cafarovceyxun.anamuslim.resources.source
import com.cafarovceyxun.anamuslim.resources.strLabelAdditionalInfo
import com.cafarovceyxun.anamuslim.resources.strLabelBasicInfo
import com.cafarovceyxun.anamuslim.resources.strLabelHadithInfo
import com.cafarovceyxun.anamuslim.resources.strLabelTexts
import com.cafarovceyxun.anamuslim.resources.strMsgFillAllFields
import com.cafarovceyxun.anamuslim.resources.strTitleAddBab
import com.cafarovceyxun.anamuslim.resources.strTitleAddBook
import com.cafarovceyxun.anamuslim.resources.strTitleAddHadith
import com.cafarovceyxun.anamuslim.resources.strTitleAddVolume
import com.cafarovceyxun.anamuslim.resources.strTitleEditHadith
import com.cafarovceyxun.anamuslim.resources.strTitleNote
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.cafarovceyxun.anamuslim.compose.components.common.Loader
import com.cafarovceyxun.anamuslim.compose.components.settings.ListItemCategoryLabel
import com.cafarovceyxun.anamuslim.utils.supabase.Hadith
import com.cafarovceyxun.anamuslim.utils.supabase.HadithBook
import com.cafarovceyxun.anamuslim.utils.supabase.HadithChapter
import com.cafarovceyxun.anamuslim.utils.supabase.HadithSubChapter
import com.cafarovceyxun.anamuslim.utils.supabase.HadithVolume
import com.cafarovceyxun.anamuslim.viewModels.HadithViewModel

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HadithPreferences

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavigationOuterHeight


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithEditorScreen(
    type: EditorType,
    initialHadith: Hadith? = null,
    volumeSlug: String? = null,
    bookSlug: String? = null,
    chapterSlug: String? = null,
    subChapterSlug: String? = null,
    reserveBottomSpace: Boolean = true,
    onBack: () -> Unit,
) {
    val viewModel = viewModel { HadithViewModel() }
    val isLoading by viewModel.isLoading.collectAsState()
    
    // val volumes by viewModel.volumes.collectAsState()
    // val books by viewModel.books.collectAsState()
    // val chapters by viewModel.chapters.collectAsState()
    // val subChapters by viewModel.subChapters.collectAsState()
    // val hadiths by viewModel.hadiths.collectAsState()
    
    var name by remember { mutableStateOf("") }
    var slugPart by remember { mutableStateOf("") }
    var no by remember { mutableStateOf(initialHadith?.hadith_no?.toString() ?: "") }
    
    var textAr by remember { mutableStateOf(initialHadith?.text_ar ?: "") }
    var textAz by remember { mutableStateOf(initialHadith?.text_az ?: "") }
    var source by remember { mutableStateOf(initialHadith?.source ?: "") }
    var note by remember { mutableStateOf(initialHadith?.note ?: "") }

    var showError by remember { mutableStateOf(value = false) }
    var isSlugManuallyEdited by remember { mutableStateOf(false) }

    val selectedFont = HadithPreferences.observeArabicFont()
    val arabicFontFamily = hadithArabicFontFamily(selectedFont)

    // Fetch next number on start
    LaunchedEffect(type, initialHadith, volumeSlug, bookSlug, chapterSlug, subChapterSlug) {
        if (initialHadith == null && (no.isEmpty() || no == "0")) {
            val next = viewModel.getNextNumber(type, volumeSlug, bookSlug, chapterSlug, subChapterSlug)
            no = next.toString()
        }
    }

    // Auto-slug logic
    LaunchedEffect(name, no, type, volumeSlug, bookSlug, chapterSlug) {
        if (!isSlugManuallyEdited) {
            val prefix = when (type) {
                EditorType.VOLUME -> "c"
                else -> {
                    val cleanName = name.trim().toSlugPart()
                    if (cleanName.length >= 2) cleanName.take(2)
                    else cleanName
                }
            }
            if (prefix.isNotEmpty() && no.isNotEmpty()) {
                val currentPart = prefix + no
                slugPart = when (type) {
                    EditorType.VOLUME -> currentPart
                    EditorType.BOOK -> (volumeSlug ?: "") + currentPart
                    EditorType.CHAPTER -> (bookSlug?.replace("/", "") ?: "") + currentPart
                    EditorType.SUB_CHAPTER -> (chapterSlug?.replace("/", "") ?: "") + currentPart
                    else -> slugPart
                }
            }
        }
    }

    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            AppBar(
                title = when {
                    initialHadith != null -> stringResource(Res.string.strTitleEditHadith)
                    type == EditorType.VOLUME -> stringResource(Res.string.strTitleAddVolume)
                    type == EditorType.BOOK -> stringResource(Res.string.strTitleAddBook)
                    type == EditorType.CHAPTER -> stringResource(Res.string.strTitleAddBab)
                    type == EditorType.SUB_CHAPTER -> stringResource(Res.string.add_sub_chapter)
                    type == EditorType.HADITH -> stringResource(Res.string.strTitleAddHadith)
                    else -> ""
                }
            )
        },
        bottomBar = {
            val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val bottomNavHeight = if (reserveBottomSpace) mainBottomNavigationOuterHeight() else navBarPadding
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.surface)
                    .padding(16.dp)
                    .padding(bottom = bottomNavHeight)
            ) {
                Button(
                    onClick = {
                        if (no.isEmpty() || (type != EditorType.HADITH && (name.isEmpty() || slugPart.isEmpty()))) {
                            showError = true
                            return@Button
                        }
                        
                        when(type) {
                            EditorType.VOLUME -> viewModel.upsertVolume(HadithVolume(slug = slugPart, name = name), onBack)
                            EditorType.BOOK -> {
                                viewModel.upsertBook(HadithBook(slug = slugPart, volume_slug = volumeSlug!!, book_no = no.toIntOrNull() ?: 0, name = name), onBack)
                            }
                            EditorType.CHAPTER -> {
                                viewModel.upsertChapter(HadithChapter(slug = slugPart, book_slug = bookSlug!!, chapter_no = no.toIntOrNull() ?: 0, name = name), onBack)
                            }
                            EditorType.SUB_CHAPTER -> {
                                viewModel.upsertSubChapter(HadithSubChapter(slug = slugPart, chapter_slug = chapterSlug!!, sub_chapter_no = no.toIntOrNull() ?: 0, name = name), onBack)
                            }
                            EditorType.HADITH -> {
                                viewModel.upsertHadith(
                                    Hadith(
                                        id = initialHadith?.id,
                                        chapter_slug = chapterSlug ?: initialHadith?.chapter_slug,
                                        sub_chapter_slug = subChapterSlug ?: initialHadith?.sub_chapter_slug,
                                        hadith_no = no.toIntOrNull() ?: 0,
                                        text_ar = textAr,
                                        text_az = textAz,
                                        source = source,
                                        note = note,
                                    ),
                                    onBack,
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        Loader(size = 24.dp)
                    } else {
                        Icon(painterResource(Res.drawable.dr_icon_refresh), null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(Res.string.save), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (type != EditorType.HADITH) {
                EditorSection(title = stringResource(Res.string.strLabelBasicInfo)) {
                    FormTextField(
                        value = name,
                        onValueChange = { name = it; showError = false },
                        label = stringResource(Res.string.name_az),
                        placeholder = stringResource(Res.string.placeholder_book_name),
                        icon = Res.drawable.dr_icon_edit,
                        error = showError && name.isEmpty(),
                        maxLines = 3 // Expandable
                    )
                    
                    FormTextField(
                        value = slugPart,
                        onValueChange = { 
                            slugPart = it
                            isSlugManuallyEdited = true
                            showError = false 
                        },
                        label = stringResource(Res.string.slug_system_name),
                        placeholder = stringResource(Res.string.placeholder_slug),
                        icon = Res.drawable.dr_icon_info,
                        error = showError && slugPart.isEmpty()
                    )

                    FormTextField(
                        value = no,
                        onValueChange = { no = it; showError = false },
                        label = stringResource(Res.string.order_no),
                        placeholder = "0",
                        icon = Res.drawable.dr_icon_quran_script,
                        keyboardType = KeyboardType.Number,
                        error = showError && no.isEmpty()
                    )
                }
            } else {
                EditorSection(title = stringResource(Res.string.strLabelHadithInfo)) {
                    FormTextField(
                        value = no,
                        onValueChange = { no = it; showError = false },
                        label = stringResource(Res.string.hadith_number),
                        placeholder = "0",
                        icon = Res.drawable.dr_icon_quran_script,
                        keyboardType = KeyboardType.Number,
                        error = showError && no.isEmpty()
                    )
                }

                EditorSection(title = stringResource(Res.string.strLabelTexts)) {
                    FormTextField(
                        value = textAr,
                        onValueChange = { textAr = it },
                        label = stringResource(Res.string.arabic_text),
                        placeholder = stringResource(Res.string.placeholder_hadith_ar),
                        minLines = 4,
                        maxLines = 15,
                        icon = Res.drawable.dr_icon_read_quran,
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            textDirection = TextDirection.Rtl,
                            fontFamily = arabicFontFamily,
                            fontSize = 20.sp
                        )
                    )
                    
                    FormTextField(
                        value = textAz,
                        onValueChange = { textAz = it },
                        label = stringResource(Res.string.az_translation),
                        placeholder = stringResource(Res.string.placeholder_hadith_az),
                        minLines = 4,
                        maxLines = 15,
                        icon = Res.drawable.dr_icon_translations
                    )
                }

                EditorSection(title = stringResource(Res.string.strLabelAdditionalInfo)) {
                    FormTextField(
                        value = source,
                        onValueChange = { source = it },
                        label = stringResource(Res.string.source),
                        placeholder = stringResource(Res.string.placeholder_source),
                        icon = Res.drawable.dr_icon_share,
                        maxLines = 3 // Expandable
                    )
                    
                    FormTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = stringResource(Res.string.strTitleNote),
                        placeholder = stringResource(Res.string.placeholder_note),
                        minLines = 2,
                        maxLines = 10,
                        icon = Res.drawable.dr_icon_info
                    )
                }
            }
            
            if (showError) {
                Text(
                    text = stringResource(Res.string.strMsgFillAllFields),
                    color = colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}

@Composable
fun EditorSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ListItemCategoryLabel(title = title)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    icon: DrawableResource,
    minLines: Int = 1,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    error: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = if (error) colorScheme.error else colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = textStyle,
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)) },
            leadingIcon = {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (error) colorScheme.error else colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            minLines = minLines,
            maxLines = if (maxLines > minLines) maxLines else minLines,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = if (minLines > 1 || maxLines > 1) ImeAction.Default else ImeAction.Next
            ),
            isError = error,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = colorScheme.surface,
                unfocusedContainerColor = colorScheme.surface,
                errorContainerColor = colorScheme.surface,
                focusedIndicatorColor = colorScheme.primary,
                unfocusedIndicatorColor = colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        )
    }
}

private fun String.toSlugPart(): String {
    val azToEn = mapOf(
        'ə' to "e", 'Ə' to "e",
        'ç' to "c", 'Ç' to "c",
        'ğ' to "g", 'Ğ' to "g",
        'ı' to "i", 'I' to "i",
        'i' to "i", 'İ' to "i",
        'ö' to "o", 'Ö' to "o",
        'ş' to "s", 'Ş' to "s",
        'ü' to "u", 'Ü' to "u"
    )
    
    val transliterated = this.map { azToEn[it] ?: it.lowercaseChar().toString() }.joinToString("")
    val slugified = transliterated.replace(" ", "-").filter { it in 'a'..'z' || it in '0'..'9' || it == '-' }
    return slugified.trim('-')
}

typealias ColumnScope = androidx.compose.foundation.layout.ColumnScope
