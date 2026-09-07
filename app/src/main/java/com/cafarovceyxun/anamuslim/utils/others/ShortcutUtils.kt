/*
 * (c) Faisal Khan. Created on 30/10/2021.
 */
package com.cafarovceyxun.anamuslim.utils.others

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.cafarovceyxun.anamuslim.R
import com.cafarovceyxun.anamuslim.activities.MainActivity
import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderMode
import com.cafarovceyxun.anamuslim.db.entities.user.ReadHistoryEntity
import com.cafarovceyxun.anamuslim.utils.IntentUtils.INTENT_ACTION_OPEN_ADMIN
import com.cafarovceyxun.anamuslim.utils.IntentUtils.INTENT_ACTION_OPEN_READER
import com.cafarovceyxun.anamuslim.utils.Log
import com.cafarovceyxun.anamuslim.utils.app.NotificationUtils
import com.cafarovceyxun.anamuslim.utils.reader.ReadType
import com.cafarovceyxun.anamuslim.utils.reader.factory.ReaderFactory

object ShortcutUtils {

    /** İdarəetmə paneli qısayolunun id-si — yaradılması və silinməsi eyni id ilə gedir. */
    private const val ADMIN_SHORTCUT_ID = "admin_hub"

    /**
     * «İdarəetmə paneli» qısayolu — ikona basıb saxlayanda çıxır, yalnız giriş edilibsə
     * ([com.cafarovceyxun.anamuslim.utils.others.AdminShortcutSync] idarə edir).
     *
     * Hədəf `MainActivity`-dir, `ActivitySettings` yox: sonuncu `exported="false"`-dur və
     * launcher-in başlada bilməyəcəyi komponentə qısayol qoymaq olmaz. `MainActivity` action-ı
     * tutub paneli özü açır — VOTD/«oxumağa davam et» qısayolları ilə eyni qəlib.
     */
    fun pushAdminShortcut(ctx: Context, subtitle: String?) {
        try {
            val intent = Intent(INTENT_ACTION_OPEN_ADMIN).apply {
                setClass(ctx, MainActivity::class.java)
            }

            // Admin UI-nin qalanı kimi azərbaycanca sabitdir — beş lokalizə faylına düşmür.
            val label = "İdarəetmə paneli"
            val builder = ShortcutInfoCompat.Builder(ctx, ADMIN_SHORTCUT_ID)
                .setShortLabel(label)
                .setLongLabel(if (subtitle.isNullOrBlank()) label else "$label — $subtitle")
                .setIcon(IconCompat.createWithResource(ctx, R.mipmap.ic_launcher))
                .setIntent(intent)

            ShortcutManagerCompat.pushDynamicShortcut(ctx, builder.build())
        } catch (e: Exception) {
            Log.saveError(e, "ShortcutUtils")
        }
    }

    fun removeAdminShortcut(ctx: Context) {
        try {
            ShortcutManagerCompat.removeDynamicShortcuts(ctx, listOf(ADMIN_SHORTCUT_ID))
        } catch (e: Exception) {
            Log.saveError(e, "ShortcutUtils")
        }
    }

    fun pushVOTDShortcut(ctx: Context, chapterNo: Int, verseNo: Int) {
        val intent = ReaderFactory.prepareSingleVerseIntent(chapterNo, verseNo).apply {
            setClass(ctx, MainActivity::class.java)
            action = INTENT_ACTION_OPEN_READER
        }

        val builder = ShortcutInfoCompat.Builder(ctx, NotificationUtils.CHANNEL_ID_VOTD)
            .setShortLabel(ctx.getString(R.string.strTitleVOTD))
            .setLongLabel(ctx.getString(R.string.strTitleVOTD))
            .setIntent(intent)

        try {
            builder.setIcon(IconCompat.createWithResource(ctx, R.drawable.dr_ic_shortcut_votd))
            ShortcutManagerCompat.pushDynamicShortcut(ctx, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pushLastVersesShortcut(
        ctx: Context,
        entity: ReadHistoryEntity,
    ) {
        try {
            val id = "last_verses"
            val intent = ReaderFactory.prepareLastVersesIntent(
                readType = ReadType.fromValue(entity.readType),
                readerMode = ReaderMode.fromValue(entity.readerMode),
                verse = ChapterVersePair(entity.chapterNo, entity.fromVerseNo),
                divisionNo = entity.divisionNo,
            )

            if (intent == null) {
                val ids: MutableList<String> = ArrayList()
                ids.add(id)
                ShortcutManagerCompat.removeDynamicShortcuts(ctx, ids)
                return
            }

            intent.setClass(ctx, MainActivity::class.java)
            intent.action = INTENT_ACTION_OPEN_READER

            val builder = ShortcutInfoCompat.Builder(ctx, id)
                .setShortLabel(ctx.getString(R.string.strLabelContinueReading))
                .setLongLabel(ctx.getString(R.string.strLabelContinueReading))
                .setIntent(intent)

            try {
                builder.setIcon(IconCompat.createWithResource(ctx, R.mipmap.ic_launcher))
                ShortcutManagerCompat.pushDynamicShortcut(ctx, builder.build())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            Log.saveError(e, "ShortcutUtils")
        }
    }
}
