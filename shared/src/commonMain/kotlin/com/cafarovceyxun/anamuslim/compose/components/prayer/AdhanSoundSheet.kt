package com.cafarovceyxun.anamuslim.compose.components.prayer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.RadioItem
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheet
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_prayer_times
import com.cafarovceyxun.anamuslim.resources.prayerSoundSheetTitle
import com.cafarovceyxun.anamuslim.resources.prayerSoundSilent
import com.cafarovceyxun.anamuslim.resources.prayerSoundCallDesc
import com.cafarovceyxun.anamuslim.resources.prayerSoundCall
import com.cafarovceyxun.anamuslim.resources.prayerSoundVibratingDesc
import com.cafarovceyxun.anamuslim.resources.prayerSoundVibrating
import com.cafarovceyxun.anamuslim.resources.prayerSoundPhoneDesc
import com.cafarovceyxun.anamuslim.resources.prayerSoundPhone
import com.cafarovceyxun.anamuslim.resources.prayerSoundRingtoneDesc
import com.cafarovceyxun.anamuslim.resources.prayerSoundRingtone
import com.cafarovceyxun.anamuslim.resources.prayerSoundVoiceArDesc
import com.cafarovceyxun.anamuslim.resources.prayerSoundVoiceAr
import com.cafarovceyxun.anamuslim.resources.prayerSoundSilentDesc
import com.cafarovceyxun.anamuslim.resources.prayerSoundSystem
import com.cafarovceyxun.anamuslim.resources.prayerSoundSystemDesc
import com.cafarovceyxun.anamuslim.resources.ic_pause
import com.cafarovceyxun.anamuslim.resources.ic_play
import com.cafarovceyxun.anamuslim.resources.prayerSoundApplyAll
import com.cafarovceyxun.anamuslim.resources.prayerSoundPreview
import com.cafarovceyxun.anamuslim.utils.prayer.AdhanPreviewProvider
import com.cafarovceyxun.anamuslim.utils.prayer.AdhanSound
import com.cafarovceyxun.anamuslim.utils.prayer.Prayer
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Bir namaz vaxtının bildiriş səsi.
 *
 * Siyahı [AdhanSound.entries]-dən qurulur: yeni səs əlavə edəndə burada dəyişilməli yeganə şey
 * [titleOf]/[descriptionOf]-dakı ad sətirləridir.
 *
 * Önizləmə düyməsi **yalnız [AdhanSound.isCustom] səslərdə** var: cihazın öz bildiriş səsini tətbiq
 * çala bilmir, «səssiz»in isə çalınacaq şeyi yoxdur — o ikisinə düymə qoysaydıq basılar və heç nə
 * olmazdı (CLAUDE.md: «inert default UI-ni azad etmir»).
 *
 * Vərəq bağlananda çalınan nümunə dayandırılır, yoxsa səs vərəqdən sonra da davam edərdi.
 */
