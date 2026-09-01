package com.cafarovceyxun.anamuslim.utils.univ

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * Ayarların ehtiyat nüsxəsi: **bütün** DataStore açarları adı və tipi ilə.
 *
 * Əvvəlki format ([ExportKeys]-dəki əl ilə saxlanan siyahı) yalnız ~18 açar daşıyırdı — mövzu rəngi,
 * hədis ayarları, ana ekran düzəni, sevimli surələr, axtarış tarixçəsi, təcvid, WBW, səhifə
 * animasiyası və sonradan əlavə olunan hər şey faylda yox idi, yəni «hər şeyi köçürdüm» deyən
 * istifadəçi yeni telefonda ayarların yarısını yenidən quraşdırırdı. Siyahını əl ilə uzatmaq həmin
 * tələni saxlayır: yeni ayar əlavə edən adam bu faylı yeniləməyi unudur və heç nə xəbərdarlıq
 * etmir. Ona görə burada **tərs** qayda var — hər şey daşınır, yalnız cihaza bağlı açarlar
 * ([DEVICE_LOCAL_KEYS]) kənarda qalır.
 *
 * Tip JSON-da itir (DataStore `Int`/`Long`/`Float`/`Double`-u ayırır, JSON isə ayırmır), ona görə
 * hər sətir `{"k": ad, "t": tip, "v": dəyər}` şəklindədir: import tərəfi açarı **eyni tiplə**
 * yenidən qurmalıdır, yoxsa `Preferences` oxunanda `ClassCastException` verir.
 */
object PreferenceBackup {

    private const val FIELD_KEY = "k"
    private const val FIELD_TYPE = "t"
    private const val FIELD_VALUE = "v"

    private const val TYPE_BOOL = "bool"
    private const val TYPE_INT = "int"
    private const val TYPE_LONG = "long"
    private const val TYPE_FLOAT = "float"
    private const val TYPE_DOUBLE = "double"
    private const val TYPE_STRING = "string"
    private const val TYPE_STRING_SET = "stringSet"

    /**
     * Cihaza bağlı açarlar — fayla düşmür və faylda gəlsə də tətbiq olunmur.
     *
     * Üç qrup var: (1) yüklənmiş resursların vəziyyəti — yeni telefonda həmin fayllar yoxdur, versiya
     * nömrəsini köçürmək yeniləməni «artıq var» sanaraq bloklayır; (2) gün/sessiya keşləri — günün
     * ayəsi növbəsi, bildiriş dublikat qoruması, pleyerin son mövqeyi; (3) miqrasiya və onboardinq
     * bayraqları — yeni quraşdırma öz miqrasiyasını özü işlətməlidir.
     */
    internal val DEVICE_LOCAL_KEYS = setOf(
        // onboardinq və yeniləmə
        "onboarding_completed_version",
        "app_update_info_json",
        // resurs vəziyyəti
        "last_resource_update_check_date",
        "current_resource_version",
        "reader.wbw.content_epoch",
        // miqrasiya bayraqları
        "reader_scroll_step_migrated",
        "reader.prefs.legacy_migrated_v1",
        // səsləndirmə sessiyası
        "recitation_has_session",
        "recitation_last_played_chapter",
        "recitation_last_played_verse",
        // günün məzmunu / bildiriş keşləri
        "votd_timestamp",
        "votd_chapter_no",
        "votd_verse_no",
        "daily_content_delivered",
        "daily_content_items_cache",
        "daily_content_story_seen",
        "daily_content_viewed",
        "recommended_notif_epoch_day",
        "recommended_notif_signature",
        // namaz vaxtlarının YER qrupu — bütöv saxlanılır
        // (yarısını köçürmək səssiz səhv verir: Bakıda alınmış nüsxə Berlində yanlış cədvəl qurar).
        // Hesablama ayarları — bucaqlar, ofsetlər, bildiriş seçimi — qəsdən portativdir.
        "prayer.lat",
        "prayer.lng",
        "prayer.elevation",
        "prayer.location_set",
        "prayer.place_name",
        "prayer.location_mode",
        "prayer.location_at",
        "prayer.saved_places",
        "prayer.delivered",
        // rəy sorğusunun ritmi bu cihazın hekayəsidir
        "review_prompt_first_seen_at",
        "review_prompt_launch_count",
        "review_prompt_last_asked_at",
        "review_prompt_ask_count",
        "review_prompt_outcome",
    )

    /** `wbw_resource_version_<id>` kimi dinamik yaranan cihaz açarları. */
    private val DEVICE_LOCAL_PREFIXES = listOf("wbw_resource_version_")

