package com.cafarovceyxun.anamuslim.utils.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.cafarovceyxun.anamuslim.compose.components.media.ImageEditorDialog
import com.cafarovceyxun.anamuslim.compose.components.media.VideoEditorDialog
import kotlinx.coroutines.delay

/** Qalereyadan seçilmiş fayl — baytlar, MIME tipi və (video üçün) uzunluq. */
data class PickedMedia(
    val bytes: ByteArray,
    val mimeType: String,
    val isVideo: Boolean,
) {
    // data class ByteArray-i referansla müqayisə edir — state-də səhv «dəyişmədi» nəticəsi
    // çıxmasın deyə əl ilə yazılır.
    override fun equals(other: Any?): Boolean =
        this === other || (
            other is PickedMedia &&
                isVideo == other.isVideo &&
                mimeType == other.mimeType &&
                bytes.contentEquals(other.bytes)
            )

    override fun hashCode(): Int =
        31 * (31 * bytes.contentHashCode() + mimeType.hashCode()) + isVideo.hashCode()
}

/** Seçicinin nəticəsi. İmtina ediləndə callback ümumiyyətlə çağırılmır. */
sealed interface MediaPickResult {
    data class Picked(val media: PickedMedia) : MediaPickResult

    /** Mənbə video [MediaPickLimits.MAX_SOURCE_VIDEO_MILLIS]-dən uzundur. */
    data object TooLong : MediaPickResult

    /** Seçilmiş fayl [MediaPickLimits.MAX_BYTES]-dan böyükdür. */
    data object TooLarge : MediaPickResult

    /** Video sıxışdırıldıqdan sonra da [MediaPickLimits.MAX_UPLOAD_BYTES]-dan böyükdür. */
    data object StillTooLarge : MediaPickResult

    data object Failed : MediaPickResult
}

/**
 * Qalereyadan seçilmiş, **hələ kodlanmamış** video. Redaktor ([VideoEditorDialog]) bunu göstərir,
 * «Hazır»-da [export] seçilən kəsmə/kadr/səs ilə sıxışdırıb yüklənəcək baytları qaytarır.
 *
 * [width]/[height] ekranda **göstərilən** ölçüdür (fırlanma tətbiq olunub) — kadr çərçivəsinin
 * payları ([VideoCrop]) də ona görədir.
 */
interface PickedVideo {
    val durationMillis: Long
    val width: Int
    val height: Int

    /** Önizləmə pleyerinin aça biləcəyi ünvan (Android `content://`, iOS `file://`). */
    val playbackUrl: String

    /** Zaman zolağı üçün bərabər aralıqlı [count] kadr, JPEG baytları (alınmayan kadr düşür). */
    suspend fun thumbnails(count: Int, maxHeightPx: Int): List<ByteArray>

    /** [onProgress] 0..1, istənilən axından çağırıla bilər. */
    suspend fun export(edit: VideoEdit, onProgress: (Float) -> Unit): MediaPickResult

    /** Müvəqqəti faylları silir. Redaktor bağlananda (hazır/ləğv) bir dəfə çağırılır. */
    fun discard()
}

/** Kadrın saxlanılan hissəsi — göstərilən kadrın payları ilə (0..1), yuxarı-sol mənşəli. */
data class VideoCrop(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f,
) {
    val isFull: Boolean get() = left <= 0f && top <= 0f && right >= 1f && bottom >= 1f

    companion object {
        val Full = VideoCrop()

        /** Hər tərəfin ən kiçik payı — tutacaqlar bir-birinin üstünə düşməsin. */
        const val MIN_SPAN = 0.2f
    }
}

data class VideoEdit(
    val startMillis: Long,
    val endMillis: Long,
    val crop: VideoCrop = VideoCrop.Full,
    val muted: Boolean = false,
) {
    val durationMillis: Long get() = (endMillis - startMillis).coerceAtLeast(0L)
}

/** Platforma seçicisinin nəticəsi: ya hazır cavab, ya da redaktora gedəcək video. */
sealed interface PlatformMediaPick {
    data class Done(val result: MediaPickResult) : PlatformMediaPick
    data class Video(val video: PickedVideo) : PlatformMediaPick

    /** Dik (EXIF tətbiq olunmuş), kiçildilmiş şəkil — redaktora gedir, dəyişməsə olduğu kimi yüklənir. */
    data class Image(val media: PickedMedia) : PlatformMediaPick
}

object MediaPickLimits {
    /** Yüklənən hekayə videosunun ən uzun hissəsi — redaktor seçimi bununla məhdudlaşdırır. */
    const val MAX_VIDEO_MILLIS = 180_000L

