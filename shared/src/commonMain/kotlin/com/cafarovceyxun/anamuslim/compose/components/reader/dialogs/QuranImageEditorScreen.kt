package com.cafarovceyxun.anamuslim.compose.components.reader.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.settings.ListItemCategoryLabel
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.adjustmentLabel
import com.cafarovceyxun.anamuslim.resources.azerbaijaniLabel
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_left
import com.cafarovceyxun.anamuslim.resources.dr_icon_share
import com.cafarovceyxun.anamuslim.resources.edgePaddingHorizontal
import com.cafarovceyxun.anamuslim.resources.edgePaddingVertical
import com.cafarovceyxun.anamuslim.resources.labelArabic
import com.cafarovceyxun.anamuslim.resources.quran_image_editor_title
import com.cafarovceyxun.anamuslim.resources.readyToShare
import com.cafarovceyxun.anamuslim.resources.strTitleShareVerse
import com.cafarovceyxun.anamuslim.resources.textSizesLabel
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.app.QuranImageGenerator
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun QuranImageEditorScreen(
    arabicText: String,
    translationText: String,
    includeArabic: Boolean,
    includeAzerbaijani: Boolean,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    var arabicFontSize by remember { mutableStateOf(10f) }
    var azerbaijaniFontSize by remember { mutableStateOf(10f) }
    var horizontalPadding by remember { mutableStateOf(15f) }
    var verticalPadding by remember { mutableStateOf(50f) }

    val chooserTitle = stringResource(Res.string.strTitleShareVerse)

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(Res.string.quran_image_editor_title),
                onBack = onBack,
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .aspectRatio(9f/16f)
                        .clip(RoundedCornerShape(12.dp))
                        .drawWithCache {
                            onDrawWithContent {
                                graphicsLayer.record {
                                    this@onDrawWithContent.drawContent()
                                }
                                drawLayer(graphicsLayer)
                            }
                        }
                ) {
                    Box(modifier = Modifier.scale(1.2f).size(width = 1080.dp, height = 1920.dp).align(Alignment.Center)) {
                        QuranImageGenerator.QuranImageCard(
                            arabicText = arabicText,
                            translationText = translationText,
                            includeArabic = includeArabic,
                            includeAzerbaijani = includeAzerbaijani,
                            arabicFontSize = arabicFontSize.toInt(),
                            azerbaijaniFontSize = azerbaijaniFontSize.toInt(),
                            horizontalPadding = horizontalPadding.toInt(),
                            verticalPadding = verticalPadding.toInt()
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.surfaceContainerLow)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                ) {
                    ListItemCategoryLabel(title = stringResource(Res.string.textSizesLabel))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        if (includeArabic) {
                            Box(modifier = Modifier.weight(1f)) {
                                TextSizeControl(
                                    label = stringResource(Res.string.labelArabic),
                                    value = arabicFontSize,
                                    onValueChange = { arabicFontSize = it },
                                    range = 1f..30f
                                )
                            }
                        }
                        if (includeArabic && includeAzerbaijani) Spacer(modifier = Modifier.width(16.dp))
                        if (includeAzerbaijani) {
                            Box(modifier = Modifier.weight(1f)) {
                                TextSizeControl(
                                    label = stringResource(Res.string.azerbaijaniLabel),
                                    value = azerbaijaniFontSize,
                                    onValueChange = { azerbaijaniFontSize = it },
                                    range = 1f..30f
                                )
                            }
                        }
                    }

                    ListItemCategoryLabel(title = stringResource(Res.string.adjustmentLabel))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f)) {
                            TextSizeControl(
                                label = stringResource(Res.string.edgePaddingHorizontal),
                                value = horizontalPadding,
                                onValueChange = { horizontalPadding = it },
                                range = 2f..50f
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            TextSizeControl(
                                label = stringResource(Res.string.edgePaddingVertical),
                                value = verticalPadding,
                                onValueChange = { verticalPadding = it },
                                range = 10f..100f
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        scope.launch {
                            try {
                                PlatformUtils.shareImage(
                                    graphicsLayer.toImageBitmap(),
                                    chooserTitle,
                                )
                            } catch (e: Exception) {
                                AppLogger.saveError(e, "QuranImageEditorScreen.share")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(painterResource(Res.drawable.dr_icon_share), null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.readyToShare), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TextSizeControl(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
            Text(text = "${value.toInt()}", style = MaterialTheme.typography.labelSmall, color = colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
