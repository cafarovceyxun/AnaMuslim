package com.cafarovceyxun.anamuslim.compose.components.prayer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafarovceyxun.anamuslim.compose.components.share.ShareCustomBackgroundScrim
import com.cafarovceyxun.anamuslim.compose.components.share.ShareImageTheme
import com.cafarovceyxun.anamuslim.compose.components.share.ShareScrim
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.app_name
import com.cafarovceyxun.anamuslim.resources.dr_logo_android
import com.cafarovceyxun.anamuslim.resources.dr_logo_apple
import com.cafarovceyxun.anamuslim.resources.dr_qr_app_store
import com.cafarovceyxun.anamuslim.resources.dr_qr_play_store
import com.cafarovceyxun.anamuslim.resources.ic_launcher_foreground
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Mağaza QR-lərinin yeri — istifadəçi seçir.
 *
 * [TOP] başlığın iki kənarında və bir az böyükdür (paylaşılan şəkil hekayə kimi baxılanda üst hissə
 * daha çox görünür), [BOTTOM] altlıqda loğonun yanında kiçikdir, [NONE] isə ümumiyyətlə çəkmir.
 */
enum class PrayerMonthQr { NONE, TOP, BOTTOM }

/** Bir günün sətri: qəməri gün nömrəsi, miladi qarşılığı və altı vaxt. */
data class PrayerMonthRow(
    val lunarDay: Int,
    val gregorian: String,
    val times: List<String>,
)

/** Kartda göstəriləcək bütöv qəməri ay. */
data class PrayerMonthContent(
    val monthName: String,
    val year: Int,
    val placeName: String,
    /** Yer adının altındakı qeyd (oruc/imsak xəbərdarlığı). Boş sətir = qeyd çəkilmir. */
    val note: String,
    /** Tarix sütununun başlığı. */
    val dateColumn: String,
    /** Qalan sütun başlıqları — altı namaz adı. */
    val columns: List<String>,
    val rows: List<PrayerMonthRow>,
)

/**
 * Namaz vaxtlarının **qəməri ay** cədvəli — paylaşılan şəkil.
 *
 * Ayə/hədis kartından ([com.cafarovceyxun.anamuslim.compose.components.share.ShareImageCard]) iki
 * yerdə ayrılır və hər ikisi məzmunun formasındandır:
 *
 *  1. **Hündürlük sabit deyil.** Qəməri ay 29 və ya 30 gündür, ona görə kətan `başlıq + sətir ×
 *     [RowHeight] + altlıq` kimi hesablanır ([heightPxFor]). Hazır nisbətə (9:16) sığdırsaydıq ya
 *     sətirlər oxunmaz kiçilər, ya da altda böyük boşluq qalardı.
 *  2. **Mətn sığdırılmır, ölçülər sabitdir.** Cədvəldə hər sütun eyni enə malikdir və məzmun
 *     `HH:mm`-dir — avtomatik ölçü seçimi burada sətirdən sətrə fərqli şrift verərdi.
 *
 * Sabit sıxlıq qaydası isə eynidir: kart `Density(1f, 1f)` altında qurulur, yəni `1.dp == 1.sp ==
 * 1px` və fayl hər cihazda eyni ölçüdə çıxır. Düzülüş **həmişə LTR**-dir; ərəb interfeysində
 * `LocalLayoutDirection` RTL olsaydı sütunlar güzgülənərdi (CLAUDE.md, 2026-08-20).
 */
