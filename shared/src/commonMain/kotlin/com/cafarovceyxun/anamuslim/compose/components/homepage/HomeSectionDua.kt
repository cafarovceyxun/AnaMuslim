package com.cafarovceyxun.anamuslim.compose.components.homepage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cafarovceyxun.anamuslim.compose.screens.dua.AsmaScreen
import com.cafarovceyxun.anamuslim.compose.screens.dua.DuaScreen
import com.cafarovceyxun.anamuslim.compose.screens.hadith.withScriptDirection
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.asmaSectionTitle
import com.cafarovceyxun.anamuslim.resources.dr_logo_dua
import com.cafarovceyxun.anamuslim.resources.dr_logo_asma
import com.cafarovceyxun.anamuslim.resources.duaSectionTitle
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Ana ekrandakı iki kiçik kart: **Dua və zikr** və **Əsmaül Hüsnə**.
 *
 * Əvvəl Namaz vaxtları ekranının içində idi (2026-09-15); istifadəçi onları ana ekrana, namaz
 * vaxtlarının altına istədi. Ona görə indi **öz bölməsidir** ([HomeSection.DUA]) — namaz bölməsinin
 * içində olsaydı, istifadəçi namaz vaxtlarını gizlədəndə dua da səssizcə yoxa çıxardı.
 *
 * Başlıq sətri yoxdur: iki kart özü başlıqdır, üstünə «Dua və zikr» yazmaq eyni sözü iki dəfə
 * göstərərdi.
 *
 * Ekranların özü buradan **tam ekran `Dialog`** kimi açılır — yeni route və yeni `Activity` lazım
 * olmasın deyə (`PrayerShareEditorScreen` ilə eyni qurğu).
 */
@Composable
fun HomeSectionDua() {
    var showDuas by remember { mutableStateOf(false) }
    var showAsma by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // ⚠️ `IntrinsicSize.Min` olmadan hər kart öz mətninə görə ölçülür və uzun adı olan kart
        // hündür qalır — iki kart eyni boyda olmalıdır.
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CompactEntryCard(
                icon = Res.drawable.dr_logo_dua,
                title = stringResource(Res.string.duaSectionTitle),
                onClick = { showDuas = true },
            )
            CompactEntryCard(
                icon = Res.drawable.dr_logo_asma,
                title = stringResource(Res.string.asmaSectionTitle),
                onClick = { showAsma = true },
            )
        }
    }

    if (showDuas) {
        FullScreenSurface(onDismiss = { showDuas = false }) {
            DuaScreen(onBack = { showDuas = false })
        }
    }

    if (showAsma) {
        FullScreenSurface(onDismiss = { showAsma = false }) {
            AsmaScreen(onBack = { showAsma = false })
        }
    }
}

/**
 * Giriş kartı — dairəvi ikon, altında ad.
 *
 * Şaquli düzülüş və **rəngli fon**: ana ekranda qonşu bölmələr ağ/neytral kartlardır, bu ikisi isə
 * məzmuna yeganə keçiddir — eyni tonda qalsaydılar sıranın içində itərdi. Ölçü də ona görə
 * böyükdür: kart nə qədər kiçikdirsə, bölmənin mövcudluğu bir o qədər təsadüfən tapılır.
 */
@Composable
private fun RowScope.CompactEntryCard(
    title: String,
    onClick: () -> Unit,
    /**
     * Nişan — mövzu rəngi ilə boyanır.
     *
     * Loqolar **alfa-maskalı** rastrdır (qara xətt + şəffaf fon), ona görə `Icon(tint = …)` onları
     * tam boyayır: işıqlı mövzuda tünd yaşıl, qaranlıqda açıq yaşıl çıxır. Rəngli (qızıl-yaşıl)
     * variant mövzudan asılı olmazdı — buna görə rəng loqonun içində yox, `tint`-dədir.
     */
    icon: DrawableResource,
) {
    Surface(
        onClick = onClick,
        color = colorScheme.primaryContainer.alpha(0.3f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, colorScheme.primary.alpha(0.28f)),
        modifier = Modifier.weight(1f).fillMaxHeight(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(BADGE_SIZE),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorScheme.primary.alpha(0.16f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(BADGE_SIZE * 0.62f),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = title,
                style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    .withScriptDirection(arabic = false),
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Tam ekran səth — məzmunu öz pəncərəsində göstərir.
 *
 * `Dialog(usePlatformDefaultWidth = false)` `PrayerShareEditorScreen`-dəki qurğudur: geri jesti
 * pəncərəni bağlayır, məzmun isə ekranı bütöv tutur. Inline emit ediləndə ana ekranın sürüşən
 * sütununun içində qalardı.
 */
@Composable
private fun FullScreenSurface(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colorScheme.background,
            content = content,
        )
    }
}

/** Kart nişanının ölçüsü — loqo da, kontur ikonun fonu da eyni dairədir. */
private val BADGE_SIZE = 48.dp
