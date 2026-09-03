package com.cafarovceyxun.anamuslim.compose.components.prayer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.RadioItem
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheet
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_prayer_times
import com.cafarovceyxun.anamuslim.resources.prayerSoundSheetTitle
import com.cafarovceyxun.anamuslim.resources.prayerSoundSilent
import com.cafarovceyxun.anamuslim.resources.prayerSoundSilentDesc
import com.cafarovceyxun.anamuslim.resources.prayerSoundSystem
import com.cafarovceyxun.anamuslim.resources.prayerSoundSystemDesc
import com.cafarovceyxun.anamuslim.utils.prayer.AdhanSound
import com.cafarovceyxun.anamuslim.utils.prayer.Prayer
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Bir namaz vaxtının bildiriş səsi.
 *
 * Siyahı [AdhanSound.entries]-dən qurulur: yeni səs əlavə edəndə burada dəyişilməli yeganə şey
 * [titleOf]/[descriptionOf]-dakı ad sətirləridir.
 *
 * ⚠️ **Önizləmə düyməsi qəsdən yoxdur.** Hazırda kataloqda öz faylı olan səs yoxdur, sistem səsini
 * isə tətbiq özü çala bilməz — düymə qoysaydıq basılar və heç nə olmazdı (CLAUDE.md-dəki «inert
 * default UI-ni azad etmir» qaydası). İlk azan faylı gələndə önizləmə [AdhanSound.isCustom] ilə
 * şərtlənərək əlavə olunur.
 */
@Composable
fun AdhanSoundSheet(
    prayer: Prayer?,
    selected: AdhanSound,
    onSelect: (AdhanSound) -> Unit,
    onClose: () -> Unit,
) {
    BottomSheet(
        isOpen = prayer != null,
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
                    onClick = { onSelect(sound) },
                )
            }
        }
    }
}

/** Səsin istifadəçiyə görünən adı. Yeni səs əlavə edəndə bura da bir sətir düşür. */
fun titleOf(sound: AdhanSound): StringResource = when (sound) {
    AdhanSound.SYSTEM_DEFAULT -> Res.string.prayerSoundSystem
    AdhanSound.SILENT -> Res.string.prayerSoundSilent
}

private fun descriptionOf(sound: AdhanSound): StringResource = when (sound) {
    AdhanSound.SYSTEM_DEFAULT -> Res.string.prayerSoundSystemDesc
    AdhanSound.SILENT -> Res.string.prayerSoundSilentDesc
}
