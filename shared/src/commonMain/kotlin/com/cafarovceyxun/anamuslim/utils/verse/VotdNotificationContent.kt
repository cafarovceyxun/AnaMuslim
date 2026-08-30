package com.cafarovceyxun.anamuslim.utils.verse

import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.VersePreferences
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.repository.supabase.DailyContentRepository
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strLabelVerseSerialWithChapter
import com.cafarovceyxun.anamuslim.resources.strTitleDailyHadith
import com.cafarovceyxun.anamuslim.resources.strTitleVOTD
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.currentLocalDateIsoString
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
 *
 * [slotKey] identifies the queue position this notification belongs to; it is what keeps a slot
 * from ringing twice, and [atMillis] is when it is due in the device's local time.
 */
data class VotdNotification(
    val title: String,
    val body: String,
    val reference: String,
    val chapterNo: Int?,
    val verseNo: Int?,
    val verseEnd: Int?,
    val slugs: Set<String>,
    val slotKey: String,
    val date: String,
    val slotIndex: Int,
    val atMillis: Long,
)

/**
 * Builds the reminders from the queue an admin published to Supabase (`daily_content_item`) — the
 * same rows the home card and the story render. Nothing is drawn locally any more: a random verse
 * the server never chose would contradict the card, and there would be no way to tell "already
 * shown" from "new".
 *
 * Gündə [com.cafarovceyxun.anamuslim.utils.supabase.DailyContentSlots.COUNT] yuva var
 * ([DailyContentSchedule]); növbə uzundursa qalan elementlər öz-özünə növbəti günlərə keçir.
 * [upcoming] gələcək yuvaları əvvəlcədən qurmaq üçündür (iOS onları
 * `UNCalendarNotificationTrigger` kimi əvvəlcədən yazır, Android gecikməli iş kimi), [due] isə
 * vaxtı çatmış, amma hələ çalınmamış yuvaları qaytarır — proses o an oyanmayıbsa itməsin.
 */
object VotdNotificationContent {

    private val repository = DailyContentRepository()

    /**
     * Vaxtı gələcəkdə olan və hələ çalınmamış yuvalar, sıra ilə.
     *
     * [refresh] false olanda yalnız keş oxunur — bildirişin çalındığı anda şəbəkə gözləmək
     * lazım deyil.
     */
    suspend fun upcoming(
        limit: Int = DEFAULT_SCHEDULE_LIMIT,
        nowMillis: Long = currentEpochMillis(),
        refresh: Boolean = true,
    ): List<VotdNotification> {
        if (!VersePreferences.getVOTDReminderEnabled()) return emptyList()

        return scheduledItems(refresh)
            .filter { (_, at) -> at > nowMillis }
            .take(limit)
            .mapNotNull { (content, at) -> buildFor(content, at) }
    }

    /**
     * Vaxtı keçmiş, amma hələ bildirilməmiş yuvalar — [graceMillis] pəncərəsi daxilində.
     *
     * Pəncərə var, çünki geriyə doğru sonsuz yoxlama günlərlə susmuş cihazı açan kimi bir dəstə
     * köhnə bildirişlə doldurardı.
     */
    suspend fun due(
        nowMillis: Long = currentEpochMillis(),
        graceMillis: Long = DEFAULT_GRACE_MILLIS,
        refresh: Boolean = true,
    ): List<VotdNotification> {
        if (!VersePreferences.getVOTDReminderEnabled()) return emptyList()

        return scheduledItems(refresh)
            .filter { (_, at) -> at in (nowMillis - graceMillis)..nowMillis }
            .mapNotNull { (content, at) -> buildFor(content, at) }
    }

    /** Konkret yuvanın bildirişi — Android-də gecikməli iş öz yuvasını bununla oxuyur. */
    suspend fun forSlot(date: String, slotIndex: Int, refresh: Boolean = false): VotdNotification? {
        if (!VersePreferences.getVOTDReminderEnabled()) return null
        if (VersePreferences.isSlotDelivered("$date#$slotIndex")) return null

        val content = queue(refresh)
            .firstOrNull { it.date == date && it.slot_index == slotIndex }
            ?: return null

        val at = DailyContentSchedule.epochMillisOf(date, slotIndex) ?: return null

        return buildFor(content, at)
    }

    /** Records [notification] as delivered, so the same slot never rings twice. */
    suspend fun markDelivered(notification: VotdNotification) {
        VersePreferences.markSlotDelivered(notification.slotKey, currentLocalDateIsoString())
    }

