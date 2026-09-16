package com.cafarovceyxun.anamuslim.compose.components.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafarovceyxun.anamuslim.compose.components.player.dialogs.AudioEndBehaviour
import com.cafarovceyxun.anamuslim.compose.components.player.dialogs.AudioOption
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.LocalAppLocale
import com.cafarovceyxun.anamuslim.compose.utils.formatOneDecimal
import com.cafarovceyxun.anamuslim.compose.utils.preferences.RecitationPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.audioOption
import com.cafarovceyxun.anamuslim.resources.audioOptionShortBoth
import com.cafarovceyxun.anamuslim.resources.audioOptionShortQuran
import com.cafarovceyxun.anamuslim.resources.audioOptionShortTranslation
import com.cafarovceyxun.anamuslim.resources.dr_icon_quran_script
import com.cafarovceyxun.anamuslim.resources.dr_icon_translations
import com.cafarovceyxun.anamuslim.resources.ic_mode_translation
import com.cafarovceyxun.anamuslim.resources.ic_mic
import com.cafarovceyxun.anamuslim.resources.ic_repeat
import com.cafarovceyxun.anamuslim.resources.ic_restart
import com.cafarovceyxun.anamuslim.resources.ic_arrow_next
import com.cafarovceyxun.anamuslim.resources.ic_arrow_next_off
import com.cafarovceyxun.anamuslim.resources.ic_speed_gauge
import com.cafarovceyxun.anamuslim.resources.nTimes
import com.cafarovceyxun.anamuslim.resources.once
import com.cafarovceyxun.anamuslim.resources.playNextSurah
import com.cafarovceyxun.anamuslim.resources.playbackCount
import com.cafarovceyxun.anamuslim.resources.playbackCountNeedsQuranOnly
import com.cafarovceyxun.anamuslim.resources.playbackSpeed
import com.cafarovceyxun.anamuslim.resources.repeatCurrentSurah
import com.cafarovceyxun.anamuslim.resources.stopPlayback
import com.cafarovceyxun.anamuslim.resources.strTitleSelectReciter
import com.cafarovceyxun.anamuslim.resources.twice
import com.cafarovceyxun.anamuslim.resources.whenChapterEnds
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** How long the name of the value a tap picked stays under the row. */
private const val FLASH_MILLIS = 3_000L

/** The values a tap cycles through — the sheet keeps the full list. */
private val REPEAT_CYCLE = listOf(0, 1, 2, 4, 9)
private val SPEED_CYCLE = listOf(0.5f, 0.7f, 1f, 1.3f, 1.5f, 2f)

/**
 * Which setting was just changed by a tap, together with the value it landed on.
 *
 * The value travels with the flash instead of being read back from preferences, so the line under
 * the row names what the tap chose even in the frame before the DataStore write comes back.
 */
private sealed interface QuickToggleFlash {
    data class Audio(val option: AudioOption) : QuickToggleFlash
    data class Repeat(val count: Int) : QuickToggleFlash
    data class Speed(val speed: Float) : QuickToggleFlash
    data class ChapterEnd(val behaviour: AudioEndBehaviour) : QuickToggleFlash

    /** Repeat is only honoured in Quran-only mode, so the dimmed button says why instead. */
    object RepeatUnavailable : QuickToggleFlash
}

/**
 * The four playback settings — audio option, playback count, speed, what happens when the surah
 * ends — as one row of one-tap buttons.
 *
 * A tap moves the setting to its next value and applies it right away; the icon carries the
 * current value and the chosen one is spelled out under the row for [FLASH_MILLIS], because no
 * icon says "Quran + translation" on its own. A long press still opens that setting's sheet, which
 * is where the values that do not cycle live — the verse group size and the speeds outside
 * [SPEED_CYCLE].
 */
