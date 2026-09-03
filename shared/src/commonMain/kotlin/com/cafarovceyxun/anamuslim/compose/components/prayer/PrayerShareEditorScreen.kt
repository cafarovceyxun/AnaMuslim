package com.cafarovceyxun.anamuslim.compose.components.prayer

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.Chip
import com.cafarovceyxun.anamuslim.compose.components.share.CustomBackgroundSwatch
import com.cafarovceyxun.anamuslim.compose.components.share.PanelLabel
import com.cafarovceyxun.anamuslim.compose.components.share.SharePreviewCanvas
import com.cafarovceyxun.anamuslim.compose.components.share.ShareImageTheme
import com.cafarovceyxun.anamuslim.compose.components.share.ShareImageThemes
import com.cafarovceyxun.anamuslim.compose.components.share.ThemeSwatch
import com.cafarovceyxun.anamuslim.compose.components.share.shareCapturedCard
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_left
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_right
import com.cafarovceyxun.anamuslim.resources.dr_icon_download
import com.cafarovceyxun.anamuslim.resources.dr_icon_share
import com.cafarovceyxun.anamuslim.resources.lunarDateColumn
import com.cafarovceyxun.anamuslim.resources.lunarNextMonth
import com.cafarovceyxun.anamuslim.resources.lunarPreviousMonth
import com.cafarovceyxun.anamuslim.resources.prayerShareChooser
import com.cafarovceyxun.anamuslim.resources.prayerShareNoLocation
import com.cafarovceyxun.anamuslim.resources.prayerShareQrBottom
import com.cafarovceyxun.anamuslim.resources.prayerShareQrLabel
import com.cafarovceyxun.anamuslim.resources.prayerShareQrNone
import com.cafarovceyxun.anamuslim.resources.prayerShareQrTop
import com.cafarovceyxun.anamuslim.resources.prayerShareFastingNote
import com.cafarovceyxun.anamuslim.resources.prayerShareSave
import com.cafarovceyxun.anamuslim.resources.prayerShareSaveFailed
import com.cafarovceyxun.anamuslim.resources.prayerShareSaved
import com.cafarovceyxun.anamuslim.resources.prayerShareTitle
import com.cafarovceyxun.anamuslim.resources.strLabelShare
import com.cafarovceyxun.anamuslim.resources.shareImageBackgroundLabel
import com.cafarovceyxun.anamuslim.resources.shareImageBrandLabel
import com.cafarovceyxun.anamuslim.resources.shareImageFailed
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.formatLocalDateMedium
import com.cafarovceyxun.anamuslim.utils.prayer.LunarMonth
import com.cafarovceyxun.anamuslim.utils.prayer.Prayer
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerDay
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerSettings
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerTimes
import com.cafarovceyxun.anamuslim.utils.univ.rememberImagePicker
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Namaz vaxtlarının bütöv qəməri ay cədvəlini şəkil kimi paylaşır.
 *
 * Ayə/hədis redaktorunun **panelini** təkrarlamır: cədvəldə düzülüş, yazı ölçüsü və format
 * xətkeşlərinin mənası yoxdur (sətir sayı ayın uzunluğundan gəlir), ona görə burada yalnız fon
 * dəsti, ay keçidi və loqo seçimi var. Önizləmə miqyası, qatın yazılması və paylaşma isə ortaqdır
 * ([SharePreviewCanvas], [shareCapturedCard]).
 *
 * Tam ekranlı olduğu üçün öz `Dialog` pəncərəsindədir — CLAUDE.md-dəki «tam ekran səth `Dialog`
 * olmalıdır» qaydası.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PrayerShareEditorScreen(
    settings: PrayerSettings,
    onBack: () -> Unit,
) = Dialog(
    onDismissRequest = onBack,
    properties = DialogProperties(
        dismissOnBackPress = true,
        dismissOnClickOutside = false,
        usePlatformDefaultWidth = false,
    ),
) {
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    var themeIndex by remember { mutableIntStateOf(0) }
    var customBackground by remember { mutableStateOf<ImageBitmap?>(null) }
    var showBranding by remember { mutableStateOf(true) }
    var qr by remember { mutableStateOf(PrayerMonthQr.TOP) }
    var sharing by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    // Göstərilən ayın lövbəri. Ay dəyişmək üçün sərhəddən bir gün kənara çıxırıq — ay uzunluğunu
    // (29/30) burada saymağa ehtiyac qalmır.
    var anchor by remember { mutableLongStateOf(currentEpochMillis()) }

    val span = LunarMonth.spanContaining(anchor, settings.lunarOffsetDays)
    val columns = Prayer.entries.map { PrayerUiFormat.label(it) }
    val dateColumn = stringResource(Res.string.lunarDateColumn)
    val fastingNote = stringResource(Res.string.prayerShareFastingNote)
    val monthName = span?.let { stringResource(PrayerUiFormat.hijriMonthName(it.month)) }.orEmpty()

    val content = remember(span, settings, columns, dateColumn, monthName, fastingNote) {
        span?.let { buildContent(it, settings, dateColumn, columns, monthName, fastingNote) }
    }

    val imagePicker = rememberImagePicker { picked ->
        if (picked != null) customBackground = picked
    }

    val failedMsg = stringResource(Res.string.shareImageFailed)
    val chooserTitle = stringResource(Res.string.prayerShareChooser)
    val savedMsg = stringResource(Res.string.prayerShareSaved)
    val saveFailedMsg = stringResource(Res.string.prayerShareSaveFailed)

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { AppBar(title = stringResource(Res.string.prayerShareTitle), onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(colorScheme.background),
        ) {
            if (content == null) {
                // Yer təyin edilməyib (və ya platforma qəməri çevirməni dəstəkləmir): boş kart
                // göstərmək əvəzinə səbəbi yazırıq.
                Text(
                    text = stringResource(Res.string.prayerShareNoLocation),
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(32.dp),
                )
            } else {
                SharePreviewCanvas(
                    widthPx = PrayerMonthPixelWidth,
                    heightPx = pixelHeightOf(
                        heightPxFor(
                            rowCount = content.rows.size,
                            showBranding = showBranding,
                            qr = qr,
                            hasNote = content.note.isNotBlank(),
                        )
                    ),
                    graphicsLayer = graphicsLayer,
                    modifier = Modifier.weight(1f),
                ) { cardModifier ->
                    PrayerMonthShareCard(
                        content = content,
                        theme = PrayerMonthThemes[themeIndex],
                        customBackground = customBackground,
                        showBranding = showBranding,
                        qr = qr,
                        modifier = cardModifier,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.surfaceContainerLow)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                MonthStepper(
                    label = if (span == null) "" else "$monthName ${span.year}",
                    onPrevious = { span?.let { anchor = it.previousAnchor } },
                    onNext = { span?.let { anchor = it.nextAnchor } },
                )

                Spacer(Modifier.height(12.dp))
                PanelLabel(stringResource(Res.string.shareImageBackgroundLabel))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PrayerMonthThemes.forEachIndexed { index, theme ->
                        ThemeSwatch(
                            theme = theme,
                            selected = customBackground == null && index == themeIndex,
                            onClick = {
                                themeIndex = index
                                customBackground = null
                            },
                        )
                    }
                    CustomBackgroundSwatch(
                        image = customBackground,
                        selected = customBackground != null,
                        onClick = { imagePicker.pick() },
                    )
                }

                Spacer(Modifier.height(12.dp))
                PanelLabel(stringResource(Res.string.prayerShareQrLabel))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Chip(
                        selected = showBranding,
                        label = { Text(stringResource(Res.string.shareImageBrandLabel)) },
                        onClick = { showBranding = !showBranding },
                    )
                    Chip(
                        selected = qr == PrayerMonthQr.NONE,
                        label = { Text(stringResource(Res.string.prayerShareQrNone)) },
                        onClick = { qr = PrayerMonthQr.NONE },
                    )
                    Chip(
                        selected = qr == PrayerMonthQr.TOP,
                        label = { Text(stringResource(Res.string.prayerShareQrTop)) },
                        onClick = { qr = PrayerMonthQr.TOP },
                    )
                    Chip(
                        selected = qr == PrayerMonthQr.BOTTOM,
                        label = { Text(stringResource(Res.string.prayerShareQrBottom)) },
                        onClick = { qr = PrayerMonthQr.BOTTOM },
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            if (saving || content == null) return@OutlinedButton

                            scope.launch {
                                saving = true
                                // Qat paylaşma ilə eyni yerdən oxunur — önizləmədə görünən nə
                                // varsa, fayla da o düşür.
                                val saved = runCatching {
                                    PlatformUtils.saveImageToGallery(
                                        image = graphicsLayer.toImageBitmap(),
                                        fileName = "AnaMuslim-$monthName-${span?.year ?: 0}",
                                    )
                                }.getOrDefault(false)
                                saving = false

                                PlatformUtils.showLongToast(if (saved) savedMsg else saveFailedMsg)
                            }
                        },
                        enabled = content != null && !saving,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_download),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(Res.string.prayerShareSave))
                    }

                    Button(
                        onClick = {
                            if (sharing || content == null) return@Button

                            scope.launch {
                                sharing = true
                                val shared = shareCapturedCard(
                                    graphicsLayer = graphicsLayer,
                                    chooserTitle = chooserTitle,
                                    logTag = "PrayerShareEditorScreen.share",
                                )
                                sharing = false

                                if (!shared) PlatformUtils.showLongToast(failedMsg)
                            }
                        },
                        enabled = content != null && !sharing,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        if (sharing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = colorScheme.onPrimary,
                            )
                        } else {
                            Icon(painterResource(Res.drawable.dr_icon_share), null, Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        // Ayə redaktorundakı uzun «Hazırdır, paylaş» burada yarı enli düyməyə
                        // sığmır və iki sətrə düşür — bu ekranda qısa etiket işlədilir.
                        Text(stringResource(Res.string.strLabelShare), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthStepper(label: String, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_chevron_left),
                contentDescription = stringResource(Res.string.lunarPreviousMonth),
            )
        }
        Text(text = label, style = typography.titleSmall, color = colorScheme.onSurface)
        IconButton(onClick = onNext) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_chevron_right),
                contentDescription = stringResource(Res.string.lunarNextMonth),
            )
        }
    }
}