    /**
     * Seçilə bilən mənbə videonun uzunluğu. Redaktor kəsdiyi üçün 3 dəqiqədən uzun yazı da seçilir,
     * amma kadr zolağı və kodlama çox uzun faylda mənasız yavaşlayır.
     */
    const val MAX_SOURCE_VIDEO_MILLIS = 10L * 60 * 1000

    /** Seçilə bilən mənbə faylın yuxarı həddi (sıxışdırmadan ƏVVƏL). */
    const val MAX_BYTES = 50L * 1024 * 1024

    /**
     * ⚠️ **Video yüklənməzdən əvvəl MÜTLƏQ sıxışdırılır** (`MediaPicker` actual-larında), çünki
     * hekayə videosu hər baxışda endirilir: 2026-09-18-də üç xam ekran yazısı (27.9, 21.2,
     * 19.6 MB) gündə **1.65 GB** egress yaradıb və Supabase-in pulsuz 5 GB-lıq «Cached Egress»
     * kvotası bir neçə günə dolub (160%). Nə kompilyator, nə test, nə də tətbiqin özü bunu göstərir
     * — yalnız hesabatda görünür.
     *
     * 2026-10-08: storage öz serverimizə (Oracle, ayda 10 TB pulsuz trafik) köçəndən sonra büdcə
     * 8 → 18 MB qaldırıldı — 405×720/0.5 Mbps-lik videoda ekran yazısının mətni oxunmurdu. Sıxışdırma
     * yenə də **məcburidir**: xam yazı 20-30 MB-dır və baxanın mobil trafiki də hesabdadır.
     *
     * Üç qapı var: hədəf ölçü ([TARGET_VIDEO_BYTES], bitrate bundan hesablanır), sərt klient həddi
     * (bu sabit) və bucket-in öz `file_size_limit`-i (25 MB, server tərəf — bundan böyük olmalıdır).
     */
    const val MAX_UPLOAD_BYTES = 22L * 1024 * 1024

    /** Sıxışdırmanın hədəfi — bitrate videonun uzunluğuna görə buradan hesablanır. */
    const val TARGET_VIDEO_BYTES = 18L * 1024 * 1024

    /**
     * **Qısa** kənar. Hekayə portretdir, yəni nəticə 720×1280 olur. (2026-10-08-ə qədər uzun kənar
     * idi — 405×720-də ekran yazısının mətni bulanırdı.) Mənbə bundan kiçikdirsə böyüdülmür.
     */
    const val TARGET_VIDEO_SHORT_SIDE = 720

    /**
     * Ekran yazısı 60-120 fps-dir, hekayəyə isə 30 bəsdir — eyni ölçüdə hər kadra iki dəfə çox bit
     * düşür. Mənbə bundan yavaşdırsa toxunulmur.
     */
    const val TARGET_VIDEO_FRAME_RATE = 30

    // Kodek H.265 (HEVC) istənilir — eyni həcmdə H.264-dən ~40% təmiz. Oynatma hər yerdə var:
    // Android-in özündə proqram HEVC dekoderi (5.0-dan), iOS 17-də isə aparat dekoderi. Kodlayıcı
    // yoxdursa (bəzi Android-lər) H.264-ə düşülür — fayl yenə `video/mp4`-dür.
    const val AUDIO_BITRATE = 96_000
    const val MIN_VIDEO_BITRATE = 400_000

    /** 720p/30 HEVC üçün bundan yuxarısı gözlə seçilmir, faylı isə boş yerə böyüdür. */
    const val MAX_VIDEO_BITRATE = 3_000_000

    /**
     * Şəkil: qısa kənar 1080-dən böyükdürsə kiçildilir, JPEG [IMAGE_JPEG_QUALITY] ilə yenidən
     * yazılır. Kiçik və onsuz da yüngül fayl ([IMAGE_PASSTHROUGH_BYTES]-dan az) olduğu kimi gedir
     * — PNG ekran görüntüsünün mətni JPEG artefaktı almasın.
     */
    const val IMAGE_MAX_SHORT_SIDE = 1080
    const val IMAGE_JPEG_QUALITY = 90
    const val IMAGE_PASSTHROUGH_BYTES = 1_536L * 1024

