/*
 * (c) Faisal Khan. Created on 18/2/2022.
 */
package com.cafarovceyxun.anamuslim.utils.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.work.ForegroundInfo
import com.cafarovceyxun.anamuslim.R
import com.cafarovceyxun.anamuslim.utils.prayer.AdhanSound
import com.cafarovceyxun.anamuslim.utils.receivers.CrashReceiver

object NotificationUtils {
    const val CHANNEL_ID_DEFAULT = "default"
    private const val CHANNEL_NAME_DEFAULT = "Default Channel"
    private const val CHANNEL_DESC_DEFAULT = "Miscellaneous notifications"

    const val CHANNEL_ID_VOTD = "votd"
    private const val CHANNEL_NAME_VOTD = "Verse of The Day"
    private const val CHANNEL_DESC_VOTD = "Daily verse reminder notifications"

    const val CHANNEL_ID_RECITATION_PLAYER = "recitation_player"
    private const val CHANNEL_NAME_RECITATION_PLAYER = "Recitation Player"
    private const val CHANNEL_DESC_RECITATION_PLAYER = "Recitation Player notifications"

    /**
     * Namaz vaxtı bildirişləri.
     *
     * ⚠️ `votd` kanalı TƏKRAR İŞLƏDİLMİR: (a) kanalın parametrləri yaradıldıqdan sonra istifadəçinin
     * cihazında dondurulur, (b) istifadəçi günün ayəsini susdurub namaz bildirişini saxlaya
     * bilməlidir — eyni kanalda bu mümkün olmazdı.
     */
    const val CHANNEL_ID_PRAYER = "prayer"
    private const val CHANNEL_NAME_PRAYER = "Prayer times"
    private const val CHANNEL_DESC_PRAYER = "Prayer time reminders"

    /** Öz səsi olan namaz kanallarının prefiksi — bax [prayerChannelId]. */
    private const val CHANNEL_ID_PRAYER_PREFIX = "prayer_"

    const val CHANNEL_ID_DOWNLOADS = "downloads"
    private const val CHANNEL_NAME_DOWNLOADS = "Downloads"
    private const val CHANNEL_DESC_DOWNLOADS = "Notifications for downloads"


