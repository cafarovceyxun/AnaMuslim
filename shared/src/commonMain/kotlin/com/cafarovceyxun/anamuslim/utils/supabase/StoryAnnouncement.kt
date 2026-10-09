package com.cafarovceyxun.anamuslim.utils.supabase

import com.cafarovceyxun.anamuslim.utils.IsoDate
import kotlinx.serialization.Serializable

/**
 * `story_announcement` sətri — təklifə və ya qəməri aya bağlı **olmayan** admin hekayəsi
 * (ana ekranda «Elanlar» qrupu).
 *
 * [media] [SuggestionMedia] modelini bölüşür: bazada forma eynidir (`[{"url","type"}]`) və slaydı
 * çəkən kod da eynidir ([LunarAnnouncement] ilə eyni səbəb). [expires_at] `null`-dursa hekayə admin
 * silənə qədər qalır; vaxtı keçmiş sətri RLS adi istifadəçidən gizlədir, admin isə idarəetmə
 * siyahısında görür — ona görə klient də [isActive] ilə süzür.
 *
 * Sxem: `docs/supabase/SCHEMA.md`.
 */
@Serializable
data class StoryAnnouncement(
    val id: Long,
    val note: String? = null,
    val media: List<SuggestionMedia> = emptyList(),
    val expires_at: String? = null,
    val view_count: Int = 0,
    /** Hekayə bəyənmələri (`like_story()` RPC). Köhnə keşdə yoxdur — default məcburidir. */
    val like_count: Int = 0,
    val created_at: String? = null,
    val updated_at: String? = null,
) {
    val hasStory: Boolean get() = media.isNotEmpty() || !note.isNullOrBlank()

    fun isActive(nowMillis: Long): Boolean {
        val expiry = expires_at ?: return true
        val expiresMillis = IsoInstant.toEpochMillis(expiry) ?: return true
        return expiresMillis > nowMillis
    }
}

/** Adminin seçdiyi görünmə müddəti. `null` saat = admin silənə qədər. */
enum class StoryDuration(val hours: Int?) {
    DAY(24),
    THREE_DAYS(72),
    WEEK(168),
    FOREVER(null),
}

/**
 * `timestamptz` ↔ epoch millisaniyə — layihədə tarix kitabxanası yoxdur, gün hesabı [IsoDate]-dədir.
 * Format PostgREST-in qaytardığıdır: `2026-10-09T15:30:00(.ffffff)(Z|+00:00)`.
 */
object IsoInstant {

    fun fromEpochMillis(millis: Long): String {
        val epochDay = millis.floorDiv(MILLIS_PER_DAY)
        val ofDay = millis.mod(MILLIS_PER_DAY) / 1000
        val hours = ofDay / 3600
        val minutes = ofDay % 3600 / 60
        val seconds = ofDay % 60
        return "${IsoDate.fromEpochDay(epochDay)}T${two(hours)}:${two(minutes)}:${two(seconds)}Z"
    }

    fun toEpochMillis(iso: String): Long? {
        if (iso.length < 19 || iso[10] != 'T') return null
        val epochDay = IsoDate.toEpochDay(iso.substring(0, 10)) ?: return null
        val hours = iso.substring(11, 13).toIntOrNull() ?: return null
        val minutes = iso.substring(14, 16).toIntOrNull() ?: return null
        val seconds = iso.substring(17, 19).toIntOrNull() ?: return null

        // Kəsr hissəsini keç, sonra zona: «Z», «+HH:MM», «-HH» və ya heç nə (UTC sayılır).
        var index = 19
        if (index < iso.length && iso[index] == '.') {
            index++
            while (index < iso.length && iso[index].isDigit()) index++
        }
        val offsetMinutes = when {
            index >= iso.length || iso[index] == 'Z' -> 0
            iso[index] == '+' || iso[index] == '-' -> {
                val sign = if (iso[index] == '-') -1 else 1
                // «HH:MM», «HHMM» və ya «HH».
                val digits = iso.substring(index + 1).filter(Char::isDigit)
                val offsetHours = digits.take(2).toIntOrNull() ?: return null
                val offsetMins = digits.drop(2).take(2).toIntOrNull() ?: 0
                sign * (offsetHours * 60 + offsetMins)
            }
            else -> return null
        }

        return epochDay * MILLIS_PER_DAY +
            ((hours * 3600L + minutes * 60L + seconds) * 1000L) -
            offsetMinutes * 60_000L
    }

    private fun two(value: Long) = value.toString().padStart(2, '0')

    private const val MILLIS_PER_DAY = 86_400_000L
}
