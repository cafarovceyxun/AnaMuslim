package com.cafarovceyxun.anamuslim.compose.utils

import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerWidgetSnapshotBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSFormattingContextBeginningOfSentence
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.setValue

/**
 * Ana ekran vidcetinin məlumat körpüsü (iOS).
 *
 * WidgetKit uzantısı **ayrı prosesdir**: nə DataStore-u, nə paylaşılan Kotlin qatını görür. Tətbiq
 * hazır məzmunu App Group-a yazır, uzantı isə yalnız oxuyub çəkir — səbəblər
 * [com.cafarovceyxun.anamuslim.utils.prayer.PrayerWidgetSnapshot] KDoc-undadır.
 *
 * ⚠️ **App Group hər iki tərəfin entitlement-ində olmalıdır** ([APP_GROUP_ID]). Qeydiyyatdan
 * keçməyibsə `NSUserDefaults(suiteName:)` **xəta vermir** — sadəcə tətbiqin öz sandbox-ına yazır və
 * uzantı boş görünür. Ona görə yazıdan sonra geri oxunub yoxlanılır.
 *
 * Yenilənmə `WidgetCenter`-lə olur, o isə **Swift-only** API-dir (ObjC-yə açılmır, Kotlin/Native
 * onu birbaşa çağıra bilmir) — buna görə host tərəf [setReloadHandler] ilə öz bağlamasını verir.
 * Eyni model `IosSystemChrome`-da da işlənir.
 */
object IosPrayerWidgetBridge {

    /** `iosApp.entitlements` və `PrayerWidget.entitlements` ilə eyni olmalıdır. */
    const val APP_GROUP_ID = "group.com.cafarovceyxun.anamuslim"

    const val SNAPSHOT_KEY = "prayer_widget_snapshot"

    private const val LOG_TAG = "prayer.widget"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val json = Json { encodeDefaults = true }

    private var reloadHandler: (() -> Unit)? = null

    /** Sonuncu uğurlu yazının dili — dil dəyişəndə snapshot yenidən qurulur. */
    private var lastLanguageTag: String? = null

    init {
        // ⚠️ Dil ilə yarış. İlk yazı bootstrap-dadır, `setAppLocale` isə **ilk kompozisiyada**
        // çağırılır — sıra hər iki cür düşə bilir, ona görə «birinci emissiyanı at» (`drop(1)`)
        // yaramır: dil artıq qurulubsa yeni emissiya heç vaxt gəlmir. Əvəzinə yazılan dil yadda
        // saxlanılır və fərqlənəndə yenidən yazılır. Sınaqda görülən nəticə: ilk snapshot-da
        // «Monday, 25 Rəbiül-əvvəl 1448» — mətnlərin qalanı azərbaycanca olduğu halda.
        scope.launch {
            appLocaleFlow.collect { locale ->
                if (locale.languageTag != lastLanguageTag) {
                    // Açılışın ən erkən anında DataStore hələ qurulmayıb — o zaman yazı atılır və
                    // `lastLanguageTag` boş qaldığı üçün növbəti emissiya təkrar cəhd edir.
                    runCatching { write() }
                }
            }
        }
    }

    /**
     * Swift host `WidgetCenter.shared.reloadAllTimelines()` verir.
     *
     * ⚠️ Burada **yazı tetiklenmir**: handler `didFinishLaunching`-də qurulur, paylaşılan qat isə
     * ilk kompozisiyada qalxır — həmin anda `DataStoreManager` hələ `init` olunmayıb və oxu
     * `UninitializedPropertyAccessException` verərdi. İlk yazını bootstrap-ın öz
     * `IosPrayerReminder.refresh()` çağırışı (və yuxarıdakı dil kollektoru) edir.
     */
    fun setReloadHandler(handler: () -> Unit) {
        reloadHandler = handler
    }

    /** Atəş-və-unut: ayar dəyişikliyindən, açılışdan və fon yenilənməsindən çağırılır. */
    fun refresh() {
        scope.launch { write() }
    }

    suspend fun write() {
        val snapshot = PrayerWidgetSnapshotBuilder.build(::weekdayName)
        val defaults = NSUserDefaults(suiteName = APP_GROUP_ID)

        defaults.setValue(json.encodeToString(snapshot), forKey = SNAPSHOT_KEY)
        defaults.synchronize()

        lastLanguageTag = appLocale().languageTag

        if (defaults.stringForKey(SNAPSHOT_KEY) == null) {
            // Sükutla itən yazının yeganə əlaməti budur: entitlement yoxdursa uzantı boş qalır.
            AppLogger.d("$LOG_TAG: App Group '$APP_GROUP_ID' yazıla bilmədi — entitlement yoxdur?")
            return
        }

        reloadHandler?.invoke()
    }

    /**
     * Həftənin günü sistemdən, tətbiqin dilində. Ayrıca tərcümə saxlanmır — `AppLocale` Compose
     * Resources ilə eyni dili verir (`IosAppLocale`).
     */
    private fun weekdayName(atMillis: Long): String {
        val formatter = NSDateFormatter().apply {
            setLocale(NSLocale(localeIdentifier = appLocale().languageTag))
            setDateFormat("EEEE")
            // CLDR-də azərbaycanca gün adları kiçik hərflədir («bazar ertəsi»), sətrin başında isə
            // böyük gözlənilir. Əl ilə böyütmək səhvdir — dilə görə qayda dəyişir; bu kontekst
            // bayrağı düzgün variantı formatterin özünə seçdirir.
            setFormattingContext(NSFormattingContextBeginningOfSentence)
        }

        return formatter.stringFromDate(
            NSDate.dateWithTimeIntervalSince1970(atMillis / 1000.0)
        )
    }
}
