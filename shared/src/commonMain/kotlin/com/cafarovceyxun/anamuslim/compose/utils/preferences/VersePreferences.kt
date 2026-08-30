@file:OptIn(ExperimentalCoroutinesApi::class)

package com.cafarovceyxun.anamuslim.compose.utils.preferences

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

object VersePreferences {
    val KEY_VOTD_DATE = PrefKey(longPreferencesKey("votd_timestamp"), -1)
    val KEY_VOTD_CHAPTER_NO = PrefKey(intPreferencesKey("votd_chapter_no"), -1)
    val KEY_VOTD_VERSE_NO = PrefKey(intPreferencesKey("votd_verse_no"), -1)
    val KEY_VOTD_REMINDER_ENABLED = PrefKey(booleanPreferencesKey("votd_reminder_enabled"), false)

    /**
     * Ana səhifədəki «Günün Ayəsi» **hekayəsi** göstərilsinmi. Bildirişdən (yuxarıdakı açar) asılı
     * deyil — hekayəni bağlayan istifadəçi gündəlik bildirişi saxlaya bilər, əksi də doğrudur.
     *
     * ℹ️ Açar adı `votd_card_enabled` qalıb: əvvəl eyni ayar ana səhifədəki böyük **kartı** idarə
     * edirdi. Kart 2026-08-30-da götürüldü (məzmun hekayəyə keçdi), ayarın mənası isə eyni qaldı —
     * «gündəlik məzmun ana səhifədə görünsün». Açarı dəyişmək istifadəçinin seçimini sıfırlayardı.
     */
    val KEY_VOTD_CARD_ENABLED = PrefKey(booleanPreferencesKey("votd_card_enabled"), true)

    /**
     * Artıq bildirilmiş yuvalar — `"tarix#slot"` açarları vergüllə.
     *
     * Gündə beş bildiriş var, hər biri öz yuvasına bağlıdır; bu siyahı isə yuvanın **iki dəfə**
     * çalınmasının qarşısını alır (WorkManager təkrar cəhd edəndə, iOS eyni günü yenidən
     * planlaşdıranda). Yazarkən keçmiş günlərin açarları atılır, ona görə siyahı böyümür.
     */
    private val KEY_DAILY_CONTENT_DELIVERED =
        PrefKey(stringPreferencesKey("daily_content_delivered"), "")

    /**
     * Supabase-dən son uğurla alınmış növbə (`daily_content_item` sətirləri), JSON siyahı kimi.
     *
     * Sorğu uğursuz olanda — praktikada internetin olmaması — kart, story və bildiriş cədvəli
     * bununla qurulur. Saxlanan sətirlərin öz `date` sahəsi yoxlanılır, ona görə keçmiş məzmun
     * bugünkü kimi göstərilmir.
     *
     * ⚠️ Açar qəsdən yenidir: köhnə `daily_content_cache` **tək obyekt** saxlayırdı, siyahı kimi
     * oxunmazdı.
     */
    private val KEY_DAILY_CONTENT_ITEMS_CACHE =
        PrefKey(stringPreferencesKey("daily_content_items_cache"), "")

    /**
     * Baxışı artıq sayılmış elementlərin id-ləri, vergüllə.
     *
     * Baxış sayğacı serverdə saxlanılır, **kimin baxdığı isə saxlanmır** (`suggestions.vote_count`
     * ilə eyni yanaşma) — «bu cihaz artıq saydı?» sualının yeganə cavabı budur. Siyahı böyüməsin
     * deyə yalnız son [VIEWED_IDS_LIMIT] id saxlanılır: növbə irəli getdikcə köhnə elementə
     * qayıdış praktikada olmur.
     */
    /**
     * Hekayəsi **baxılmış** elementlərin id-ləri — ana səhifədəki dairənin halqası buna görə sönür.
     *
     * [KEY_DAILY_CONTENT_VIEWED]-dən ayrıdır və qəsdən: o, serverdəki baxış sayğacının dedupe-udur
     * və yalnız sorğu uğurlu olanda yazılır. Halqa isə şəbəkədən asılı olmamalıdır — oflayn baxılan
     * hekayə də baxılmış sayılır.
     */
    private val KEY_DAILY_CONTENT_STORY_SEEN =
        PrefKey(stringPreferencesKey("daily_content_story_seen"), "")

    private val KEY_DAILY_CONTENT_VIEWED =
        PrefKey(stringPreferencesKey("daily_content_viewed"), "")

    private val KEY_RECOMMENDED_NOTIF_EPOCH_DAY =
        PrefKey(longPreferencesKey("recommended_notif_epoch_day"), -1L)
    private val KEY_RECOMMENDED_NOTIF_SIGNATURE =
        PrefKey(stringPreferencesKey("recommended_notif_signature"), "")

    fun getVotd(): ChapterVersePair? {
        val chapterNo = DataStoreManager.read(KEY_VOTD_CHAPTER_NO)
        val verseNo = DataStoreManager.read(KEY_VOTD_VERSE_NO)

        return if (chapterNo != -1 && verseNo != -1) ChapterVersePair(chapterNo, verseNo) else null
    }

    fun getVotdTimestamp(): Long {
        return DataStoreManager.read(KEY_VOTD_DATE)
    }

    suspend fun saveVotd(
        chapterNo: Int,
        verseNo: Int,
        timestamp: Long
    ) {
        DataStoreManager.edit {
            this[KEY_VOTD_CHAPTER_NO.key] = chapterNo
            this[KEY_VOTD_VERSE_NO.key] = verseNo
            this[KEY_VOTD_DATE.key] = timestamp
        }
    }

