package com.cafarovceyxun.anamuslim.utils.prayer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Bildiriş səsinin önizləməsi — səs vərəqindəki oxut düyməsi bunu çağırır.
 *
 * Ayrı seam-dir, çünki fayl hər platformada başqa yerdədir: Android-də `res/raw`, iOS-da tətbiq
 * paketindəki `.caf`. `RecitationPlayer` yaramır — o, ayə audiosunun növbəsini və vəziyyətini
 * idarə edir; burada isə bir-iki saniyəlik nümunə çalınır və oxunuş növbəsinə toxunulmamalıdır.
 *
 * ⚠️ **Yalnız [AdhanSound.isCustom] üçün çağır.** Cihazın öz bildiriş səsini tətbiq çala bilməz
 * (`SYSTEM_DEFAULT`), «səssiz»in isə çalınacaq şeyi yoxdur — UI həmin ikisinə düymə göstərməməlidir,
 * yoxsa basılır və heç nə olmur (CLAUDE.md: «inert default UI-ni azad etmir»).
 *
 * Səs **bildiriş** kanalının səviyyəsi ilə çalınır, media səviyyəsi ilə yox: istifadəçi əslində
 * bildiriş gələndə nə eşidəcəyini yoxlayır.
 */
interface AdhanPreviewPlayer {
    /**
     * Çalınanı dayandırıb yenisini başladır və nümunənin uzunluğunu **saniyə** ilə qaytarır
     * (bilinmirsə 0.0).
     *
     * Uzunluq [AdhanPreviewProvider]-ə lazımdır: nümunə öz-özünə bitəndə oxut ikonu geri qayıtmalıdır.
     * Platforma dinləyicisi (Android `OnCompletionListener`, iOS `AVAudioPlayerDelegate`) əvəzinə
     * uzunluq qaytarılır — delegate-i Kotlin/Native tərəfdə saxlamaq üçün əlavə obyekt ömrü
     * idarəsi lazım gələrdi, nəticə isə eynidir.
     */
    fun play(sound: AdhanSound): Double

    fun stop()
}

/**
 * Qeydiyyat: Android `QuranApp.onCreate()`, iOS `initSharedForIos()`.
 *
 * ⚠️ **Fabrik yox, TƏK İNSTANSİYA saxlanılır.** Əvvəl burada `provider: (() -> AdhanPreviewPlayer)?`
 * vardı və hər müraciətdə `invoke()` olunurdu — yəni `play()` bir obyektdə çalır, `stop()` isə
 * **tamamilə yeni, boş** obyektdə heç nə etmirdi. Nəticədə üç şey birdən sınırdı: pauza işləmirdi,
 * ikinci səs birincini kəsmirdi, vərəq bağlananda səs davam edirdi. Vəziyyət saxlayan seam
 * ([PrayerReminderProvider] kimi vəziyyətsizlərdən fərqli olaraq) instansiya kimi verilməlidir.
 */
object AdhanPreviewProvider {
    private var instance: AdhanPreviewPlayer? = null

    fun setProvider(value: AdhanPreviewPlayer) {
        instance = value
    }

    private val player: AdhanPreviewPlayer
        get() = instance
            ?: error("AdhanPreviewProvider qurulmayıb — platforma bootstrap-ında setProvider çağır")

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var resetJob: Job? = null

    private val _playing = MutableStateFlow<AdhanSound?>(null)

    /**
     * Hazırda çalınan nümunə; heç nə çalınmırsa null.
     *
     * UI ikonu **bundan** oxuyur, öz yerli açarından yox: yerli açar nümunə öz-özünə biləndə
     * «pauza» ikonunda ilişib qalırdı və növbəti toxunuş heç nə etmirdi.
     */
    val playing: StateFlow<AdhanSound?> = _playing.asStateFlow()

    fun play(sound: AdhanSound) {
        val seconds = player.play(sound)
        _playing.value = sound

        resetJob?.cancel()
        resetJob = scope.launch {
            delay((seconds * 1000).toLong().coerceAtLeast(0L))
            // Şərt vacibdir: gözləyərkən istifadəçi başqa səsə keçə bilər.
            if (_playing.value == sound) _playing.value = null
        }
    }

    fun stop() {
        resetJob?.cancel()
        resetJob = null
        instance?.stop()
        _playing.value = null
    }
}
