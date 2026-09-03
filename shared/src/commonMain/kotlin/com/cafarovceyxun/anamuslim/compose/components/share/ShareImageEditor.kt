package com.cafarovceyxun.anamuslim.compose.components.share

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.Chip
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
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
import com.cafarovceyxun.anamuslim.utils.univ.rememberImagePicker
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

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
    var textScale by remember { mutableFloatStateOf(ShareDefaultTextScale) }
    var margin by remember { mutableFloatStateOf(ShareDefaultMargin) }
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
                            textScale = ShareDefaultTextScale
                            margin = ShareDefaultMargin
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
            SharePreviewCanvas(
                widthPx = style.ratio.widthPx,
                heightPx = style.ratio.heightPx,
                graphicsLayer = graphicsLayer,
                modifier = Modifier.weight(1f),
            ) { cardModifier ->
                ShareImageCard(
                    content = content,
                    style = style,
                    arabicFontFamily = arabicFontFamily,
                    modifier = cardModifier,
                )
            }

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
                            range = ShareTextScaleRange,
                        )
                        EditorSlider(
                            label = stringResource(Res.string.shareImageMarginLabel),
                            value = margin,
                            onValueChange = { margin = it },
                            range = ShareMarginRange,
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
                            val shared = shareCapturedCard(
                                graphicsLayer = graphicsLayer,
                                chooserTitle = chooserTitle,
                                logTag = "ShareImageEditorScreen.share",
                            )
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
