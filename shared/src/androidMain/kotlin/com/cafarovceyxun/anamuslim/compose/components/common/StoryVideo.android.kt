package com.cafarovceyxun.anamuslim.compose.components.common

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.cafarovceyxun.anamuslim.utils.AppLogger
import kotlinx.coroutines.delay
import java.io.File

@OptIn(UnstableApi::class)
@Composable
actual fun StoryVideo(
    url: String,
    modifier: Modifier,
    paused: Boolean,
    playbackSpeed: Float,
    onProgress: (Float) -> Unit,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val currentOnFinished by rememberUpdatedState(onFinished)
    val currentOnProgress by rememberUpdatedState(onProgress)

    val player = remember(url) {
        ExoPlayer.Builder(context)
            // Eyni hekayəyə ikinci baxış şəbəkəyə getmir: oynanan baytlar eyni anda diskə yazılır.
            .setMediaSourceFactory(DefaultMediaSourceFactory(storyCacheDataSourceFactory(context)))
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(url))
                prepare()
                playWhenReady = true
            }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) currentOnFinished()
            }

            // Fayl açılmasa hekayə qara kadrda ilişib qalardı — növbəti slayda keçirik.
            override fun onPlayerError(error: PlaybackException) {
                AppLogger.d(TAG, "Playback failed: ${error.message}")
                currentOnFinished()
            }
        }
        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // Hekayə dayandırılanda video da dayanır; davam edəndə qaldığı yerdən oynayır. Sürət: `2` irəli
    // (media3 səsin tonunu qoruyur), mənfi — geri sarınma: pleyer tərsinə oynamır, ona görə video
    // dayanır və mövqe addım-addım geri çəkilir (dəqiq axtarış — kadr görünsün).
    LaunchedEffect(player, paused, playbackSpeed) {
        if (playbackSpeed < 0f) {
            player.playWhenReady = false
            val step = (STORY_REWIND_TICK_MILLIS * -playbackSpeed).toLong()
            while (true) {
                val target = (player.currentPosition - step).coerceAtLeast(0L)
                player.seekTo(target)
                if (target == 0L) break
                delay(STORY_REWIND_TICK_MILLIS)
            }
            return@LaunchedEffect
        }

        player.setPlaybackSpeed(playbackSpeed)
        player.playWhenReady = !paused
    }

    // ExoPlayer mövqe axını vermir, ona görə kadr sürətinə yaxın intervalla oxunur — zolaq
    // videonun öz vaxtı ilə irəliləsin.
    LaunchedEffect(player) {
        while (true) {
            val duration = player.duration
            if (duration > 0) {
                currentOnProgress((player.currentPosition.toFloat() / duration).coerceIn(0f, 1f))
            }
            delay(POSITION_POLL_MILLIS)
        }
    }

    // ⚠️ `AndroidView`-in factory-si düyün ömründə **bir dəfə** işləyir. Url dəyişəndə
    // `remember(url)` yeni `ExoPlayer` qaytarır, ekrandakı `PlayerView` isə köhnə — artıq
    // buraxılmış — pleyerdə qalırdı: ikinci hekayədə kadr açılmır, zolaq isə yeni pleyerin
    // mövqeyi ilə irəliləyirdi. `key` görünüşü də pleyerlə birlikdə yeniləyir.
    key(url) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                }
            },
            // Eyni url-da belə görünüş yenidən qurulsa pleyer bağlanmış qalmasın.
            update = { view -> view.player = player },
            modifier = modifier,
        )
    }
}

@OptIn(UnstableApi::class)
private fun storyCacheDataSourceFactory(context: Context): CacheDataSource.Factory =
    CacheDataSource.Factory()
        .setCache(StoryVideoCache.get(context))
        .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context))
        // Keş faylı pozulubsa video şəbəkədən oynasın, hekayə qara kadrda qalmasın.
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

/**
 * ⚠️ `SimpleCache` bir qovluğa **bir** instansiya ilə bağlanır — ikincisi `IllegalStateException`
 * verir. Ona görə proses boyu tək obyekt (səs keşləri də `:app`-da eyni qaydadadır, öz qovluqları ilə).
 */
@OptIn(UnstableApi::class)
private object StoryVideoCache {
    @Volatile
    private var cache: SimpleCache? = null

    fun get(context: Context): SimpleCache = cache ?: synchronized(this) {
        cache ?: context.applicationContext.let { app ->
            SimpleCache(
                File(app.cacheDir, "story_video_cache"),
                LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES),
                StandaloneDatabaseProvider(app),
            )
        }.also { cache = it }
    }
}

/** ~10 hekayə videosu; köhnəsi ilk silinir. `cacheDir`-dədir — sistem yer lazım olanda özü təmizləyir. */
private const val MAX_CACHE_BYTES = 200L * 1024 * 1024
private const val POSITION_POLL_MILLIS = 60L
private const val TAG = "StoryVideo"
