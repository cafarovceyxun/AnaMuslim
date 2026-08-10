package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.theme.hadithArabicFontFamily
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HadithPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_right
import com.cafarovceyxun.anamuslim.resources.strLabelEdit
import com.cafarovceyxun.anamuslim.resources.strMsgSearchNoResultsFoundAbsolute
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The height every card settles on in multi-column mode — see [HadithEntryCard]'s `uniformHeight`.
 *
 * Sized for the tallest thing the card is allowed to hold there: 12+12dp of padding around a
 * two-line title (~40dp) over one line of Arabic (~20dp). That also clears the 52dp leading tile the
 * volume rows use, so all four hadith index levels land on the same figure.
 */
private val UNIFORM_CARD_HEIGHT = 88.dp

/** Shown in place of the list when a filter matches nothing. */
@Composable
fun HadithIndexEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.strMsgSearchNoResultsFoundAbsolute),
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * What a [HadithEntryCard]'s counter is counting.
 *
 * A bab shows either its sub-babs or — when it has none — the hadiths sitting directly under it, so
 * the two have to be told apart at a glance: [HADITH] is a filled, fully-rounded accent pill,
 * [SECTION] a flat neutral one.
 */
enum class HadithCountKind { SECTION, HADITH }

/** The counter shown at the end of a [HadithEntryCard]. */
@Composable
private fun HadithCountBadge(text: String, kind: HadithCountKind) {
    val isHadith = kind == HadithCountKind.HADITH
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (isHadith) FontWeight.Bold else FontWeight.Medium,
        color = if (isHadith) colorScheme.primary else colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier
            .background(
                color = if (isHadith) {
                    colorScheme.primaryContainer.alpha(0.55f)
                } else {
                    colorScheme.surfaceVariant.alpha(0.5f)
                },
                shape = if (isHadith) CircleShape else MaterialTheme.shapes.small,
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/**
 * One row of the hadith index — a volume, book, bab or sub-bab.
 *
 * Shared by all four index screens so they stay identical as they change. An existing entry is
 * edited by long-pressing its row — deliberately the only affordance, so the row keeps a single
 * visible action and nothing competes with the tap that opens it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HadithEntryCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingText: String? = null,
    leadingIcon: DrawableResource? = null,
    leadingColor: Color = colorScheme.primary,
    leadingContainerColor: Color = colorScheme.primaryContainer,
    subtitle: String? = null,
    supportingText: String? = null,
    /** Arabic name of the level, shown right under the title in the hadith Arabic font. */
    arabicTitle: String? = null,
    countText: String? = null,
    countKind: HadithCountKind = HadithCountKind.SECTION,
    titleStyle: TextStyle = MaterialTheme.typography.titleSmall,
    titleMaxLines: Int = 3,
    /**
     * Give every card the same height, for the multi-column grid.
     *
     * In a `LazyVerticalGrid` each cell keeps its own content height — the row is only as tall as its
     * tallest cell, and the shorter one sits against the top with a gap under it. Side by side on an
     * iPad that reads as two mismatched boxes. `fillMaxHeight` cannot fix it: the grid measures cells
     * with an unbounded height, where the modifier is a no-op.
     *
     * So the height is pinned from below and the title is capped at two lines, which is what makes
     * the cards actually coincide rather than merely share a floor. It stays a *minimum*: at large
     * Dynamic Type sizes a card still grows instead of clipping its own text.
     *
     * Left off in single-column mode, where each row is alone and nothing is there to match.
     */
    uniformHeight: Boolean = false,
    onEdit: (() -> Unit)? = null,
) {
    val editLabel = stringResource(Res.string.strLabelEdit)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Before `padding`, so the floor covers the whole card rather than just its content.
                .then(
                    if (uniformHeight) {
                        Modifier.defaultMinSize(minHeight = UNIFORM_CARD_HEIGHT)
                    } else {
                        Modifier
                    }
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onEdit,
                    onLongClickLabel = editLabel,
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(if (leadingIcon != null) 52.dp else 44.dp)
                    .background(
                        color = leadingContainerColor.alpha(0.3f),
                        shape = MaterialTheme.shapes.medium,
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (leadingIcon != null) {
                    Icon(
                        painter = painterResource(leadingIcon),
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = leadingColor,
                    )
                } else {
                    Text(
                        text = leadingText.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = leadingColor,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = titleStyle,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface,
                    maxLines = if (uniformHeight) titleMaxLines.coerceAtMost(2) else titleMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!arabicTitle.isNullOrBlank()) {
                    Text(
                        text = arabicTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDirection = TextDirection.Rtl,
                            fontFamily = hadithArabicFontFamily(HadithPreferences.observeArabicFont()),
                        ),
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!supportingText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // The badge sits tight against the chevron rather than a full 16.dp from it, so the two
            // read as one trailing block and the title keeps the width.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (countText != null) {
                    HadithCountBadge(text = countText, kind = countKind)
                }

                Icon(
                    painter = painterResource(Res.drawable.dr_icon_chevron_right),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colorScheme.onSurfaceVariant.alpha(0.4f),
                )
            }
        }
    }
}
