package com.cafarovceyxun.anamuslim.compose.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.theme.tightTextStyle
import com.cafarovceyxun.anamuslim.compose.components.settings.DailyReminderSheet
import com.cafarovceyxun.anamuslim.compose.components.homepage.LocalHomeActions
import com.cafarovceyxun.anamuslim.compose.utils.screenHeightDp
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dailyReminderMsg
import com.cafarovceyxun.anamuslim.resources.dr_icon_heart_filled
import com.cafarovceyxun.anamuslim.resources.ic_bell
import com.cafarovceyxun.anamuslim.resources.ic_bell_ring
import com.cafarovceyxun.anamuslim.resources.ic_bookmark
import com.cafarovceyxun.anamuslim.resources.ic_bookmark_added
import com.cafarovceyxun.anamuslim.resources.ic_pause
import com.cafarovceyxun.anamuslim.resources.ic_play
import com.cafarovceyxun.anamuslim.resources.strLabelBookmark
import com.cafarovceyxun.anamuslim.resources.strLabelPause
import com.cafarovceyxun.anamuslim.resources.strLabelPlay
import com.cafarovceyxun.anamuslim.resources.strLabelRead
import com.cafarovceyxun.anamuslim.resources.strLabelVerseSerialWithChapter
import com.cafarovceyxun.anamuslim.resources.strTitleDailyHadith
import com.cafarovceyxun.anamuslim.resources.strTitleVOTD
import com.cafarovceyxun.anamuslim.utils.mediaplayer.WbwAudioProvider
import com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.cafarovceyxun.anamuslim.compose.utils.preferences.DataStoreManager
import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.compose.components.common.IconButton
import com.cafarovceyxun.anamuslim.compose.components.common.Loader
import com.cafarovceyxun.anamuslim.compose.components.reader.LocalRecitation
import com.cafarovceyxun.anamuslim.compose.components.reader.QuranTextWbw
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderLayoutItem
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderProvider
import com.cafarovceyxun.anamuslim.compose.components.reader.TextStyleProvider
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.LocalAppLocale
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.compose.theme.hadithArabicFontFamily
import com.cafarovceyxun.anamuslim.compose.screens.hadith.withScriptDirection
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HadithPreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.VersePreferences
import com.cafarovceyxun.anamuslim.utils.reader.LocalVerseActions
import com.cafarovceyxun.anamuslim.utils.reader.QuranTextStyleParams
import com.cafarovceyxun.anamuslim.utils.reader.VerseActions
import com.cafarovceyxun.anamuslim.utils.reader.atlas.QuranAtlasLoader
import com.cafarovceyxun.anamuslim.utils.reader.buildTranslationAnnotatedString
import com.cafarovceyxun.anamuslim.utils.reader.factory.QuranTranslationFactory
import com.cafarovceyxun.anamuslim.utils.reader.TranslationTextStyleParams
import com.cafarovceyxun.anamuslim.utils.reader.getQuranTextStyle
import com.cafarovceyxun.anamuslim.utils.reader.getTranslationTextStyle
import com.cafarovceyxun.anamuslim.utils.reader.isQuranAtlasScript
import com.cafarovceyxun.anamuslim.utils.verse.VerseUtils
import com.cafarovceyxun.anamuslim.viewModels.DailyContentViewModel
import com.cafarovceyxun.anamuslim.viewModels.ReaderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * Ərəb mətni tərcümənin altında ikinci dərəcəli sətir kimi durur, ona görə istifadəçinin seçdiyi
 * ölçüyə bu əmsal vurulur — tənzimləmə işləməyə davam edir, sadəcə iyerarxiya qorunur.
 */
private const val ARABIC_SECONDARY_SCALE = 0.85f

/**
 * Hədis mətninin baza ölçüləri — hədis oxucusundakı qiymətlərlə eyni saxlanılır
 * (`HadithItemsScreen` / `HadithSettingsSheet`), ki kart və oxucu eyni ayarda eyni görünsün.
 */
private val HADITH_AZ_BASE_SP = 16.sp
private val HADITH_AR_BASE_SP = 24.sp

