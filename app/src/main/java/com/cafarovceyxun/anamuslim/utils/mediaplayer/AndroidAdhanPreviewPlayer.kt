package com.cafarovceyxun.anamuslim.utils.mediaplayer

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.cafarovceyxun.anamuslim.utils.app.rawResIdOrNull
import com.cafarovceyxun.anamuslim.utils.prayer.AdhanPreviewPlayer
import com.cafarovceyxun.anamuslim.utils.prayer.AdhanSound

/**
 * `res/raw`-dakı nümunəni çalır.
 *
 * `USAGE_NOTIFICATION` qəsdlidir: istifadəçi bildiriş gələndə nə eşidəcəyini yoxlayır, ona görə
 * media deyil, **bildiriş** səviyyəsindən getməlidir — telefonu səssizə salıbsa nümunə də
 * eşidilməməlidir.
 */
class AndroidAdhanPreviewPlayer(private val context: Context) : AdhanPreviewPlayer {

    private var player: MediaPlayer? = null

    override fun play(sound: AdhanSound): Double {
        stop()

        // ⚠️ `getIdentifier` DEYİL: adla axtarış release-də səsi tamamilə yox edirdi —
        // shrinker `R.raw.*`-a müraciət görmədiyi üçün faylları APK-dan silirdi, burada isə
        // resId 0 gəlib önizləmə **səssizcə** heç nə etmirdi (bax [rawResIdOrNull]).
        val resId = sound.rawResIdOrNull() ?: return 0.0

        val created = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(context, Uri.parse("android.resource://${context.packageName}/$resId"))
            // ⚠️ `this@AndroidAdhanPreviewPlayer` şərtdir: `apply` blokunda `this` MediaPlayer-dir,
            // ona görə sadəcə `stop()` yazsaq **MediaPlayer.stop()** çağırılır — obyekt buraxılmır,
            // sahə də null olmur, yəni hər önizləmə bir pleyer sızdırır.
            setOnCompletionListener { this@AndroidAdhanPreviewPlayer.stop() }
            setOnErrorListener { _, _, _ -> this@AndroidAdhanPreviewPlayer.stop(); true }
            prepare()
            start()
        }

        player = created
        return created.duration.coerceAtLeast(0) / 1000.0
    }

    override fun stop() {
        player?.run {
            runCatching { if (isPlaying) stop() }
            release()
        }
        player = null
    }
}
