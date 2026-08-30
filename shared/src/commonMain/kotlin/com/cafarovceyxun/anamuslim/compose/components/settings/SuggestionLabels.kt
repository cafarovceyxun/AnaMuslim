package com.cafarovceyxun.anamuslim.compose.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.status_pending
import com.cafarovceyxun.anamuslim.resources.status_rejected
import com.cafarovceyxun.anamuslim.resources.suggestionsCategoryBug
import com.cafarovceyxun.anamuslim.resources.suggestionsCategoryContent
import com.cafarovceyxun.anamuslim.resources.suggestionsCategoryFeature
import com.cafarovceyxun.anamuslim.resources.suggestionsCategoryOther
import com.cafarovceyxun.anamuslim.resources.suggestionsStatusApproved
import com.cafarovceyxun.anamuslim.resources.suggestionsStatusDone
import com.cafarovceyxun.anamuslim.resources.suggestionsStatusOpen
import com.cafarovceyxun.anamuslim.resources.suggestionsStatusPlanned
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionCategory
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionStatus
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionSubmissionStatus
import org.jetbrains.compose.resources.stringResource

/** Kateqoriya/status açarlarının göstərilən adları — istifadəçi ekranı və panel eynisini oxuyur. */

@Composable
fun suggestionCategoryLabel(category: String): String = when (category) {
    SuggestionCategory.FEATURE -> stringResource(Res.string.suggestionsCategoryFeature)
    SuggestionCategory.BUG -> stringResource(Res.string.suggestionsCategoryBug)
    SuggestionCategory.CONTENT -> stringResource(Res.string.suggestionsCategoryContent)
    else -> stringResource(Res.string.suggestionsCategoryOther)
}

/** `suggestions.status` — yayımlanmış təklifin iş vəziyyəti. */
@Composable
fun suggestionStatusLabel(status: String): String = when (status) {
    SuggestionStatus.PLANNED -> stringResource(Res.string.suggestionsStatusPlanned)
    SuggestionStatus.DONE -> stringResource(Res.string.suggestionsStatusDone)
    else -> stringResource(Res.string.suggestionsStatusOpen)
}

/** `suggestion_submissions.status` — moderasiya vəziyyəti. */
@Composable
fun suggestionSubmissionStatusLabel(status: String): String = when (status) {
    SuggestionSubmissionStatus.APPROVED -> stringResource(Res.string.suggestionsStatusApproved)
    SuggestionSubmissionStatus.REJECTED -> stringResource(Res.string.status_rejected)
    else -> stringResource(Res.string.status_pending)
}

/**
 * Təklif mətninin istiqamətini **məzmuna** bağlayır.
 *
 * İnterfeys dili ərəbcə olanda `QuranAppTheme` bütün düzülüşü RTL edir və öz istiqamətini təyin
 * etməyən mətn güzgülənir — sətrin əvvəlindəki nömrə ilə sonundakı durğu işarəsi yer dəyişir
 * (bax CLAUDE.md, «Ərəbcə interfeys bütün düzülüşü RTL edir»). Hədis adlarından fərqli olaraq
 * təklifin dili əvvəlcədən bilinmir — istifadəçi nə yazsa, odur. Ona görə burada sabit istiqamət
 * yox, [TextDirection.Content] verilir: paraqrafın istiqamətini mətnin öz ilk hərfi seçir.
 */
fun TextStyle.withContentDirection(): TextStyle = copy(textDirection = TextDirection.Content)