    /** Açar başqa cihaza köçürülə bilərmi. */
    fun isPortable(name: String): Boolean =
        name !in DEVICE_LOCAL_KEYS && DEVICE_LOCAL_PREFIXES.none { name.startsWith(it) }

    /**
     * [preferences] (ad → dəyər) fayl sətirlərinə çevirir. Açarlar ada görə sıralanır ki, ardıcıl
     * iki eksport eyni faylı versin — istifadəçi fərqi görmək istəyəndə faylları müqayisə edə bilsin.
     */
    fun encode(preferences: Map<String, Any>): JsonArray = buildJsonArray {
        preferences.entries
            .sortedBy { it.key }
            .forEach { (name, value) ->
                if (!isPortable(name)) return@forEach
                encodeEntry(name, value)?.let { add(it) }
            }
    }

    /**
     * Fayl sətirlərini yazıla bilən DataStore açarlarına çevirir. Tanınmayan tip, pozulmuş sətir və
     * cihaza bağlı açar **səssizcə atılır**: fayl istifadəçinin yaddaşından gəlir və hər şey ola
     * bilər, bir sətrə görə bütün importu dayandırmaq isə ən pis nəticədir.
     */
    fun decode(entries: JsonArray): Map<Preferences.Key<*>, Any> {
        val decoded = LinkedHashMap<Preferences.Key<*>, Any>()

        entries.forEach { element ->
            val obj = element as? JsonObject ?: return@forEach
            val name = obj[FIELD_KEY]?.jsonPrimitive?.contentOrNull ?: return@forEach
            if (!isPortable(name)) return@forEach

            val type = obj[FIELD_TYPE]?.jsonPrimitive?.contentOrNull ?: return@forEach
            val raw = obj[FIELD_VALUE] ?: return@forEach

            // `Pair(...)`, `to` deyil: DataStore `Preferences.Key<T>` üzərində öz `to`
            // infiksini elan edir və `key to value` `Preferences.Pair` qaytarır.
            val pair: Pair<Preferences.Key<*>, Any> = when (type) {
                TYPE_BOOL -> Pair(
                    booleanPreferencesKey(name),
                    (raw as? JsonPrimitive)?.booleanOrNull ?: return@forEach,
                )

                TYPE_INT -> Pair(
                    intPreferencesKey(name),
                    (raw as? JsonPrimitive)?.intOrNull ?: return@forEach,
                )

                TYPE_LONG -> Pair(
                    longPreferencesKey(name),
                    (raw as? JsonPrimitive)?.longOrNull ?: return@forEach,
                )

                TYPE_FLOAT -> Pair(
                    floatPreferencesKey(name),
                    (raw as? JsonPrimitive)?.floatOrNull ?: return@forEach,
                )

                TYPE_DOUBLE -> Pair(
                    doublePreferencesKey(name),
                    (raw as? JsonPrimitive)?.doubleOrNull ?: return@forEach,
                )

                // Mətn açarı üçün `contentOrNull` kifayət deyil: JSON `null`-u da mətn kimi verir.
                TYPE_STRING -> Pair(
                    stringPreferencesKey(name),
                    (raw as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return@forEach,
                )

                // Boş dəst də etibarlı dəyərdir — istifadəçi bütün tərcümələri söndürə bilər.
                TYPE_STRING_SET -> Pair(
                    stringSetPreferencesKey(name),
                    runCatching {
                        raw.jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull }.toSet()
                    }.getOrNull() ?: return@forEach,
                )

                else -> return@forEach
            }

            decoded[pair.first] = pair.second
        }

        return decoded
    }

    private fun encodeEntry(name: String, value: Any): JsonObject? {
        val (type, primitive) = when (value) {
            is Boolean -> TYPE_BOOL to JsonPrimitive(value)
            is Int -> TYPE_INT to JsonPrimitive(value)
            is Long -> TYPE_LONG to JsonPrimitive(value)
            is Float -> TYPE_FLOAT to JsonPrimitive(value)
            is Double -> TYPE_DOUBLE to JsonPrimitive(value)
            is String -> TYPE_STRING to JsonPrimitive(value)
            is Set<*> -> return buildJsonObject {
                put(FIELD_KEY, name)
                put(FIELD_TYPE, TYPE_STRING_SET)
                put(
                    FIELD_VALUE,
                    buildJsonArray { value.filterIsInstance<String>().forEach { add(it) } },
                )
            }
            // DataStore başqa tip saxlamır; gələcəkdə saxlasa, açar sadəcə fayla düşmür.
            else -> return null
        }

        return buildJsonObject {
            put(FIELD_KEY, name)
            put(FIELD_TYPE, type)
            put(FIELD_VALUE, primitive)
        }
    }
}
