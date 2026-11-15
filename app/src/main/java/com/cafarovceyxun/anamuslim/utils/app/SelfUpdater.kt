package com.cafarovceyxun.anamuslim.utils.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.supabase.ApkStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * In-app updater for the `direct` build (APK installs outside Play): downloads the APK from public
 * storage, checks it really is a newer build of this app, then installs it through
 * [PackageInstaller]. Registered in `QuranApp` only when `BuildConfig.SELF_UPDATE` is set.
 *
 * Runs on its own process-lifetime scope, not a composition scope: the installer hands control to
 * the system and the screen that pressed "Update" may be recreated or gone by the time the 130 MB
 * download ends.
 */
object SelfUpdater : AppSelfUpdate {

    private const val TAG = "SelfUpdater"
    private const val DIR = "self-update"
    private const val BUFFER_SIZE = 64 * 1024

    /** Give up only after this many attempts in a row that added no bytes at all. */
    private const val MAX_STALLED_ATTEMPTS = 4
    private const val RETRY_DELAY_MS = 2_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<SelfUpdateState>(SelfUpdateState.Idle)
    override val state: StateFlow<SelfUpdateState> = _state

    private var appContext: Context? = null
    private var job: Job? = null

    /**
     * The installer's "confirm this update" screen, kept until the user answers it.
     *
     * The system asks through [SelfUpdateReceiver], and Android 10+ silently refuses to start an
     * activity from the background — so if the user left the app during the 130 MB download, the
     * prompt never appears and nothing says so. Keeping the intent lets the next press of "Update"
     * open it from the foreground instead of leaving the button stuck on "Installing…".
     */
    @Volatile
    private var pendingConfirmation: Intent? = null

