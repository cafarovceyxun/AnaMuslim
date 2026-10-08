package com.cafarovceyxun.anamuslim.compose.utils.preferences

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.intPreferencesKey
import com.cafarovceyxun.anamuslim.compose.utils.HomeWidgetKind

/**
 * Ana ekran vidcetlərinin görünüşü — fon qatılığı və yazı ölçüsü, **hər vidcet növü üçün ayrıca**.
 *
 * Növə görədir, yerləşdirilmiş nüsxəyə görə yox: tətbiq Ayarları vidcet nüsxələrini tanımır (onlar
 * launcher-indir), ayar isə həm oradan, həm də vidcetə uzun basanda açılan ekrandan dəyişdirilir —
 * iki yerin eyni dəyəri göstərməsi üçün açar hər ikisinin bildiyi şey olmalıdır. Nəticə: iki pleyer
 * vidceti eyni görünür, pleyer ilə namaz vidceti isə bir-birindən asılı deyil.
 *
 * Görünüş ayarıdır, cihaza bağlı deyil — ehtiyat nüsxə açarları özü daşıyır
 * ([com.cafarovceyxun.anamuslim.utils.univ.PreferenceBackup]), `DEVICE_LOCAL_KEYS`-ə əlavə
 * edilməməlidir.
 *
 * Vidcet kompozisiyası fon işçisində qurulur — ona görə `get*` adi oxudur, `observe*` yalnız
 * ekranlar üçündür.
 */
object WidgetAppearancePreferences {

    /**
     * Fon qatılığı, faizlə. `100` = tam qeyri-şəffaf kart, kiçildikcə fon şəffaflaşır və divar
     * kağızı görünür. Aşağı ucu `0` deyil: tam şəffaf fonda ağ mətn açıq divar kağızında oxunmur,
     * `20` isə hələ də «şüşə» təsiri verir.
     */
    val OPACITY_RANGE = 20..100

    /** Sürüşdürücünün addımı — faizin hər bir vahidi vidcetdə fərq etmir. */
    const val OPACITY_STEP = 5

    /** Vidcetin tarixi görünüşü: 85% qatılıq. */
    const val DEFAULT_OPACITY = 85

    /**
     * Yazı ölçüsü, faizlə — vidcetin öz `sp` dəyərlərinə vurulur. Aralıq qəsdən dardır: xana sabit
     * ölçülüdür, `150%`-dən iri yazı 2x1 namaz kartında saatı belə kəsir; `70%`-dən kiçiyi isə
     * oxunmur (vidcetin ən xırda yazısı 11sp-dir → ~8sp).
     */
    val TEXT_SCALE_RANGE = 70..150

    /** `5%` fərqi vidcetdə gözlə seçilmir. */
    const val TEXT_SCALE_STEP = 10

    const val DEFAULT_TEXT_SCALE = 100

    /** «Hələ yazılmayıb» — [opacityKeys] üçün köhnə ümumi dəyərə düşmək işarəsi. */
    private const val UNSET = -1

    /**
     * ⚠️ Açar adları enum adından **yox**, sabit sətirdəndir: enum-u yenidən adlandırmaq saxlanmış
     * ayarları (və ehtiyat nüsxələri) səssizcə yetim qoyardı.
     */
    private val HomeWidgetKind.storageId: String
        get() = when (this) {
            HomeWidgetKind.RecitationPlayer -> "player"
            HomeWidgetKind.VerseOfTheDay -> "votd"
            HomeWidgetKind.PrayerTimes -> "prayer"
            HomeWidgetKind.PrayerTimesWithLogo -> "prayer_logo"
        }

    private val opacityKeys = HomeWidgetKind.entries.associateWith {
        PrefKey(intPreferencesKey("widget.${it.storageId}.opacity"), UNSET)
    }

    private val textScaleKeys = HomeWidgetKind.entries.associateWith {
        PrefKey(intPreferencesKey("widget.${it.storageId}.text_scale"), DEFAULT_TEXT_SCALE)
    }

    /**
     * Növün öz dəyəri yoxdursa köhnə ümumi [PrayerPreferences.KEY_WIDGET_OPACITY] götürülür — ayar
     * növlərə bölünməzdən əvvəl seçilmiş qatılıq yeniləmədən sonra hər vidcetdə eyni qalır.
     */
    private fun resolveOpacity(stored: Int, legacy: Int): Int =
        (if (stored == UNSET) legacy else stored).coerceIn(OPACITY_RANGE)

    fun getOpacityPercent(kind: HomeWidgetKind): Int = resolveOpacity(
        DataStoreManager.read(opacityKeys.getValue(kind)),
        DataStoreManager.read(PrayerPreferences.KEY_WIDGET_OPACITY),
    )

    @Composable
    fun observeOpacityPercent(kind: HomeWidgetKind): Int = resolveOpacity(
        DataStoreManager.observe(opacityKeys.getValue(kind)),
        DataStoreManager.observe(PrayerPreferences.KEY_WIDGET_OPACITY),
    )

    suspend fun setOpacityPercent(kind: HomeWidgetKind, percent: Int) =
        DataStoreManager.write(opacityKeys.getValue(kind), percent.coerceIn(OPACITY_RANGE))

    fun getTextScalePercent(kind: HomeWidgetKind): Int =
        DataStoreManager.read(textScaleKeys.getValue(kind)).coerceIn(TEXT_SCALE_RANGE)

    @Composable
    fun observeTextScalePercent(kind: HomeWidgetKind): Int =
        DataStoreManager.observe(textScaleKeys.getValue(kind)).coerceIn(TEXT_SCALE_RANGE)

    suspend fun setTextScalePercent(kind: HomeWidgetKind, percent: Int) =
        DataStoreManager.write(textScaleKeys.getValue(kind), percent.coerceIn(TEXT_SCALE_RANGE))
}
