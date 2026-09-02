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
 * that went wrong and takes over the screen; labelled buttons over the FAB ask the same thing where
 * the answer will be acted on, and cost the same single tap.
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
 * What the reader's edit button offers where the user is standing.
 *
 * **A bab holds one kind of child, not two.** Either it is divided into alt-babs and the hadiths sit
 * inside those, or it carries its hadiths directly. So the two roads are only both open while the
 * bab is still empty — that, and only that, is when the menu appears. Once the bab has gone one way,
 * the button is a plain button again and does the one thing that fits: another alt-bab for a divided
 * bab, another hadith for a direct one, and a hadith whenever the reader is inside an alt-bab.
 *
 * The caller decides, because only it knows what the current bab holds; the flags are kept separate
 * rather than derived here so that "what can be added" stays one expression next to the counts it
 * is read from.
 */
@Composable
fun rememberHadithAddActions(
    canAddSubChapter: Boolean,
    canAddHadith: Boolean,
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
        if (canAddHadith) {
            add(
                HadithFabAction(
                    label = hadithLabel,
                    icon = Res.drawable.hedis,
                    onClick = onAddHadith,
                )
            )
        }
    }
}
