package com.cafarovceyxun.anamuslim.compose.components.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.Chip
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_refresh
import com.cafarovceyxun.anamuslim.resources.dr_icon_share
import com.cafarovceyxun.anamuslim.resources.labelArabic
import com.cafarovceyxun.anamuslim.resources.labelTranslation
import com.cafarovceyxun.anamuslim.resources.readyToShare
import com.cafarovceyxun.anamuslim.resources.reset
import com.cafarovceyxun.anamuslim.resources.shareImageAlignCenter
import com.cafarovceyxun.anamuslim.resources.shareImageAlignLabel
import com.cafarovceyxun.anamuslim.resources.shareImageAlignLeft
import com.cafarovceyxun.anamuslim.resources.shareImageAlignRight
import com.cafarovceyxun.anamuslim.resources.shareImageBackgroundLabel
import com.cafarovceyxun.anamuslim.resources.shareImageBrandLabel
import com.cafarovceyxun.anamuslim.resources.shareImageCustomBackground
import com.cafarovceyxun.anamuslim.resources.shareImageEmptyHint
import com.cafarovceyxun.anamuslim.resources.shareImageFailed
import com.cafarovceyxun.anamuslim.resources.shareImageLayoutLabel
import com.cafarovceyxun.anamuslim.resources.shareImageMarginLabel
import com.cafarovceyxun.anamuslim.resources.source
import com.cafarovceyxun.anamuslim.resources.textSizesLabel
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.utils.univ.rememberImagePicker
import com.cafarovceyxun.anamuslim.utils.AppLogger
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.min

/**
 * Xətkeşlərin aralığı və defoltları. «Yazı ölçüsü» mətnin kadrı **nə qədər doldurduğudur**: 1.0-da
 * mətn kətana maksimum sığdırılır, aşağı dəyərlər daha havadar görünüş verir. Defolt aralığın
 * ortasındadır ki, xətkeşi hər iki tərəfə çəkmək nəticə versin.
 */
private val TextScaleRange = 0.5f..1f
private const val DefaultTextScale = 0.82f
private val MarginRange = 0.4f..1.7f
private const val DefaultMargin = 1f

