package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_down
import org.jetbrains.compose.resources.painterResource

/**
 * Axan siyahı rejimlərində (ərəbcə / tərcümə) ekranın üstündə üzən cari bab adı kutusu.
 *
 * Uzun siyahıda aşağı sürüşəndə app bar gizlənir və başlığı aparır — bu kutu həmin başlığı əvəz
 * edir ki, hansı babda olduğun görünsün. Görünmə app bar-ın çökməsindən yox, birbaşa siyahının
 * sürüşməsindən asılıdır ([visible]) — ona görə tam ekran rejimində də (app bar ümumiyyətlə yoxdur)
 * düzgün işləyir. Görkəm [HadithAppBar]-dakı Navigator pill-i ilə eynidir (yaşıl mətn + aşağı ox),
 * amma altında daha qabarıq kölgə var ki, mətnin üstündə üzdüyü bilinsin.
 *
 * @param title cari babın adı ([currentTitle]); boşdursa çəkilmir.
 * @param visible siyahı yuxarıdan sürüşübmü — `true` olanda kutu fade ilə görünür, `false`-da itir.
 */
@Composable
fun HadithChapterPill(
    title: String,
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible && title.isNotBlank()) 1f else 0f,
        label = "hadith-chapter-pill-alpha",
    )
    // Fade bitib kutu tam gizlənəndə ümumiyyətlə çəkmirik: alpha 0 Surface hələ də toxunuşu tutub
    // üstdəki tək-toxunuş «xromu göstər» jestini udardı.
    if (alpha <= 0.02f) return

    val pillShape = RoundedCornerShape(12.dp)
    Surface(
        onClick = onClick,
        shape = pillShape,
        // Tam qapaq (şəffaf deyil): altındakı mətn kutunun içindən görünməsin — istifadəçi düz rəng
        // istədi. `surfaceVariant` mövzuya uyğun opaq rəngdir, sabit yaşıl mətnlə hər iki temada oxunur.
        color = colorScheme.surfaceVariant,
        tonalElevation = 3.dp,
        // Öz kölgəsi yox: aşağıdakı `Modifier.shadow` tam qara ambient/spot rəngi ilə daha tünd
        // kölgə verir ki, qaranlıq fonda da mətnin üstündə üzdüyü aydın görünsün.
        shadowElevation = 0.dp,
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
            .shadow(
                elevation = 16.dp,
                shape = pillShape,
                clip = false,
                ambientColor = Color.Black,
                spotColor = Color.Black,
            )
            .clip(pillShape)
            .widthIn(max = 320.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            AnimatedContent(
                targetState = title,
                transitionSpec = {
                    (slideInVertically { it } + fadeIn()) togetherWith
                        (slideOutVertically { -it } + fadeOut()) using
                        SizeTransform(clip = false)
                },
                label = "hadith-chapter-pill",
            ) { animatedTitle ->
                Text(
                    text = animatedTitle,
                    style = typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                painter = painterResource(Res.drawable.dr_icon_chevron_down),
                contentDescription = null,
                tint = Color(0xFF2E7D32).alpha(0.6f),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(16.dp),
            )
        }
    }
}
