package com.cafarovceyxun.anamuslim.utils.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.core.content.IntentCompat
import com.cafarovceyxun.anamuslim.utils.AppLogger

/**
 * Receives [PackageInstaller]'s verdict for the session [SelfUpdater] committed. When the system
 * needs the user to confirm, it asks through this receiver rather than opening anything itself, so
 * the confirmation screen is started here.
 */
class SelfUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
                if (confirm == null) {
                    SelfUpdater.onInstallResult(success = false, aborted = false)
                    return
                }
                try {
                    context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (e: Exception) {
                    AppLogger.saveError(e, "SelfUpdateReceiver.confirm")
                    SelfUpdater.onInstallResult(success = false, aborted = false)
                }
            }

            PackageInstaller.STATUS_SUCCESS -> SelfUpdater.onInstallResult(success = true, aborted = false)

            PackageInstaller.STATUS_FAILURE_ABORTED -> SelfUpdater.onInstallResult(success = false, aborted = true)

            else -> {
                AppLogger.d(
                    "SelfUpdateReceiver",
                    "Install failed ($status): ${intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)}",
                )
                SelfUpdater.onInstallResult(success = false, aborted = false)
            }
        }
    }
}
