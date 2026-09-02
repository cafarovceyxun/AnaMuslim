package com.cafarovceyxun.anamuslim.compose.components.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * [subtitleAnnotated] — alt sətrin stilli variantı; verilibsə [subtitle]-i əvəz edir (bax
 * [ListItemContent]). Sətrin yalnız bir hissəsi rənglənəndə lazımdır: hədis «rəvayətləri
 * qarşılaşdır» açarının qırmızı xəbərdarlığı belədir.
 */
@Composable
fun SwitchItem(
    modifier: Modifier = Modifier,
    title: StringResource,
    subtitle: StringResource? = null,
    subtitleAnnotated: AnnotatedString? = null,
    icon: DrawableResource? = null,
    checked: Boolean,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .clip(shape)
            .clickable(enabled = enabled) {
                if (enabled) onCheckedChange(!checked)
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        ListItemContent(
            titleStr = stringResource(title),
            subtitleStr = subtitle?.let { stringResource(it) },
            modifier = Modifier.weight(1f),
            subtitleAnnotated = subtitleAnnotated,
        )
        Switch(
            modifier = Modifier
                .padding(start = 16.dp)
                .height(24.dp),
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}
