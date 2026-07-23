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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafarovceyxun.anamuslim.compose.theme.hadithArabicFontFamily
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HadithPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.ic_launcher_foreground
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.cafarovceyxun.anamuslim.utils.supabase.Hadith

object HadithImageGenerator {
    @Composable
    fun HadithImageCard(
        hadith: Hadith,
        includeArabic: Boolean,
        includeAzerbaijani: Boolean,
        arabicFontSize: Int = 24,
        azerbaijaniFontSize: Int = 18,
        horizontalPadding: Int = 50,
        verticalPadding: Int = 50
    ) {
        val selectedFont = HadithPreferences.observeArabicFont()
        // The `getHadithArabicFontRes` Int hook returns 0 on iOS; this seam resolves the font
        // through Compose Resources on both platforms.
        val arabicFontFamily = hadithArabicFontFamily(selectedFont)

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
                verticalArrangement = Arrangement.Top // Məntiqi Top etdik
            ) {
                Spacer(modifier = Modifier.height(verticalPadding.dp)) // Xətkeş bura nəzarət edir
                if (includeArabic) {
                    Text(
                        text = hadith.text_ar,
                        color = Color.White,
                        fontSize = arabicFontSize.sp,
                        lineHeight = (arabicFontSize * 1.5).sp,
                        textAlign = TextAlign.Right,
                        fontFamily = arabicFontFamily,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (includeArabic && includeAzerbaijani) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(modifier = Modifier.height(1.dp).width(60.dp).background(Color(0xFF4DB6AC).copy(alpha = 0.2f)))
                    Spacer(modifier = Modifier.height(2.dp))
                }

                if (includeAzerbaijani) {
                    Text(
                        text = hadith.text_az,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = azerbaijaniFontSize.sp,
                        lineHeight = (azerbaijaniFontSize * 1.4).sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    hadith.source?.takeIf { it.isNotEmpty() }?.let {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = it,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = (azerbaijaniFontSize * 0.8).sp,
                            lineHeight = (azerbaijaniFontSize * 1.2).sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
