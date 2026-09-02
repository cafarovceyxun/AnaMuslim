package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.add_sub_chapter
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.dr_icon_edit
import com.cafarovceyxun.anamuslim.resources.hedis
import com.cafarovceyxun.anamuslim.resources.ic_mode_book
import com.cafarovceyxun.anamuslim.resources.strTitleAddHadith
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Minimalist edit affordance shared across the hadith screens.
 *
 * Deliberately quieter than a filled primary [androidx.compose.material3.FloatingActionButton]: a
 * flat, borderless-feeling circle in the surface tone with a primary-tinted glyph and no shadow, so
 * it reads as a subtle control over the content rather than a heavy call to action.
 */
@Composable
fun HadithEditFab(
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    icon: DrawableResource = Res.drawable.dr_icon_edit,
    size: Dp = 48.dp,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = CircleShape,
        color = colorScheme.surfaceContainerHigh.alpha(0.9f),
        contentColor = colorScheme.primary,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.alpha(0.6f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                modifier = Modifier.size(size * 0.4f),
                tint = colorScheme.primary,
            )
        }
    }
}

/** One choice in a [HadithEditFabMenu] — the label the user reads, and what tapping it does. */
data class HadithFabAction(
    val label: String,
    val icon: DrawableResource,
    val onClick: () -> Unit,
)

/**
 * The edit affordance when it opens onto more than one thing to add: the choices unfold **above**
 * the button, labelled, and the button itself turns into a close control.
 *
 * Replaces the alert dialog this used to be. A dialog for "bab, yoxsa hədis?" reads as a question
 * that went wrong, takes over the screen, and — the reason it existed at all — could only be shown
 * when the app could not work the answer out itself, so the other case was unreachable: a bab that
 * already held hadiths had no way left to gain an alt-bab. Labelled buttons over the FAB show both
 * roads at once and cost one tap.
 *
 * A single-action screen keeps using [HadithEditFab] directly; this one starts collapsed and is
 * dismissed by the back gesture as well as by the button, so it never traps the screen behind it.
 */
@Composable
fun HadithEditFabMenu(
    actions: List<HadithFabAction>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    var expanded by remember { mutableStateOf(false) }

    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    BackHandler(enabled = expanded) { expanded = false }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                actions.forEach { action ->
                    HadithFabActionPill(
                        action = action,
                        onSelected = {
                            expanded = false
                            action.onClick()
                        },
                    )
                }
            }
        }

        HadithEditFab(
            onClick = { expanded = !expanded },
            contentDescription = contentDescription,
            icon = if (expanded) Res.drawable.dr_icon_close else Res.drawable.dr_icon_edit,
            size = size,
        )
    }
}

/** Same surface tone as the button below it, so the open menu reads as one control, not a popup. */
@Composable
private fun HadithFabActionPill(
    action: HadithFabAction,
    onSelected: () -> Unit,
) {
    Surface(
        onClick = onSelected,
        shape = MaterialTheme.shapes.large,
        color = colorScheme.surfaceContainerHigh.alpha(0.95f),
        contentColor = colorScheme.onSurface,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.alpha(0.6f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = action.label,
                style = MaterialTheme.typography.labelLarge,
            )
            Icon(
                painter = painterResource(action.icon),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = colorScheme.primary,
            )
        }
    }
}

/**
 * What the reader's edit button offers where the user is standing: a hadith always, an alt-bab
 * whenever there is a bab to hang one on.
 *
 * The only thing an alt-bab needs is its parent **bab** — [canAddSubChapter] is therefore
 * `currentChapterSlug != null` and nothing else. Reading inside an alt-bab is no reason to hide it:
 * the new one becomes the next alt-bab of the same bab, which is exactly what someone entering a
 * book chapter by chapter wants next. Tying it to "am I at bab level?" instead left the option
 * reachable only from a bab with no alt-babs yet — the one place it was least needed.
 *
 * The app used to decide this on the user's behalf from what the bab already held, which closed the
 * other road entirely: a bab with hadiths could never gain an alt-bab, one with alt-babs could never
 * gain a direct hadith.
 */
@Composable
fun rememberHadithAddActions(
    canAddSubChapter: Boolean,
    onAddSubChapter: () -> Unit,
    onAddHadith: () -> Unit,
): List<HadithFabAction> {
    val subChapterLabel = stringResource(Res.string.add_sub_chapter)
    val hadithLabel = stringResource(Res.string.strTitleAddHadith)

    return buildList {
        if (canAddSubChapter) {
            add(
                HadithFabAction(
                    label = subChapterLabel,
                    icon = Res.drawable.ic_mode_book,
                    onClick = onAddSubChapter,
                )
            )
        }
        add(
            HadithFabAction(
                label = hadithLabel,
                icon = Res.drawable.hedis,
                onClick = onAddHadith,
            )
        )
    }
}
