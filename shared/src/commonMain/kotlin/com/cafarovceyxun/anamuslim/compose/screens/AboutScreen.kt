package com.cafarovceyxun.anamuslim.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.api.ApiConfig
import com.cafarovceyxun.anamuslim.api.NetworkConfig
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val TELEGRAM_URL = "https://t.me/mhymnn"

@Composable
fun AboutScreen() {
    // Every support link is this project's own. It used to prefer a remotely-served urls.json, but
    // that file belongs to the upstream project, so a successful fetch sent users to
    // quran.alfaazplus.com — including for the privacy policy, which must be ours.
    Scaffold(
        topBar = { AppBar(stringResource(Res.string.strTitleAboutUs)) }
    ) { padding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            AboutHero()

            AboutGroup {
                AboutRow(
                    icon = Res.drawable.dr_icon_bug,
                    title = stringResource(Res.string.strTitleSendFeedback),
                ) {
                    PlatformUtils.browseLink(ApiConfig.GITHUB_ISSUES_BUG_REPORT_URL)
                }
                RowDivider()
                AboutRow(
                    icon = Res.drawable.dr_icon_help,
                    title = stringResource(Res.string.strTitleHelpSupport),
                ) {
                    PlatformUtils.browseLink(ApiConfig.GITHUB_ISSUES_URL)
                }
                RowDivider()
                AboutRow(
                    icon = Res.drawable.dr_icon_privacy_policy,
                    title = stringResource(Res.string.strTitlePrivacyPolicy),
                ) {
                    PlatformUtils.browseLink(ApiConfig.GITHUB_PRIVACY_POLICY_URL)
                }
            }

            AboutGroup {
                AboutRow(
                    icon = Res.drawable.icon_github_2,
                    title = stringResource(Res.string.github),
                    iconTint = colorScheme.onSurface,
                ) {
                    PlatformUtils.browseLink(ApiConfig.GITHUB_REPOSITORY_URL)
                }
                RowDivider()
                AboutRow(
                    icon = Res.drawable.icon_telegram,
                    title = stringResource(Res.string.telegram),
                ) {
                    PlatformUtils.browseLink(TELEGRAM_URL)
                }
            }
        }
    }
}

@Composable
private fun AboutHero() {
    val versionName = NetworkConfig.appVersionName()
    val versionLabel = versionName.ifBlank { stringResource(Res.string.strTitleAppVersion) }
    val copiedMsg = stringResource(Res.string.copiedToClipboard)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colorScheme.primary.alpha(0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(76.dp),
                tint = Color.Unspecified
            )
        }

        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface
        )

        // Tap the version chip to copy it — handy when filing a bug report.
        Surface(
            onClick = {
                PlatformUtils.copyToClipboard(versionName)
                PlatformUtils.showToast(copiedMsg)
            },
            shape = RoundedCornerShape(50),
            color = colorScheme.surfaceContainerHigh,
            contentColor = colorScheme.onSurfaceVariant,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = versionLabel,
                    style = MaterialTheme.typography.labelMedium,
                )
                Icon(
                    painter = painterResource(Res.drawable.icon_copy),
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

@Composable
private fun AboutGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colorScheme.surfaceContainer),
        content = content,
    )
}

@Composable
private fun AboutRow(
    icon: DrawableResource,
    title: String,
    enabled: Boolean = true,
    iconTint: Color? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.5f)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(colorScheme.primary.alpha(0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                tint = iconTint ?: colorScheme.primary
            )
        }

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = colorScheme.onSurface
        )

        Icon(
            painter = painterResource(Res.drawable.dr_icon_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = colorScheme.onSurfaceVariant.alpha(0.6f)
        )
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 66.dp),
        thickness = 1.dp,
        color = colorScheme.outlineVariant.alpha(0.4f)
    )
}