@Composable
fun PrayerMonthShareCard(
    content: PrayerMonthContent,
    theme: ShareImageTheme,
    customBackground: androidx.compose.ui.graphics.ImageBitmap?,
    showBranding: Boolean,
    qr: PrayerMonthQr,
    modifier: Modifier = Modifier,
) {
    val appName = stringResource(Res.string.app_name)
    val heightPx = heightPxFor(content.rows.size, showBranding, qr, content.note.isNotBlank())

    // ⚠️ Sıxlıq [PrayerMonthRenderScale]-dir, 1 deyil: aşağıdakı bütün ölçülər **məntiqi**
    // pikseldir və faylda hər biri bu əmsala vurulur. Yəni kart 1080 yox, 1080 × əmsal enində
    // çıxır — böyük ölçüdə çap üçün. Ayrıca «çap variantı» qurmaq əvəzinə sıxlığı dəyişmək
    // kifayətdir: `sp` də sıxlıqla miqyaslandığı üçün mətn və düzülüş nisbətləri toxunulmaz qalır.
    CompositionLocalProvider(
        LocalDensity provides Density(density = PrayerMonthRenderScale, fontScale = 1f),
        LocalLayoutDirection provides LayoutDirection.Ltr,
    ) {
        Box(
            modifier = modifier
                .requiredSize(CardWidth.dp, heightPx.dp)
                .background(Brush.verticalGradient(theme.gradient)),
        ) {
            if (customBackground != null) {
                Image(
                    bitmap = customBackground,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                ShareScrim(ShareCustomBackgroundScrim)
            } else theme.photo?.let { photo ->
                Image(
                    painter = painterResource(photo),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                ShareScrim(theme.scrim)
            }

            Column(modifier = Modifier.fillMaxSize()) {
                Header(content = content, theme = theme, qr = qr)

                if (content.note.isNotBlank()) {
                    BasicText(
                        text = content.note,
                        style = TextStyle(
                            color = theme.secondaryText,
                            fontSize = 26.sp,
                            lineHeight = 34.sp,
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(NoteHeight.dp)
                            .padding(horizontal = 64.dp, vertical = 10.dp),
                    )
                }

                ColumnHeader(
                    dateColumn = content.dateColumn,
                    columns = content.columns,
                    theme = theme,
                )

                content.rows.forEachIndexed { index, row ->
                    DayRow(row = row, theme = theme, even = index % 2 == 0)
                }

                // Altlıq QR aşağıda olanda da lazımdır: loğo söndürülübsə sətir boş qalır, amma
                // QR-lərin duracağı yer qalmalıdır.
                if (showBranding || qr == PrayerMonthQr.BOTTOM) {
                    Spacer(Modifier.weight(1f))
                    Branding(
                        appName = appName.takeIf { showBranding },
                        theme = theme,
                        showQr = qr == PrayerMonthQr.BOTTOM,
                    )
                }
            }
        }
    }
}

/**
 * Kətanın hündürlüyü — kart çəkilməzdən **əvvəl** bilinməlidir, çünki önizləmə miqyası və yazılan
 * qat eyni ölçünü işlədir.
 */
fun heightPxFor(
    rowCount: Int,
    showBranding: Boolean,
    qr: PrayerMonthQr,
    hasNote: Boolean,
): Int {
    val footer = if (showBranding || qr == PrayerMonthQr.BOTTOM) BrandingHeight else BottomPadding
    val note = if (hasNote) NoteHeight else 0

    return HeaderHeight + note + ColumnHeaderHeight + rowCount * RowHeight + footer
}

/**
 * Kətanın **məntiqi** ölçüsünün faylda neçəyə vurulduğu.
 *
 * 2 = 2160px en; 300 DPI-da təxminən 18 sm, 200 DPI-da 27 sm — A4 çapı üçün bəs edir. Daha
 * yuxarı qaldırmaq mümkündür, amma bitmap sahəsi kvadratik böyüyür: 30 sətirlik ay artıq
 * 2160×4400 (≈9 MP, ≈37 MB) çıxır və 3-də 84 MB-a qalxır ki, bu da telefonda paylaşma anında
 * risklidir.
 */
const val PrayerMonthRenderScale = 2f

/** Kartın faylda çıxan piksel eni. Önizləmə miqyası da bunu işlədir. */
val PrayerMonthPixelWidth: Int get() = (CardWidth * PrayerMonthRenderScale).toInt()

/** [heightPxFor]-un məntiqi nəticəsini faylın həqiqi piksel hündürlüyünə çevirir. */
fun pixelHeightOf(logicalHeight: Int): Int = (logicalHeight * PrayerMonthRenderScale).toInt()

private const val CardWidth = 1080
private const val HeaderHeight = 392
private const val ColumnHeaderHeight = 74
private const val RowHeight = 52
private const val BrandingHeight = 132
private const val BottomPadding = 28
private const val NoteHeight = 116

/** «Tarix» sütunu qalan altısından enlidir: içində həm qəməri gün, həm miladi tarix var. */
private const val DateColumnWeight = 2.6f

/** Başlıqdakı ayırıcı xəttin eni — QR-lər kənarlarda olanda da qalan sahəyə sığır. */
private const val HeaderRuleWidth = 220

private const val TopQrSize = 116
private const val BottomQrSize = 74

/**
 * Başlıq: **il üstdə, ay adı ortada** — nümunədəki düzülüş. Ad böyük, il ondan xeyli kiçikdir ki,
 * baxan gözü əvvəlcə aya düşsün.
 */
@Composable
private fun Header(content: PrayerMonthContent, theme: ShareImageTheme, qr: PrayerMonthQr) {
    val topQr = qr == PrayerMonthQr.TOP

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HeaderHeight.dp)
            .padding(horizontal = 48.dp, vertical = 34.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(36.dp))
                .background(theme.accent.copy(alpha = 0.16f)),
        ) {
            if (topQr) {
                QrCode(
                    image = Res.drawable.dr_qr_app_store,
                    logo = Res.drawable.dr_logo_apple,
                    sizePx = TopQrSize,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 28.dp),
                )
                QrCode(
                    image = Res.drawable.dr_qr_play_store,
                    logo = Res.drawable.dr_logo_android,
                    sizePx = TopQrSize,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 28.dp),
                )
            }

        Column(
            modifier = Modifier
                .fillMaxSize()
                // QR-lər kənarlarda duranda mətn onların altına girməməlidir; ay adı da bir
                // pillə kiçilir, yoxsa «Cəmadiyələvvəl» qalan enə sığmır.
                .padding(horizontal = if (topQr) 186.dp else 32.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BasicText(
                text = content.year.toString(),
                style = TextStyle(
                    color = theme.accent,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 8.sp,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
            )

            // İl ilə ay adının arasındakı ayırıcı: ikisi alt-alta durduğu üçün «1448
            // Rəbiül-əvvəl» tək sətir kimi oxuna bilirdi. Xətt hansının il, hansının ay olduğunu
            // baxışdan ayırır.
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .width(HeaderRuleWidth.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(theme.accent.copy(alpha = 0.75f)),
            )
            Spacer(Modifier.height(12.dp))

            BasicText(
                text = content.monthName,
                style = TextStyle(
                    color = theme.text,
                    fontSize = (if (topQr) 70 else 92).sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (content.placeName.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                BasicText(
                    text = content.placeName,
                    style = TextStyle(
                        color = theme.secondaryText,
                        fontSize = 34.sp,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        }
    }
}

/**
 * Mağaza QR-i — mərkəzində platformanın loğosu ilə.
 *
 * ⚠️ Loğo QR-in bir hissəsini **örtür**, ona görə kodlar `error="h"` (30% düzəliş) ilə
 * generasiya olunub (`tools`-suz, birdəfəlik: `segno.make(url, error="h")`). Aşağı səviyyədə
 * (M, 15%) mərkəzi örtmək kodu oxunmaz edərdi — və bunu nə kompilyator, nə test tutar, yalnız
 * telefonla skan edəndə bilinər.
 *
 * Loğonun altındakı ağ lövhə də qəsdəndir: qara modulların üstündə birbaşa duran qara loğo
 * seçilmir, ağ fon isə skanere «boş sahə» kimi görünür və düzəliş bunu onsuz da bərpa edir.
 */
@Composable
private fun QrCode(
    image: DrawableResource,
    logo: DrawableResource,
    sizePx: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(sizePx.dp)
            .clip(RoundedCornerShape((sizePx * 0.09f).dp)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )

        Box(
            modifier = Modifier
                .size((sizePx * QrLogoFraction).dp)
                .clip(RoundedCornerShape((sizePx * 0.06f).dp))
                .background(Color.White)
                .padding((sizePx * 0.03f).dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(logo),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(Color.Black),
            )
        }
    }
}

/** Loğo lövhəsinin QR-ə nisbəti. 0.28-dən yuxarı `error="h"` düzəlişini də aşır. */
private const val QrLogoFraction = 0.26f

@Composable
private fun ColumnHeader(dateColumn: String, columns: List<String>, theme: ShareImageTheme) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ColumnHeaderHeight.dp)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cell(
            text = dateColumn,
            color = theme.secondaryText,
            weight = DateColumnWeight,
            align = TextAlign.Left,
            bold = true,
        )
        columns.forEach { label ->
            Cell(text = label, color = theme.secondaryText, weight = 1f, bold = true)
        }
    }
}

@Composable
private fun DayRow(row: PrayerMonthRow, theme: ShareImageTheme, even: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(RowHeight.dp)
            .padding(horizontal = 24.dp)
            // Zebra: kölgə fondan deyil, vurğu rəngindən alınır — foto fonda da sətirlər seçilir.
            .background(theme.accent.copy(alpha = if (even) 0.13f else 0.06f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DateCell(row = row, theme = theme)
        row.times.forEach { time ->
            Cell(text = time, color = theme.text, weight = 1f)
        }
    }
}

/**
 * Tarix xanası: qəməri gün **qutuda**, miladi qarşılığı yanında.
 *
 * Əvvəl «1 (14 avq 2026)» tək sətir idi və qəməri günlə miladi tarix bir-birinə qarışırdı —
 * qutu ikisini bir baxışda ayırır və sütunun sol kənarını da düzləndirir.
 */
@Composable
private fun androidx.compose.foundation.layout.RowScope.DateCell(
    row: PrayerMonthRow,
    theme: ShareImageTheme,
) {
    Row(
        modifier = Modifier.weight(DateColumnWeight).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(DayBoxWidth.dp)
                .height(DayBoxHeight.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(theme.accent.copy(alpha = 0.26f)),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = row.lunarDay.toString(),
                style = TextStyle(
                    color = theme.text,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
            )
        }

        Spacer(Modifier.width(12.dp))

        BasicText(
            text = row.gregorian,
            style = TextStyle(color = theme.text, fontSize = 30.sp),
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

private const val DayBoxWidth = 54
private const val DayBoxHeight = 36

@Composable
private fun androidx.compose.foundation.layout.RowScope.Cell(
    text: String,
    color: Color,
    weight: Float,
    align: TextAlign = TextAlign.Center,
    bold: Boolean = false,
) {
    BasicText(
        text = text,
        style = TextStyle(
            color = color,
            fontSize = 30.sp,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = align,
        ),
        maxLines = 1,
        overflow = TextOverflow.Clip,
        modifier = Modifier.weight(weight).padding(horizontal = 6.dp),
    )
}

@Composable
private fun Branding(appName: String?, theme: ShareImageTheme, showQr: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(BrandingHeight.dp)
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (appName != null) {
            // Loqo rənglənmir: `ic_launcher_foreground` tam nişandır, `tint` onu bir rəngli disk edir.
            Image(
                painter = painterResource(Res.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.width(12.dp))
            BasicText(
                text = appName,
                style = TextStyle(
                    color = theme.secondaryText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp,
                ),
                maxLines = 1,
            )
        }

        if (showQr) {
            Spacer(Modifier.width(20.dp))
            QrCode(Res.drawable.dr_qr_app_store, Res.drawable.dr_logo_apple, BottomQrSize)
            Spacer(Modifier.width(10.dp))
            QrCode(Res.drawable.dr_qr_play_store, Res.drawable.dr_logo_android, BottomQrSize)
        }
    }
}