@Composable
fun VerseOfTheDay() {
    // Kart opsionaldır (Ayarlar → Günün ayəsi kartı) və default açıqdır. Söndürüləndə heç nə
    // qurulmur — nə ViewModel, nə də Supabase sorğusu.
    if (!VersePreferences.observeVOTDCardEnabled()) return

    val dailyVm = viewModel { DailyContentViewModel() }
    val dailyContent by dailyVm.todayContent.collectAsStateWithLifecycle()

    // If there is no manual content (dailyContent is null), don't show the section.
    if (dailyContent == null) return

    ReaderProvider {
        HomePremiumBannerContainer {
            VotdContent(
                dailyContent = dailyContent,
                header = {
                    val title = if (dailyContent?.content_type == "hadith") {
                        stringResource(Res.string.strTitleDailyHadith)
                    } else {
                        stringResource(Res.string.strTitleVOTD)
                    }
                    HomePremiumHeaderPill(
                        icon = Res.drawable.dr_icon_heart_filled,
                        title = title
                    )
                }
            )
        }
    }
}

@Composable
internal fun VotdContent(
    dailyContent: com.cafarovceyxun.anamuslim.utils.supabase.DailyContent?,
    header: @Composable () -> Unit
) {
    val colors by rememberUpdatedState(MaterialTheme.colorScheme)
    val type by rememberUpdatedState(MaterialTheme.typography)
    val appLocale = LocalAppLocale.current

    val vm = viewModel { ReaderViewModel() }
    val verseActions = LocalVerseActions.current

    val prefs by DataStoreManager.flowMultiple(
        ReaderPreferences.KEY_ARABIC_TEXT_ENABLED,
        ReaderPreferences.KEY_TEXT_SIZE_MULT_ARABIC,
        ReaderPreferences.KEY_TEXT_SIZE_MULT_TRANSL,
        ReaderPreferences.KEY_SCRIPT,
        ReaderPreferences.KEY_TRANSLATIONS,
        ReaderPreferences.KEY_TRANSL_HIGHLIGHT_PARENTHESES,
        ReaderPreferences.KEY_TRANSL_SHOW_PARENTHESES,
        VersePreferences.KEY_VOTD_REMINDER_ENABLED,
    ).collectAsStateWithLifecycle(null)

    val preferences = prefs ?: return

    val arabicEnabled = preferences.get(ReaderPreferences.KEY_ARABIC_TEXT_ENABLED)
    val arabicTextMultiplier = preferences.get(ReaderPreferences.KEY_TEXT_SIZE_MULT_ARABIC)
    val translationTextMultiplier = preferences.get(ReaderPreferences.KEY_TEXT_SIZE_MULT_TRANSL)
    val scriptCode = preferences.get(ReaderPreferences.KEY_SCRIPT)
    val translationSlugs = preferences.get(ReaderPreferences.KEY_TRANSLATIONS)
    val highlightParentheses = preferences.get(ReaderPreferences.KEY_TRANSL_HIGHLIGHT_PARENTHESES)
    val showParentheses = preferences.get(ReaderPreferences.KEY_TRANSL_SHOW_PARENTHESES)
    val votdEnabled = preferences.get(VersePreferences.KEY_VOTD_REMINDER_ENABLED)

    var showDailyReminderSheet by rememberSaveable { mutableStateOf(false) }

    DailyReminderSheet(
        isOpen = showDailyReminderSheet,
        onClose = { showDailyReminderSheet = false },
    )

    val state by produceState<ReaderLayoutItem.VerseUI?>(
        initialValue = null,
        dailyContent,
        scriptCode,
        translationSlugs,
        highlightParentheses,
        showParentheses,
        appLocale,
        colors,
        type,
    ) {
        val translationFactory = QuranTranslationFactory()

        value = withContext(Dispatchers.IO) {
            val verse = if (dailyContent != null && dailyContent.content_type == "verse") {
                vm.repository.getVerseWithDetails(
                    dailyContent.chapter_no!!,
                    dailyContent.verse_no!!,
                    arabicEnabled = true
                )
            } else {
                VerseUtils.getVOTD()
            } ?: return@withContext null

            val slugs = setOf(ReaderPreferences.primaryTranslationSlug())

            val translations = translationFactory.getTranslationsSingleVerse(
                slugs,
                verse.chapterNo,
                verse.verseNo
            )

            val booksInfo = translationFactory.getTranslationBooksInfoValidated(slugs)

            val atlasBundle = if (scriptCode.isQuranAtlasScript()) {
                QuranAtlasLoader.getBundle(vm.externalQuranDb, scriptCode)
            } else null

            val parsedTranslationTexts = translations.mapNotNull { translation ->
                val bookInfo = booksInfo[translation.bookSlug] ?: return@mapNotNull null

                val text = buildTranslationAnnotatedString(
                    translation,
                    colors,
                    actions = VerseActions(verseActions.onReferenceClick),
                    highlightParentheses = highlightParentheses,
                    showParentheses = showParentheses,
                )

                ReaderLayoutItem.TranslationUI(
                    slug = translation.bookSlug,
                    langCode = bookInfo.langCode,
                    text = text,
                    rawText = translation.text
                )
            }

            ReaderLayoutItem.VerseUI(
                verse = verse,
                atlasPlacements = atlasBundle?.getPlacementsForWords(verse.words, verse.pageNo) ?: emptyMap(),
                parsedTranslationTexts = parsedTranslationTexts,
                wbwByWordIndex = null,
                showDivider = false,
                key = "verse-votd"
            )
        }
    }

    val verseUi = state

    if (dailyContent != null && dailyContent.content_type == "hadith") {
        VotdHadithContent(dailyContent, header, votdEnabled, onReminderClick = { showDailyReminderSheet = true })
        return
    }

    if (verseUi == null) {
        VerseOfTheDayLoading()
        return
    }

    val verse = verseUi.verse
    val isBookmarked by vm.userRepository.isBookmarkedFlow(
        verse.chapterNo,
        verse.verseNo..verse.verseNo
    ).collectAsStateWithLifecycle(false)

    val isDarkTheme = ThemeUtils.observeDarkTheme()
    val contentColor = homePremiumContentColor()
    val iconTint = contentColor.alpha(0.7f)

    val recState = LocalRecitation.current
    val isVersePlaying = recState.isAnyPlaying && recState.playingVerse.doesEqual(verse)

    LaunchedEffect(verse) {
        WbwAudioProvider.warmUp()
    }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            header()
        }

        val screenHeight = screenHeightDp()
        val maxHeight = (screenHeight * 0.25f).coerceAtLeast(180.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .verticalScroll(rememberScrollState())
        ) {
            // Tərcümə öndədir: kart açılan kimi oxunan mətn budur. Ərəbcə altda, orijinal mətn
            // rolunda qalır — klassik tərcümə nəşrlərinin düzülüşü.
            val translation = verseUi.parsedTranslationTexts
                .firstOrNull()
                ?.takeIf { it.text.isNotBlank() }
            val hasTranslation = translation != null

            if (translation != null) {
                val translationStyle = remember(type, translation.langCode, translationTextMultiplier) {
                    getTranslationTextStyle(
                        TranslationTextStyleParams(
                            // `slug` RTL yoxlaması üçün dil kodu gözləyir (bax
                            // `getTranslationTextStyle` → `StringUtils.isRtlLanguage`).
                            slug = translation.langCode,
                            sizeMultiplier = translationTextMultiplier,
                        )
                    )
                }

                SelectionContainer {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        text = translation.text,
                        style = translationStyle,
                        color = contentColor,
                        textAlign = TextAlign.Start,
                    )
                }
            }

            if (arabicEnabled) {
                if (hasTranslation) {
                    VotdSectionDivider(contentColor, horizontalPadding = 20.dp)
                }

                val quranTextStyle = remember(
                    colors,
                    type,
                    scriptCode,
                    verse.pageNo,
                    arabicTextMultiplier,
                    isDarkTheme,
                    contentColor,
                    hasTranslation,
                ) {
                    getQuranTextStyle(
                        QuranTextStyleParams(
                            fontResolver = vm.fontResolver,
                            colors = colors,
                            type = type,
                            pageNo = verse.pageNo,
                            script = scriptCode,
                            // Tərcümə yoxdursa (məsələn heç bir tərcümə endirilməyib) ərəbcə
                            // kartın yeganə mətnidir — o zaman ikinci dərəcəli görünməməlidir.
                            sizeMultiplier = if (hasTranslation) {
                                arabicTextMultiplier * ARABIC_SECONDARY_SCALE
                            } else arabicTextMultiplier,
                            useSmallSize = true,
                            isDark = isDarkTheme
                        )
                    ).copy(
                        color = if (hasTranslation) contentColor.alpha(0.75f) else contentColor
                    )
                }

                TextStyleProvider(
                    mapOf(
                        verse.pageNo to quranTextStyle
                    )
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        QuranTextWbw(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verseUi = verseUi,
                            horizontalArrangement = Arrangement.Center,
                        )
                    }
                }
            }

            VotdReferenceLine(
                text = stringResource(
                    Res.string.strLabelVerseSerialWithChapter,
                    verse.chapter.getCurrentName(),
                    verse.chapterNo,
                    verse.verseNo,
                ),
                contentColor = contentColor,
                horizontalPadding = 20.dp,
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = contentColor.alpha(0.2f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(
                painter = if (isVersePlaying) painterResource(Res.drawable.ic_pause)
                else painterResource(Res.drawable.ic_play),
                contentDescription = if (isVersePlaying) stringResource(Res.string.strLabelPause)
                else stringResource(Res.string.strLabelPlay),
                tint = if (isVersePlaying) colorScheme.primary else iconTint,
                small = true,
                onClick = { recState.controller.playControl(ChapterVersePair(verse)) }
            )

            IconButton(
                painter = painterResource(if (isBookmarked) Res.drawable.ic_bookmark_added else Res.drawable.ic_bookmark),
                contentDescription = stringResource(Res.string.strLabelBookmark),
                tint = if (isBookmarked) colorScheme.primary else iconTint,
                small = true,
                onClick = {
                    verseActions.onBookmarkRequest?.invoke(
                        verseUi.verse.chapterNo,
                        verseUi.verse.verseNo..verseUi.verse.verseNo
                    )
                }
            )

            IconButton(
                painter = painterResource(if (votdEnabled) Res.drawable.ic_bell_ring else Res.drawable.ic_bell),
                contentDescription = stringResource(Res.string.dailyReminderMsg),
                tint = if (votdEnabled) colorScheme.primary else iconTint,
                small = true,
                onClick = { showDailyReminderSheet = true }
            )

            Spacer(Modifier.weight(1f))

            FilledTonalButton(
                modifier = Modifier.height(28.dp),
                onClick = { ReaderUiHooks.openVerse?.invoke(verse.chapterNo, verse.verseNo) },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
            ) {
                Text(
                    stringResource(Res.string.strLabelRead),
                    style = typography.labelMedium
                )
            }
        }
    }
}