    fun votdStorageFlow(): Flow<Triple<Long, Int, Int>> {
        return DataStoreManager.flowMultiple(
            KEY_VOTD_DATE,
            KEY_VOTD_CHAPTER_NO,
            KEY_VOTD_VERSE_NO,
        ).map { result ->
            Triple(
                result.get(KEY_VOTD_DATE),
                result.get(KEY_VOTD_CHAPTER_NO),
                result.get(KEY_VOTD_VERSE_NO),
            )
        }.distinctUntilChanged()
    }

    suspend fun removeVotd() {
        DataStoreManager.removeAll(
            KEY_VOTD_CHAPTER_NO.key,
            KEY_VOTD_VERSE_NO.key,
            KEY_VOTD_DATE.key,
        )
    }

    suspend fun setVOTDReminderEnabled(enabled: Boolean) {
        DataStoreManager.write(KEY_VOTD_REMINDER_ENABLED, enabled)
    }

    fun getVOTDReminderEnabled(): Boolean {
        return DataStoreManager.read(KEY_VOTD_REMINDER_ENABLED)
    }

    @Composable
    fun observeVOTDReminderEnabled(): Boolean {
        return DataStoreManager.observe(KEY_VOTD_REMINDER_ENABLED)
    }

    suspend fun setVOTDCardEnabled(enabled: Boolean) {
        DataStoreManager.write(KEY_VOTD_CARD_ENABLED, enabled)
    }

    @Composable
    fun observeVOTDCardEnabled(): Boolean {
        return DataStoreManager.observe(KEY_VOTD_CARD_ENABLED)
    }

    fun getDailyContentItemsCache(): String {
        return DataStoreManager.read(KEY_DAILY_CONTENT_ITEMS_CACHE)
    }

    suspend fun setDailyContentItemsCache(json: String) {
        DataStoreManager.write(KEY_DAILY_CONTENT_ITEMS_CACHE, json)
    }

    /** Bu yuva artıq bildirilibmi. */
    fun isSlotDelivered(slotKey: String): Boolean {
        return slotKey in deliveredSlotKeys()
    }

    /**
     * Yuvanı bildirilmiş kimi qeyd edir və [today]-dan əvvəlki açarları atır.
     * Bildiriş **göndərildikdən sonra** çağırılmalıdır: uğursuz göndərişi növbəti yoxlama təkrarlasın.
     */
    suspend fun markSlotDelivered(slotKey: String, today: String) {
        val kept = (deliveredSlotKeys() + slotKey)
            .filter { it.substringBefore('#') >= today }
            .distinct()

        DataStoreManager.write(KEY_DAILY_CONTENT_DELIVERED, kept.joinToString(","))
    }

    /** Hekayəsi bu cihazda baxılmış elementlərin id-ləri. */
    fun seenStoryIds(): Set<Long> =
        DataStoreManager.read(KEY_DAILY_CONTENT_STORY_SEEN)
            .split(',')
            .mapNotNullTo(HashSet()) { it.trim().toLongOrNull() }

    /** Elementi baxılmış kimi qeyd edir; siyahı [VIEWED_IDS_LIMIT] ilə məhdudlaşır. */
    suspend fun markStorySeen(itemId: Long) {
        val kept = (DataStoreManager.read(KEY_DAILY_CONTENT_STORY_SEEN)
            .split(',')
            .filter { it.isNotBlank() } + itemId.toString())
            .distinct()
            .takeLast(VIEWED_IDS_LIMIT)

        DataStoreManager.write(KEY_DAILY_CONTENT_STORY_SEEN, kept.joinToString(","))
    }

    /** Bu elementin baxışı bu cihazdan artıq sayılıbmı. */
    fun isViewCounted(itemId: Long): Boolean = itemId.toString() in viewedIds()

    /** Elementi sayılmış kimi qeyd edir; siyahı [VIEWED_IDS_LIMIT] ilə məhdudlaşır. */
    suspend fun markViewCounted(itemId: Long) {
        val kept = (viewedIds() + itemId.toString())
            .distinct()
            .takeLast(VIEWED_IDS_LIMIT)

        DataStoreManager.write(KEY_DAILY_CONTENT_VIEWED, kept.joinToString(","))
    }

    private fun viewedIds(): List<String> {
        return DataStoreManager.read(KEY_DAILY_CONTENT_VIEWED)
            .split(',')
            .filter { it.isNotBlank() }
    }

    private const val VIEWED_IDS_LIMIT = 200

    private fun deliveredSlotKeys(): List<String> {
        return DataStoreManager.read(KEY_DAILY_CONTENT_DELIVERED)
            .split(',')
            .filter { it.isNotBlank() }
    }

    fun getRecommendedNotifDedupeEpochDay(): Long {
        return DataStoreManager.read(KEY_RECOMMENDED_NOTIF_EPOCH_DAY)
    }

    fun getRecommendedNotifDedupeSignature(): String {
        return DataStoreManager.read(KEY_RECOMMENDED_NOTIF_SIGNATURE)
    }

    suspend fun setRecommendedNotifDedupeState(epochDay: Long, signature: String) {
        DataStoreManager.write(KEY_RECOMMENDED_NOTIF_EPOCH_DAY, epochDay)
        DataStoreManager.write(KEY_RECOMMENDED_NOTIF_SIGNATURE, signature)
    }
}
