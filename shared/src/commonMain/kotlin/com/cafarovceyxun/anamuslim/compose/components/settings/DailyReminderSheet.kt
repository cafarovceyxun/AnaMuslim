package com.cafarovceyxun.anamuslim.compose.components.settings

import com.cafarovceyxun.anamuslim.resources.dr_icon_heart_filled
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.msgVerseReminderNotifPermission
import com.cafarovceyxun.anamuslim.resources.notification_permission
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelGotIt
import com.cafarovceyxun.anamuslim.resources.strLabelOpenSettings
import com.cafarovceyxun.anamuslim.resources.strTitleVOTD
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.RadioItem
import com.cafarovceyxun.anamuslim.resources.dailyReminderMsg
import com.cafarovceyxun.anamuslim.resources.strLabelOff
import com.cafarovceyxun.anamuslim.resources.strLabelOn
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheet
import com.cafarovceyxun.anamuslim.compose.utils.app.openAppSettings
import com.cafarovceyxun.anamuslim.compose.utils.app.rememberNotificationPermission
import com.cafarovceyxun.anamuslim.compose.utils.DailyReminderProvider
import com.cafarovceyxun.anamuslim.compose.utils.preferences.VersePreferences
import kotlinx.coroutines.launch

@Composable
fun DailyReminderSheet(
    isOpen: Boolean,
    onClose: () -> Unit,
) {
    val votdEnabled = VersePreferences.observeVOTDReminderEnabled()
    var showPermissionDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val permissionState = rememberNotificationPermission()

    val items = listOf(
        Triple(true, Res.string.strLabelOn, Res.string.dailyReminderMsg),
        Triple(false, Res.string.strLabelOff, null),
    )

    LaunchedEffect(permissionState) {
        if (permissionState != null && !permissionState.isGranted) {
            VersePreferences.setVOTDReminderEnabled(false)
            DailyReminderProvider.scheduler.cancel()
        }
    }

    suspend fun validate(newStatus: Boolean): Boolean {
        if (newStatus == false) {
            return true
        }

        if (permissionState != null) {
            if (!permissionState.isGranted) {
                showPermissionDialog = true
                return false
            }
        }

        return true
    }


    BottomSheet(
        isOpen = isOpen,
        onDismiss = onClose,
        icon = Res.drawable.dr_icon_heart_filled,
        title = stringResource(Res.string.strTitleVOTD),
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            items.forEach { (key, title, desc) ->
                RadioItem(
                    title = title,
                    subtitle = desc,
                    selected = key == votdEnabled,
                    onClick = {
                        coroutineScope.launch {
                            if (validate(key)) {
                                VersePreferences.setVOTDReminderEnabled(key)

                                if (key == true) {
                                    DailyReminderProvider.scheduler.schedule()
                                } else {
                                    DailyReminderProvider.scheduler.cancel()
                                }
                            }
                        }

                        onClose()
                    },
                )
            }
        }
    }

    AlertDialog(
        isOpen = showPermissionDialog,
        onClose = { showPermissionDialog = false },
        title = stringResource(Res.string.notification_permission),
        actions = listOf(
            AlertDialogAction(
                text = stringResource(Res.string.strLabelCancel)
            ),
            AlertDialogAction(
                text = stringResource(
                    if (permissionState?.canPrompt != false) {
                        Res.string.strLabelGotIt
                    } else {
                        Res.string.strLabelOpenSettings
                    }
                ),
                style = AlertDialogActionStyle.Primary,
                onClick = {
                    // Qərar KLİK anında oxunur — dialoq açılanda hesablanan snepşot istifadəçi arxa
                    // fondan qayıdanda köhnəlmiş olurdu.
                    permissionState?.let {
                        if (it.canPrompt) it.request() else openAppSettings()
                    }
                    showPermissionDialog = false
                }
            )
        ),
        content = {
            Text(
                text = stringResource(Res.string.msgVerseReminderNotifPermission),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
    )
}
