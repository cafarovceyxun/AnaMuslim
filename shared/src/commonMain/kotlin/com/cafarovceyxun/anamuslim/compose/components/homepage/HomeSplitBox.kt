package com.cafarovceyxun.anamuslim.compose.components.homepage

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafarovceyxun.anamuslim.compose.theme.LocalAppTextScale
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strLabelViewAll
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Ana ekranda **yan-yana** duran iki qutu — «Dua və zikr / Əsmaül Hüsnə» sırasının məntiqi.
 *
 * Boş tərəf çəkilmir: [left] və ya [right] `null` qaytaranda qalan qutu **bütün eni** tutur, yəni
 * yarısı boş sıra qalmır. İkisi də boşdursa sıra ümumiyyətlə emit olunmur.
 *
 * ⚠️ `IntrinsicSize.Min` şərtdir: onsuz hər qutu öz məzmununa görə ölçülür və qısa siyahısı olan
 * tərəf alçaq qalır. `HomeSectionDua`-dakı ilə eyni səbəb.
 */
@Composable
fun HomeSplitRow(
    left: (@Composable RowScope.() -> Unit)?,
    right: (@Composable RowScope.() -> Unit)?,
) {
    if (left == null && right == null) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp)
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        left?.invoke(this)
        right?.invoke(this)
    }
}

/**
 * Yarım enli qutu: başlıq, bir neçə sətir və altda «Hamısına bax».
 *
 * «Hamısına bax» başlığın yanında deyil, **altdadır**: yarım endə başlıq mətni ilə düymə yan-yana
 * sığmır və biri o birini kəsirdi.
 */
@Composable
fun RowScope.HomeSplitBox(
    icon: DrawableResource,
    title: String,
    onViewAll: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(shapes.large)
            .border(0.8.dp, colorScheme.outlineVariant.alpha(0.45f), shapes.large)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = colorScheme.primary,
            )
            Text(
                text = title,
                style = typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp), content = content)

        // Sətirlərin sayı/hündürlüyü iki qutuda fərqli olur (uzun hədis başlığı iki sətrə düşür),
        // ona görə «Hamısına bax» məzmunun ardınca yox, qutunun **dibinə** bərkidilir — əks halda
        // iki düymə fərqli hündürlükdə dayanıb sıranı əyri göstərirdi.
        Spacer(Modifier.weight(1f))

        Text(
            text = stringResource(Res.string.strLabelViewAll),
            style = typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clip(shapes.small)
                .clickable(onClick = onViewAll)
                .padding(vertical = 4.dp),
        )
    }
}

/** Qutunun içindəki bir sətir — başlıq və altında kiçik izah (tarix, ayə nömrəsi və s.). */
@Composable
fun HomeSplitBoxEntry(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .clickable(onClick = onClick),
        color = colorScheme.surfaceVariant.alpha(0.2f),
    ) {
        Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)) {
            Text(
                text = title,
                style = typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = typography.labelSmall
                        .copy(fontSize = 10.sp * LocalAppTextScale.current),
                    color = colorScheme.onSurface.alpha(0.45f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Yan-yana qutuda neçə sətir göstərilir — yarım endə ikidən çoxu sıranı hündür edir. */
const val HOME_SPLIT_BOX_PREVIEW_LIMIT = 2
