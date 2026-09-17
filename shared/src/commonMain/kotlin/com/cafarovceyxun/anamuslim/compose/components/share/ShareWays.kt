package com.cafarovceyxun.anamuslim.compose.components.share

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_aspect_ratio
import com.cafarovceyxun.anamuslim.resources.dr_icon_share
import com.cafarovceyxun.anamuslim.resources.icon_copy
import com.cafarovceyxun.anamuslim.resources.shareAsImage
import com.cafarovceyxun.anamuslim.resources.shareAsText
import com.cafarovceyxun.anamuslim.resources.shareCopyText
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * «Necə paylaşaq?» — mətn, şəkil, pano.
 *
 * Hədis və ayə vərəqləri eyni sıranı işlədir: mətn hazırlandıqdan sonra qalan sual yalnız yoldur.
 * Üç eyni ölçülü xana bir sətirdədir — əvvəl üç ayrı rəngli, tam enli düymə üst-üstə düzülürdü və
 * vərəqin yarısını tuturdu; ölçü, rəng və yer baxımından heç biri digərindən vacib deyil, ona görə
 * hamısı eyni görünür.
 *
 * Ortaq komponent olması təsadüfi deyil: iki vərəqin sırası ayrı yazılsaydı, biri dəyişəndə o biri
 * səssizcə geridə qalardı.
 */
@Composable
fun ShareWaysRow(
    onShareAsText: () -> Unit,
    onShareAsImage: () -> Unit,
    onCopyText: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Şəkil xanası basıla bilirmi.
     *
     * Dua vərəqi mətn əl ilə redaktə olunanda `false` verir: şəkil kartı bloklardan qurulur və
     * yazılan mətni daşıya bilmir — xana sönmüş görünür ki, basıb **başqa** mətn almasın. Sönmüş
     * xananın səbəbi vərəqdə bir sətirlə yazılır.
     */
    imageEnabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ShareWayTile(
            icon = Res.drawable.dr_icon_share,
            label = Res.string.shareAsText,
            onClick = onShareAsText,
        )

        ShareWayTile(
            icon = Res.drawable.dr_icon_aspect_ratio,
            label = Res.string.shareAsImage,
            onClick = onShareAsImage,
            enabled = imageEnabled,
        )

        ShareWayTile(
            icon = Res.drawable.icon_copy,
            label = Res.string.shareCopyText,
            onClick = onCopyText,
        )
    }
}

@Composable
private fun RowScope.ShareWayTile(
    icon: DrawableResource,
    label: StringResource,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val text = stringResource(label)

    Column(
        modifier = Modifier
            .weight(1f)
            .clip(shapes.large)
            .background(colorScheme.surfaceVariant.alpha(if (enabled) 0.4f else 0.2f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = text,
            modifier = Modifier.size(24.dp),
            tint = if (enabled) colorScheme.primary else colorScheme.primary.alpha(0.35f),
        )

        Text(
            text = text,
            style = typography.labelMedium,
            color = if (enabled) colorScheme.onSurface else colorScheme.onSurface.alpha(0.35f),
            textAlign = TextAlign.Center,
        )
    }
}
