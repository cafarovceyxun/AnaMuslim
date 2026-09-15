package com.cafarovceyxun.anamuslim.repository.supabase

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Baza «bu sətir artıq var» deyəndə atılan xəta.
 *
 * Ayrıca tip lazımdır, çünki istifadəçiyə göstərilən mesaj fərqlidir: dublikat nə icazə, nə də
 * bağlantı problemidir. 2026-09-15-də məhz bu qarışıqlıq vaxt aparmışdı — ada ikinci dəlil əlavə
 * etmək unikal indeksə dəyirdi, ekranda isə «icazə və ya bağlantı problemi» yazırdı.
 */
class DuaDuplicateException : Exception("duplicate row")

/**
 * Xəta PostgreSQL-in unikal pozuntusudurmu (`23505`).
 *
 * Mətnə baxır, çünki supabase-kt xətanı öz sarğısında verir və SQLSTATE yalnız gövdədə qalır;
 * tipli sahə hər versiyada eyni yerdə deyil.
 */
internal fun Throwable.isDuplicateRow(): Boolean {
    val text = buildString {
        append(message.orEmpty())
        append(' ')
        append(cause?.message.orEmpty())
    }

    return text.contains("23505") || text.contains("duplicate key", ignoreCase = true)
}

/** Yazma xətasını tanınan tipə çevirir; qalanları olduğu kimi ötürür. */
internal fun mapWriteError(error: Throwable): Throwable =
    if (error.isDuplicateRow()) DuaDuplicateException() else error

/**
 * Sorğunu **səhifə-səhifə** sona qədər oxuyur.
 *
 * PostgREST bir cavabda ən çox [PAGE_SIZE] sətir verir (Supabase-in `db-max-rows` ayarı); limitə
 * dəyən sorğu xəta vermir, sadəcə qısa cavab qaytarır — yəni əskik məzmun **səssizcə** keçib gedər.
 * Dövrə tam olmayan səhifə gələndə dayanır.
 */
internal suspend fun <T> fetchAllPages(page: suspend (from: Long, to: Long) -> List<T>): List<T> {
    val all = mutableListOf<T>()
    var from = 0L

    while (true) {
        val chunk = page(from, from + PAGE_SIZE - 1)
        all += chunk

        if (chunk.size < PAGE_SIZE) break
        from += PAGE_SIZE

        // Ağlabatan tavan: səhv süzgəc üzündən sonsuz dövrəyə düşməkdənsə dayanmaq yaxşıdır.
        if (all.size >= MAX_ROWS) break
    }

    return all
}

private const val PAGE_SIZE = 1000L
private const val MAX_ROWS = 50_000

/**
 * Bir siyahının yeni sırasını sətir-sətir yazır və **neçəsinin həqiqətən dəyişdiyini** sayır.
 *
 * Niyə sətir-sətir: hər sətrə **başqa** `sort_no` düşür, ona görə tək `update` ifadəsi ilə
 * yazılmır; `upsert` isə INSERT siyasətindən keçər və `created_by`-ı yenidən yazma riski yaradardı.
 *
 * ⚠️ RLS bir sətri bloklayanda PostgREST xəta yox, **boş nəticə** qaytarır (CLAUDE.md): başqasının
 * əlavə etdiyi başlığı sıralamağa çalışan redaktor «yadda saxlanıldı» görüb heç nə dəyişməmiş
 * ola bilərdi. Ona görə [update] hər sətir üçün «dəyişdimi» qaytarır və bir dənəsi belə keçməyəndə
 * nəticə uğursuzdur.
 *
 * Sorğular [ORDER_BATCH] ölçüsündə dəstələrlə **paralel** gedir: yüz duanı bir-bir yazmaq şəbəkə
 * gecikməsini yüz dəfə toplayardı, hamısını birdən açmaq isə bağlantı hovuzunu boğardı.
 */
internal suspend fun <K> writeSortOrder(
    changes: List<Pair<K, Int>>,
    update: suspend (key: K, sortNo: Int) -> Boolean,
): Result<Unit> = runCatching {
    var blocked = 0

    changes.chunked(ORDER_BATCH).forEach { batch ->
        coroutineScope { batch.map { (key, sortNo) -> async { update(key, sortNo) } }.awaitAll() }
            .forEach { changed -> if (!changed) blocked++ }
    }

    if (blocked > 0) throw IllegalStateException("$blocked sətrin sırası dəyişmədi (RLS?)")
}

private const val ORDER_BATCH = 8
