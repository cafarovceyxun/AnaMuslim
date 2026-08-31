package com.cafarovceyxun.anamuslim.compose.components.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.ListItem
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_right
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsItem(
    modifier: Modifier = Modifier,
    title: StringResource? = null,
    titleStr: String? = null,
    subtitle: StringResource? = null,
    subtitleStr: String? = null,
    icon: DrawableResource? = null,
    iconImage: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    flat: Boolean = false,
    onClick: () -> Unit
) {
    ListItem(
        modifier = modifier,
        flat = flat,
        leading = {
            if (icon != null) SettingsItemIcon(
                icon = icon,
                contentDescription = titleStr ?: (if (title != null) stringResource(title) else "")
            )
            else if (iconImage != null) iconImage()
        },
        trailing = {
            SettingsItemArrow()
        },
        titleStr = titleStr ?: title?.let { stringResource(it) },
        subtitleStr = subtitleStr ?: subtitle?.let { stringResource(it) },
        enabled = enabled,
        onClick = onClick
    )
}

/**
 * Sətrin sol nişanı. Ölçü **açıq verilir**: `Icon` ölçüsüz qalanda vektorun öz ölçüsünü alır,
 * ayarların nişanları isə eyni ölçüdə deyil — `dr_icon_feature` 75dp-lik kətandır və «Təkliflər»
 * sətri qalan hamısından üç dəfə böyük loqo ilə çıxırdı. Kompilyator da, testlər də bunu tutmur.
 */
@Composable
fun SettingsItemIcon(
    icon: DrawableResource,
    contentDescription: String
) {
    Icon(
        painter = painterResource(icon),
        contentDescription = contentDescription,
        modifier = Modifier.size(SettingsIconSize),
    )
}

/** Ayarlar sətrinin nişan ölçüsü — Material siyahı nişanı ilə eyni. */
val SettingsIconSize = 24.dp

@Composable
fun SettingsItemArrow() {
    Icon(
        painter = painterResource(Res.drawable.dr_icon_chevron_right),
        contentDescription = null,
        modifier = Modifier.padding(start = 15.dp)
    )
}
