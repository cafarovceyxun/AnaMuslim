package com.cafarovceyxun.anamuslim.compose.utils.app

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberExactAlarmAccess(): SystemSettingAccess? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null

    val context = LocalContext.current
    val granted = rememberCheckedOnResume {
        (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)
            ?.canScheduleExactAlarms() ?: true
    }

    return remember(context, granted) {
        object : SystemSettingAccess {
            override val isGranted = granted

            override fun open() {
                // `package:` URI ekranı birbaşa tətbiqin öz açarında açır — bir toxunuş.
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:${context.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }.onFailure { openAppSettings() }
            }
        }
    }
}