@Composable
internal fun VotdHadithContent(
    dailyContent: com.cafarovceyxun.anamuslim.utils.supabase.DailyContent,
    header: @Composable () -> Unit,
    votdEnabled: Boolean,
    onReminderClick: () -> Unit
) {
    val homeActions = LocalHomeActions.current
    val contentColor = homePremiumContentColor()
    val iconTint = contentColor.alpha(0.7f)

    // Hədis oxucusunun tənzimləmələri burada da hörmət olunur — əvvəl kart onları heç oxumurdu.
    val arabicEnabled = HadithPreferences.observeArabicEnabled()
    val arabicSizeMultiplier = HadithPreferences.observeArabicSizeMultiplier()
    val azerbaijaniSizeMultiplier = HadithPreferences.observeAzerbaijaniSizeMultiplier()
    val sourceEnabled = HadithPreferences.observeSourceEnabled()
    val arabicFont = hadithArabicFontFamily(HadithPreferences.observeArabicFont())

    val hasArabic = arabicEnabled && dailyContent.text_ar.isNotBlank()
    val hasTranslation = dailyContent.text_az.isNotBlank()

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            header()
        }

        val screenHeight = screenHeightDp()
        val maxHeight = (screenHeight * 0.25f).coerceAtLeast(180.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp, horizontal = 20.dp)
        ) {
            if (hasTranslation) {
                // Hədis məzmunu interfeys dilindən asılı olmayaraq azərbaycancadır: istiqaməti
                // düzülüşdən yox, öz yazısından alır, yoxsa ərəbcə interfeysdə güzgülənir.
                SelectionContainer {
                    Text(
                        text = dailyContent.text_az,
                        // Ölçü və sətir hündürlüyü hədis oxucusundakı ilə eyni qaydadan gəlir
                        // (16.sp baza, ×1.5 sətir) — yalnız fontSize-ı əmsallayıb lineHeight-ı
                        // olduğu kimi buraxmaq 0.5× ayarında mətni 8sp/24sp-yə salır.
                        style = typography.bodyLarge
                            .copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = HADITH_AZ_BASE_SP * azerbaijaniSizeMultiplier,
                                lineHeight = HADITH_AZ_BASE_SP * azerbaijaniSizeMultiplier * 1.5f,
                            )
                            .withScriptDirection(arabic = false),
                        color = contentColor,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (hasArabic) {
                if (hasTranslation) {
                    VotdSectionDivider(contentColor)
                }

                val arabicFontSize = HADITH_AR_BASE_SP * arabicSizeMultiplier *
                    if (hasTranslation) ARABIC_SECONDARY_SCALE else 1f

                SelectionContainer {
                    Text(
                        text = dailyContent.text_ar,
                        style = typography.titleMedium
                            .copy(
                                fontWeight = FontWeight.Normal,
                                // Tərcümə yoxdursa ərəbcə yeganə mətndir — endirilmir.
                                fontSize = arabicFontSize,
                                lineHeight = arabicFontSize * 1.6f,
                            )
                            .withScriptDirection(arabic = true, arabicFontFamily = arabicFont),
                        color = if (hasTranslation) contentColor.alpha(0.75f) else contentColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (sourceEnabled && !dailyContent.source.isNullOrBlank()) {
                VotdReferenceLine(
                    text = dailyContent.source,
                    contentColor = contentColor,
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = contentColor.alpha(0.2f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(
                painter = painterResource(if (votdEnabled) Res.drawable.ic_bell_ring else Res.drawable.ic_bell),
                contentDescription = stringResource(Res.string.dailyReminderMsg),
                tint = if (votdEnabled) colorScheme.primary else iconTint,
                small = true,
                onClick = onReminderClick
            )

            Spacer(Modifier.weight(1f))

            FilledTonalButton(
                modifier = Modifier.height(28.dp),
                onClick = { homeActions.onOpenHadithById(dailyContent.hadith_id) },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
            ) {
                Text(
                    stringResource(Res.string.strLabelRead),
                    style = typography.labelMedium
                )
            }
        }
    }
}

/**
 * Tərcümə ilə orijinal mətn arasındakı incə ayırıcı. Kartın altındakı əməliyyat ayırıcısından
 * qəsdən sönükdür — iki səviyyə eyni ağırlıqda olsa iyerarxiya itir.
 */
@Composable
private fun VotdSectionDivider(contentColor: Color, horizontalPadding: Dp = 0.dp) {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 12.dp),
        color = contentColor.alpha(0.12f)
    )
}

/**
 * Mətnin altındakı istinad: ayə üçün «Surə 2:255», hədis üçün mənbə. Hər ikisi azərbaycanca yazıdır,
 * ona görə istiqaməti düzülüşdən yox, öz yazısından alır.
 */
@Composable
private fun VotdReferenceLine(text: String, contentColor: Color, horizontalPadding: Dp = 0.dp) {
    Text(
        text = text,
        style = typography.labelSmall.withScriptDirection(arabic = false),
        color = contentColor.alpha(0.6f),
        textAlign = TextAlign.End,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = horizontalPadding, end = horizontalPadding, top = 12.dp)
    )
}

/**
 * Content color for the home banner: it is a near-black card in dark theme and a light tinted one in
 * light theme, so everything drawn on it follows the theme instead of being hardcoded white.
 */
@Composable
internal fun homePremiumContentColor(): Color =
    if (ThemeUtils.observeDarkTheme()) Color.White else colorScheme.onSurface

@Composable
internal fun HomePremiumBannerContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = colorScheme
    val isDark = ThemeUtils.observeDarkTheme()

    val gradient = remember(colors, isDark) {
        val edge = if (isDark) Color.Black else colors.surface
        Brush.linearGradient(
            colors = listOf(
                edge,
                colors.primary,
                edge,
            )
        )
    }
    // The scrim keeps the primary band a faint tint rather than a stripe; it matches the card, so it
    // is dark on a dark card and light on a light one.
    val scrim = if (isDark) Color.Black.alpha(0.8f) else colors.surface.alpha(0.85f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(shapes.medium)
                .animateContentSize()
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(gradient)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(scrim)
            )

            content()
        }
    }
}

@Composable
internal fun HomePremiumHeaderPill(
    modifier: Modifier = Modifier,
    icon: DrawableResource? = null,
    title: String,
    isSelected: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val alpha = if (isSelected) 0.12f else 0f
    val contentAlpha = if (isSelected) 1f else 0.6f
    val contentColor = homePremiumContentColor()

    Surface(
        modifier = modifier.clip(RoundedCornerShape(999.dp)),
        shape = RoundedCornerShape(999.dp),
        color = contentColor.copy(alpha = alpha),
        onClick = onClick ?: {},
        enabled = onClick != null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = contentColor.copy(alpha = contentAlpha),
                    modifier = Modifier.size(16.dp),
                )
            }

            Text(
                text = title,
                style = typography.titleSmall.merge(tightTextStyle),
                fontWeight = FontWeight.SemiBold,
                color = contentColor.copy(alpha = contentAlpha),
            )
        }
    }
}


@Composable
private fun VerseOfTheDayLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Loader()
    }
}
