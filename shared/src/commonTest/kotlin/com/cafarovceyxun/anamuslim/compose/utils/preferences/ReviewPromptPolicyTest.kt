package com.cafarovceyxun.anamuslim.compose.utils.preferences

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The prompt's whole rule set is here, so this is what stops it from turning into a nag: every
 * branch below is a way the dialog must *not* appear.
 */
class ReviewPromptPolicyTest {

    private val day = 24L * 60L * 60L * 1000L
    private val installedAt = 1_700_000_000_000L

    private fun ready(now: Long) = ReviewPromptState(
        firstSeenAtMillis = installedAt,
        launchCount = ReviewPromptPolicy.MIN_LAUNCHES,
        lastAskedAtMillis = 0L,
        askCount = 0,
        outcome = ReviewPromptOutcome.UNANSWERED,
    ).let { it to now }

    @Test
    fun asksOnceTheLaunchAndAgeBarsAreBothCleared() {
        val (state, now) = ready(installedAt + ReviewPromptPolicy.MIN_DAYS_INSTALLED * day)

        assertTrue(ReviewPromptPolicy.isDue(state, now))
    }

    @Test
    fun staysQuietBeforeEnoughLaunches() {
        val (state, now) = ready(installedAt + ReviewPromptPolicy.MIN_DAYS_INSTALLED * day)

        assertFalse(
            ReviewPromptPolicy.isDue(
                state.copy(launchCount = ReviewPromptPolicy.MIN_LAUNCHES - 1),
                now,
            )
        )
    }

    @Test
    fun staysQuietWhenAllTheLaunchesHappenedInOneEvening() {
        val (state, _) = ready(0L)
        val now = installedAt + (ReviewPromptPolicy.MIN_DAYS_INSTALLED - 1) * day

        assertFalse(ReviewPromptPolicy.isDue(state.copy(launchCount = 50), now))
    }

    @Test
    fun neverAsksAgainAfterRatingOrDeclining() {
        val (state, now) = ready(installedAt + 400 * day)

        assertFalse(ReviewPromptPolicy.isDue(state.copy(outcome = ReviewPromptOutcome.RATED), now))
        assertFalse(
            ReviewPromptPolicy.isDue(state.copy(outcome = ReviewPromptOutcome.DECLINED), now)
        )
    }

    @Test
    fun snoozeHoldsForItsFullWindowAndThenReleases() {
        val snoozedAt = installedAt + 10 * day
        val (state, _) = ready(0L)
        val snoozed = state.copy(
            lastAskedAtMillis = snoozedAt,
            askCount = 1,
            outcome = ReviewPromptOutcome.SNOOZED,
        )

        val duringWindow = snoozedAt + (ReviewPromptPolicy.SNOOZE_DAYS - 1) * day
        val afterWindow = snoozedAt + (ReviewPromptPolicy.SNOOZE_DAYS + 1) * day

        assertFalse(ReviewPromptPolicy.isDue(snoozed, duringWindow))
        assertTrue(ReviewPromptPolicy.isDue(snoozed, afterWindow))
    }

    @Test
    fun givesUpAfterTheAskCap() {
        val (state, _) = ready(0L)
        val exhausted = state.copy(
            lastAskedAtMillis = installedAt + 10 * day,
            askCount = ReviewPromptPolicy.MAX_ASKS,
            outcome = ReviewPromptOutcome.SNOOZED,
        )

        assertFalse(ReviewPromptPolicy.isDue(exhausted, installedAt + 1000 * day))
    }

    @Test
    fun aClockThatWentBackwardsOnlyDelays() {
        val (state, _) = ready(0L)

        // "Now" before the install stamp: elapsed time floors at zero, so the age bar is simply not
        // cleared yet — the alternative (a negative elapsed) would read as centuries and ask at once.
        assertFalse(ReviewPromptPolicy.isDue(state, installedAt - 100 * day))
    }

    @Test
    fun staysQuietWithoutAnInstallStamp() {
        val (state, now) = ready(installedAt + 100 * day)

        assertFalse(ReviewPromptPolicy.isDue(state.copy(firstSeenAtMillis = 0L), now))
    }
}
