package com.cafarovceyxun.anamuslim.compose.components.share

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.Chip
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.shareImageColorAuto
import com.cafarovceyxun.anamuslim.resources.shareImageColorAutoHint
import com.cafarovceyxun.anamuslim.resources.shareImageColorBlack
import com.cafarovceyxun.anamuslim.resources.shareImageColorHue
import com.cafarovceyxun.anamuslim.resources.shareImageColorShade
import com.cafarovceyxun.anamuslim.resources.shareImageColorWhite
import org.jetbrains.compose.resources.stringResource

/**
 * «Yazı rəngi» aləti: **Avto** çipi, ağ/qara qısayolları və iki rəng xətkeşi (çalar + tündlük).
 *
 * ### Niyə iki xətkeş, HSV üçlüyü yox
 * Panelin hündürlüyü 196dp-lə məhduddur (bax [ShareImageEditorScreen]) və üçüncü xətkeş
 * doyğunluq üçün olardı — halbuki mətn rəngi kimi işə yarayan dəyərlər ya demək olar ağ/qara, ya da
 * tam doymuş çalarlardır. Ona görə doyğunluq **tündlüyə bağlıdır**: `shade` 0-da qara, 0.5-də xalis
 * çalar, 1-də ağdır — iki uc onsuz da ən çox istifadə olunan iki rəngi verir.
 *
 * ### RTL
 * `Brush.horizontalGradient` həmişə piksel üzrə sol→sağ çəkir, Material `Slider` isə dəyəri
 * istiqamətə görə yerləşdirir. Ərəbcə interfeysdə (bütün düzülüş RTL) ikisi bir-birinə uyğun
 * gəlməzdi — düymə bir rəngin üstündə dayanıb başqasını seçərdi. Ona görə dayaqlar RTL-də
 * çevrilir; bu, məzmuna görə deyil, **idarə elementinin özünə** görədir, ona görə burada
 * `LocalLayoutDirection` əzilmir.
 */
@Composable
internal fun ShareTextColorPanel(
    /** `null` = avtomatik rejim. */
    color: Color?,
    hue: Float,
    shade: Float,
    onAuto: () -> Unit,
    onPick: (hue: Float, shade: Float) -> Unit,
    /**
     * Avto rejimin izahı görünsünmü. Fon şəkli olmayanda avto sadəcə temanın rənglərini saxlayır —
     * yəni «Avto» seçili qalır, amma heç nə dəyişmir; söndürülmüş qaraltma xətkeşi ilə eyni tələ,
     * eyni həll.
     */
    showAutoHint: Boolean,
) {
    val current = color ?: shareTextColor(hue, shade)

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Chip(
                selected = color == null,
                label = { Text(stringResource(Res.string.shareImageColorAuto)) },
                onClick = onAuto,
            )

            // Ağ/qara xətkeşin uclarıdır, ona görə qısayol həm rəngi seçir, həm də xətkeşi ora
            // sürüşdürür — əks halda düymə ilə xətkeşin göstərdiyi rəng ayrılırdı.
            ColorSwatch(
                color = Color.White,
                selected = color == Color.White,
                contentDescription = stringResource(Res.string.shareImageColorWhite),
                onClick = { onPick(hue, 1f) },
            )
            ColorSwatch(
                color = Color.Black,
                selected = color == Color.Black,
                contentDescription = stringResource(Res.string.shareImageColorBlack),
                onClick = { onPick(hue, 0f) },
            )
        }

        Spacer(Modifier.height(6.dp))

        PanelLabel(stringResource(Res.string.shareImageColorHue))
        ColorRuler(
            value = hue / 360f,
            onValueChange = { onPick((it * 360f).coerceIn(0f, 360f), shade) },
            stops = remember { HueStops },
            thumbColor = Color.hsv(hue.coerceIn(0f, 360f), 1f, 1f),
        )

        PanelLabel(stringResource(Res.string.shareImageColorShade))
        ColorRuler(
            value = shade,
            onValueChange = { onPick(hue, it) },
            stops = remember(hue) {
                listOf(Color.Black, Color.hsv(hue.coerceIn(0f, 360f), 1f, 1f), Color.White)
            },
            thumbColor = current,
        )

        if (showAutoHint && color == null) {
            Text(
                text = stringResource(Res.string.shareImageColorAutoHint),
                style = typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Çalar (0…360) və tündlükdən (0 = qara, 0.5 = xalis çalar, 1 = ağ) son rəngi hesablayır.
 *
 * Xalis çalar **ortada** dayanır ki, hər iki uc — ağ və qara — bir xətkeşdən əlçatan olsun; bu iki
 * rəng şəkil üstündə ən çox istifadə olunanlardır.
 */
internal fun shareTextColor(hue: Float, shade: Float): Color {
    val pure = Color.hsv(hue.coerceIn(0f, 360f), 1f, 1f)
    val clamped = shade.coerceIn(0f, 1f)
    return if (clamped <= 0.5f) {
        lerp(Color.Black, pure, clamped / 0.5f)
    } else {
        lerp(pure, Color.White, (clamped - 0.5f) / 0.5f)
    }
}

/** Rəng xətkeşinin başlanğıc dəyərləri — sıfırlama düyməsi də bunlara qayıdır. */
internal const val ShareDefaultHue = 45f
internal const val ShareDefaultShade = 0.92f

/** Göy qurşağı dayaqları: hər 60°-dən bir, hər iki ucda eyni qırmızı ki, keçid kəsilməsin. */
private val HueStops = listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f)
    .map { Color.hsv(it, 1f, 1f) }

/**
 * Gradiyent yollu xətkeş. Material `Slider`-in öz `track`/`thumb` yuvalarını doldurur — sürükləmə,
 * toxunuşla tullanma və semantika hazır gəlir; sıfırdan `pointerInput` yazsaydıq, üçünü də əl ilə
 * bərpa etmək lazım olardı.
 */
// `track`/`thumb` yuvaları olan `Slider` aşırılması hələ eksperimentaldır; alternativ öz jest
// idarəçini yazmaqdır — sürükləmə, toxunuşla tullanma və semantikanı əl ilə bərpa etmək demək.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorRuler(
    value: Float,
    onValueChange: (Float) -> Unit,
    stops: List<Color>,
    thumbColor: Color,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val painted = if (rtl) stops.asReversed() else stops

    Slider(
        value = value.coerceIn(0f, 1f),
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp),
        track = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(painted))
                    .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(50)),
            )
        },
        thumb = {
            // Ağ halqa: seçilən rəng yolun üstündəki qonşu rənglə eyni olanda düymə itirdi.
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(thumbColor),
            )
        },
    )
}

/** Panelin ağ/qara qısayolları. */
@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colorScheme.primary else colorScheme.outlineVariant,
                shape = CircleShape,
            )
            .padding(3.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClickLabel = contentDescription, onClick = onClick),
    )
}
