package com.cafarovceyxun.anamuslim.compose.components.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_search
import com.cafarovceyxun.anamuslim.resources.searchEverywhereFor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * İndeks ekranlarındakı süzgəc qutusunun altındakı keçid: «bu sözü **hər yerdə** axtar».
 *
 * Səbəb: Quran və hədis indekslərindəki qutu yalnız **adları** süzür (surə adı, cild/kitab/bab
 * adı) — istifadəçi ora söz yazıb nəticə gözləyir, ekran isə boş qalır. Sətir həmin anda sualın
 * cavabını verir: axtarış ekranı eyni sorğu ilə açılır və mətnlərin içində axtarır.
 *
 * Yalnız sorğu boş olmayanda və axtarış seam-i qeydiyyatdan keçəndə göstərilməlidir
 * ([com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks.openSearch]) — yoxsa sətir basılır və
 * heç nə olmur.
 */
@Composable
fun SearchEverywhereRow(
    query: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = shapes.small,
        color = colorScheme.surfaceVariant.alpha(0.35f),
        contentColor = colorScheme.onSurface,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.alpha(0.7f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_search),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = colorScheme.primary,
            )

            Text(
                text = stringResource(Res.string.searchEverywhereFor, query),
                style = typography.bodyMedium,
                color = colorScheme.onSurface.alpha(0.9f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
