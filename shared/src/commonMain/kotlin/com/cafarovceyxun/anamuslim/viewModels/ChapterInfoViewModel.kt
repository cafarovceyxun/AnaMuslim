package com.cafarovceyxun.anamuslim.viewModels

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import com.cafarovceyxun.anamuslim.api.JsonHelper
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.utils.chapterInfo.ChapterInfoAssets
import com.cafarovceyxun.anamuslim.resources.strLabelSurah
import com.cafarovceyxun.anamuslim.resources.strMsgChapInfoFailedLoad
import com.cafarovceyxun.anamuslim.resources.strMsgSomethingWrong
import com.cafarovceyxun.anamuslim.resources.strTitleChapInfoChapterNo
import com.cafarovceyxun.anamuslim.resources.strTitleChapInfoJuzNo
import com.cafarovceyxun.anamuslim.resources.strTitleChapInfoRevOrder
import com.cafarovceyxun.anamuslim.resources.strTitleChapInfoRevType
import com.cafarovceyxun.anamuslim.resources.strTitleChapInfoRukus
import com.cafarovceyxun.anamuslim.resources.strTitleChapInfoVerses
import com.cafarovceyxun.anamuslim.resources.strTitleMadani
import com.cafarovceyxun.anamuslim.resources.strTitleMakki
import com.cafarovceyxun.anamuslim.utils.chapterInfo.ChapterInfoUtils
import com.cafarovceyxun.anamuslim.utils.network.isNetworkConnected
import com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem
import org.jetbrains.compose.resources.getString
import com.cafarovceyxun.anamuslim.api.AlfaazPlusApi
import com.cafarovceyxun.anamuslim.api.models.chapterInfo.ChapterInfoApiResponse
import com.cafarovceyxun.anamuslim.compose.theme.toCssHex
import com.cafarovceyxun.anamuslim.compose.theme.toCssRgba
import com.cafarovceyxun.anamuslim.db.entities.quran.RevelationType
import com.cafarovceyxun.anamuslim.db.relations.SurahWithLocalizations
import com.cafarovceyxun.anamuslim.utils.quran.QuranGlyphs
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import com.cafarovceyxun.anamuslim.utils.univ.StringUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString


sealed interface ChapterInfoContentState {
    object Loading : ChapterInfoContentState
    data class Success(val html: String, val langCode: String) : ChapterInfoContentState
    data class Error(val message: String, val canRetry: Boolean) : ChapterInfoContentState
    object NoInternet : ChapterInfoContentState
    object InvalidParams : ChapterInfoContentState
}

data class ChapterInfoUiState(
    val chapterNo: Int = 0,
    val swl: SurahWithLocalizations? = null,
    val juzNos: List<Int> = emptyList(),
    val contentState: ChapterInfoContentState = ChapterInfoContentState.Loading,
)

sealed interface ChapterInfoEvent {
    data class Init(val chapterNo: Int, val language: String?) : ChapterInfoEvent
    object Retry : ChapterInfoEvent
}

class ChapterInfoViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChapterInfoUiState())
    val uiState: StateFlow<ChapterInfoUiState> = _uiState.asStateFlow()

    private val repository get() = RepositoryProvider.quranRepository

    /** Cache file for one language's chapter-info payload (shared okio layer). */
    private fun chapterInfoFile(langCode: String, chapterNo: Int) =
        AppFileSystem.makeAndGetAppResourceDir(ChapterInfoUtils.DIR_NAME) /
            ChapterInfoUtils.prepareChapterInfoFilePath(langCode, chapterNo)

    private var contentLoadJob: Job? = null
    private val lastLangCode = mutableStateOf<String?>(null)

    fun onEvent(event: ChapterInfoEvent) {
        when (event) {
            is ChapterInfoEvent.Init -> {
                viewModelScope.launch {
                    initialize(event.chapterNo, event.language)
                }
            }

            is ChapterInfoEvent.Retry -> {
                val langCode = lastLangCode.value ?: return
                loadContent(langCode)
            }
        }
    }

    private suspend fun initialize(chapterNo: Int, language: String?) {
        if (!QuranMeta.isChapterValid(chapterNo)) {
            _uiState.update {
                it.copy(
                    chapterNo = chapterNo,
                    contentState = ChapterInfoContentState.InvalidParams
                )
            }
            return
        }

        val langCode = language?.takeIf { it.isNotEmpty() } ?: "en"

        val chapterMeta = withContext(Dispatchers.IO) {
            repository.getSurahWithLocalizations(chapterNo)
        }

        if (chapterMeta == null) {
            _uiState.update {
                it.copy(
                    chapterNo = chapterNo,
                    contentState = ChapterInfoContentState.InvalidParams
                )
            }
            return
        }

        val juzNos = withContext(Dispatchers.IO) {
            repository.getJuzNosForChapter(chapterNo)
        }

        _uiState.update {
            it.copy(
                chapterNo = chapterNo,
                swl = chapterMeta,
                juzNos = juzNos,
                contentState = ChapterInfoContentState.Loading,
            )
        }

        loadContent(langCode)
    }

    private fun loadContent(langCode: String) {
        val state = _uiState.value
        if (state.chapterNo < 1) return

        lastLangCode.value = langCode

        contentLoadJob?.cancel()
        contentLoadJob = viewModelScope.launch {
            _uiState.update { it.copy(contentState = ChapterInfoContentState.Loading) }

            val outcome = try {
                withContext(Dispatchers.IO) {
                    loadChapterInfoData(langCode, state.chapterNo)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                e.printStackTrace()
                LoadOutcome.Failed(e.message ?: getString(Res.string.strMsgChapInfoFailedLoad))
            }

            when (outcome) {
                is LoadOutcome.Success -> {
                    val resultLangCode = outcome.res.chapterInfo.languageCode ?: "en"
                    val html = buildHtml(
                        outcome.res.chapterInfo.primaryContent(),
                        resultLangCode
                    )

                    _uiState.update {
                        it.copy(
                            contentState = ChapterInfoContentState.Success(html, resultLangCode)
                        )
                    }
                }

                LoadOutcome.NoNetwork -> _uiState.update {
                    it.copy(contentState = ChapterInfoContentState.NoInternet)
                }

                is LoadOutcome.Failed -> {
                    deleteSavedFileIfExists(langCode)

                    _uiState.update {
                        it.copy(
                            contentState = ChapterInfoContentState.Error(
                                outcome.message,
                                true
                            )
                        )
                    }
                }
            }
        }
    }

    private suspend fun loadChapterInfoData(langCode: String, chapterNo: Int): LoadOutcome {
        val chapterInfoFile = chapterInfoFile(langCode, chapterNo)

        // Try reading from cache first (full JSON) or legacy plain text
        if (AppFileSystem.exists(chapterInfoFile)) {
            val cached = AppFileSystem.readText(chapterInfoFile)

            if (cached.isNotEmpty()) {
                val decoded =
                    runCatching { JsonHelper.json.decodeFromString<ChapterInfoApiResponse>(cached) }
                        .getOrNull()

                if (decoded != null) {
                    return LoadOutcome.Success(decoded)
                }
            }
        } else {
            AppFileSystem.createFile(chapterInfoFile)
        }

        // Need network
        if (!isNetworkConnected()) {
            return LoadOutcome.NoNetwork
        }

        return try {
            val payload = AlfaazPlusApi.getChapterInfo(chapterNo, langCode, null)
            val text = payload.chapterInfo.primaryContent()

            if (text.isNotEmpty()) {
                val stored = JsonHelper.json.encodeToString(payload)

                AppFileSystem.writeText(chapterInfoFile, stored)

                LoadOutcome.Success(payload)
            } else {
                LoadOutcome.Failed(getString(Res.string.strMsgSomethingWrong))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            AppFileSystem.delete(chapterInfoFile)
            LoadOutcome.Failed(e.message ?: getString(Res.string.strMsgChapInfoFailedLoad))
        }
    }

    private suspend fun buildHtml(contentText: String, langCode: String): String {
        val state = _uiState.value
        val meta = state.swl ?: return ""
        val surah = meta.surah
        val isMeccan = surah.revelationType == RevelationType.meccan

        val isDark = ThemeUtils.isDarkThemeNow()
        val theme = if (isDark) "dark" else "light"
        val colorScheme = ThemeUtils.colorSchemeNow(isDark)
        val textDirection = if (StringUtils.isRtlLanguage(langCode)) "rtl" else "ltr"
        val chapterIcon = QuranGlyphs.Chapter.get(state.chapterNo) + QuranGlyphs.Chapter.getPrefix()

        val juzStr = when {
            state.juzNos.isEmpty() -> ""
            state.juzNos.size == 1 -> state.juzNos.first().toString()
            else -> "${state.juzNos.first()}-${state.juzNos.last()}"
        }

        val chapterTitle = getString(
            Res.string.strLabelSurah,
            meta.getCurrentName(),
        )
        val chapterNoLabel =
            "${getString(Res.string.strTitleChapInfoChapterNo)}: ${state.chapterNo}"

        val revTypeLabel = getString(
            if (isMeccan) Res.string.strTitleMakki
            else Res.string.strTitleMadani,
        )

        // Self-contained page: CSS, JS, the surah-icon font and the revelation image are inlined,
        // so no platform-specific request interception is involved (Android WebView / iOS WKWebView).
        val template = ChapterInfoAssets.inline(
            Res.readBytes("files/chapter_info/chapter_info_page.html").decodeToString(),
            surah.revelationType,
        )

        return template
            .replace("{{STYLE}}", buildChapterInfoThemeStyle(colorScheme))
            .replace("{{THEME}}", theme)
            .replace("{{TEXT_DIRECTION}}", textDirection)
            .replace("{{CHAPTER_ICON}}", chapterIcon)
            .replace("{{CHAPTER_TITLE}}", chapterTitle)
            .replace("{{CHAPTER_NO}}", chapterNoLabel)
            .replace(
                "{{JUZ_TITLE}}",
                getString(Res.string.strTitleChapInfoJuzNo)
            )
            .replace("{{JUZ_VALUE}}", juzStr)
            .replace(
                "{{VERSES_TITLE}}",
                getString(Res.string.strTitleChapInfoVerses)
            )
            .replace("{{VERSES_VALUE}}", surah.ayahCount.toString())
            .replace(
                "{{RUKUS_TITLE}}",
                getString(Res.string.strTitleChapInfoRukus)
            )
            .replace("{{RUKUS_VALUE}}", surah.rukusCount.toString())
            .replace(
                "{{REV_ORDER_TITLE}}",
                getString(Res.string.strTitleChapInfoRevOrder)
            )
            .replace("{{REV_ORDER_VALUE}}", surah.revelationOrder.toString())
            .replace(
                "{{REV_TYPE_TITLE}}",
                getString(Res.string.strTitleChapInfoRevType)
            )
            .replace("{{REV_TYPE_VALUE}}", revTypeLabel)
            .replace("{{CONTENT}}", contentText)
    }

    private fun buildChapterInfoThemeStyle(scheme: ColorScheme): String {
        val vars = linkedMapOf(
            "--color-primary" to scheme.primary.toCssHex(),
            "--color-on-primary" to scheme.onPrimary.toCssHex(),
            "--color-background" to scheme.background.toCssHex(),
            "--color-surface" to scheme.surface.toCssHex(),
            "--color-on-surface" to scheme.onSurface.toCssHex(),
            "--color-on-surface-variant" to scheme.onSurfaceVariant.toCssHex(),
            "--color-surface-variant" to scheme.surfaceVariant.toCssHex(),
            "--color-outline-variant" to scheme.outlineVariant.toCssHex(),
            "--color-primary-container" to scheme.primaryContainer.toCssHex(),
            "--color-on-primary-container" to scheme.onPrimaryContainer.toCssHex(),
            "--color-surface-container" to scheme.surfaceContainer.toCssHex(),
            "--color-link-bg" to scheme.primary.copy(alpha = 0.2f).toCssRgba(),
            "--color-link-bg-active" to scheme.primary.copy(alpha = 0.3f).toCssRgba(),
        )

        val css = vars.entries.joinToString("") { "${it.key}:${it.value};" }

        return "<style>:root{$css}</style>"
    }

    private fun deleteSavedFileIfExists(langCode: String) {
        val state = _uiState.value

        AppFileSystem.delete(chapterInfoFile(langCode, state.chapterNo))
    }

    private sealed interface LoadOutcome {
        data class Success(val res: ChapterInfoApiResponse) : LoadOutcome
        object NoNetwork : LoadOutcome
        data class Failed(val message: String) : LoadOutcome
    }
}
