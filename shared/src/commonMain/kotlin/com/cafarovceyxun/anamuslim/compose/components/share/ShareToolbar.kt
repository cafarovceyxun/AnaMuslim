package com.cafarovceyxun.anamuslim.compose.components.share

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_align_center
import com.cafarovceyxun.anamuslim.resources.dr_icon_aspect_ratio
import com.cafarovceyxun.anamuslim.resources.dr_icon_layers
import com.cafarovceyxun.anamuslim.resources.dr_icon_opacity
import com.cafarovceyxun.anamuslim.resources.dr_icon_quran_script
import com.cafarovceyxun.anamuslim.resources.dr_icon_theme
import com.cafarovceyxun.anamuslim.resources.icon_font_size
import com.cafarovceyxun.anamuslim.resources.shareImageAlignLabel
import com.cafarovceyxun.anamuslim.resources.shareImageBackgroundLabel
import com.cafarovceyxun.anamuslim.resources.shareImageFormatLabel
import com.cafarovceyxun.anamuslim.resources.shareImageScrimLabel
import com.cafarovceyxun.anamuslim.resources.strLabelContent
import com.cafarovceyxun.anamuslim.resources.strTitleScripts
import com.cafarovceyxun.anamuslim.resources.textSizesLabel
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * [ShareImageEditorScreen]-in alət sırası: dairə içində ikon, altında etiket.
 *
 * Niyə ayrı fayl və `ShareEditorParts.kt`-də deyil: həmin faylın müqaviləsi «**hər iki** redaktorun
 * paylaşdığı hissələr» olduğu halda toolbar hazırda yalnız ayə/hədis redaktorundadır. Namaz cədvəli
 * redaktoru onu sonra qəbul etsə, fayl olduğu yerdə qalır.
 *
 * Bir anda **bir** alət seçilidir və yalnız onun paneli açıqdır. Əvvəl bütün tənzimləmələr eyni
 * anda açıq bir sütunda idi (`heightIn(max = 320.dp)` + `verticalScroll`) — alçaq ekranlarda
 * xətkeşlər qatlanma xəttinin altında qalırdı, yəni etiketlər görünür, idarə elementləri yox idi.
 */
internal enum class ShareTool(
    val icon: DrawableResource,
    val label: StringResource,
) {
    Background(Res.drawable.dr_icon_theme, Res.string.shareImageBackgroundLabel),
    Format(Res.drawable.dr_icon_aspect_ratio, Res.string.shareImageFormatLabel),
    Content(Res.drawable.dr_icon_layers, Res.string.strLabelContent),
    Align(Res.drawable.dr_icon_align_center, Res.string.shareImageAlignLabel),
    TextSize(Res.drawable.icon_font_size, Res.string.textSizesLabel),
    Scrim(Res.drawable.dr_icon_opacity, Res.string.shareImageScrimLabel),
    Font(Res.drawable.dr_icon_quran_script, Res.string.strTitleScripts),
}

@Composable
internal fun ShareToolbar(
    tools: List<ShareTool>,
    selected: ShareTool,
    onSelect: (ShareTool) -> Unit,
    modifier: Modifier = Modifier,
) {
    // ⚠️ `LazyRow`, `Row(horizontalScroll)` YOX. Ərəbcə interfeysdə bütün düzülüş RTL-dir və
    // `horizontalScroll`-un 0 mövqeyi hər halda **sol** kənardır — yəni sıra tərs ucundan açılırdı.
    // `LazyRow` başlanğıc mövqeyini istiqamətə görə özü tutur.
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(tools, key = { it.name }) { tool ->
            ShareToolButton(
                tool = tool,
                selected = tool == selected,
                onClick = { onSelect(tool) },
            )
        }
    }
}

@Composable
private fun ShareToolButton(
    tool: ShareTool,
    selected: Boolean,
    onClick: () -> Unit,
) {
    // Rəng keçidi animasiyalıdır ki, seçim panelin öz açılışı ilə eyni ritmdə oxunsun.
    val container by animateColorAsState(
        targetValue = if (selected) colorScheme.primary else colorScheme.surfaceContainerHighest,
        animationSpec = tween(200),
        label = "shareToolContainer",
    )
    val content by animateColorAsState(
        targetValue = if (selected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "shareToolContent",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) colorScheme.primary else colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "shareToolLabel",
    )

    Column(
        // Sabit en: etiketlər müxtəlif uzunluqdadır və eni məzmuna buraxsaq dairələr arasındakı
        // məsafə hər dildə fərqli olurdu.
        modifier = Modifier
            .width(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(container)
                .border(
                    width = 1.dp,
                    color = if (selected) colorScheme.primary else colorScheme.outlineVariant,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(tool.icon),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = content,
            )
        }

        Text(
            text = stringResource(tool.label),
            style = typography.labelSmall,
            color = labelColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