@Composable
fun PlayerQuickToggleRow(
    controller: RecitationPlayer,
    onOpenReciterSelector: () -> Unit,
    onOpenAudioOptions: () -> Unit,
    onOpenRepeatOptions: () -> Unit,
    onOpenSpeedOptions: () -> Unit,
    onOpenEndBehaviourOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appLocale = LocalAppLocale.current
    val scope = rememberCoroutineScope()

    val audioOption = RecitationPreferences.observeAudioOption()
    val repeatCount = RecitationPreferences.observeRepeatCount()
    val speed = RecitationPreferences.observeSpeed()
    val endBehaviour = RecitationPreferences.observeAudioEndBehaviour()

    val repeatSupported = audioOption == AudioOption.ONLY_QURAN

    var flash by remember { mutableStateOf<QuickToggleFlash?>(null) }
    // Bumped on every tap so the three seconds restart even when the same value comes round again.
    var flashTick by remember { mutableIntStateOf(0) }

    fun show(next: QuickToggleFlash) {
        flash = next
        flashTick++
    }

    LaunchedEffect(flashTick) {
        if (flash == null) return@LaunchedEffect

        delay(FLASH_MILLIS)
        flash = null
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // The only one that does not cycle: a reciter needs a list, so a tap opens the sheet.
            PlayerQuickToggleButton(
                modifier = Modifier.weight(1f),
                icon = painterResource(Res.drawable.ic_mic),
                label = stringResource(Res.string.strTitleSelectReciter),
                onClick = onOpenReciterSelector,
                onLongClick = onOpenReciterSelector,
            )

            PlayerQuickToggleButton(
                modifier = Modifier.weight(1f),
                icon = painterResource(
                    when (audioOption) {
                        AudioOption.ONLY_QURAN -> Res.drawable.dr_icon_quran_script
                        AudioOption.ONLY_TRANSLATION -> Res.drawable.dr_icon_translations
                        AudioOption.BOTH -> Res.drawable.ic_mode_translation
                    }
                ),
                label = stringResource(Res.string.audioOption),
                onClick = {
                    val next = audioOption.next()

                    show(QuickToggleFlash.Audio(next))
                    scope.launch {
                        RecitationPreferences.setAudioOption(next)
                        controller.setAudioOption(next)
                    }
                },
                onLongClick = onOpenAudioOptions,
            )

            PlayerQuickToggleButton(
                modifier = Modifier.weight(1f),
                icon = painterResource(Res.drawable.ic_repeat),
                label = stringResource(Res.string.playbackCount),
                badge = "${repeatCount + 1}×",
                // Dimmed while the mode ignores it, but still cycling: a tap that does nothing
                // leaves the count stuck at whatever it was when the mode changed.
                dimmed = !repeatSupported,
                onClick = {
                    val next = REPEAT_CYCLE.nextAfter(repeatCount)

                    show(
                        if (repeatSupported) QuickToggleFlash.Repeat(next)
                        else QuickToggleFlash.RepeatUnavailable
                    )
                    scope.launch {
                        RecitationPreferences.setRepeatCount(next)
                        controller.setRepeatCount(next)
                    }
                },
                onLongClick = onOpenRepeatOptions,
            )

            PlayerQuickToggleButton(
                modifier = Modifier.weight(1f),
                icon = painterResource(Res.drawable.ic_speed_gauge),
                label = stringResource(Res.string.playbackSpeed),
                badge = appLocale.formatOneDecimal(speed) + "x",
                onClick = {
                    val next = SPEED_CYCLE.nextAfter(speed)

                    show(QuickToggleFlash.Speed(next))
                    scope.launch {
                        RecitationPreferences.setSpeed(next)
                        controller.setSpeed(next)
                    }
                },
                onLongClick = onOpenSpeedOptions,
            )

            PlayerQuickToggleButton(
                modifier = Modifier.weight(1f),
                // One arrow, three answers: it carries on, it is crossed out, it turns back.
                icon = painterResource(
                    when (endBehaviour) {
                        AudioEndBehaviour.STOP_PLAYBACK -> Res.drawable.ic_arrow_next_off
                        AudioEndBehaviour.NEXT_CHAPTER -> Res.drawable.ic_arrow_next
                        AudioEndBehaviour.REPEAT_CHAPTER -> Res.drawable.ic_restart
                    }
                ),
                label = stringResource(Res.string.whenChapterEnds),
                onClick = {
                    val next = endBehaviour.next()

                    show(QuickToggleFlash.ChapterEnd(next))
                    scope.launch {
                        RecitationPreferences.setAudioEndBehaviour(next)
                        controller.setAudioEndBehaviour(next)
                    }
                },
                onLongClick = onOpenEndBehaviourOptions,
            )
        }

        QuickToggleFlashLine(flash = flash)
    }
}

