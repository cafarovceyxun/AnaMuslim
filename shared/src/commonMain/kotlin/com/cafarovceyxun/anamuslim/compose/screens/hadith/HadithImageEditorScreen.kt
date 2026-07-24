package com.cafarovceyxun.anamuslim.compose.screens.hadith

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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.settings.ListItemCategoryLabel
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.adjustmentLabel
import com.cafarovceyxun.anamuslim.resources.arabicLabel
import com.cafarovceyxun.anamuslim.resources.azerbaijaniLabel
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_left
import com.cafarovceyxun.anamuslim.resources.dr_icon_share
import com.cafarovceyxun.anamuslim.resources.edgePaddingHorizontal
import com.cafarovceyxun.anamuslim.resources.edgePaddingVertical
import com.cafarovceyxun.anamuslim.resources.hadithShareTitle
import com.cafarovceyxun.anamuslim.resources.imageEditorTitle
import com.cafarovceyxun.anamuslim.resources.readyToShare
import com.cafarovceyxun.anamuslim.resources.textSizesLabel
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.app.HadithImageGenerator
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.cafarovceyxun.anamuslim.utils.supabase.Hadith
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithImageEditorScreen(
    hadith: Hadith,
    includeArabic: Boolean,
    includeAzerbaijani: Boolean,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    var arabicFontSize by remember { mutableFloatStateOf(5f) }
    var azerbaijaniFontSize by remember { mutableFloatStateOf(5f) }
    var horizontalPadding by remember { mutableFloatStateOf(20f) }
    var verticalPadding by remember { mutableFloatStateOf(50f) }

    val chooserTitle = stringResource(Res.string.hadithShareTitle)

    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(Res.string.imageEditorTitle),
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
            // Yuxarı Hissə: Ortalanmış Şəkil Önizləməsi
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
                    // Şəkli daha da böyütmək üçün scale dəyərini 1.2 edirik
                    Box(modifier = Modifier.scale(1.2f).size(width = 1080.dp, height = 1920.dp).align(Alignment.Center)) {
                        HadithImageGenerator.HadithImageCard(
                            hadith = hadith,
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

            // Aşağı Hissə: Sabitlənmiş İdarəetmə Paneli
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.surfaceContainerLow)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp) // Xətkeşlərin enini bir az da qısaltdıq
                ) {
                    ListItemCategoryLabel(title = stringResource(Res.string.textSizesLabel))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        if (includeArabic) {
                            Box(modifier = Modifier.weight(1f)) {
                                TextSizeControl(
                                    label = stringResource(Res.string.arabicLabel),
                                    value = arabicFontSize,
                                    onValueChange = { arabicFontSize = it },
                                    range = 1f..30f
                                )
                            }
                        }
                        if (includeArabic && includeAzerbaijani) {
                            Spacer(modifier = Modifier.width(16.dp))
                        }
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
                                range = 2f..100f
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
                                AppLogger.saveError(e, "HadithImageEditorScreen.share")
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
            Text(text = value.toInt().toString(), style = MaterialTheme.typography.labelSmall, color = colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