/**
 * Ayın hər günü üçün cədvəli hesablayır.
 *
 * Saf funksiyadır və `remember` altında çağırılır: 30 gün × 6 vaxt astronomiya hesabı ucuzdur, amma
 * hər rekompozisiyada təkrarlansaydı önizləmənin sürüşməsi hiss olunardı.
 */
private fun buildContent(
    span: LunarMonth.Span,
    settings: PrayerSettings,
    dateColumn: String,
    columns: List<String>,
    monthName: String,
    note: String,
): PrayerMonthContent? {
    val point = settings.point ?: return null

    val rows = span.days.mapIndexedNotNull { index, atMillis ->
        val dateIso = PrayerDay.localDateOfDevice(atMillis)
        val day = PrayerTimes.calculate(dateIso, point, settings.params) ?: return@mapIndexedNotNull null

        PrayerMonthRow(
            lunarDay = index + 1,
            gregorian = formatLocalDateMedium(atMillis),
            times = Prayer.entries.map { prayer ->
                day[prayer]?.let { PrayerUiFormat.clock(it.atMillis) } ?: "—"
            },
        )
    }

    return rows.takeIf { it.isNotEmpty() }?.let {
        PrayerMonthContent(
            monthName = monthName,
            year = span.year,
            placeName = settings.placeName,
            note = note,
            dateColumn = dateColumn,
            columns = columns,
            rows = it,
        )
    }
}

/**
 * Təqvim fonları. Birinci **tünd yaşıl**dır (nümunədəki ton) və defoltdur; qalanları ayə/hədis
 * kartı ilə eyni dəstdir ki, tətbiqin paylaşdığı şəkillər bir ailəyə oxşasın.
 */
val PrayerMonthThemes: List<ShareImageTheme> = listOf(
    ShareImageTheme(
        gradient = listOf(
            androidx.compose.ui.graphics.Color(0xFF0B2420),
            androidx.compose.ui.graphics.Color(0xFF04100E),
        ),
        text = androidx.compose.ui.graphics.Color(0xFFEDF6F3),
        secondaryText = androidx.compose.ui.graphics.Color(0xFFA8C6BE),
        accent = androidx.compose.ui.graphics.Color(0xFF4FD1C5),
    ),
) + ShareImageThemes
