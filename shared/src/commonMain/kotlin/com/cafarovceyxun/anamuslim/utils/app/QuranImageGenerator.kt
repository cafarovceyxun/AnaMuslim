package com.cafarovceyxun.anamuslim.utils.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.ic_launcher_foreground
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

object QuranImageGenerator {
    @Composable
    fun QuranImageCard(
        arabicText: String,
        translationText: String,
        includeArabic: Boolean,
        includeAzerbaijani: Boolean,
        arabicFontSize: Int = 24,
        azerbaijaniFontSize: Int = 18,
        horizontalPadding: Int = 50,
        verticalPadding: Int = 50
    ) {
        Box(
            modifier = Modifier
                .width(720.dp)
                .height(1280.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1A1C1E), Color(0xFF000000))
                    )
                )
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier
                    .size(400.dp)
                    .align(Alignment.Center),
                alpha = 0.05f,
                colorFilter = ColorFilter.tint(Color(0xFF4DB6AC)),
                contentScale = ContentScale.Fit
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(verticalPadding.dp))
                if (includeArabic) {
                    Text(
                        text = arabicText,
                        color = Color.White,
                        fontSize = arabicFontSize.sp,
                        lineHeight = (arabicFontSize * 1.6).sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (includeArabic && includeAzerbaijani) {
                    Spacer(modifier = Modifier.height(2.dp))
                }

                if (includeAzerbaijani) {
                    Text(
                        text = translationText,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = azerbaijaniFontSize.sp,
                        lineHeight = (azerbaijaniFontSize * 1.5).sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
