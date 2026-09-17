package com.cafarovceyxun.anamuslim.compose.screens.dua

/**
 * Naviqator vərəqinin bir sətri — vərəqləyicidəki bir **mövzu**.
 *
 * Vərəqləyici bütün dualar üzərində yastıdır ([flattenDuas]), ona görə naviqasiya vahidi səhifə
 * deyil, mövzudur: istifadəçi «Namazdan sonra edilən zikr»ə keçmək istəyir, 23-cü səhifəyə yox.
 */
internal data class DuaNavigatorGroup(
    val key: String,
    val title: String,
    /**
     * Başlığın adı — alt başlıq sətrinin üstündəki kontekst.
     *
     * `null` = qrup elə başlığın özüdür (birbaşa dualar). Kontekst olmasa siyahıda iki eyni adlı
     * alt başlıq («Səhər zikri») hansı başlığa aid olduğunu demirdi.
     */
    val parentTitle: String?,
    /** Qrupun ilk səhifəsinin indeksi — vərəqləyici bura tullanır. */
    val firstIndex: Int,
    val count: Int,
)

/**
 * Yastı siyahını naviqator sətirlərinə çevirir.
 *
 * Qruplar siyahıdakı **sıra ilə** gəlir ([flattenDuas] onları bitişik yazır), yəni naviqator
 * vərəqləyicinin sırasını təkrarlayır: iki yerdə iki fərqli sıra «dua itdi» hissi yaradardı.
 */
internal fun duaNavigatorGroups(entries: List<DuaFlatEntry>): List<DuaNavigatorGroup> {
    if (entries.isEmpty()) return emptyList()

    return buildList {
        entries.forEachIndexed { index, entry ->
            val last = lastOrNull()
            if (last != null && last.key == entry.groupKey) {
                // Qrup bitişikdir, ona görə sayğacı artırmaq kifayətdir — siyahını yenidən süzmək
                // lazım deyil.
                set(lastIndex, last.copy(count = last.count + 1))
            } else {
                add(
                    DuaNavigatorGroup(
                        key = entry.groupKey,
                        title = entry.groupTitle,
                        parentTitle = entry.subcategory?.let { entry.category.name },
                        firstIndex = index,
                        count = 1,
                    ),
                )
            }
        }
    }
}

/**
 * Sətirləri süzgəcə görə daraldır — ad **və** başlığın adı üzrə.
 *
 * Başlığın adı da axtarılır, çünki istifadəçi «Namaz» yazanda onun bütün alt mövzularını gözləyir;
 * yalnız sətrin öz adına baxsaydıq, «Rükudan başını qaldıranda…» sətri süzgəcdən düşərdi.
 */
internal fun filterDuaNavigatorGroups(
    groups: List<DuaNavigatorGroup>,
    query: String,
): List<DuaNavigatorGroup> {
    val needle = query.trim().lowercase()
    if (needle.isEmpty()) return groups

    return groups.filter { group ->
        group.title.lowercase().contains(needle) ||
            group.parentTitle?.lowercase()?.contains(needle) == true
    }
}

/**
 * Bütün mövzuları bitmiş **başlıqların** slug-ları.
 *
 * Başlıq özü bazada yazılmır ([com.cafarovceyxun.anamuslim.db.entities.user.DuaReadProgressEntity]):
 * onun tamamlanması alt mövzularından çıxarılır, ona görə yeni alt mövzu əlavə olunan kimi ✓ **öz-özünə**
 * sönür. Əks halda «başlıq bitdi» sətri yalan danışardı.
 *
 * Duası olmayan başlıq bitmiş sayılmır: siyahıda hər halda görünmür, amma boş dəst üzərində
 * `all { }` `true` qaytarardı və gələcəkdə boş başlıq göstərilsə ✓ ilə çıxardı.
 */
internal fun completedDuaCategories(
    entries: List<DuaFlatEntry>,
    completedGroups: Set<String>,
): Set<String> {
    if (entries.isEmpty() || completedGroups.isEmpty()) return emptySet()

    val groupsByCategory = entries
        .groupBy({ it.category.slug }, { it.groupKey })
        .mapValues { (_, keys) -> keys.toSet() }

    return groupsByCategory
        .filterValues { keys -> keys.isNotEmpty() && completedGroups.containsAll(keys) }
        .keys
}
