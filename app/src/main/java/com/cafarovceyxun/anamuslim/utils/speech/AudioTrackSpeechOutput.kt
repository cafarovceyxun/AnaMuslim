package com.cafarovceyxun.anamuslim.utils.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Sintez olunmuş PCM-i `AudioTrack` ilə (axın rejimi) çalır.
 *
 * Yazı kiçik hissələrlə gedir ki, ləğv (`stop`, növbəti hədis) bir hissədən gec çatmasın; tamponun
 * sonu isə oynatma başlığı ilə gözlənilir — yoxsa [play] səs hələ çalınarkən qayıdar və panel
 * növbəti parçaya qabaqcadan keçərdi. Audio fokus keçici alınır: musiqi/qiraət pauza edir, zəng
 * gələndə isə səs özü susmur (sadəlik üçün) — sistem onsuz da səsi azaldır.
 */
class AudioTrackSpeechOutput(context: Context) : PcmSpeechOutput {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private var track: AudioTrack? = null
    private var focusRequest: AudioFocusRequest? = null
    private var framesWritten = 0L
    @Volatile private var paused = false

    override fun open(sampleRate: Int) {
        close()
        requestFocus()
        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT,
        )
        track = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            // ~250 ms float mono. ⚠️ Ölçü çərçivəyə (4 bayt) bölünməlidir — 22050 bölünmür və AudioTrack
            // telefonda «Invalid audio buffer size» ilə tətbiqi yıxırdı (2026-11-13, SM-A556E).
            .setBufferSizeInBytes(maxOf(minBuffer, sampleRate) / 4 * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            .also { it.play() }
        framesWritten = 0
        paused = false
    }

    override suspend fun play(samples: FloatArray) = withContext(Dispatchers.IO) {
        val track = track ?: return@withContext
        val step = (track.sampleRate / 10).coerceAtLeast(256) // ~100 ms
        var offset = 0
        while (offset < samples.size) {
            coroutineContext.ensureActive()
            if (paused) {
                delay(50)
                continue
            }
            val count = minOf(step, samples.size - offset)
            val written = track.write(samples, offset, count, AudioTrack.WRITE_NON_BLOCKING)
            if (written < 0) return@withContext
            if (written == 0) delay(20)
            offset += written
            framesWritten += written
        }
        // Yazılan hamısı eşidilənə qədər gözlə (pauza zamanı başlıq dayanır, gözləmə də uzanır).
        while ((track.playbackHeadPosition.toLong() and 0xFFFFFFFFL) < framesWritten) {
            coroutineContext.ensureActive()
            delay(30)
        }
    }

    // Düymə əsas axından gəlir, `close()` isə pleyerin axınından — növbə bitən anda basılan pauza
    // artıq buraxılmış treki tuta bilər (IllegalStateException). Ona görə sakitcə udulur.
    override fun pause() {
        paused = true
        runCatching { track?.pause() }
    }

    override fun resume() {
        paused = false
        runCatching { track?.play() }
    }

    override fun close() {
        track?.let {
            runCatching { it.pause() }
            runCatching { it.flush() }
            runCatching { it.stop() }
            it.release()
        }
        track = null
        abandonFocus()
    }

    private fun requestFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes)
                .build()
            focusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        }
    }

    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let(audioManager::abandonAudioFocusRequest)
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }
}
