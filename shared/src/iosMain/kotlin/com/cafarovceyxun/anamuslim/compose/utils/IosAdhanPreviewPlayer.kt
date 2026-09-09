package com.cafarovceyxun.anamuslim.compose.utils

import kotlinx.cinterop.ExperimentalForeignApi
import com.cafarovceyxun.anamuslim.utils.prayer.AdhanPreviewPlayer
import com.cafarovceyxun.anamuslim.utils.prayer.AdhanSound
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.AVFAudio.setActive
import platform.Foundation.NSBundle
import platform.Foundation.NSURL

/**
 * Tətbiq paketindəki `.caf` nümunəsini çalır.
 *
 * ⚠️ Sessiya kateqoriyası **`Ambient`**-dir, `Playback` deyil: nümunə zəng düyməsi ilə səssizə
 * salınmalı və arxa fonda gedən musiqini kəsməməlidir — bildiriş səsi elə belə davranır, ona görə
 * önizləmə də eyni olmalıdır. `Playback` seçsəydik nümunə səssiz rejimdə də çalınar və istifadəçi
 * yanlış nəticə çıxarardı.
 */
@OptIn(ExperimentalForeignApi::class)
class IosAdhanPreviewPlayer : AdhanPreviewPlayer {

    private var player: AVAudioPlayer? = null

    override fun play(sound: AdhanSound): Double {
        stop()

        val fileName = sound.iosFileName ?: return 0.0
        val name = fileName.substringBeforeLast('.')
        val extension = fileName.substringAfterLast('.', "")
        val path = NSBundle.mainBundle.pathForResource(name, extension) ?: return 0.0

        AVAudioSession.sharedInstance().apply {
            setCategory(AVAudioSessionCategoryAmbient, null)
            setActive(true, null)
        }

        val created = AVAudioPlayer(contentsOfURL = NSURL.fileURLWithPath(path), error = null).apply {
            prepareToPlay()
            play()
        }

        player = created
        return created.duration
    }

    override fun stop() {
        player?.stop()
        player = null
    }
}