    /**
     * Bitrate **uzunluqdan** hesablanır: hədəf sabit ölçüdür, yəni 10 saniyəlik yazı da, iki
     * dəqiqəlik yazı da təxminən eyni fayl həcminə düşür (qısa yazını [MAX_VIDEO_BITRATE] kəsir).
     * Sabit bitrate uzun videonu yenidən onlarla MB edərdi. Hər iki platforma eyni düsturu işlədir.
     */
    fun targetVideoBitrate(durationMillis: Long?): Int {
        val seconds = ((durationMillis ?: 0L) / 1000.0).coerceAtLeast(1.0)
        val budget = (TARGET_VIDEO_BYTES * 8 / seconds).toLong() - AUDIO_BITRATE
        return budget.coerceIn(MIN_VIDEO_BITRATE.toLong(), MAX_VIDEO_BITRATE.toLong()).toInt()
    }

    /**
     * Çıxış ölçüsü: qısa kənar ən çox [maxShortSide], nisbət saxlanılır, ölçülər cüt ədəddir
     * (kodlayıcılar tək ölçünü ya rədd edir, ya da kənarda yaşıl zolaq verir). Kiçiltmə lazım
     * deyilsə `null`.
     */
    fun scaledSize(width: Int, height: Int, maxShortSide: Int): Pair<Int, Int>? {
        val shortSide = minOf(width, height)
        if (width <= 0 || height <= 0 || shortSide <= maxShortSide) return null
        val scale = maxShortSide.toDouble() / shortSide
        fun even(value: Double) = (value / 2).toInt().coerceAtLeast(1) * 2
        return even(width * scale) to even(height * scale)
    }
}

/**
 * Sistem media seçicisi (şəkil + video). Qaytarılan lambda seçicini açır; **`null`** o deməkdir ki,
 * bu platformada seçici yoxdur — çağıran tərəf onda düyməni ümumiyyətlə göstərməməlidir.
 *
 * Seçimdən sonra redaktor açılır — video üçün kəsmə/kadr/səs, şəkil üçün hekayə çərçivəsində
 * yerləşdirmə (böyütmə, fırlatma) — və [onResult] yalnız «Hazır»-dan sonra gəlir; redaktordan
 * imtina ediləndə heç çağırılmır, seçicinin özündən imtina kimi.
 */
@Composable
fun rememberMediaPicker(onResult: (MediaPickResult) -> Unit): (() -> Unit)? {
    val currentOnResult by rememberUpdatedState(onResult)
    var editing by remember { mutableStateOf<PickedVideo?>(null) }
    var editingImage by remember { mutableStateOf<PickedMedia?>(null) }

    val launch = rememberPlatformMediaPicker { pick ->
        when (pick) {
            is PlatformMediaPick.Done -> currentOnResult(pick.result)
            is PlatformMediaPick.Video -> {
                editing?.discard()
                editing = pick.video
            }

            is PlatformMediaPick.Image -> editingImage = pick.media
        }
    }

    // Redaktor açıq ikən çağıran kart kompozisiyadan çıxsa (siyahı sürüşdürüldü) müvəqqəti fayl
    // qalmasın.
    DisposableEffect(Unit) {
        onDispose { editing?.discard() }
    }

    // Nəticə dialoq ekrandan **çıxandan sonra** verilir: iOS-da toast açar pəncərəyə qoşulur və
    // dialoq hələ açıqkən göstərilən xəta toastı («Alınmadı», «Çox böyükdür») onunla birlikdə itirdi.
    var pendingResult by remember { mutableStateOf<MediaPickResult?>(null) }
    LaunchedEffect(pendingResult, editing, editingImage) {
        val result = pendingResult ?: return@LaunchedEffect
        if (editing != null || editingImage != null) return@LaunchedEffect
        delay(RESULT_DELIVERY_DELAY_MILLIS)
        pendingResult = null
        currentOnResult(result)
    }

    editing?.let { video ->
        VideoEditorDialog(
            video = video,
            onFinished = { result ->
                editing = null
                video.discard()
                pendingResult = result
            },
        )
    }

    editingImage?.let { media ->
        ImageEditorDialog(
            media = media,
            onFinished = { result ->
                editingImage = null
                pendingResult = result
            },
        )
    }

    return launch
}

/**
 * Platformanın seçicisi. `?: error(...)` yerinə null qaytarılır, çünki bu, hər platformada olmaya
 * bilən imkandır (bax CLAUDE.md, «Provider/DI seam qaydası»): düymə basılıb heç nə etməkdənsə,
 * görünməsin.
 */
@Composable
internal expect fun rememberPlatformMediaPicker(onResult: (PlatformMediaPick) -> Unit): (() -> Unit)?

/** Dialoq pəncərəsinin sökülməsinə vaxt — bir-iki kadr bəs edir, ehtiyatla bir az artıq. */
private const val RESULT_DELIVERY_DELAY_MILLIS = 300L
