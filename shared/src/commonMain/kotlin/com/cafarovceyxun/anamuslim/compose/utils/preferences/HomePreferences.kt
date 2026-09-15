package com.cafarovceyxun.anamuslim.compose.utils.preferences

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Ana ekranda görünən bölmələr — sırası və görünüşü istifadəçinin ixtiyarındadır.
 *
 * [key] **saxlanılan** addır: enum sabitinin adını yazsaydıq, sabit sonradan adlandırılanda
 * istifadəçinin düzəni səssizcə sıfırlanardı. Yeni bölmə əlavə edəndə açar bir dəfə seçilir və
 * dəyişmir.
 */
enum class HomeSection(val key: String) {
    /**
     * Günün ayəsi/hədisi və əlavə funksiyaların hekayə zolağı. Enum-da **birinci**dir, ona görə
     * ana ekranın ən başında, namaz vaxtlarından üstdə görünür.
     *
     * Mövcud istifadəçilərdə də başa qaldırılır — bax [HomePreferences.migrateStoriesToTop]: bölmə
     * tətbiqə sonradan gəldiyi üçün köhnə düzənlərdə **sonda** qalmışdı və gündəlik məzmun ekranın
     * dibində gözdən qaçırdı.
     */
    STORIES("stories"),

    /** Namaz vaxtları — defolt sırada hekayə zolağının altındadır. */
    PRAYER("prayer"),

    /**
     * «Dua və zikr» + «Əsmaül Hüsnə» giriş kartları — defolt sırada **namaz vaxtlarının altında**.
     *
     * Namaz bölməsinin içində deyil, öz bölməsidir: orada olsaydı namaz vaxtlarını gizlədən
     * istifadəçi duanı da səssizcə itirərdi. Mövcud istifadəçilərdə yeri
     * [HomePreferences.migrateDuaAfterPrayer] ilə bir dəfə düzəlir.
     */
    DUA("dua"),
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

    /**
     * Hekayə zolağı bir dəfə başa qaldırılıbmı — bax [migrateStoriesToTop].
     *
     * Bayraq **eksport olunur** (`DEVICE_LOCAL_KEYS`-də deyil): düzənin özü ilə birlikdə səyahət
     * etməlidir, yoxsa ehtiyat nüsxə yeni telefona köçəndə miqrasiya təzədən işləyib istifadəçinin
     * sonradan seçdiyi yeri əzərdi.
     */
    private val KEY_STORIES_ON_TOP_MIGRATED = booleanPreferencesKey("home.layout.stories_on_top")

    /** Dua bölməsi bir dəfə namazın altına salınıbmı — bax [migrateDuaAfterPrayer]. */
    private val KEY_DUA_AFTER_PRAYER_MIGRATED =
        booleanPreferencesKey("home.layout.dua_after_prayer")

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

    /**
     * Hekayə zolağını **bir dəfə** düzənin başına gətirir. Açılışda, [DataStoreManager.warmUp]-dan
     * sonra çağırılır.
     *
     * Bölmə tətbiqə sonradan gəldi: o vaxt [parse] onu saxlanılan sətrin **sonuna** əlavə edirdi ki,
     * mövcud düzən pozulmasın, «Ana ekranı düzənlə»-ni bir dəfə açan istifadəçidə isə həmin yer
     * sətrə yazıldı. Nəticədə gündəlik məzmun — tətbiqin əsas vədi — ekranın dibində, sürüşdürmədən
     * görünməyən yerdə qaldı.
     *
     * Bayraq şərtdir: bölməni sürükləyib aşağı salan istifadəçinin seçimi **növbəti açılışda geri
     * qaytarılmamalıdır**, ona görə köçürmə ömürdə bir dəfə işləyir. Görünmə vəziyyətinə toxunmur —
     * gizlədilibsə gizli qalır, sadəcə sırası dəyişir.
     */
    suspend fun migrateStoriesToTop() {
        if (DataStoreManager.readFirst(KEY_STORIES_ON_TOP_MIGRATED, false)) return

        DataStoreManager.write(KEY_STORIES_ON_TOP_MIGRATED, true)

        // Heç vaxt düzənlənməyib: default sıra onsuz da hekayələrlə başlayır, sətir yazmağa dəyməz.
        val raw = DataStoreManager.readFirst(KEY_LAYOUT, "")
        if (raw.isBlank()) return

        val current = parse(raw)
        val index = current.indexOfFirst { it.section == HomeSection.STORIES }
        if (index <= 0) return

        val moved = current.toMutableList().apply { add(0, removeAt(index)) }
        setLayout(moved)
    }

    /**
     * Dua bölməsini **bir dəfə** namaz vaxtlarının altına salır. Açılışda, [migrateStoriesToTop] ilə
     * yanaşı çağırılır.
     *
     * Lazımdır, çünki [parse] yeni bölməni saxlanılan sətrin **sonuna** əlavə edir (mövcud düzən
     * pozulmasın deyə) — istifadəçi isə onu namazın altında istəyib. Bayraq şərtdir: sonradan
     * bölməni özü başqa yerə sürükləyən istifadəçinin seçimi növbəti açılışda geri qaytarılmamalıdır.
     * Görünmə vəziyyətinə toxunmur.
     */
    suspend fun migrateDuaAfterPrayer() {
        if (DataStoreManager.readFirst(KEY_DUA_AFTER_PRAYER_MIGRATED, false)) return

        DataStoreManager.write(KEY_DUA_AFTER_PRAYER_MIGRATED, true)

        // Heç vaxt düzənlənməyib: default sıra onsuz da doğrudur (enum-da DUA namazdan sonradır).
        val raw = DataStoreManager.readFirst(KEY_LAYOUT, "")
        if (raw.isBlank()) return

        val current = parse(raw).toMutableList()
        val duaIndex = current.indexOfFirst { it.section == HomeSection.DUA }
        val prayerIndex = current.indexOfFirst { it.section == HomeSection.PRAYER }
        if (duaIndex < 0 || prayerIndex < 0) return

        val target = if (duaIndex < prayerIndex) prayerIndex else prayerIndex + 1
        if (duaIndex == target) return

        current.add(target, current.removeAt(duaIndex))
        setLayout(current)
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
