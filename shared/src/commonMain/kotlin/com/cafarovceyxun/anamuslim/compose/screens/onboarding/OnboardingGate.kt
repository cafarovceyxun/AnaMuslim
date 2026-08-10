package com.cafarovceyxun.anamuslim.compose.screens.onboarding

/**
 * Decides whether onboarding runs, and is the one place that decides it for both platforms.
 *
 * What is stored is not "did they finish it" but **which version of onboarding they finished**.
 * That makes a release able to demand the screens again: raise [REQUIRED_VERSION] and every user
 * who updates walks through onboarding once more, which is how a build that adds a resource the
 * app now depends on (a new script, a new word-by-word pack) gets everyone to pick one — a plain
 * boolean could only ever ask a brand-new install.
 *
 * [completedThisSession] keeps that from becoming a loop: Android completes onboarding by finishing
 * one activity and starting another, so without an in-process memory the next activity would read
 * a not-yet-flushed preference and send the user straight back.
 */
object OnboardingGate {

    /**
     * ⬆️ **Raise this to force onboarding on the next release.**
     *
     * 1 = the original three pages. 2 = the resources page (script, translations, hadith, reciter,
     * word-by-word), which every installed user should see once.
     */
    const val REQUIRED_VERSION = 2

    /** Development switch: ignore what is stored and show onboarding on every cold start. */
    const val ALWAYS_SHOW = false

    /** Written when onboarding finishes; anything lower means it has to run. */
    const val NOT_COMPLETED = 0

    private var completedThisSession = false

    /** @param completedVersion the stored [REQUIRED_VERSION] from the run that finished onboarding. */
    fun shouldShow(completedVersion: Int): Boolean {
        if (completedThisSession) return false
        if (ALWAYS_SHOW) return true

        return completedVersion < REQUIRED_VERSION
    }

    /** Called the moment onboarding is finished, before any navigation happens. */
    fun markCompleted() {
        completedThisSession = true
    }
}
