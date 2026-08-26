package com.cafarovceyxun.anamuslim.compose.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReviewPromptOutcome
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReviewPromptPolicy
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReviewPromptPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strLabelRateLater
import com.cafarovceyxun.anamuslim.resources.strLabelRateNow
import com.cafarovceyxun.anamuslim.resources.strMsgRatePrompt
import com.cafarovceyxun.anamuslim.resources.strTitleRatePrompt
import com.cafarovceyxun.anamuslim.utils.app.AppStoreReviewProvider
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * How long after the app settles before the prompt may appear.
 *
 * The launch is busy — splash, bootstrap, the home screen's first loads — and a dialog landing in
 * the middle of it reads as an error rather than a question.
 */
private const val SETTLE_DELAY_MS = 4_000L

/**
 * Asks, at most a few times in the life of an install, for a store rating and a written review.
 *
 * Hosted once per composition root (Android `MainScreen`, iOS `MainViewController`) rather than on a
 * screen, so the launch count is counted once per app start and the dialog cannot appear twice.
 *
 * ⚠️ The wording deliberately asks for an **honest** rating, not for five stars. Both stores
 * prohibit asking for a specific star count, and prohibit "review gating" — showing the store flow
 * only to users who first said they were happy. An app that does either can be pulled from the
 * store, so the dialog offers the same two buttons to everyone and never asks how they feel first.
 */
@Composable
fun AppReviewPromptHost() {
    // Nothing to hand off to on a platform that has not registered the seam: an inert default would
    // give us a dialog whose only button does nothing at all.
    if (!AppStoreReviewProvider.isAvailable) return

    var isOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // Wrapped because this runs in the composition root's effect scope: a throw here (a store
        // that is somehow not ready yet) would take down the whole app for a feature nobody asked
        // for. A rating prompt is never worth a crash — silence is the correct failure.
        val state = runCatching { ReviewPromptPreferences.registerLaunch() }.getOrNull()
            ?: return@LaunchedEffect

        if (!ReviewPromptPolicy.isDue(state, currentEpochMillis())) return@LaunchedEffect

        delay(SETTLE_DELAY_MS)

        runCatching { ReviewPromptPreferences.recordShown() }
        isOpen = true
    }

    // Process-lifetime scope, not this composition's: on Android "Rate it" leaves for the Play
    // listing, and on iOS the rating sheet takes over — either way the write must not depend on
    // this composable still being around. (`rememberCoroutineScope` is fine here only because the
    // dialog closes first and the writes are single-step; the store hand-off is called directly.)
    val close: (ReviewPromptOutcome) -> Unit = { outcome ->
        isOpen = false
        scope.launch { runCatching { ReviewPromptPreferences.recordOutcome(outcome) } }
    }

    AlertDialog(
        isOpen = isOpen,
        // Dismissing without choosing is not a "no": `recordShown` already booked the cooldown, so
        // the next ask is a month and a half away regardless.
        onClose = { isOpen = false },
        title = stringResource(Res.string.strTitleRatePrompt),
        actions = listOf(
            AlertDialogAction(
                text = stringResource(Res.string.strLabelRateLater),
                onClick = { close(ReviewPromptOutcome.SNOOZED) },
            ),
            AlertDialogAction(
                text = stringResource(Res.string.strLabelRateNow),
                style = AlertDialogActionStyle.Primary,
                onClick = {
                    close(ReviewPromptOutcome.RATED)

                    val review = AppStoreReviewProvider.review

                    // iOS draws its own sheet in place; Android has none without Play Core, so it
                    // reports false and we open the listing, which is where a review is written
                    // anyway — the whole point of asking.
                    if (!review.requestInAppRating()) review.openReviewPage()
                },
            ),
        ),
    ) {
        Text(
            text = stringResource(Res.string.strMsgRatePrompt),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