    /**
     * Növbə elementindən bildiriş qurur; mətn oxunmursa (nə tərcümə, nə də adminin yazdığı mətn)
     * null qaytarır.
     */
    suspend fun buildFor(content: DailyContent, atMillis: Long): VotdNotification? {
        val date = content.date ?: return null

        return if (content.isHadith) {
            buildHadith(content, date, atMillis)
        } else {
            buildVerse(content, date, atMillis)
        }
    }

    /** Növbə + hər elementin yerli anı, çalınmışlar çıxılmaqla. */
    private suspend fun scheduledItems(refresh: Boolean): List<Pair<DailyContent, Long>> =
        queue(refresh)
            .filterNot { VersePreferences.isSlotDelivered(it.slotKey) }
            .mapNotNull { content ->
                val date = content.date ?: return@mapNotNull null
                val at = DailyContentSchedule.epochMillisOf(date, content.slot_index)
                    ?: return@mapNotNull null

                content to at
            }

    private suspend fun queue(refresh: Boolean): List<DailyContent> =
        if (refresh) repository.fetchUpcoming() else repository.cachedUpcoming()

    private suspend fun buildVerse(
        content: DailyContent,
        date: String,
        atMillis: Long,
    ): VotdNotification? {
        val chapterNo = content.chapter_no ?: return null
        val verseNo = content.verse_no ?: return null

        var slugs = ReaderPreferences.getTranslations()
        if (slugs.isEmpty()) {
            slugs = TranslUtils.defaultTranslationSlugs()
        }

        val verseNumbers = content.verseNumbers

        val factory = QuranTranslationFactory()
        val translated = try {
            verseNumbers.mapNotNull { number ->
                factory.getTranslationsSingleVerse(slugs, chapterNo, number)
                    .firstOrNull()
                    ?.let { StringUtils.removeHTML(it.text, false) }
            }
        } finally {
            factory.close()
        }

        // The downloaded translation is preferred — it is what the reader will show on tap — but a
        // user with no translation installed still gets the text the admin published.
        val body = translated
            .takeIf { it.size == verseNumbers.size && it.isNotEmpty() }
            ?.joinToString(" ")
            ?: content.displayTextAz.takeIf { it.isNotBlank() }
            ?: return null

        val verse = RepositoryProvider.quranRepository.getVerseWithDetails(
            chapterNo,
            verseNo,
            arabicEnabled = false,
        )

        val base = verse?.let {
            getString(
                Res.string.strLabelVerseSerialWithChapter,
                it.chapter.getCurrentName(),
                chapterNo,
                verseNo,
            )
        } ?: "$chapterNo:$verseNo"

        // Çoxayəli element: istinad aralığın sonunu da göstərsin.
        val lastVerse = content.verse_end
        val reference = if (lastVerse != null && lastVerse > verseNo) "$base-$lastVerse" else base

        return VotdNotification(
            title = getString(Res.string.strTitleVOTD),
            body = body,
            reference = reference,
            chapterNo = chapterNo,
            verseNo = verseNo,
            verseEnd = lastVerse,
            slugs = slugs,
            slotKey = content.slotKey,
            date = date,
            slotIndex = content.slot_index,
            atMillis = atMillis,
        )
    }

    private suspend fun buildHadith(
        content: DailyContent,
        date: String,
        atMillis: Long,
    ): VotdNotification? {
        // Admin hədisin yalnız bir hissəsini seçibsə bildirişə də həmin hissə düşür.
        val body = content.displayTextAz.takeIf { it.isNotBlank() }
            ?: content.displayTextAr.takeIf { it.isNotBlank() }
            ?: return null

        return VotdNotification(
            title = getString(Res.string.strTitleDailyHadith),
            body = body,
            reference = content.source.orEmpty(),
            chapterNo = null,
            verseNo = null,
            verseEnd = null,
            slugs = emptySet(),
            slotKey = content.slotKey,
            date = date,
            slotIndex = content.slot_index,
            atMillis = atMillis,
        )
    }

    /**
     * iOS-un gözləyən bildiriş limiti 64-dür və tətbiqin başqa bildirişləri də var; 40 element
     * səkkiz günlük növbə deməkdir, bu da fon yenilənməsinin ritmindən qat-qat uzundur.
     */
    private const val DEFAULT_SCHEDULE_LIMIT = 40

    /** Altı saat: cihaz yatıb qalsa da günün yuvası itmir, dünənkilər isə qayıtmır. */
    private const val DEFAULT_GRACE_MILLIS = 6L * 60L * 60L * 1000L
}