/**
 * The line under the row. Its height is reserved whether or not something is showing, so picking a
 * value never shifts the seek bar and the transport controls under it.
 */
@Composable
private fun QuickToggleFlashLine(flash: QuickToggleFlash?) {
    val appLocale = LocalAppLocale.current

    val message = when (flash) {
        is QuickToggleFlash.Audio -> when (flash.option) {
            AudioOption.ONLY_QURAN -> stringResource(Res.string.audioOptionShortQuran)
            AudioOption.ONLY_TRANSLATION -> stringResource(Res.string.audioOptionShortTranslation)
            AudioOption.BOTH -> stringResource(Res.string.audioOptionShortBoth)
        }

        is QuickToggleFlash.Repeat -> when (flash.count) {
            0 -> stringResource(Res.string.once)
            1 -> stringResource(Res.string.twice)
            else -> stringResource(Res.string.nTimes, flash.count + 1)
        }

        is QuickToggleFlash.Speed -> appLocale.formatOneDecimal(flash.speed) + "x"

        is QuickToggleFlash.ChapterEnd -> when (flash.behaviour) {
            AudioEndBehaviour.STOP_PLAYBACK -> stringResource(Res.string.stopPlayback)
            AudioEndBehaviour.NEXT_CHAPTER -> stringResource(Res.string.playNextSurah)
            AudioEndBehaviour.REPEAT_CHAPTER -> stringResource(Res.string.repeatCurrentSurah)
        }

        QuickToggleFlash.RepeatUnavailable ->
            stringResource(Res.string.playbackCountNeedsQuranOnly)

        null -> null
    }

    // Kept across the fade-out so the text does not blank out before it has finished leaving.
    var shown by remember { mutableStateOf("") }
    LaunchedEffect(message) {
        if (message != null) shown = message
    }

    val alpha by animateFloatAsState(
        targetValue = if (message != null) 1f else 0f,
        animationSpec = tween(if (message != null) 150 else 400),
        label = "quickToggleFlashAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .padding(top = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = shown,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = playerContentColor().alpha(alpha * 0.85f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A bare icon — no card, no border — with the ripple as the only press feedback. */
@Composable
private fun PlayerQuickToggleButton(
    icon: Painter,
    label: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    dimmed: Boolean = false,
) {
    val contentAlpha = if (dimmed) 0.4f else 1f

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = icon,
                contentDescription = label,
                tint = playerContentColor().alpha(0.9f * contentAlpha),
                modifier = Modifier.size(20.dp),
            )

            if (badge != null) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = playerContentColor().alpha(0.65f * contentAlpha),
                    maxLines = 1,
                )
            }
        }
    }
}

private fun AudioOption.next(): AudioOption =
    AudioOption.entries[(ordinal + 1) % AudioOption.entries.size]

private fun AudioEndBehaviour.next(): AudioEndBehaviour =
    AudioEndBehaviour.entries[(ordinal + 1) % AudioEndBehaviour.entries.size]

/**
 * The next value up, wrapping at the top. A value the sheet set outside the cycle (0.1x, 3x, or a
 * repeat count that is no longer offered) lands on the first cycle value above it rather than
 * dropping the tap.
 */
private fun <T : Comparable<T>> List<T>.nextAfter(current: T): T =
    firstOrNull { it > current } ?: first()