/**
 * Ayə və hədis şəkillərinin **ortaq** redaktoru: solda-sağda eyni önizləmə, eyni fon dəsti, eyni
 * paylaşma axını. Ayrıca `QuranImageEditorScreen`/`HadithImageEditorScreen` yalnız məzmunu bura
 * ötürən nazik adapterlərdir.
 *
 * Tam ekranlı olduğu üçün öz `Dialog` pəncərəsində açılır (CLAUDE.md-dəki «tam ekran səth `Dialog`
 * olmalıdır» qaydası): redaktoru açan vərəqlər `ReaderProvider`-in altında yaşayır və inline emit
 * ediləndə modal vərəqin pəncərəsinin altında qalırdı.
 *
 * ### Önizləmə niyə belə qurulub
 * Kart həmişə tam ölçüsündə (1080px) qurulur, ekranda isə `graphicsLayer` miqyası ilə kiçildilir.
 * `graphicsLayer.record` miqyasın **içindəki** qatda işlədiyi üçün paylaşılan fayl tam
 * ölçüdə çıxır, önizləmə isə eyni şeyi göstərir — WYSIWYG. Əvvəlki qurğu kartı kiçik qutuya
 * **kəsirdi**, ona görə istifadəçi şrifti 5-ə salmadan mətn kadra sığmırdı.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ShareImageEditorScreen(
    title: String,
    chooserTitle: String,
    content: ShareImageContent,
    arabicFontFamily: FontFamily?,
    initialShowArabic: Boolean,
    initialShowTranslation: Boolean,
    onBack: () -> Unit,
) = Dialog(
    onDismissRequest = onBack,
    properties = DialogProperties(
        dismissOnBackPress = true,
        // Tam ekrandır — «kənar» yoxdur, təsadüfi toxunuş redaktoru bağlamamalıdır.
        dismissOnClickOutside = false,
        usePlatformDefaultWidth = false,
    ),
) {
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    val hasArabicText = content.segments.any { it.arabic.isNotBlank() }
    val hasTranslationText = content.segments.any { it.translation.isNotBlank() }
    val hasReferenceText = content.reference.isNotBlank()

    var themeIndex by remember { mutableIntStateOf(0) }
    var ratio by remember { mutableStateOf(ShareImageRatio.Story) }
    var align by remember { mutableStateOf(ShareImageAlign.Center) }
    var customBackground by remember { mutableStateOf<ImageBitmap?>(null) }
    var textScale by remember { mutableFloatStateOf(DefaultTextScale) }
    var margin by remember { mutableFloatStateOf(DefaultMargin) }
    var showArabic by remember { mutableStateOf(initialShowArabic && hasArabicText) }
    var showTranslation by remember { mutableStateOf(initialShowTranslation && hasTranslationText) }
    var showReference by remember { mutableStateOf(hasReferenceText) }
    var showBranding by remember { mutableStateOf(true) }
    var sharing by remember { mutableStateOf(false) }

    val style = ShareImageStyle(
        theme = ShareImageThemes[themeIndex],
        ratio = ratio,
        textScale = textScale,
        margin = margin,
        align = align,
        customBackground = customBackground,
        showArabic = showArabic,
        showTranslation = showTranslation,
        showReference = showReference,
        showBranding = showBranding,
    )

    val imagePicker = rememberImagePicker { picked ->
        // İmtina edəndə `null` gəlir — mövcud fonu silmirik, sadəcə heç nə etmirik.
        if (picked != null) customBackground = picked
    }

    val canShare = (showArabic && hasArabicText) || (showTranslation && hasTranslationText)
    val failedMsg = stringResource(Res.string.shareImageFailed)

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppBar(
                title = title,
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = {
                            themeIndex = 0
                            ratio = ShareImageRatio.Story
                            align = ShareImageAlign.Center
                            customBackground = null
                            textScale = DefaultTextScale
                            margin = DefaultMargin
                            showArabic = initialShowArabic && hasArabicText
                            showTranslation = initialShowTranslation && hasTranslationText
                            showReference = hasReferenceText
                            showBranding = true
                        },
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.dr_icon_refresh),
                            contentDescription = stringResource(Res.string.reset),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(colorScheme.background),
        ) {
            CardPreview(
                content = content,
                style = style,
                arabicFontFamily = arabicFontFamily,
                graphicsLayer = graphicsLayer,
                modifier = Modifier.weight(1f),
            )

            // Panel iki hissədir: tənzimləmələr sürüşür, paylaşma düyməsi isə **sürüşmür**.
            // Alçaq ekranlarda (landscape) tək sürüşən sütun düyməni kadrdan çıxarırdı.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.surfaceContainerLow)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Düzülüş sırası əlavə olunandan sonra 260dp xətkeşləri qatlanma xəttinin
                        // altında saxlayırdı — etiketlər görünür, xətkeşlər yox idi.
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    PanelLabel(stringResource(Res.string.shareImageBackgroundLabel))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ShareImageThemes.forEachIndexed { index, theme ->
                            ThemeSwatch(
                                theme = theme,
                                selected = index == themeIndex && customBackground == null,
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
                    PanelLabel(stringResource(Res.string.shareImageLayoutLabel))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ShareImageRatio.entries.forEach { entry ->
                            Chip(
                                selected = ratio == entry,
                                label = { Text(entry.label) },
                                onClick = { ratio = entry },
                            )
                        }

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .width(1.dp)
                                .height(24.dp)
                                .background(colorScheme.outlineVariant),
                        )

                        if (hasArabicText) {
                            Chip(
                                selected = showArabic,
                                label = { Text(stringResource(Res.string.labelArabic)) },
                                onClick = { showArabic = !showArabic },
                            )
                        }
                        if (hasTranslationText) {
                            Chip(
                                selected = showTranslation,
                                label = { Text(stringResource(Res.string.labelTranslation)) },
                                onClick = { showTranslation = !showTranslation },
                            )
                        }
                        if (hasReferenceText) {
                            Chip(
                                selected = showReference,
                                label = { Text(stringResource(Res.string.source)) },
                                onClick = { showReference = !showReference },
                            )
                        }
                        Chip(
                            selected = showBranding,
                            label = { Text(stringResource(Res.string.shareImageBrandLabel)) },
                            onClick = { showBranding = !showBranding },
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    PanelLabel(stringResource(Res.string.shareImageAlignLabel))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Chip(
                            selected = align == ShareImageAlign.Left,
                            label = { Text(stringResource(Res.string.shareImageAlignLeft)) },
                            onClick = { align = ShareImageAlign.Left },
                        )
                        Chip(
                            selected = align == ShareImageAlign.Center,
                            label = { Text(stringResource(Res.string.shareImageAlignCenter)) },
                            onClick = { align = ShareImageAlign.Center },
                        )
                        Chip(
                            selected = align == ShareImageAlign.Right,
                            label = { Text(stringResource(Res.string.shareImageAlignRight)) },
                            onClick = { align = ShareImageAlign.Right },
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        EditorSlider(
                            label = stringResource(Res.string.textSizesLabel),
                            value = textScale,
                            onValueChange = { textScale = it },
                            range = TextScaleRange,
                        )
                        EditorSlider(
                            label = stringResource(Res.string.shareImageMarginLabel),
                            value = margin,
                            onValueChange = { margin = it },
                            range = MarginRange,
                        )
                    }
                }

                if (!canShare) {
                    Text(
                        text = stringResource(Res.string.shareImageEmptyHint),
                        style = typography.bodySmall,
                        color = colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                Button(
                    onClick = {
                        if (sharing) return@Button

                        scope.launch {
                            sharing = true
                            val shared = try {
                                PlatformUtils.shareImage(graphicsLayer.toImageBitmap(), chooserTitle)
                            } catch (e: Exception) {
                                AppLogger.saveError(e, "ShareImageEditorScreen.share")
                                false
                            }
                            sharing = false

                            if (!shared) PlatformUtils.showLongToast(failedMsg)
                        }
                    },
                    enabled = canShare && !sharing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
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
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(Res.string.readyToShare), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Kartı mövcud sahəyə **tam sığdırıb** göstərir və eyni anda tam ölçülü qatı yazır.
 *
 * Miqyas `graphicsLayer` modifikatoru ilə verilir, ölçü modifikatoru ilə yox: belə olanda kartın
 * daxili koordinat sistemi 1080px qalır, `record` da elə həmin ölçüdə yazır. Kartı kiçik qutuya
 * yerləşdirməklə kiçiltmək isə mətni kəsərdi.
 */
