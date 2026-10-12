package com.cafarovceyxun.anamuslim.compose.utils.app

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Versiya qoruması yoxdur — `isIgnoringBatteryOptimizations` API 23-dəndir, `minSdk` isə 24.
 * Intent-ə `package:` URI **verilmir**: siyahı intent-i data qəbul etmir və verildikdə bəzi
 * ROM-larda `ActivityNotFoundException` atır.
 */
@Composable
actual fun rememberBatteryOptimizationAccess(): SystemSettingAccess? {
    val context = LocalContext.current
    val granted = rememberCheckedOnResume {
        (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)
            ?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    }

    return remember(context, granted) {
        object : SystemSettingAccess {
            override val isGranted = granted

            override fun open() {
                // Bəzi OEM ROM-larında bu intent həll olunmur — ölü toxunuş buraxmamaq üçün
                // tətbiqin öz ayarlarına düşürük (orada da pil bölməsi var).
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }.onFailure { openAppSettings() }
            }
        }
    }
}