    fun init(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx
        // A finished install leaves its APK behind (the process dies with the replaced package, so
        // nothing cleans up after it); at 130 MB that is worth removing on the next launch.
        scope.launch {
            try {
                val installed = installedVersionCode(ctx)
                File(ctx.cacheDir, DIR).listFiles()?.forEach { file ->
                    val version = if (file.name.endsWith(".part")) {
                        // An unfinished download cannot be parsed yet, but it is what the next
                        // press resumes from — keep it while its version is still ahead of us.
                        file.name.removePrefix("anamuslim-").removeSuffix(".apk.part").toLongOrNull()
                    } else {
                        ctx.packageManager.getPackageArchiveInfo(file.path, 0)
                            ?.let { PackageInfoCompat.getLongVersionCode(it) }
                    }
                    if (version == null || version <= installed) file.delete()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.saveError(e, "$TAG.cleanup")
            }
        }
    }

    override fun start(versionCode: Long) {
        val ctx = appContext ?: return
        if (job?.isActive == true) return

        pendingConfirmation?.let { confirm ->
            if (launchConfirmation(ctx, confirm)) return
            pendingConfirmation = null
        }

        // A failed attempt simply retries on the next press.
        _state.value = SelfUpdateState.Idle
        job = scope.launch {
            try {
                val dir = File(ctx.cacheDir, DIR).apply { mkdirs() }
                val target = File(dir, "anamuslim-$versionCode.apk")

                if (!(target.exists() && isInstallable(ctx, target))) {
                    _state.value = SelfUpdateState.Downloading(null)
                    val part = File(dir, "${target.name}.part")
                    download(ApkStorage.publicUrl(versionCode), part)
                    target.delete()
                    if (!part.renameTo(target)) error("Could not move the downloaded APK into place")
                    if (!isInstallable(ctx, target)) {
                        target.delete()
                        error("Downloaded APK is not a newer build of this app")
                    }
                }

                install(ctx, target)
                _state.value = SelfUpdateState.Installing
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.saveError(e, "$TAG.start")
                _state.value = SelfUpdateState.Failed
            }
        }
    }

    /** Called by [SelfUpdateReceiver] with the installer's verdict. */
    internal fun onInstallResult(success: Boolean, aborted: Boolean) {
        pendingConfirmation = null
        _state.value = if (success || aborted) SelfUpdateState.Idle else SelfUpdateState.Failed
    }

    /**
     * Called by [SelfUpdateReceiver] when the system wants the user to confirm. The button goes back
     * to "Update" rather than staying on "Installing…": if the prompt was blocked (see
     * [pendingConfirmation]) that press is how it gets opened.
     */
    internal fun onConfirmationNeeded(context: Context, confirm: Intent) {
        pendingConfirmation = confirm
        _state.value = SelfUpdateState.Idle
        launchConfirmation(context, confirm)
    }

    private fun launchConfirmation(context: Context, confirm: Intent): Boolean =
        try {
            context.startActivity(Intent(confirm).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: Exception) {
            AppLogger.saveError(e, "$TAG.confirm")
            false
        }

    private fun installedVersionCode(ctx: Context): Long =
        PackageInfoCompat.getLongVersionCode(ctx.packageManager.getPackageInfo(ctx.packageName, 0))

    /** Rejects a truncated file, someone else's package, or a build that would be a downgrade. */
    private fun isInstallable(ctx: Context, file: File): Boolean {
        val info = ctx.packageManager.getPackageArchiveInfo(file.path, 0) ?: return false
        return info.packageName == ctx.packageName &&
            PackageInfoCompat.getLongVersionCode(info) > installedVersionCode(ctx)
    }

    /**
     * Downloads [url] into [out], **resuming** from what is already there.
     *
     * A single request for the whole 130 MB does not survive: the server's gateway ends every
     * storage request after a fixed time (30 s on the self-hosted Envoy, seen as `unexpected end of
     * stream` here and `ABORTED REQ` in the storage log), and mobile connections drop on their own.
     * So each attempt asks only for the bytes still missing (`Range`), and a cut is retried as long
     * as the previous attempt moved forward. The partial file also survives a failed press, so the
     * next one continues instead of starting over.
     */
    private suspend fun download(url: String, out: File) {
        var stalledAttempts = 0
        var lastPercent = -1

        while (true) {
            val have = if (out.exists()) out.length() else 0L
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 60_000
                instanceFollowRedirects = true
                if (have > 0) setRequestProperty("Range", "bytes=$have-")
            }
            var received = have
            try {
                val code = connection.responseCode
                // 416 = nothing left to send: the part file is already whole.
                if (code == 416 && have > 0) return
                if (code !in 200..299) error("Download failed with HTTP $code")

                // A server that ignores `Range` answers 200 with the whole file: start over.
                val append = code == 206 && have > 0
                if (!append) received = 0L
                val remaining = connection.contentLengthLong
                val total = if (remaining > 0) received + remaining else -1L
                val buffer = ByteArray(BUFFER_SIZE)

                connection.inputStream.use { input ->
                    FileOutputStream(out, append).use { output ->
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            received += read
                            if (total > 0) {
                                // Only the integer percent is published: a flow write per 64 KB
                                // chunk would recompose the button thousands of times for nothing.
                                val percent = (received * 100 / total).toInt()
                                if (percent != lastPercent) {
                                    lastPercent = percent
                                    _state.value = SelfUpdateState.Downloading(percent / 100f)
                                }
                            }
                        }
                    }
                }
                if (total > 0 && received < total) throw IOException("Download cut at $received of $total bytes")
                return
            } catch (e: IOException) {
                stalledAttempts = if (received > have) 0 else stalledAttempts + 1
                if (stalledAttempts >= MAX_STALLED_ATTEMPTS) throw e
                AppLogger.d(TAG, "Download interrupted at $received bytes, resuming: ${e.message}")
                delay(RETRY_DELAY_MS * (stalledAttempts + 1))
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun install(ctx: Context, apk: File) {
        val installer = ctx.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(ctx.packageName)
            setSize(apk.length())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }

        val sessionId = installer.createSession(params)
        val session = installer.openSession(sessionId)
        try {
            session.openWrite("base.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out, BUFFER_SIZE) }
                session.fsync(out)
            }

            var flags = PendingIntent.FLAG_UPDATE_CURRENT
            // The system adds the status extras to this intent, so it cannot be immutable; the
            // intent is explicit, which API 34 demands of a mutable one.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) flags = flags or PendingIntent.FLAG_MUTABLE
            val pending = PendingIntent.getBroadcast(
                ctx,
                sessionId,
                Intent(ctx, SelfUpdateReceiver::class.java),
                flags,
            )
            session.commit(pending.intentSender)
        } catch (e: Exception) {
            session.abandon()
            throw e
        } finally {
            session.close()
        }
    }
}
