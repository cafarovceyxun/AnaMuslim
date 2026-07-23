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
import com.cafarovceyxun.anamuslim.utils.IntentUtils.INTENT_ACTION_OPEN_READER
import com.cafarovceyxun.anamuslim.utils.Log
import com.cafarovceyxun.anamuslim.utils.app.NotificationUtils
import com.cafarovceyxun.anamuslim.utils.reader.ReadType
import com.cafarovceyxun.anamuslim.utils.reader.factory.ReaderFactory

object ShortcutUtils {
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
