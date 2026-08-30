package com.cafarovceyxun.anamuslim.compose.utils.preferences

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Ana ekranda görünən bölmələr — sırası və görünüşü istifadəçinin ixtiyarındadır.
 *
 * [key] **saxlanılan** addır: enum sabitinin adını yazsaydıq, sabit sonradan adlandırılanda
 * istifadəçinin düzəni səssizcə sıfırlanardı. Yeni bölmə əlavə edəndə açar bir dəfə seçilir və
 * dəyişmir.
 */
enum class HomeSection(val key: String) {
    /** Günün ayəsi/hədisi və əlavə funksiyaların hekayə zolağı. */
    STORIES("stories"),
    READ_HISTORY("read_history"),
    HADITH_READ_HISTORY("hadith_read_history"),
    BOOKMARKS("bookmarks"),
    SUGGESTIONS("suggestions");

    companion object {
        fun fromKey(key: String): HomeSection? = entries.firstOrNull { it.key == key }
    }
}

/** Bir bölmənin ana ekrandakı vəziyyəti — sıradakı yeri sətrin özündən gəlir. */
data class HomeSectionState(
    val section: HomeSection,
    val visible: Boolean,
)

object HomePreferences {
    /**
     * Düzən **bir sətirdə** saxlanılır: vergüllə ayrılmış açarlar, gizlədilənin qarşısında `!`.
     *
     * Hər bölmə üçün ayrıca açar saxlamırıq, çünki sıra onsuz da ayrıca yazılmalı olardı və iki
     * mənbə bir-birindən sürüşür. Sətirdə olmayan bölmə **sona, görünən** halda əlavə olunur —
     * yəni tətbiqə yeni bölmə gələndə köhnə düzən qorunur və yenilik gözdən qaçmır.
     */
    private val KEY_LAYOUT = stringPreferencesKey("home.layout")

    val DEFAULT_ORDER: List<HomeSection> = HomeSection.entries.toList()

    @Composable
    fun observeLayout(): List<HomeSectionState> = parse(DataStoreManager.observe(KEY_LAYOUT, ""))

    suspend fun getLayout(): List<HomeSectionState> =
        parse(DataStoreManager.readFirst(KEY_LAYOUT, ""))

    suspend fun setLayout(states: List<HomeSectionState>) {
        DataStoreManager.write(KEY_LAYOUT, serialize(states))
    }

    /** Ayarlardakı «bərpa et» — saxlanılan sətri silmək default düzənə qaytarır. */
    suspend fun resetLayout() {
        DataStoreManager.remove(KEY_LAYOUT)
    }

    internal fun serialize(states: List<HomeSectionState>): String =
        states.joinToString(",") { (if (it.visible) "" else "!") + it.section.key }

    /**
     * Saxlanılan sətri oxuyur; tanınmayan açarları atır, çatışmayanları sona görünən halda əlavə
     * edir. Boş sətir (heç vaxt düzənlənməyib) default sıranı verir.
     */
    internal fun parse(raw: String): List<HomeSectionState> {
        val stored = raw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { token ->
                val hidden = token.startsWith("!")
                val section = HomeSection.fromKey(if (hidden) token.drop(1) else token)
                section?.let { HomeSectionState(it, visible = !hidden) }
            }
            .distinctBy { it.section }

        val missing = DEFAULT_ORDER
            .filter { section -> stored.none { it.section == section } }
            .map { HomeSectionState(it, visible = true) }

        return stored + missing
    }
}
