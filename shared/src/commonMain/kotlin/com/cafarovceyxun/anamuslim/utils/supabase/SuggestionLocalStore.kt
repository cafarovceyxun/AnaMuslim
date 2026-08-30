package com.cafarovceyxun.anamuslim.utils.supabase

import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cafarovceyxun.anamuslim.compose.utils.preferences.DataStoreManager
import com.cafarovceyxun.anamuslim.compose.utils.preferences.PrefKey
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Təkliflərin **cihazda qalan** hissəsi: göndəriş qəbzləri və hansı təkliflərə səs verildiyi.
 *
 * Bunlar qəsdən serverdə deyil. Baza tərəfində nə cihaz id-si, nə hesab bağlantısı, nə də səs
 * cədvəli var — yəni «kim nə göndərib, kim nəyə səs verib» sualının cavabı heç yerdə toplanmır.
 * Bunun qiyməti: tətbiq silinəndə bu siyahılar da gedir (göndərilmiş təkliflər bazada qalır, sadəcə
 * cihaz onları artıq «mənim təklifim» kimi tanımır) və eyni təklifə başqa cihazdan yenidən səs
 * vermək mümkündür. Qeydiyyatsız axının qəbul edilmiş güzəştidir.
 */
object SuggestionLocalStore {

    /** Göndəriş qəbzləri (`uuid`), vergüllə. RPC bir sorğuda 100 qəbz oxuyur, siyahı da o qədər. */
    private val KEY_TICKETS = PrefKey(stringPreferencesKey("suggestion_tickets"), "")

    /** Səs verilmiş `suggestions.id` dəyərləri, vergüllə. */
    private val KEY_VOTED = PrefKey(stringPreferencesKey("suggestion_voted_ids"), "")

    /** Baxılmış «yenilik» hekayələri (`suggestions.id`) — halqa yalnız baxılmayanda görünür. */
    private val KEY_SEEN_FEATURES = PrefKey(stringPreferencesKey("suggestion_seen_features"), "")

    /** Son göndərişin vaxtı — lokal soyuma müddəti üçün. */
    private val KEY_LAST_SUBMIT = PrefKey(longPreferencesKey("suggestion_last_submit_at"), 0L)

    /** İki göndəriş arasındakı ən az fasilə. Serverdə şəxs başına limit qoymaq mümkün deyil. */
    const val SUBMIT_COOLDOWN_MILLIS = 30_000L

    private const val MAX_TICKETS = 100

    private val mutex = Mutex()

    suspend fun tickets(): List<String> = DataStoreManager.readFirst(KEY_TICKETS).toList()

    suspend fun addTicket(ticket: String) = mutex.withLock {
        val current = DataStoreManager.readFirst(KEY_TICKETS).toList()
        if (ticket in current) return@withLock

        DataStoreManager.write(KEY_TICKETS, (listOf(ticket) + current).take(MAX_TICKETS).joinToString(","))
    }

    /** Artıq bazada olmayan (admin tərəfindən silinmiş) qəbzləri siyahıdan atır. */
    suspend fun retainTickets(alive: Set<String>) = mutex.withLock {
        val current = DataStoreManager.readFirst(KEY_TICKETS).toList()
        val kept = current.filter { it in alive }
        if (kept.size != current.size) {
            DataStoreManager.write(KEY_TICKETS, kept.joinToString(","))
        }
    }

    suspend fun votedIds(): Set<Long> =
        DataStoreManager.readFirst(KEY_VOTED).toList().mapNotNull(String::toLongOrNull).toSet()

    suspend fun setVoted(id: Long, voted: Boolean) = mutex.withLock {
        val current = DataStoreManager.readFirst(KEY_VOTED).toList().mapNotNull(String::toLongOrNull)
        val updated = if (voted) (current + id).distinct() else current - id
        DataStoreManager.write(KEY_VOTED, updated.joinToString(","))
    }

    suspend fun seenFeatureIds(): Set<Long> =
        DataStoreManager.readFirst(KEY_SEEN_FEATURES).toList().mapNotNull(String::toLongOrNull).toSet()

    suspend fun markFeatureSeen(id: Long) = mutex.withLock {
        val current = DataStoreManager.readFirst(KEY_SEEN_FEATURES).toList().mapNotNull(String::toLongOrNull)
        if (id in current) return@withLock

        // Siyahı yalnız «halqa görünsünmü» sualına cavab verir, ona görə son 200 ilə kifayətdir.
        DataStoreManager.write(KEY_SEEN_FEATURES, (listOf(id) + current).take(200).joinToString(","))
    }

    /** Soyuma müddəti bitibsə `null`, bitməyibsə qalan millisaniyə. */
    suspend fun submitCooldownRemaining(): Long {
        val last = DataStoreManager.readFirst(KEY_LAST_SUBMIT)
        if (last <= 0L) return 0L

        val elapsed = currentEpochMillis() - last
        return if (elapsed >= 0 && elapsed < SUBMIT_COOLDOWN_MILLIS) SUBMIT_COOLDOWN_MILLIS - elapsed else 0L
    }

    suspend fun markSubmitted() {
        DataStoreManager.write(KEY_LAST_SUBMIT, currentEpochMillis())
    }

    private fun String.toList(): List<String> =
        split(',').map(String::trim).filter(String::isNotEmpty)
}
