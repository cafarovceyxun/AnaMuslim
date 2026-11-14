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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<SelfUpdateState>(SelfUpdateState.Idle)
    override val state: StateFlow<SelfUpdateState> = _state

    private var appContext: Context? = null
    private var job: Job? = null

    fun init(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx
        // A finished install leaves its APK behind (the process dies with the replaced package, so
        // nothing cleans up after it); at 130 MB that is worth removing on the next launch.
        scope.launch {
            try {
                val installed = installedVersionCode(ctx)
                File(ctx.cacheDir, DIR).listFiles()?.forEach { file ->
                    val info = ctx.packageManager.getPackageArchiveInfo(file.path, 0)
                    if (info == null || PackageInfoCompat.getLongVersionCode(info) <= installed) {
                        file.delete()
                    }
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
        _state.value = if (success || aborted) SelfUpdateState.Idle else SelfUpdateState.Failed
    }

    private fun installedVersionCode(ctx: Context): Long =
        PackageInfoCompat.getLongVersionCode(ctx.packageManager.getPackageInfo(ctx.packageName, 0))

    /** Rejects a truncated file, someone else's package, or a build that would be a downgrade. */
    private fun isInstallable(ctx: Context, file: File): Boolean {
        val info = ctx.packageManager.getPackageArchiveInfo(file.path, 0) ?: return false
        return info.packageName == ctx.packageName &&
            PackageInfoCompat.getLongVersionCode(info) > installedVersionCode(ctx)
    }

    private fun download(url: String, out: File) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 60_000
            instanceFollowRedirects = true
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) error("Download failed with HTTP $code")

            val total = connection.contentLengthLong
            var received = 0L
            var lastPercent = -1
            val buffer = ByteArray(BUFFER_SIZE)

            connection.inputStream.use { input ->
                out.outputStream().use { output ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        received += read
                        if (total > 0) {
                            // Only the integer percent is published: a flow write per 64 KB chunk
                            // would recompose the button thousands of times for nothing visible.
                            val percent = (received * 100 / total).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                _state.value = SelfUpdateState.Downloading(percent / 100f)
                            }
                        }
                    }
                }
            }
            if (total > 0 && received != total) error("Download truncated: $received of $total bytes")
        } finally {
            connection.disconnect()
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
