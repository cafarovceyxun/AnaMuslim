/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 1/3/2022.
 * All rights reserved.
 */
package com.cafarovceyxun.anamuslim.utils.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.TimeInterpolator
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.cafarovceyxun.anamuslim.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.cafarovceyxun.anamuslim.databinding.LytUpdateAppDialogBinding
import com.cafarovceyxun.anamuslim.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.pow

/**
 * The blocking half of the update flow: the dialog shown when this build is below `min_version`.
 *
 * Everything else — deciding whether an update exists at all, and the dismissible homepage banner —
 * lives in the shared [AppUpdateChecker] / `AppUpdateBanner`, so iOS gets the same behaviour.
 */
class UpdateManager private constructor(private val ctx: Context) {
    companion object {
        /** The removed peacedesign `ColorUtils.DANGER`, kept so the button keeps its red. */
        private const val UPDATE_BUTTON_COLOR = 0xFFDC3545.toInt()

        private var INSTANCE: UpdateManager? = null

        fun getInstance(context: Context): UpdateManager {
            if (INSTANCE == null) {
                INSTANCE = UpdateManager(context)
            }

            return INSTANCE!!
        }
    }

    private val mIconAnimationHandler = Handler(Looper.getMainLooper())
    private var mIconAnimators = ArrayList<ObjectAnimator>()

    init {
        refreshAppUpdatesJson()
    }

    fun refreshAppUpdatesJson() {
        CoroutineScope(Dispatchers.IO).launch {
            AppUpdateChecker.refresh()
            ResourceUpdateManager.getInstance(ctx).checkAndPerformUpdates()
        }
    }

    /**
     * Blocks the launch when this build is below `min_version`. Answers from the cached file, since
     * it runs before the first frame — a fresh install with no cache yet simply lets the user in,
     * and the homepage banner picks the update up once the fetch lands.
     */
    fun check4CriticalUpdate(): Boolean {
        if (AppUpdateChecker.currentStatus() != AppUpdateStatus.REQUIRED) return false

        Logger.print("UpdateManager:", "Required update available")
        showUpdateAvailableDialog(true)
        return true
    }

    fun openPlayStore() {
        PlayStore.open(ctx)
    }

    private fun showUpdateAvailableDialog(isCritical: Boolean) {
        val binding = LytUpdateAppDialogBinding.inflate(android.view.LayoutInflater.from(ctx))
        binding.txt.setText(
            if (isCritical) R.string.strMsgUpdateAvailable2Continue else R.string.strMsgUpdateAvailable4Dialog
        )
        mIconAnimators.add(animateUpdateIcon(binding.icon))

        val dialog = MaterialAlertDialogBuilder(ctx)
            .setView(binding.root)
            .setCancelable(false)
            // The listener is wired in `setOnShowListener` instead, so the critical case can keep
            // the dialog open; passing one here would dismiss on every press.
            .setPositiveButton(R.string.strLabelUpdate, null)
            .setOnDismissListener {
                mIconAnimators.forEach { it.cancel() }
                mIconAnimationHandler.removeCallbacksAndMessages(null)
            }
            .apply { if (!isCritical) setNeutralButton(R.string.strLabelLater, null) }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                setTextColor(UPDATE_BUTTON_COLOR)
                setOnClickListener {
                    openPlayStore()
                    // A critical update leaves this build unusable, so the dialog stays up: the
                    // user comes back from Play either updated or to the same blocking prompt.
                    if (!isCritical) dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun animateUpdateIcon(iconView: View): ObjectAnimator {
        val pvhTransY = PropertyValuesHolder.ofFloat(
            View.TRANSLATION_Y,
            0f,
            11f,
            -20f,
            10f,
            -3f,
            0f
        )
        val pvhScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.1f, .8f, 1.3f, 1.03f, 1f)
        val pvhScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, .8f, 1.1f, 0.9f, 1f, 1f)
        return ObjectAnimator.ofPropertyValuesHolder(iconView, pvhTransY, pvhScaleX, pvhScaleY)
            .apply {
                interpolator =
                    TimeInterpolator { v -> (1.toFloat() - (1 - v).toDouble().pow(2.0)).toFloat() }
                duration = 1000
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (iconView.isAttachedToWindow) {
                            mIconAnimationHandler.postDelayed({ start() }, 1500)
                        }
                    }
                })
                start()
            }
    }

    fun onPause() {
        mIconAnimators.forEach { it.cancel() }
        mIconAnimationHandler.removeCallbacksAndMessages(null)
    }

    fun onResume() {
        mIconAnimators.forEach { it.start() }
    }
}
