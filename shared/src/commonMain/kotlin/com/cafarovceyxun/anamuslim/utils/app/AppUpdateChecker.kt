package com.cafarovceyxun.anamuslim.utils.app

import com.cafarovceyxun.anamuslim.api.ApiConfig
import com.cafarovceyxun.anamuslim.api.JsonHelper
import com.cafarovceyxun.anamuslim.api.NetworkConfig
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.supabase.AppRelease
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString

enum class AppUpdateStatus {
    /** Nothing to say: this build is current, or we cannot tell. */
    NONE,

    /** A newer build is published. Dismissible banner. */
    AVAILABLE,

    /** This build is below `min_version` and must not keep running. Blocking dialog. */
    REQUIRED,
}

/**
 * Whether a newer build of the app has been published, read from the `app_releases` Supabase table
 * — one row per platform, written by the admin from Settings → "Buraxılış Bildirişi".
 *
 * Supabase rather than a file in the repo because the announcement has to be publishable from the
 * phone the moment Play or App Store review clears, without a commit; and because a row per
 * platform is what makes the iOS build number and the Android `versionCode` comparable at all —
 * each is only ever compared against its own row.
 */
object AppUpdateChecker {
    /**
     * Which `app_releases` row this build reads — its update channel.
     *
     * Defaults to [appPlatformId] (`android` / `ios`). The Android host overrides it at startup from
     * `BuildConfig.UPDATE_PLATFORM`: the `direct` build (APK handed out outside Play) reads
     * [CHANNEL_ANDROID_APK], so a forced APK update never forces Play installs and back.
     *
     * `android` deliberately stays the Play channel: every build shipped before `direct` existed
     * reads that row and cannot be changed, and nearly all of those are Play installs. Giving the
     * row to the APK instead would push each APK release onto them.
     */
    var updatePlatformId: String = appPlatformId

    /** The `direct` build's row; see [updatePlatformId]. */
    const val CHANNEL_ANDROID_APK = "android_apk"

    /** How long a successful fetch stays fresh. Releases happen a few times a year at most. */
    private const val REFRESH_INTERVAL_MS = 6 * 60 * 60 * 1000L

    private const val TABLE = "app_releases"

    private val _release = MutableStateFlow<AppRelease?>(null)

    /** This platform's row — from the disk cache first, replaced by each fetch. */
    val release: StateFlow<AppRelease?> = _release.asStateFlow()

    private val _dismissedVersion = MutableStateFlow(0L)

    /**
     * The version the user closed the banner for, held in memory only — so the banner is back on
     * the next launch and keeps asking until the update is actually installed. Closing it silences
     * the current session, nothing more.
     */
    val dismissedVersion: StateFlow<Long> = _dismissedVersion.asStateFlow()

    fun dismiss(version: Long) {
        _dismissedVersion.value = version
    }

    private val fetchMutex = Mutex()
    private var lastFetchMillis = 0L
    private var cacheLoaded = false

    /**
     * This build's version code — `versionCode` on Android, `CFBundleVersion` on iOS. Both are
     * compared only against their own platform's row, so the two number spaces never meet.
     *
     * 0 means "unknown", and an unknown build never claims to be out of date.
     */
    private fun currentVersionCode(): Long = NetworkConfig.appVersionCode().toLongOrNull() ?: 0L

    /**
     * Publishes the cached row, so the banner is right on a cold start with no network. Safe to
     * call early — a DataStore that is not initialised yet simply leaves the state null.
     */
    fun loadCache() {
        if (cacheLoaded) return

        try {
            val cached = AppPreferences.getCachedAppUpdateInfo()
            if (cached.isNotBlank()) {
                val decoded = JsonHelper.json.decodeFromString<AppRelease>(cached)
                // A cache written while this install read another row (e.g. a Play build replaced by
                // the `direct` APK) would otherwise judge it against the wrong channel's numbers.
                if (decoded.platform == updatePlatformId) {
                    _release.value = decoded
                }
            }
            cacheLoaded = true
        } catch (_: Exception) {
            // A cache written by an older schema, or a store that is not ready. Either way the
            // fetch below replaces it; leave it unloaded so a later call can retry.
        }
    }