@Composable
fun AdhanSoundSheet(
    prayer: Prayer?,
    selected: AdhanSound,
    onSelect: (AdhanSound) -> Unit,
    onApplyToAll: (AdhanSound) -> Unit,
    onClose: () -> Unit,
) {
    val isOpen = prayer != null

    // Vərəq bağlananda nümunə susmalıdır — `onClose` bütün bağlanma yollarını tutmur (kənara
    // toxunma, sürüşdürüb bağlama), ona görə effekt vərəqin öz vəziyyətinə bağlanır.
    DisposableEffect(isOpen) {
        onDispose { if (isOpen) AdhanPreviewProvider.stop() }
    }

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onClose,
        icon = Res.drawable.dr_icon_prayer_times,
        title = prayer?.let {
            "${stringResource(PrayerUiFormat.labelOf(it))} · " +
                stringResource(Res.string.prayerSoundSheetTitle)
        },
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            AdhanSound.entries.forEach { sound ->
                RadioItem(
                    title = titleOf(sound),
                    subtitle = descriptionOf(sound),
                    selected = sound == selected,
                    leading = if (sound.isCustom) {
                        { PreviewButton(sound) }
                    } else {
                        // Öz faylı olmayan səs üçün yer saxlanılır ki, adlar bir xəttə düşsün.
                        { Spacer(Modifier.size(PreviewSlotSize)) }
                    },
                    onClick = { onSelect(sound) },
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // «Hamısına tətbiq et» seçimi DƏYİŞMİR, ona görə ayrıca düymədir: sətrə toxunmaq
            // yalnız bu vaxtı qurur, bu düymə isə eyni səsi altı vaxtın hamısına yayır.
            TextButton(
                onClick = { onApplyToAll(selected) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                Text(stringResource(Res.string.prayerSoundApplyAll))
            }
        }
    }
}

/** Önizləmə düyməsinin yuvası — düyməsiz sətirlərdə də eyni en saxlanılır. */
private val PreviewSlotSize = 40.dp

/**
 * Nümunəni çalır/dayandırır.
 *
 * Vəziyyət düymənin özündədir, çünki eyni anda yalnız bir səs çalınır ([AdhanPreviewPlayer.play]
 * əvvəlkini dayandırır) — hansının çaldığını vərəq səviyyəsində saxlamaq lazım gəlsəydi, ikonu
 * geri qaytarmaq üçün çalınmanın bitdiyini də izləmək lazım olardı; bu düymə isə sadəcə açar kimi
 * işləyir və növbəti toxunuş onsuz da doğru nəticə verir.
 */
@Composable
private fun PreviewButton(sound: AdhanSound) {
    // Vəziyyət PAYLAŞILANDIR, düymənin öz açarı deyil. Yerli açar üç yerdə yalan danışırdı: nümunə
    // öz-özünə biləndə ikon «pauza»da ilişirdi, başqa səs başlayanda köhnə düymə hələ də «pauza»
    // göstərirdi, vərəq bağlanıb açılanda isə vəziyyət itirdi.
    val playing by AdhanPreviewProvider.playing.collectAsState()
    val isPlaying = playing == sound

    IconButton(
        onClick = {
            if (isPlaying) AdhanPreviewProvider.stop() else AdhanPreviewProvider.play(sound)
        },
        modifier = Modifier.size(PreviewSlotSize),
    ) {
        Icon(
            painter = painterResource(
                if (isPlaying) Res.drawable.ic_pause else Res.drawable.ic_play
            ),
            contentDescription = stringResource(Res.string.prayerSoundPreview),
            tint = colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Səsin istifadəçiyə görünən adı. Yeni səs əlavə edəndə bura da bir sətir düşür. */
fun titleOf(sound: AdhanSound): StringResource = when (sound) {
    AdhanSound.SYSTEM_DEFAULT -> Res.string.prayerSoundSystem
    AdhanSound.SILENT -> Res.string.prayerSoundSilent
    AdhanSound.CALL -> Res.string.prayerSoundCall
    AdhanSound.VOICE_AR -> Res.string.prayerSoundVoiceAr
    AdhanSound.RINGTONE -> Res.string.prayerSoundRingtone
    AdhanSound.PHONE -> Res.string.prayerSoundPhone
    AdhanSound.VIBRATING -> Res.string.prayerSoundVibrating
}

private fun descriptionOf(sound: AdhanSound): StringResource = when (sound) {
    AdhanSound.SYSTEM_DEFAULT -> Res.string.prayerSoundSystemDesc
    AdhanSound.SILENT -> Res.string.prayerSoundSilentDesc
    AdhanSound.CALL -> Res.string.prayerSoundCallDesc
    AdhanSound.VOICE_AR -> Res.string.prayerSoundVoiceArDesc
    AdhanSound.RINGTONE -> Res.string.prayerSoundRingtoneDesc
    AdhanSound.PHONE -> Res.string.prayerSoundPhoneDesc
    AdhanSound.VIBRATING -> Res.string.prayerSoundVibratingDesc
}
