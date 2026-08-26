package com.cafarovceyxun.anamuslim.compose.utils.preferences

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis

/**
 * How the "rate the app" prompt decides whether today is the day to ask.
 *
 * Pure data + a pure decision ([ReviewPromptPolicy.isDue]) so the rule can be tested without a
 * DataStore or a clock; [ReviewPromptPreferences] is the thin storage around it.
 */
data class ReviewPromptState(
    /** When the prompt first saw this install, in epoch millis. 0 means "not recorded yet". */
    val firstSeenAtMillis: Long = 0L,
    /** How many app launches the prompt has counted since the install. */
    val launchCount: Int = 0,
    /** When the prompt was last shown, in epoch millis. 0 means "never shown". */
    val lastAskedAtMillis: Long = 0L,
    /** How many times the prompt has been shown. Capped by [ReviewPromptPolicy.MAX_ASKS]. */
    val askCount: Int = 0,
    val outcome: ReviewPromptOutcome = ReviewPromptOutcome.UNANSWERED,
)

enum class ReviewPromptOutcome(val value: String) {
    /** Never shown, or shown and dismissed without a choice. */
    UNANSWERED("unanswered"),

    /** "Later" — ask again after [ReviewPromptPolicy.SNOOZE_DAYS]. */
    SNOOZED("snoozed"),

    /** The store flow was opened. Never ask again on this install. */
    RATED("rated"),

    /** "Don't ask again". Never ask again on this install. */
    DECLINED("declined");

    companion object {
        fun fromValue(value: String): ReviewPromptOutcome =
            entries.firstOrNull { it.value == value } ?: UNANSWERED
    }
}

object ReviewPromptPolicy {

    /**
     * Launches before the prompt may appear.
     *
     * Both stores' guidance is the same here: ask someone who has actually used the app, never on
     * first run and never right after an action, so the rating reflects the app rather than the
     * moment. Nothing about the app is withheld from someone who never answers it.
     */
    const val MIN_LAUNCHES = 5

    /** Days since the install was first seen, so five launches in one evening do not qualify. */
    const val MIN_DAYS_INSTALLED = 3

    /** How long "Later" (or a dismissal) holds the prompt off. */
    const val SNOOZE_DAYS = 45

    /**
     * After this many unanswered asks the prompt gives up for good.
     *
     * Without a cap, "Later" every time would mean asking forever at [SNOOZE_DAYS] intervals, which
     * is the nagging both stores' guidance is written against.
     */
    const val MAX_ASKS = 3

    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

    fun isDue(state: ReviewPromptState, nowMillis: Long): Boolean {
        when (state.outcome) {
            ReviewPromptOutcome.RATED, ReviewPromptOutcome.DECLINED -> return false
            ReviewPromptOutcome.UNANSWERED, ReviewPromptOutcome.SNOOZED -> Unit
        }

        if (state.askCount >= MAX_ASKS) return false
        if (state.launchCount < MIN_LAUNCHES) return false

        // A clock that moved backwards (time zone edit, manual change) would otherwise make every
        // elapsed check negative and hold the prompt off forever; treating it as "no time passed"
        // only delays, never repeats.
        val installedDays = elapsedDays(state.firstSeenAtMillis, nowMillis)
        if (state.firstSeenAtMillis == 0L || installedDays < MIN_DAYS_INSTALLED) return false

        // Shown before and left unanswered (or snoozed): wait out the cooldown.
        if (state.lastAskedAtMillis != 0L &&
            elapsedDays(state.lastAskedAtMillis, nowMillis) < SNOOZE_DAYS
        ) {
            return false
        }

        return true
    }

    private fun elapsedDays(sinceMillis: Long, nowMillis: Long): Long =
        ((nowMillis - sinceMillis).coerceAtLeast(0L)) / MILLIS_PER_DAY
}

object ReviewPromptPreferences {
    private val KEY_FIRST_SEEN_AT = longPreferencesKey("review_prompt_first_seen_at")
    private val KEY_LAUNCH_COUNT = intPreferencesKey("review_prompt_launch_count")
    private val KEY_LAST_ASKED_AT = longPreferencesKey("review_prompt_last_asked_at")
    private val KEY_ASK_COUNT = intPreferencesKey("review_prompt_ask_count")
    private val KEY_OUTCOME = stringPreferencesKey("review_prompt_outcome")

    suspend fun read(): ReviewPromptState = ReviewPromptState(
        firstSeenAtMillis = DataStoreManager.readFirst(KEY_FIRST_SEEN_AT, 0L),
        launchCount = DataStoreManager.readFirst(KEY_LAUNCH_COUNT, 0),
        lastAskedAtMillis = DataStoreManager.readFirst(KEY_LAST_ASKED_AT, 0L),
        askCount = DataStoreManager.readFirst(KEY_ASK_COUNT, 0),
        outcome = ReviewPromptOutcome.fromValue(DataStoreManager.readFirst(KEY_OUTCOME, "")),
    )

    /**
     * Counts one app launch and returns the state that results, so the caller can decide in the
     * same pass. Also stamps the install date on the first call ever.
     */
    suspend fun registerLaunch(): ReviewPromptState {
        val state = read()

        // An install that predates this feature has no stamp; taking "now" means those users wait
        // out the same MIN_DAYS_INSTALLED as a fresh one, which is the conservative direction.
        val firstSeen = state.firstSeenAtMillis.takeIf { it != 0L } ?: currentEpochMillis()
        val launches = state.launchCount + 1

        if (firstSeen != state.firstSeenAtMillis) {
            DataStoreManager.write(KEY_FIRST_SEEN_AT, firstSeen)
        }
        DataStoreManager.write(KEY_LAUNCH_COUNT, launches)

        return state.copy(firstSeenAtMillis = firstSeen, launchCount = launches)
    }

    suspend fun recordShown() {
        DataStoreManager.write(KEY_LAST_ASKED_AT, currentEpochMillis())
        DataStoreManager.write(KEY_ASK_COUNT, DataStoreManager.readFirst(KEY_ASK_COUNT, 0) + 1)
    }

    suspend fun recordOutcome(outcome: ReviewPromptOutcome) {
        DataStoreManager.write(KEY_OUTCOME, outcome.value)
    }
}
