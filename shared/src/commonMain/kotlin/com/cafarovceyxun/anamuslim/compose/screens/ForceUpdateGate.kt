package com.cafarovceyxun.anamuslim.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.compose.utils.appLocaleFlow
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.ic_arrow_up
import com.cafarovceyxun.anamuslim.resources.strLabelUpdate
import com.cafarovceyxun.anamuslim.resources.strLabelWhatsNew
import com.cafarovceyxun.anamuslim.resources.strMsgAppUpdateRequired
import com.cafarovceyxun.anamuslim.resources.strTitleAppUpdateRequired
import com.cafarovceyxun.anamuslim.utils.supabase.AppRelease
import com.cafarovceyxun.anamuslim.utils.app.AppUpdateChecker
import com.cafarovceyxun.anamuslim.utils.app.AppUpdateStatus
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Keeps a build below `min_version` from being used at all: while the row demands a newer build,
 * this draws in place of [content] instead of over it, so there is no screen underneath to reach.
 *
 * Android does the same job in its own hosting Activity (`UpdateManager.check4CriticalUpdate`),
 * where a non-cancellable dialog goes up before the first frame. iOS has no Activity to hang that
 * on, and the shared `AppUpdateBanner` only drops its close button in the required case — a banner
 * sits on the homepage, so the user could simply move to another tab and carry on. That is why the
 * gate exists as its own composable: on iOS it is the *only* thing `min_version` acts through.
 *
 * A build whose row has not arrived yet is not blocked — [AppUpdateChecker] answers `NONE` from an
 * empty cache, and the fetch started here raises the gate a moment later if it is warranted. A
 * fresh install with no network therefore stays usable, which is the right way round: the block is
 * a deliberate, published decision, never the default when nothing is known.
 */
@Composable
fun ForceUpdateGate(content: @Composable () -> Unit) {
    val release by AppUpdateChecker.release.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { AppUpdateChecker.refresh() }

    val update = release
    if (update == null || AppUpdateChecker.statusOf(update) != AppUpdateStatus.REQUIRED) {
        content()
        return
    }

    ForceUpdateScreen(update)
}

@Composable
private fun ForceUpdateScreen(update: AppRelease) {
    val locale by appLocaleFlow.collectAsStateWithLifecycle()
    val notes = remember(update, locale) { update.releaseNotesFor(locale.fallbackLanguageCodes()) }
    val accent = colorScheme.error

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Same badge the banner builds: `dr_icon_update_app` is a two-tone glyph that collapses
            // into a solid disc when it is tinted flat, so a plain arrow is drawn on a tinted circle
            // instead and follows the theme.
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent.alpha(0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_arrow_up),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(30.dp),
                )
            }

            Text(
                text = stringResource(Res.string.strTitleAppUpdateRequired),
                modifier = Modifier.padding(top = 6.dp),
                style = typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Text(
                text = stringResource(Res.string.strMsgAppUpdateRequired),
                style = typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            update.latest_version_name?.takeIf { it.isNotBlank() }?.let { versionName ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent.alpha(0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    // Composed in code rather than as a string resource, exactly as the banner does:
                    // a version number reads the same in every language.
                    Text(text = "v$versionName", style = typography.labelSmall, color = accent)
                }
            }

            if (notes.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(shapes.large)
                        .background(colorScheme.onSurface.alpha(0.05f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.strLabelWhatsNew),
                        style = typography.labelMedium,
                        color = colorScheme.onSurface.alpha(0.75f),
                    )

                    notes.forEach { note ->
                        Row {
                            Text(text = "•", style = typography.bodySmall, color = accent)
                            Text(
                                text = note,
                                modifier = Modifier.padding(start = 8.dp),
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // No listing to send anyone to means no button: the text above still says what is wrong,
            // while a button that opens nothing would only look broken.
            AppUpdateChecker.actionUrl(update)?.let { actionUrl ->
                Button(
                    onClick = { PlatformUtils.browseLink(actionUrl) },
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .height(44.dp),
                    shape = shapes.small,
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 0.dp),
                ) {
                    Text(text = stringResource(Res.string.strLabelUpdate), style = typography.labelLarge)
                }
            }
        }
    }
}
