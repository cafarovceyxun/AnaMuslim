package com.cafarovceyxun.anamuslim.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.app_name
import com.cafarovceyxun.anamuslim.resources.ic_launcher_foreground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppBar() {
    val windowInfo = LocalWindowInfo.current
    val isLandscape = windowInfo.containerSize.width > windowInfo.containerSize.height

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        shadowElevation = 1.dp,
        color = colorScheme.surfaceContainer
    ) {
        TopAppBar(
            modifier = if (isLandscape) Modifier.height(48.dp) else Modifier,
            title = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_launcher_foreground),
                        contentDescription = stringResource(Res.string.app_name),
                        tint = Color.Unspecified,
                        modifier = Modifier.size(if (isLandscape) 40.dp else 56.dp)
                    )

                    Text(
                        text = stringResource(Res.string.app_name),
                        style = if (isLandscape) typography.titleMedium else typography.titleLarge,
                        color = colorScheme.onSurface,
                        fontWeight = FontWeight.Black,
                    )
                }
            },
            actions = {
                IndexMenuButton()
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
        )
    }
}
