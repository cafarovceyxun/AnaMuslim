package com.cafarovceyxun.anamuslim.utils.app

/**
 * Sürüm **adlarının** müqayisəsi (`2026.08.31`) — nə Android `versionCode`-u, nə də iOS
 * `CFBundleVersion`-u. Səbəb: hekayə «bu funksiya hansı buraxılışda gəldi» sualına cavab verir,
 * buraxılış adı isə iki mağazada eynidir (tarix formatı), nömrələr isə tamam ayrı say sahələridir
 * (bax `AppUpdateChecker`).
 *
 * Müqayisə rəqəm qruplarına görədir: `2026.8.9` < `2026.08.31`, çünki hissələr **ədəd** kimi
 * oxunur; sətir müqayisəsi bunu tərsinə çevirərdi. Çatışmayan hissə sıfır sayılır
 * (`2026.09` == `2026.09.0`), rəqəm olmayan hissələr (`-debug`, `rc1`) atılır.
 */
object AppVersionName {

    /**
     * [current] ən azı [required] qədərdirmi.
     *
     * `required` boşdursa hədd yoxdur → `true`. `current` boşdursa da `true`: sürümü oxuya
     * bilmiriksə (seam bağlanmayıb) məzmunu **gizlətmək** daha pis nəticədir — istifadəçi heç nə
     * görmür və səbəbi görünmür.
     */
    fun isAtLeast(current: String?, required: String?): Boolean {
        val target = parse(required) ?: return true
        val mine = parse(current) ?: return true

        return compare(mine, target) >= 0
    }

    private fun parse(value: String?): List<Int>? {
        if (value.isNullOrBlank()) return null

        val parts = value.split('.', '-', '_', ' ', '(', ')')
            .mapNotNull { part -> part.takeWhile { it.isDigit() }.toIntOrNull() }

        return parts.takeIf { it.isNotEmpty() }
    }

    private fun compare(left: List<Int>, right: List<Int>): Int {
        val size = maxOf(left.size, right.size)

        for (i in 0 until size) {
            val diff = (left.getOrElse(i) { 0 }).compareTo(right.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }

        return 0
    }
}