    fun createNotificationChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.getSystemService(NotificationManager::class.java).apply {
                createNotificationChannel(createDefaultChannel())
                createNotificationChannel(createVOTDChannel())
                createNotificationChannel(createPrayerChannel())
                createNotificationChannel(createDownloadsChannel())
                createNotificationChannel(createRecitationChannel())
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createDefaultChannel(): NotificationChannel {
        return NotificationChannel(
            CHANNEL_ID_DEFAULT,
            CHANNEL_NAME_DEFAULT,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_DESC_DEFAULT
            lightColor = Color.GREEN
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            vibrationPattern = longArrayOf(500, 500)

            enableLights(true)
            setShowBadge(true)
            enableVibration(true)

            setSound(
                Settings.System.DEFAULT_NOTIFICATION_URI,
                AudioAttributes.Builder().apply {
                    setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                }.build()
            )
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createVOTDChannel(): NotificationChannel {
        return NotificationChannel(
            CHANNEL_ID_VOTD,
            CHANNEL_NAME_VOTD,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESC_VOTD
            lightColor = Color.GREEN
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            vibrationPattern = longArrayOf(500, 500)

            enableLights(true)
            setShowBadge(true)
            enableVibration(true)

            setSound(
                Settings.System.DEFAULT_NOTIFICATION_URI,
                AudioAttributes.Builder().apply {
                    setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                }.build()
            )
        }
    }

    /** Günün ayəsi kanalının eyni forması: yüksək önəm + sistem default bildiriş səsi. */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createPrayerChannel(): NotificationChannel {
        return NotificationChannel(
            CHANNEL_ID_PRAYER,
            CHANNEL_NAME_PRAYER,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESC_PRAYER
            lightColor = Color.GREEN
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            vibrationPattern = longArrayOf(500, 500)

            enableLights(true)
            setShowBadge(true)
            enableVibration(true)

            setSound(
                Settings.System.DEFAULT_NOTIFICATION_URI,
                AudioAttributes.Builder().apply {
                    setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                }.build()
            )
        }
    }

    /**
     * Seçilmiş səs üçün kanal id-si və (lazımdırsa) kanalın özü.
     *
     * ⚠️ **Kanalın səsi yaradıldıqdan sonra dondurulur** — mövcud `prayer` kanalının səsini
     * dəyişmək istifadəçinin cihazında heç bir təsir vermir. Ona görə **hər səsin öz kanalı** var:
     * defolt səs köhnə `prayer` kanalında qalır (mövcud istifadəçilər üçün heç nə dəyişmir), qalan
     * səslər isə ilk dəfə seçiləndə `prayer_<id>` kimi yaradılır.
     *
     * Kanal siyahısında hər səs ayrıca sətir kimi görünür — bu, sistem ayarlarında hər namaz səsini
     * ayrıca susdurmağa da imkan verir.
     */
    fun prayerChannelId(ctx: Context, sound: AdhanSound): String {
        if (sound == AdhanSound.DEFAULT) return CHANNEL_ID_PRAYER

        val channelId = CHANNEL_ID_PRAYER_PREFIX + sound.id
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(createPrayerSoundChannel(ctx, channelId, sound))
        }

        return channelId
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createPrayerSoundChannel(
        ctx: Context,
        channelId: String,
        sound: AdhanSound,
    ): NotificationChannel {
        return NotificationChannel(
            channelId,
            "$CHANNEL_NAME_PRAYER — ${sound.id}",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESC_PRAYER
            lightColor = Color.GREEN
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            vibrationPattern = longArrayOf(500, 500)

            enableLights(true)
            setShowBadge(true)
            enableVibration(true)

            val rawName = sound.androidRawName
            if (rawName == null) {
                // Səssiz: vibrasiya və ekran bildirişi qalır, səs yoxdur.
                setSound(null, null)
            } else {
                // `getIdentifier` əvəzinə resurs URI-si: ad refleksiya ilə axtarılmır, R8 da
                // faylı toxunulmamış saxlayır.
                setSound(
                    "android.resource://${ctx.packageName}/raw/$rawName".toUri(),
                    AudioAttributes.Builder().apply {
                        setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    }.build()
                )
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createDownloadsChannel(): NotificationChannel {
        return createChannel(
            CHANNEL_ID_DOWNLOADS,
            CHANNEL_NAME_DOWNLOADS,
            CHANNEL_DESC_DOWNLOADS
        )
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createRecitationChannel(): NotificationChannel {
        return createChannel(
            CHANNEL_ID_RECITATION_PLAYER,
            CHANNEL_NAME_RECITATION_PLAYER,
            CHANNEL_DESC_RECITATION_PLAYER
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel(
        channelId: String,
        channelName: String,
        desc: String
    ): NotificationChannel {
        return NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = desc
            vibrationPattern = null
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC

            enableLights(false)
            setSound(null, null)
            enableVibration(false)
        }
    }

    fun createEmptyNotif(ctx: Context, channelId: String): Notification {
        return NotificationCompat.Builder(ctx, channelId).apply {
            setContentTitle("")
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentText("")
        }.build()
    }

    fun createForegroundInfoFallback(
        context: Context,
    ): ForegroundInfo {
        return ForegroundInfo(1, createEmptyNotif(context, CHANNEL_ID_DEFAULT))
    }

    fun showCrashNotification(ctx: Context, stackTraceString: String) {
        var flag = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flag = flag or PendingIntent.FLAG_IMMUTABLE
        }

        val copyIntent = Intent(ctx, CrashReceiver::class.java).apply {
            action = CrashReceiver.CRASH_ACTION_COPY_LOG
            putExtra(Intent.EXTRA_TEXT, stackTraceString)
        }

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID_DEFAULT).apply {
            setContentTitle(ctx.getString(R.string.lastCrashLog))
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentText(stackTraceString)
            setStyle(NotificationCompat.BigTextStyle().bigText(stackTraceString))
            addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.icon_copy,
                    ctx.getString(R.string.strLabelCopy),
                    PendingIntent.getBroadcast(ctx, 0, copyIntent, flag)
                ).build()
            )
        }.build()

        ContextCompat.getSystemService(ctx, NotificationManager::class.java)
            ?.notify(CrashReceiver.NOTIFICATION_ID_CRASH, notification)
    }
}
