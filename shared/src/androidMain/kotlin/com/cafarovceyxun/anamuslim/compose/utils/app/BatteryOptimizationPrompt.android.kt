package com.cafarovceyxun.anamuslim.compose.utils.app

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.prayerBatteryOptimizationPrompt
import org.jetbrains.compose.resources.stringResource

/**
 * `ExactAlarmPrompt.android.kt`-nin güzgüsü. İki fərq:
 * - versiya qorumasi yoxdur — `isIgnoringBatteryOptimizations` API 23-dəndir, `minSdk` isə 24;
 * - intent-ə `package:` URI **verilmir**: siyahı intent-i data qəbul etmir və verildikdə bəzi
 *   ROM-larda `ActivityNotFoundException` atır.
 */
@Composable
actual fun BatteryOptimizationPrompt() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    fun isExempt(): Boolean =
        (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)
            ?.isIgnoringBatteryOptimizations(context.packageName) ?: true

    var exempt by remember(context) { mutableStateOf(isExempt()) }

    // İstifadəçi sistem siyahısından tətbiqi seçib qayıda bilər — sətir dərhal itməlidir.
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) exempt = isExempt()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (exempt) return

    Text(
        text = stringResource(Res.string.prayerBatteryOptimizationPrompt),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Bəzi OEM ROM-larında bu intent həll olunmur — ölü toxunuş buraxmamaq üçün
                // tətbiqin öz ayarlarına düşürük (orada da pil bölməsi var).
                val opened = runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }.isSuccess
                if (!opened) openAppSettings()
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}
