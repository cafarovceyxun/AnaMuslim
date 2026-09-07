package com.cafarovceyxun.anamuslim.compose.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.screens.hadith.withScriptDirection
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.utils.text.DiffSpan
import com.cafarovceyxun.anamuslim.utils.text.TextHighlightYellow
import com.cafarovceyxun.anamuslim.utils.text.diffText

/**
 * Moderasiya kartında bir sahənin **hazırkı** və **təklif olunan** halını yan-yana göstərir;
 * dəyişən hərf, durğu işarəsi və rəqəm sarı ilə vurğulanır (bax
 * [com.cafarovceyxun.anamuslim.utils.text.diffText]).
 *
 * [old] `null` olanda (yeni hədis təklifi, ya da əsas mətn şəbəkə xətası ucbatından gəlməyib)
 * yalnız təklif göstərilir — kart heç vaxt boş qalmır.
 */
@Composable
fun DiffPair(
    old: String?,
    new: String,
    modifier: Modifier = Modifier,
    oldLabel: String = "Hazırkı mətn",
    newLabel: String = "Təklif",
    arabic: Boolean = false,
) {
    val diff = remember(old, new) { old?.let { diffText(it, new) } }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (old != null) {
            DiffField(
                label = if (diff?.hasChanges == false) "$oldLabel (dəyişiklik yoxdur)" else oldLabel,
                text = old,
                spans = diff?.oldSpans.orEmpty(),
                arabic = arabic,
            )
        }
        DiffField(
            label = newLabel,
            text = new,
            spans = diff?.newSpans.orEmpty(),
            arabic = arabic,
        )
    }
}

@Composable
private fun DiffField(
    label: String,
    text: String,
    spans: List<DiffSpan>,
    arabic: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 2.dp),
        )
        Text(
            text = highlightSpans(text, spans),
            style = typography.bodyMedium.withScriptDirection(arabic),
            color = if (arabic) colorScheme.primary else colorScheme.onSurface.alpha(0.85f),
        )
    }
}

/** Verilmiş intervalları sarı fonla işarələyir; interval yoxdursa mətn olduğu kimi qalır. */
fun highlightSpans(text: String, spans: List<DiffSpan>): AnnotatedString {
    if (spans.isEmpty()) return AnnotatedString(text)
    val style = SpanStyle(background = TextHighlightYellow)
    return buildAnnotatedString {
        append(text)
        spans.forEach { span ->
            // Mətn araya gələn yeniləmə ilə qısala bilər — interval hüdudları qəsdən qısılır.
            val start = span.start.coerceIn(0, text.length)
            val end = span.end.coerceIn(start, text.length)
            if (end > start) addStyle(style, start, end)
        }
    }
}
