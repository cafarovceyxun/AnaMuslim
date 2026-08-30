package com.cafarovceyxun.anamuslim.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.AppBarDefaults
import com.cafarovceyxun.anamuslim.compose.components.common.appBarInsetsPadding
import com.cafarovceyxun.anamuslim.compose.components.common.appBarRowHeight
import com.cafarovceyxun.anamuslim.compose.utils.isLandscape
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.app_name
import com.cafarovceyxun.anamuslim.resources.ic_launcher_foreground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppBar() {
    val isLandscape = isLandscape()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = AppBarDefaults.ShadowElevation,
        color = colorScheme.surfaceContainer
    ) {
        TopAppBar(
            // Insets are cleared once on the padded wrapper; the row keeps a clean content height.
            modifier = Modifier.appBarInsetsPadding().appBarRowHeight(),
            windowInsets = WindowInsets(0, 0, 0, 0),
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
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
        )
    }
}
