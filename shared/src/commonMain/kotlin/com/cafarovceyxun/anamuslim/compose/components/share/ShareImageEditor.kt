package com.cafarovceyxun.anamuslim.compose.components.share

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.Color
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
import com.cafarovceyxun.anamuslim.compose.utils.app.PortraitLockEffect
import com.cafarovceyxun.anamuslim.compose.utils.isLandscape
import com.cafarovceyxun.anamuslim.utils.reader.getQuranScriptName
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
import com.cafarovceyxun.anamuslim.resources.shareImageMarginLabel
import com.cafarovceyxun.anamuslim.resources.shareImageScrimLabel
import com.cafarovceyxun.anamuslim.resources.strTitleScripts
import com.cafarovceyxun.anamuslim.resources.strTitleNote
import com.cafarovceyxun.anamuslim.resources.shareImageTextColorLabel
import com.cafarovceyxun.anamuslim.resources.shareImageTextStyleLabel
import com.cafarovceyxun.anamuslim.resources.shareImageStyleSerif
import com.cafarovceyxun.anamuslim.resources.shareImageStyleSans
import com.cafarovceyxun.anamuslim.resources.shareImageStyleMono
import com.cafarovceyxun.anamuslim.resources.shareImageStyleBold
import com.cafarovceyxun.anamuslim.resources.shareImageBrandingLocked
import com.cafarovceyxun.anamuslim.resources.shareImageQrLabel
import com.cafarovceyxun.anamuslim.resources.shareImageScrimHint
import com.cafarovceyxun.anamuslim.resources.source
import com.cafarovceyxun.anamuslim.resources.textSizesLabel
import com.cafarovceyxun.anamuslim.utils.univ.rememberImagePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
 * Ekran **portretə kilidlənir** ([PortraitLockEffect]): kart 9:16/4:5/1:1-dir və alət paneli
 * önizləmənin altındadır, yəni landşaftda önizləmə bir neçə santimetrə enirdi. Kilid platforma
 * sorğusudur, zəmanət deyil — bölünmüş ekranda rədd oluna bilər, ona görə aşağıda landşaft üçün
 * yan-yana düzülüş də var.
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
    /**
     * Redaktorun içindən seçilə bilən ərəb üzləri ([QuranScriptUtils] açarları), göstəriləcək sıra
     * ilə. **Boş siyahı = seçim təklif olunmur** və Şrift aləti sıraya düşmür.
     *
     * Quran üçün qəsdən boşdur, səbəb estetik deyil: ayə mətni Uthmani kodlaşdırılmış mənbədən
     * gəlir və Uthmani-yə xas diakritik işarələr başqa üzdə düşür və ya yerini dəyişir — yəni
     * **səhv Quran mətni paylaşılar**.
     */
    arabicFonts: List<String>,
    /** Başlanğıc üz. `null` = çağıranın sabit üzü; [arabicFontFamily] onu özü həll edir. */
    initialArabicFont: String?,
    /**
     * Açardan üzə çevirici. Redaktor yalnız açarı saxlayır, üzü isə çağıran həll edir — belə olanda
     * `uthmaniFontFamily()` / `hadithArabicFontFamily()` seçimi paylaşılan komponentə sızmır.
     */
    arabicFontFamily: @Composable (String?) -> FontFamily?,
    initialShowArabic: Boolean,
    initialShowTranslation: Boolean,
    /**
     * Kartda tətbiqin loğosu və mağaza QR-i ola bilərmi.
     *
     * `false` olanda nişan **çəkilmir** və «Məzmun» alətindəki iki çip ümumiyyətlə göstərilmir —
     * söndürülmüş çip qoysaq, istifadəçi onu yandırar və heç nə dəyişməzdi. Hədis paylaşımı bunu
     * mətn redaktə ediləndə bağlayır: nişan «bu, tətbiqdəki mətndir» deməkdir, əl gəzdirilmiş
     * sitatda isə bu doğru olmaz.
     */
    brandingAllowed: Boolean = true,
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

    val hasNoteText = !content.note.isNullOrBlank()

    var themeIndex by remember { mutableIntStateOf(0) }
    var ratio by remember { mutableStateOf(ShareImageRatio.Story) }
    var align by remember { mutableStateOf(ShareImageAlign.Center) }
    var arabicAlign by remember { mutableStateOf(ShareImageAlign.Center) }
    var noteAlign by remember { mutableStateOf(ShareImageAlign.Center) }
    // Qaynaq və loqo artıq `align`-dan asılı deyil: kartın imza hissəsi mətnin düzülüşü ilə
    // birlikdə sürüşməməlidir (istifadəçi tələbi), ona görə öz vəziyyətləri var.
    var referenceAlign by remember { mutableStateOf(ShareImageAlign.Center) }
    var brandingAlign by remember { mutableStateOf(ShareImageAlign.Center) }
    var customBackground by remember { mutableStateOf<ImageBitmap?>(null) }
    var arabicScale by remember { mutableFloatStateOf(ShareDefaultTextScale) }
    var translationScale by remember { mutableFloatStateOf(ShareDefaultTextScale) }
    var noteScale by remember { mutableFloatStateOf(ShareDefaultNoteScale) }
    var brandingScale by remember { mutableFloatStateOf(ShareDefaultBrandingScale) }
    var margin by remember { mutableFloatStateOf(ShareDefaultMargin) }
    var translationFamily by remember { mutableStateOf(ShareTextFamily.Sans) }
    var translationBold by remember { mutableStateOf(false) }
    var showArabic by remember { mutableStateOf(initialShowArabic && hasArabicText) }
    var showTranslation by remember { mutableStateOf(initialShowTranslation && hasTranslationText) }
    var showReference by remember { mutableStateOf(hasReferenceText) }
    var showNote by remember { mutableStateOf(hasNoteText) }
    var showBranding by remember { mutableStateOf(true) }
    // QR **standart olaraq açıqdır** — namaz cədvəli kartı ilə eyni (`PrayerMonthQr.TOP`). Şəkil
    // paylaşımının məqsədi mağaza kodunu da daşımaqdır; istəməyən «Məzmun» alətindən söndürür.
    var showQr by remember { mutableStateOf(true) }
    // Nişan qadağası keçidlərin ÜSTÜNDƏDİR: istifadəçi onları söndürüb-yandıra bilər, amma
    // `brandingAllowed = false` olanda kartda heç bir halda görünmür.
    val brandingVisible = showBranding && brandingAllowed
    val qrVisible = showQr && brandingAllowed
    var sharing by remember { mutableStateOf(false) }
    var scrim by remember { mutableFloatStateOf(ShareImageThemes[0].scrim) }
    var selectedFont by remember { mutableStateOf(initialArabicFont) }
    var selectedTool by remember { mutableStateOf(ShareTool.Background) }

    // `null` = avtomatik rejim (temadan, fon şəkli varsa parlaqlığından). Çalar/tündlük rəng seçilmiş
    // olmasa da saxlanılır ki, «Avto»-dan qayıdanda xətkeşlər sonuncu yerində dursun.
    var textColor by remember { mutableStateOf<Color?>(null) }
    var hue by remember { mutableFloatStateOf(ShareDefaultHue) }
    var shade by remember { mutableFloatStateOf(ShareDefaultShade) }

    /**
     * Seçilmiş fon şəklinin ölçülmüş orta parlaqlığı. Paket fotosu üçün **qəsdən** null qalır:
     * onun tema rəngləri əl ilə uyğunlaşdırılıb.
     */
    var backgroundLuminance by remember { mutableStateOf<Float?>(null) }

    // Şrift aləti HƏMİŞƏ var: ərəb üzü seçimi çağırandan asılıdır (Quran boş siyahı verir), amma
    // tərcümə stili hər iki halda lazımdır. Şərt panelin içindədir, alət sırasında yox.
    val tools = remember { ShareTool.entries.toList() }

    // Qaraltma yalnız fon ŞƏKLİ olanda görünür; düz qradiyentdə xətkeş heç nəyi dəyişmir.
    val hasBackgroundImage = customBackground != null || ShareImageThemes[themeIndex].photo != null

    val style = ShareImageStyle(
        theme = ShareImageThemes[themeIndex],
        ratio = ratio,
        arabicScale = arabicScale,
        translationScale = translationScale,
        margin = margin,
        align = align,
        arabicAlign = arabicAlign,
        customBackground = customBackground,
        scrim = scrim,
        translationFamily = translationFamily,
        translationBold = translationBold,
        noteScale = noteScale,
        noteAlign = noteAlign,
        referenceAlign = referenceAlign,
        brandingScale = brandingScale,
        brandingAlign = brandingAlign,
        showArabic = showArabic,
        showTranslation = showTranslation,
        showNote = showNote,
        showReference = showReference,
        showBranding = brandingVisible,
        showQr = qrVisible,
        textColor = textColor,
        backgroundLuminance = backgroundLuminance,
    )

    val imagePicker = rememberImagePicker { picked ->
        // İmtina edəndə `null` gəlir — mövcud fonu silmirik, sadəcə heç nə etmirik.
        if (picked != null) {
            customBackground = picked
            scrim = ShareCustomBackgroundScrim
            // Parlaqlıq ~576 piksel oxuyur, amma şəkil 2160px-ə qədər ola bilər — kompozisiya
            // ipində saxlamaq üçün fon dispetçerinə verilir. Nəticə gələnə qədər avtomatik rəng
            // temanın dəyərlərində qalır, sonra bir kadrda dəyişir.
            scope.launch {
                val measured = withContext(Dispatchers.Default) { picked.averageLuminance() }
                // Ölçmə bitənə qədər istifadəçi başqa fona keçmiş ola bilər; köhnə nəticə yenisini
                // əzməsin.
                if (customBackground === picked) backgroundLuminance = measured
            }
        }
    }

    val canShare = (showArabic && hasArabicText) || (showTranslation && hasTranslationText)
    val failedMsg = stringResource(Res.string.shareImageFailed)

    BackHandler(onBack = onBack)

    // Kart 9:16/4:5/1:1-dir və panel önizləmənin altındadır — landşaftda önizləmə bir neçə
    // santimetrə enirdi. Kilid platformadan xahiş edilir; bölünmüş ekranda sistem onu rədd edə
    // bilər, ona görə aşağıda landşaft düzülüşü də var.
    PortraitLockEffect()
    val landscape = isLandscape()

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
                            arabicAlign = ShareImageAlign.Center
                            noteAlign = ShareImageAlign.Center
                            referenceAlign = ShareImageAlign.Center
                            brandingAlign = ShareImageAlign.Center
                            customBackground = null
                            backgroundLuminance = null
                            textColor = null
                            hue = ShareDefaultHue
                            shade = ShareDefaultShade
                            arabicScale = ShareDefaultTextScale
                            translationScale = ShareDefaultTextScale
                            noteScale = ShareDefaultNoteScale
                            brandingScale = ShareDefaultBrandingScale
                            margin = ShareDefaultMargin
                            scrim = ShareImageThemes[0].scrim
                            translationFamily = ShareTextFamily.Sans
                            translationBold = false
                            selectedFont = initialArabicFont
                            selectedTool = ShareTool.Background
                            showArabic = initialShowArabic && hasArabicText
                            showTranslation = initialShowTranslation && hasTranslationText
                            showNote = hasNoteText
                            showReference = hasReferenceText
                            showBranding = true
                            showQr = true
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
        // Önizləmə və panel hər iki düzülüşdə eyni məzmundur, ona görə lambda kimi bir dəfə yazılır:
        // portretdə alt-alta, landşaftda yan-yana. Kopyalasaydıq, biri düzələndə digəri geridə qalardı.
        val preview: @Composable (Modifier) -> Unit = { previewModifier ->
            SharePreviewCanvas(
                widthPx = style.ratio.widthPx,
                heightPx = style.ratio.heightPx,
                graphicsLayer = graphicsLayer,
                modifier = previewModifier,
            ) { cardModifier ->
                ShareImageCard(
                    content = content,
                    style = style,
                    arabicFontFamily = arabicFontFamily(selectedFont),
                    modifier = cardModifier,
                )
            }
        }

        // Panel iki hissədir: alət sırası + seçilmiş alətin paneli sürüşür, paylaşma düyməsi
        // isə **sürüşmür**. Alçaq ekranlarda tək sürüşən sütun düyməni kadrdan çıxarırdı.
        val panel: @Composable (Modifier) -> Unit = { panelModifier ->
            Column(
                modifier = panelModifier
                    .background(colorScheme.surfaceContainerLow)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                ShareToolbar(
                    tools = tools,
                    selected = selectedTool,
                    onSelect = { selectedTool = it },
                )

                Spacer(Modifier.height(10.dp))

                AnimatedContent(
                    targetState = selectedTool,
                    transitionSpec = {
                        (fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 8 })
                            .togetherWith(fadeOut(tween(120)))
                            // clip = false: panellərin hündürlüyü fərqlidir (bir xətkeş ~72dp, şrift
                            // sırası daha çox) — konteyner kəsmək əvəzinə hamar böyüyüb-kiçilməlidir.
                            .using(SizeTransform(clip = false))
                    },
                    label = "shareToolPanel",
                    modifier = Modifier
                        .fillMaxWidth()
                        // min: alət dəyişəndə Paylaş düyməsi aşağı-yuxarı tullanmasın.
                        // max + scroll: çipləri sətirlərə bölünən dillər üçün ehtiyat.
                        .heightIn(min = 96.dp, max = 196.dp),
                ) { tool ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        when (tool) {
                            ShareTool.Background -> Row(
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
                                            // Ölçülmüş parlaqlıq öz şəklinə aiddir; paket temasına
                                            // keçəndə qalsa, avto rejim yanlış fona görə rəng
                                            // seçərdi.
                                            backgroundLuminance = null
                                            // Tema öz qaraltmasını toxum kimi verir; istifadəçi
                                            // sonra Qaraltma alətindən dəyişə bilər.
                                            scrim = theme.scrim
                                        },
                                    )
                                }

                                CustomBackgroundSwatch(
                                    image = customBackground,
                                    selected = customBackground != null,
                                    onClick = { imagePicker.pick() },
                                )
                            }

                            ShareTool.Format -> Row(
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
                            }

                            ShareTool.Content -> Column(Modifier.fillMaxWidth()) {
                              Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                              ) {
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
                                // Qeyd çipi yalnız məzmunda qeyd olanda: hədisin `note`-u və ayənin
                                // əlfəcin qeydi mətn paylaşımında vardı, şəkildə isə heç vaxt yox idi.
                                if (hasNoteText) {
                                    Chip(
                                        selected = showNote,
                                        label = { Text(stringResource(Res.string.strTitleNote)) },
                                        onClick = { showNote = !showNote },
                                    )
                                }
                                if (hasReferenceText) {
                                    Chip(
                                        selected = showReference,
                                        label = { Text(stringResource(Res.string.source)) },
                                        onClick = { showReference = !showReference },
                                    )
                                }
                                // Nişan qadağan olunubsa çiplər ümumiyyətlə yoxdur — bax
                                // [brandingAllowed]. Səbəbi aşağıdakı sətir yazır.
                                if (brandingAllowed) {
                                    Chip(
                                        selected = showBranding,
                                        label = { Text(stringResource(Res.string.shareImageBrandLabel)) },
                                        onClick = { showBranding = !showBranding },
                                    )
                                    Chip(
                                        selected = showQr,
                                        label = { Text(stringResource(Res.string.shareImageQrLabel)) },
                                        onClick = { showQr = !showQr },
                                    )
                                }
                              }

                                // İzah sıranın ALTINDADIR, içində yox: sıra `horizontalScroll`-dur
                                // və cümlə orada kadrın kənarında kəsilirdi — istifadəçi iki çipin
                                // niyə yox olduğunu oxuya bilmirdi.
                                if (!brandingAllowed) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(Res.string.shareImageBrandingLocked),
                                        style = typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            // Hər blokun **öz** sırası: ərəbcə ilə tərcümə çox vaxt fərqli düzülüş
                            // istəyir (ərəbcə sağa, tərcümə sola), qeyd izahat kimi ayrıca durur,
                            // qaynaq və loqo isə kartın imzasıdır — mətn sola çəkiləndə onların
                            // da sürüşməsi istifadəçi tərəfindən qüsur sayıldı.
                            //
                            // ⚠️ Ərəbcə ilə tərcümə arasındakı ornament bu siyahıda **yoxdur**:
                            // o, artıq həmişə mərkəzdədir (bax `ShareImageCard.Ornament`).
                            ShareTool.Align -> Column(Modifier.fillMaxWidth()) {
                                if (hasArabicText) {
                                    PanelLabel(stringResource(Res.string.labelArabic))
                                    AlignRow(selected = arabicAlign) { arabicAlign = it }
                                    Spacer(Modifier.height(10.dp))
                                }

                                PanelLabel(stringResource(Res.string.labelTranslation))
                                AlignRow(selected = align) { align = it }

                                if (hasNoteText) {
                                    Spacer(Modifier.height(10.dp))
                                    PanelLabel(stringResource(Res.string.strTitleNote))
                                    AlignRow(selected = noteAlign) { noteAlign = it }
                                }

                                if (hasReferenceText) {
                                    Spacer(Modifier.height(10.dp))
                                    PanelLabel(stringResource(Res.string.source))
                                    AlignRow(selected = referenceAlign) { referenceAlign = it }
                                }

                                if (brandingAllowed) {
                                    Spacer(Modifier.height(10.dp))
                                    PanelLabel(stringResource(Res.string.shareImageBrandLabel))
                                    AlignRow(selected = brandingAlign) { brandingAlign = it }
                                }
                            }

                            ShareTool.TextSize -> Column(Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    // Ərəbcə və tərcümə AYRI: eyni piksel hündürlüyündə ərəb xətti
                                    // latından kiçik oxunur, ona görə tək xətkeş həmişə birini
                                    // qurban verirdi.
                                    EditorSlider(
                                        label = stringResource(Res.string.labelArabic),
                                        value = arabicScale,
                                        onValueChange = { arabicScale = it },
                                        range = ShareTextScaleRange,
                                        enabled = hasArabicText && showArabic,
                                    )
                                    EditorSlider(
                                        label = stringResource(Res.string.labelTranslation),
                                        value = translationScale,
                                        onValueChange = { translationScale = it },
                                        range = ShareTextScaleRange,
                                        enabled = hasTranslationText && showTranslation,
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    EditorSlider(
                                        label = stringResource(Res.string.strTitleNote),
                                        value = noteScale,
                                        onValueChange = { noteScale = it },
                                        range = ShareNoteScaleRange,
                                        enabled = hasNoteText && showNote,
                                    )
                                    EditorSlider(
                                        label = stringResource(Res.string.shareImageBrandLabel),
                                        value = brandingScale,
                                        onValueChange = { brandingScale = it },
                                        range = ShareBrandingScaleRange,
                                        enabled = brandingVisible || qrVisible,
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    EditorSlider(
                                        label = stringResource(Res.string.shareImageMarginLabel),
                                        value = margin,
                                        onValueChange = { margin = it },
                                        range = ShareMarginRange,
                                    )
                                    // İkinci yarı boş qalır ki, xətkeş yuxarıdakılarla eyni enə
                                    // düşsün — tək `weight(1f)` onu iki dəfə enli edirdi.
                                    Spacer(Modifier.weight(1f))
                                }
                            }

                            ShareTool.TextColor -> ShareTextColorPanel(
                                color = textColor,
                                hue = hue,
                                shade = shade,
                                onAuto = { textColor = null },
                                onPick = { pickedHue, pickedShade ->
                                    hue = pickedHue
                                    shade = pickedShade
                                    textColor = shareTextColor(pickedHue, pickedShade)
                                },
                                // Avto yalnız ÖZ şəklində nəsə edir: paket temalarının rəngləri əl
                                // ilə uyğunlaşdırılıb və avto onlara toxunmur. İzah olmasa istifadəçi
                                // «Avto seçdim, heç nə dəyişmədi» nəticəsi çıxarardı.
                                showAutoHint = customBackground == null,
                            )

                            ShareTool.Scrim -> Column(Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    EditorSlider(
                                        label = stringResource(Res.string.shareImageScrimLabel),
                                        value = scrim,
                                        onValueChange = { scrim = it },
                                        range = ShareScrimRange,
                                        enabled = hasBackgroundImage,
                                    )
                                }
                                // Aləti gizlətmək əvəzinə sönükləşdirmək seçildi: səbəb ekranda
                                // yazılanda «düymə basılır, heç nə olmur» tələsi bağlanır.
                                if (!hasBackgroundImage) {
                                    Text(
                                        text = stringResource(Res.string.shareImageScrimHint),
                                        style = typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            ShareTool.Font -> Column(Modifier.fillMaxWidth()) {
                                // Ərəb üzü seçimi yalnız çağıran təklif edəndə. Quran boş siyahı
                                // verir, çünki ayə mətni Uthmani kodlaşdırılıb və başqa üzdə
                                // Uthmani-yə xas işarələr itir — səhv Quran mətni paylaşılar.
                                if (arabicFonts.isNotEmpty()) {
                                    PanelLabel(stringResource(Res.string.strTitleScripts))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        // ⚠️ Seçim `HadithPreferences.ARABIC_FONT`-a YAZILMIR: şəkil
                                        // üçün birdəfəlikdir, oxuma ekranının şriftini dəyişməməlidir.
                                        arabicFonts.forEach { font ->
                                            Chip(
                                                selected = selectedFont == font,
                                                label = { Text(font.getQuranScriptName()) },
                                                onClick = { selectedFont = font },
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(10.dp))
                                }

                                PanelLabel(stringResource(Res.string.shareImageTextStyleLabel))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Chip(
                                        selected = translationFamily == ShareTextFamily.Sans,
                                        label = { Text(stringResource(Res.string.shareImageStyleSans)) },
                                        onClick = { translationFamily = ShareTextFamily.Sans },
                                    )
                                    Chip(
                                        selected = translationFamily == ShareTextFamily.Serif,
                                        label = { Text(stringResource(Res.string.shareImageStyleSerif)) },
                                        onClick = { translationFamily = ShareTextFamily.Serif },
                                    )
                                    Chip(
                                        selected = translationFamily == ShareTextFamily.Mono,
                                        label = { Text(stringResource(Res.string.shareImageStyleMono)) },
                                        onClick = { translationFamily = ShareTextFamily.Mono },
                                    )

                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .width(1.dp)
                                            .height(24.dp)
                                            .background(colorScheme.outlineVariant),
                                    )

                                    Chip(
                                        selected = translationBold,
                                        label = { Text(stringResource(Res.string.shareImageStyleBold)) },
                                        onClick = { translationBold = !translationBold },
                                    )
                                }
                            }
                        }
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

        // ⚠️ Landşaft qolu kilid işləmədiyi hallar üçündür (bölünmüş ekran, iPad çoxvəzifəliliyi),
        // adi telefonda ora heç vaxt düşülmür. Panelin eni sabitdir: `weight` versək, xətkeşlər
        // geniş ekranda mənasız uzanır, önizləmə isə kartın nisbətinə görə onsuz da qalanı doldura
        // bilmir.
        if (landscape) {
            Row(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(colorScheme.background),
            ) {
                preview(Modifier.weight(1f).fillMaxHeight())
                panel(
                    Modifier
                        .width(340.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(colorScheme.background),
            ) {
                preview(Modifier.weight(1f))
                panel(Modifier.fillMaxWidth())
            }
        }
    }
}

/** Üç düzülüş çipi — panel indi üç ayrı sıra çəkdiyi üçün ayrıca funksiyaya çıxarıldı. */
@Composable
private fun AlignRow(selected: ShareImageAlign, onSelect: (ShareImageAlign) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Chip(
            selected = selected == ShareImageAlign.Left,
            label = { Text(stringResource(Res.string.shareImageAlignLeft)) },
            onClick = { onSelect(ShareImageAlign.Left) },
        )
        Chip(
            selected = selected == ShareImageAlign.Center,
            label = { Text(stringResource(Res.string.shareImageAlignCenter)) },
            onClick = { onSelect(ShareImageAlign.Center) },
        )
        Chip(
            selected = selected == ShareImageAlign.Right,
            label = { Text(stringResource(Res.string.shareImageAlignRight)) },
            onClick = { onSelect(ShareImageAlign.Right) },
        )
    }
}
