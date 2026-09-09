package com.cafarovceyxun.anamuslim.utils.managers

import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.repository.supabase.TranslationCatalogBook
import com.cafarovceyxun.anamuslim.repository.supabase.TranslationCatalogRepository
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.reader.TranslUtils
import com.cafarovceyxun.anamuslim.utils.reader.factory.QuranTranslationFactory
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * Kataloqda **bağlanmış** kitabı (`quran_translation_books.is_public = false`) cihazdan da çıxarır.
 *
 * `is_public` süzgəci klientdədir və uzun müddət **yalnız siyahını qurarkən** işləyirdi: kitab
 * artıq endirilibsə, `TranslationViewModel.mergeTranslations` onu «kataloqda yoxdur, amma cihazda
 * var» qolundan siyahıya geri salırdı, oxucu isə onsuz da yerli bazadan oxuduğu üçün mətn ekranda
 * qalırdı. Yəni açarı bağlamaq **yalnız hələ endirməmiş** istifadəçiyə təsir edirdi — admin
 * tərcüməni sınayıb bağlayanda onu sınamış hər cihazda görünməyə davam edirdi.
 *
 * İki hal qəsdən fərqlidir:
 * - kitab kataloqdadır, amma `is_public = false` → **gizlədilir və yerli nüsxəsi silinir**
 *   (server «bu hələ hazır deyil» deyir; mətn yenidən açılanda endirilə bilər);
 * - kitab kataloqda **ümumiyyətlə yoxdur** → toxunulmur, istifadəçi özü silənə qədər qalır
 *   (kataloqdan çıxarılmış köhnə kitabı əlindən almırıq).
 */
object TranslationVisibilitySync {

    /** Kataloqun bu istifadəçiyə göstərilməməli kitabları. Girişli (admin) sessiya hamısını görür. */
    fun hiddenSlugs(catalog: List<TranslationCatalogBook>, signedIn: Boolean): Set<String> {
        if (signedIn) return emptySet()
        return catalog.filterNot { it.is_public }
            .map { it.slug }
            .filterNot { TranslUtils.isPrebuilt(it) }
            .toSet()
    }

    /**
     * Gizli kitabların yerli nüsxəsini silir, seçimdən çıxarır və axtarış indeksindən götürür.
     *
     * @return həqiqətən silinən sluqlar (heç nə endirilməyibsə boş).
     */
    suspend fun purgeHidden(
        catalog: List<TranslationCatalogBook>,
        signedIn: Boolean,
    ): Set<String> {
        val hidden = hiddenSlugs(catalog, signedIn)
        if (hidden.isEmpty()) return emptySet()

        val removed = withContext(Dispatchers.IO) {
            QuranTranslationFactory().use { factory ->
                hidden.filter { factory.isTranslationDownloaded(it) }
                    .onEach { slug ->
                        factory.deleteTranslation(slug)
                        TranslationPlatformHooks.removeSlugFromSearchIndex?.invoke(slug)
                    }
                    .toSet()
            }
        }

        // Seçim ayrıca saxlanılır: kitab silinsə də sluq oxucunun siyahısında qalır və
        // `ReaderItemsBuilder` hər dəfə boş nəticə üçün sorğu göndərir.
        val selected = ReaderPreferences.getTranslations()
        val kept = selected - hidden
        if (kept != selected) {
            ReaderPreferences.setTranslations(kept.ifEmpty { TranslUtils.defaultTranslationSlugs() })
        }

        return removed
    }

    /**
     * Açılışdakı baxım addımı: kataloqu serverdən təzələyib gizli kitabları təmizləyir.
     *
     * Tərcümə ekranı da eyni işi görür, amma ora heç vaxt girməyən istifadəçi üçün bağlanmış kitab
     * əks halda oxucuda qalırdı. Şəbəkə yoxdursa [TranslationCatalogRepository.refresh] keşi
     * qaytarır və heç nə silinmir.
     */
    suspend fun refreshAndPurge() {
        try {
            val catalog = TranslationCatalogRepository.refresh()
            val signedIn = SupabaseProvider.client.auth.currentSessionOrNull() != null
            purgeHidden(catalog, signedIn)
        } catch (e: Exception) {
            AppLogger.saveError(e, "TranslationVisibilitySync.refreshAndPurge")
        }
    }
}
