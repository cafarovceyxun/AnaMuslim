package com.cafarovceyxun.anamuslim.compose.components.homepage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cafarovceyxun.anamuslim.compose.components.common.IconButton
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.compose.utils.appLocaleFlow
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.ic_arrow_up
import com.cafarovceyxun.anamuslim.resources.strLabelClose
import com.cafarovceyxun.anamuslim.resources.strLabelUpdate
import com.cafarovceyxun.anamuslim.resources.strLabelWhatsNew
import com.cafarovceyxun.anamuslim.resources.strMsgAppUpdateAvailable
import com.cafarovceyxun.anamuslim.resources.strMsgAppUpdateRequired
import com.cafarovceyxun.anamuslim.resources.strTitleAppUpdateAvailable
import com.cafarovceyxun.anamuslim.resources.strTitleAppUpdateRequired
import com.cafarovceyxun.anamuslim.utils.app.AppUpdateChecker
import com.cafarovceyxun.anamuslim.utils.app.AppUpdateStatus
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Tells the reader, at the top of the homepage, that a newer build has been published — see
 * [AppUpdateChecker] for where that answer comes from.
 *
 * Closing it silences the current session only — the banner is back on the next launch and keeps
 * asking until the update is actually installed. A build below `min_version` never reaches this
 * banner in the first place: both hosts block that case before the homepage exists — Android in
 * `UpdateManager.check4CriticalUpdate`, iOS in `ForceUpdateGate`.
 */
@Composable
fun AppUpdateBanner() {
    val release by AppUpdateChecker.release.collectAsStateWithLifecycle()
    val dismissedVersion by AppUpdateChecker.dismissedVersion.collectAsStateWithLifecycle()
    val locale by appLocaleFlow.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        AppUpdateChecker.refresh()
    }

    val update = release
    val status = AppUpdateChecker.statusOf(update)
    if (update == null || status == AppUpdateStatus.NONE) return

    val isRequired = status == AppUpdateStatus.REQUIRED
    if (!isRequired && dismissedVersion >= update.latest_version) return

    val notes = remember(update, locale) { update.releaseNotesFor(locale.fallbackLanguageCodes()) }
    val accent = if (isRequired) colorScheme.error else colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp)
            .clip(shapes.large)
            .background(accent.alpha(0.07f))
            .border(0.8.dp, accent.alpha(0.35f), shapes.large)
            .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // `dr_icon_update_app` is a two-tone glyph (white disc + arrow) drawn for the coloured
            // dialog; tinted flat it collapses into a solid circle. The badge here rebuilds that
            // look from a plain arrow, so it follows the theme.
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent.alpha(0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_arrow_up),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp),
                )
            }

            Text(
                text = stringResource(
                    if (isRequired) Res.string.strTitleAppUpdateRequired
                    else Res.string.strTitleAppUpdateAvailable
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
                style = typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
            )

            update.latest_version_name?.takeIf { it.isNotBlank() }?.let { versionName ->
                VersionChip(versionName = versionName, accent = accent)
            }

            if (isRequired) {
                // No way out of a required update. The spacer keeps the title's width the same in
                // both states, so the banner does not reflow when one turns into the other.
                Spacer(Modifier.size(36.dp))
            } else {
                IconButton(
                    painter = painterResource(Res.drawable.dr_icon_close),
                    contentDescription = stringResource(Res.string.strLabelClose),
                    tint = colorScheme.onSurfaceVariant,
                    small = true,
                    onClick = { AppUpdateChecker.dismiss(update.latest_version) },
                )
            }
        }

        Text(
            text = stringResource(
                if (isRequired) Res.string.strMsgAppUpdateRequired
                else Res.string.strMsgAppUpdateAvailable
            ),
            modifier = Modifier.padding(end = 8.dp),
            style = typography.bodySmall,
            color = colorScheme.onSurfaceVariant,
        )

        if (notes.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.strLabelWhatsNew),
                modifier = Modifier.padding(top = 4.dp),
                style = typography.labelMedium,
                color = colorScheme.onSurface.alpha(0.75f),
            )

            notes.forEach { note ->
                Row(modifier = Modifier.padding(end = 8.dp)) {
                    Text(
                        text = "•",
                        style = typography.bodySmall,
                        color = accent,
                    )
                    Text(
                        text = note,
                        modifier = Modifier.padding(start = 8.dp),
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // No listing to open at all — the rest of the banner still informs, but a button that goes
        // nowhere would not.
        val actionUrl = AppUpdateChecker.actionUrl(update)
        if (actionUrl != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, end = 6.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(
                onClick = { PlatformUtils.browseLink(actionUrl) },
                modifier = Modifier.height(34.dp),
                shape = shapes.small,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
            ) {
                Text(
                    text = stringResource(Res.string.strLabelUpdate),
                    style = typography.labelMedium,
                )
            }
        }
        }
    }
}

@Composable
private fun VersionChip(versionName: String, accent: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(accent.alpha(0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            // Composed in code rather than as a string resource: a version number reads the same in
            // every language, and a placeholder here would need translating four times over.
            text = "v$versionName",
            style = typography.labelSmall,
            color = accent,
        )
    }
}