    /**
     * Refreshes from Supabase at most once per [REFRESH_INTERVAL_MS], unless [force].
     * Never throws: a failed fetch leaves whatever was cached in place.
     */
    suspend fun refresh(force: Boolean = false) {
        loadCache()

        fetchMutex.withLock {
            val now = currentEpochMillis()
            // `now - lastFetchMillis` is range-checked rather than just compared, so a clock moved
            // backwards cannot pin the throttle open until the interval elapses in real time.
            if (!force && lastFetchMillis != 0L && now - lastFetchMillis in 0 until REFRESH_INTERVAL_MS) {
                return
            }

            fetch()?.let { publish(it, now) }
        }
    }

    /**
     * Asks the server right now, ignoring the throttle — the Settings "Update" row, where the user
     * expects an answer about this moment. Returns this channel's row, or null when the server could
     * not be reached (the cached row is then left as it was).
     */
    suspend fun checkNow(): AppRelease? {
        loadCache()
        return fetchMutex.withLock {
            fetch()?.also { publish(it, currentEpochMillis()) }
        }
    }

    private suspend fun fetch(): AppRelease? =
        try {
            SupabaseProvider.client.from(TABLE)
                .select { filter { eq("platform", updatePlatformId) } }
                .decodeSingleOrNull<AppRelease>()
        } catch (_: Exception) {
            null
        }

    private suspend fun publish(fetched: AppRelease, now: Long) {
        lastFetchMillis = now
        _release.value = fetched

        try {
            AppPreferences.setCachedAppUpdateInfo(JsonHelper.json.encodeToString(fetched))
        } catch (_: Exception) {
            // Caching is an optimisation; the in-memory value above is what the UI reads.
        }
    }

    fun statusOf(release: AppRelease?): AppUpdateStatus {
        val current = currentVersionCode()
        if (release == null || current <= 0L) return AppUpdateStatus.NONE

        return when {
            current < release.min_version -> AppUpdateStatus.REQUIRED
            current < release.latest_version -> AppUpdateStatus.AVAILABLE
            else -> AppUpdateStatus.NONE
        }
    }

    /** Status from whatever is known right now, without touching the network. */
    fun currentStatus(): AppUpdateStatus {
        loadCache()
        return statusOf(_release.value)
    }

    /**
     * Where the update button sends the user, or null when there is nowhere to send them — a button
     * that opens nothing is worse than no button.
     *
     * The row's own `action_url` wins, so a listing can be redirected without a release. Failing
     * that the platform's store listing is used: [AppStoreReviewProvider] already knows it on both
     * sides (Play on Android, `apps.apple.com/app/id…` on iOS), which is what stopped the iOS
     * banner from ever growing a button while its row was left blank. The Play constant stays as a
     * last resort for the Android paths that run before that seam is registered.
     *
     * The APK channel never falls back to a store: that build is not from Play, and its button
     * downloads the update itself (`AppSelfUpdateProvider`) — a link is only used if one is set.
     */
    fun actionUrl(release: AppRelease?): String? =
        release?.action_url?.takeIf { it.isNotBlank() }
            ?: if (updatePlatformId == CHANNEL_ANDROID_APK) null
            else AppStoreReviewProvider.review.listingUrl.takeIf { it.isNotBlank() }
                ?: ApiConfig.PLAY_STORE_LISTING_URL.takeIf { appPlatformId == "android" }

    // ---- Admin (Settings → Buraxılış Bildirişi) ----

    /** Every platform's row, for the admin editor. Empty list on failure. */
    suspend fun fetchAllReleases(): List<AppRelease> =
        try {
            SupabaseProvider.client.from(TABLE).select().decodeList<AppRelease>()
        } catch (_: Exception) {
            emptyList()
        }

    /**
     * Writes one platform's row. Returns the stored row, or null when nothing was written.
     *
     * The `select()` is not decoration: when RLS blocks a write, PostgREST answers with an empty
     * result rather than an error, so an upsert that changed nothing still looks like a success.
     * Asking for the affected row back is the only way to tell the two apart.
     */
    suspend fun saveRelease(release: AppRelease): AppRelease? =
        try {
            SupabaseProvider.client.from(TABLE)
                .upsert(release.copy(updated_at = null)) { select() }
                .decodeSingleOrNull<AppRelease>()
        } catch (_: Exception) {
            null
        }

    /** Drops the throttle so the admin sees their own change on the next screen, not in six hours. */
    fun invalidateThrottle() {
        lastFetchMillis = 0L
    }
}
