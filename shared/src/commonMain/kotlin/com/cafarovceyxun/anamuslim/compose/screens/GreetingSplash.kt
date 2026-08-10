package com.cafarovceyxun.anamuslim.compose.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.theme.arabicFontFamily
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.app_name
import com.cafarovceyxun.anamuslim.resources.ic_launcher_foreground
import com.cafarovceyxun.anamuslim.resources.strGreetingSalam
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** The greeting itself is never translated — it is the Arabic phrase, in Arabic script. */
private const val SALAM_ARABIC = "السَّلَامُ عَلَيْكُمْ"

private const val LOGO_IN_MS = 460
private const val ARABIC_DELAY_MS = 220L
private const val ARABIC_IN_MS = 460
private const val LATIN_DELAY_MS = 200L
private const val LATIN_IN_MS = 340
private const val HOLD_MS = 420L
private const val EXIT_MS = 360

/**
 * Process-scoped, not `rememberSaveable`: the greeting belongs to *launching the app*, and an
 * activity recreation is not a launch. Rotating the device, or picking a new language (which calls
 * `AppCompatDelegate.setApplicationLocales`, and on iOS re-keys the tree on `appLocaleFlow`) both
 * rebuild the composition, and replaying the greeting there would read as a stutter.
 */
private var greetingPlayed = false

/**
 * The launch greeting — the app logo, then "as-salamu alaykum" in Arabic script with the same
 * greeting in the app's language beneath it. It takes over from the static logo the platform splash
 * screens used to display, which is why those are now colour-only (`Theme.QuranApp.Splash` on
 * Android, `UILaunchScreen` on iOS).
 *
 * Meant to be overlaid on the app content (last child of a full-size `Box`), not to gate it: the
 * real UI composes and loads underneath while this plays, so the greeting costs no startup time and
 * simply fades away when it is done. It consumes pointer input while visible so a tap aimed at the
 * greeting cannot land on whatever is behind it.
 */
@Composable
fun GreetingSplash(modifier: Modifier = Modifier) {
    // Read once. `greetingPlayed` is flipped as soon as the animation starts, and re-reading it on
    // every recomposition would drop the overlay mid-play.
    val shouldPlay = remember { !greetingPlayed }

    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.82f) }
    val arabicAlpha = remember { Animatable(0f) }
    val arabicScale = remember { Animatable(0.9f) }
    val latinAlpha = remember { Animatable(0f) }
    val overlayAlpha = remember { Animatable(1f) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(shouldPlay) {
        if (!shouldPlay) return@LaunchedEffect
        greetingPlayed = true

        launch { logoScale.animateTo(1f, tween(LOGO_IN_MS + 260, easing = FastOutSlowInEasing)) }
        launch { logoAlpha.animateTo(1f, tween(LOGO_IN_MS, easing = LinearOutSlowInEasing)) }

        delay(ARABIC_DELAY_MS)
        launch { arabicScale.animateTo(1f, tween(ARABIC_IN_MS + 260, easing = FastOutSlowInEasing)) }
        launch { arabicAlpha.animateTo(1f, tween(ARABIC_IN_MS, easing = LinearOutSlowInEasing)) }

        delay(LATIN_DELAY_MS)
        latinAlpha.animateTo(1f, tween(LATIN_IN_MS, easing = LinearOutSlowInEasing))

        delay(HOLD_MS)
        overlayAlpha.animateTo(0f, tween(EXIT_MS, easing = FastOutLinearInEasing))
        finished = true
    }

    if (!shouldPlay || finished) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = overlayAlpha.value }
            .background(colorScheme.background)
            .pointerInput(Unit) {
                awaitPointerEventScope { while (true) { awaitPointerEvent() } }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_launcher_foreground),
                contentDescription = stringResource(Res.string.app_name),
                modifier = Modifier
                    .size(112.dp)
                    .graphicsLayer {
                        alpha = logoAlpha.value
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                    },
            )

            Spacer(Modifier.height(28.dp))

            Text(
                text = SALAM_ARABIC,
                fontFamily = arabicFontFamily(),
                fontSize = 42.sp,
                lineHeight = 68.sp,
                color = colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = arabicAlpha.value
                    scaleX = arabicScale.value
                    scaleY = arabicScale.value
                    translationY = (1f - arabicAlpha.value) * 16.dp.toPx()
                },
            )

            Spacer(Modifier.height(18.dp))

            // Drawn outwards from the centre as the transliteration arrives, so the two read as one
            // movement rather than two separate fades.
            Box(
                Modifier
                    .width(72.dp)
                    .height(1.dp)
                    .graphicsLayer { scaleX = latinAlpha.value }
                    .background(colorScheme.primary.alpha(0.45f)),
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(Res.string.strGreetingSalam),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = latinAlpha.value
                    translationY = (1f - latinAlpha.value) * 8.dp.toPx()
                },
            )
        }
    }
}