@Composable
private fun CardPreview(
    content: ShareImageContent,
    style: ShareImageStyle,
    arabicFontFamily: FontFamily?,
    graphicsLayer: GraphicsLayer,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        val density = LocalDensity.current
        val availableWidth = with(density) { maxWidth.toPx() }
        val availableHeight = with(density) { maxHeight.toPx() }
        val scale = min(
            availableWidth / style.ratio.widthPx,
            availableHeight / style.ratio.heightPx,
        ).coerceAtLeast(0.01f)

        Box(
            modifier = Modifier
                .size(
                    width = with(density) { (style.ratio.widthPx * scale).toDp() },
                    height = with(density) { (style.ratio.heightPx * scale).toDp() },
                )
                .shadow(12.dp, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            ShareImageCard(
                content = content,
                style = style,
                arabicFontFamily = arabicFontFamily,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .drawWithCache {
                        onDrawWithContent {
                            graphicsLayer.record {
                                this@onDrawWithContent.drawContent()
                            }
                            drawLayer(graphicsLayer)
                        }
                    },
            )
        }
    }
}

@Composable
private fun PanelLabel(text: String) {
    Text(
        text = text,
        style = typography.labelMedium,
        color = colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun ThemeSwatch(
    theme: ShareImageTheme,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colorScheme.primary else colorScheme.outlineVariant,
                shape = CircleShape,
            )
            .padding(4.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(theme.gradient))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        theme.photo?.let { photo ->
            Image(
                painter = painterResource(photo),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(theme.accent),
        )
    }
}

/**
 * Fon dəstinin sonundakı «öz şəklin» yeri: şəkil seçilməyibsə «+», seçiləndən sonra seçilmiş şəklin
 * özü göstərilir. Yenidən toxunmaq başqa şəkil seçdirir; paket fonlarından birinə keçmək seçimi
 * təmizləyir (`ThemeSwatch.onClick`).
 */
@Composable
private fun CustomBackgroundSwatch(
    image: ImageBitmap?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colorScheme.primary else colorScheme.outlineVariant,
                shape = CircleShape,
            )
            .padding(4.dp)
            .clip(CircleShape)
            .background(colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = stringResource(Res.string.shareImageCustomBackground),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            // Aktivlərdə «+» ikonu yoxdur; iki zolaqla çəkmək yeni fayl əlavə etməkdən ucuzdur və
            // istənilən ölçüdə kəskin qalır.
            Box(Modifier.size(width = 16.dp, height = 2.dp).background(colorScheme.onSurfaceVariant))
            Box(Modifier.size(width = 2.dp, height = 16.dp).background(colorScheme.onSurfaceVariant))
        }
    }
}

@Composable
private fun RowScope.EditorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
) {
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = label,
            style = typography.bodySmall,
            color = colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
