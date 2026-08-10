package com.cafarovceyxun.anamuslim.utils.verse

import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.VersePreferences
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.repository.supabase.DailyContentRepository
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strLabelVerseSerialWithChapter
import com.cafarovceyxun.anamuslim.resources.strTitleDailyHadith
import com.cafarovceyxun.anamuslim.resources.strTitleVOTD
import com.cafarovceyxun.anamuslim.utils.reader.TranslUtils
import com.cafarovceyxun.anamuslim.utils.reader.factory.QuranTranslationFactory
import com.cafarovceyxun.anamuslim.utils.supabase.DailyContent
import com.cafarovceyxun.anamuslim.utils.univ.StringUtils
import org.jetbrains.compose.resources.getString

/**
 * What the daily reminder shows, independent of how it is delivered.
 *
 * Same split as the download/sync seams: the *work* (fetch the content, resolve the translation,
 * localize the reference) is shared; only the delivery mechanism is platform-bound — a
 * `NotificationCompat` post from `VerseOfTheDayWorker` on Android, a `UNNotificationRequest` on
 * iOS. [slugs] travels with it so tapping the notification opens the reader with exactly the
 * translation the notification displayed. [chapterNo]/[verseNo] are null for a hadith, which has
 * no reader destination — tapping it just opens the app.
 */
data class VotdNotification(
    val title: String,
    val body: String,
    val reference: String,
    val chapterNo: Int?,
    val verseNo: Int?,
    val slugs: Set<String>,
    /** Identifies *this* content; see [VotdNotificationContent.buildIfUnseen]. */
    val signature: String,
)

/**
 * Builds the reminder from the content an admin published to Supabase (`daily_content`) — the same
 * row the home card renders. Nothing is drawn locally any more: a random verse the server never
 * chose would contradict the card, and there would be no way to tell "already shown" from "new".
 *
 * Both platforms poll roughly every six hours (four times a day) rather than once, because the row
 * can be published or edited at any hour. [buildIfUnseen] is what makes that safe: it returns null
 * while the row is unchanged, so only the first poll that sees new content notifies.
 */
object VotdNotificationContent {

    private val repository = DailyContentRepository()

    /**
     * Today's reminder, or null when there is nothing to show — the reminder is off, no row is
     * published for today, or the row carries no readable text.
     */
    suspend fun build(): VotdNotification? {
        if (!VersePreferences.getVOTDReminderEnabled()) return null

        val content = repository.fetchTodayContent() ?: return null

        return when (content.content_type) {
            "hadith" -> buildHadith(content)
            else -> buildVerse(content)
        }
    }

    /**
     * [build], but null as well when the published content is the same one the user was already
     * notified about. Callers must confirm with [markNotified] once the notification is actually
     * posted, so a failed post is retried by the next poll.
     */
    suspend fun buildIfUnseen(): VotdNotification? {
        val notification = build() ?: return null

        if (notification.signature == VersePreferences.getDailyContentNotifSignature()) return null

        return notification
    }

    /** Records [notification] as delivered, silencing the remaining polls of the day. */
    suspend fun markNotified(notification: VotdNotification) {
        VersePreferences.setDailyContentNotifSignature(notification.signature)
    }

    private suspend fun buildVerse(content: DailyContent): VotdNotification? {
        val chapterNo = content.chapter_no ?: return null
        val verseNo = content.verse_no ?: return null

        var slugs = ReaderPreferences.getTranslations()
        if (slugs.isEmpty()) {
            slugs = TranslUtils.defaultTranslationSlugs()
        }

        val factory = QuranTranslationFactory()
        val translation = try {
            factory.getTranslationsSingleVerse(slugs, chapterNo, verseNo).firstOrNull()
        } finally {
            factory.close()
        }

        // The downloaded translation is preferred — it is what the reader will show on tap — but a
        // user with no translation installed still gets the text the admin published.
        val body = translation
            ?.let { StringUtils.removeHTML(it.text, false) }
            ?: content.text_az.takeIf { it.isNotBlank() }
            ?: return null

        val verse = RepositoryProvider.quranRepository.getVerseWithDetails(
            chapterNo,
            verseNo,
            arabicEnabled = false,
        )

        val reference = verse?.let {
            getString(
                Res.string.strLabelVerseSerialWithChapter,
                it.chapter.getCurrentName(),
                chapterNo,
                verseNo,
            )
        } ?: "$chapterNo:$verseNo"

        return VotdNotification(
            title = getString(Res.string.strTitleVOTD),
            body = body,
            reference = reference,
            chapterNo = chapterNo,
            verseNo = verseNo,
            slugs = slugs,
            signature = content.signature(),
        )
    }

    private suspend fun buildHadith(content: DailyContent): VotdNotification? {
        val body = content.text_az.takeIf { it.isNotBlank() }
            ?: content.text_ar.takeIf { it.isNotBlank() }
            ?: return null

        return VotdNotification(
            title = getString(Res.string.strTitleDailyHadith),
            body = body,
            reference = content.source.orEmpty(),
            chapterNo = null,
            verseNo = null,
            slugs = emptySet(),
            signature = content.signature(),
        )
    }

    /**
     * Identity of a published row *including its text*: the table is upserted on `date`, so an
     * admin correcting today's entry keeps the same id — only the text tells the two apart.
     */
    private fun DailyContent.signature(): String = listOf(
        content_type,
        date.orEmpty(),
        chapter_no?.toString().orEmpty(),
        verse_no?.toString().orEmpty(),
        hadith_id?.toString().orEmpty(),
        text_az.hashCode().toString(),
    ).joinToString("|")
}
