package com.cafarovceyxun.anamuslim.utils.hadith

/**
 * «Oxunub qurtarılıb» nişanının hər səviyyə üçün hesablanmış halı.
 *
 * Bazada **yalnız yarpaqlar** saxlanılır (alt-bab, alt-babı olmayan babda isə babın özü) — yuxarı
 * səviyyələr buradan çıxarılır. Səbəb: yeni hədis və ya yeni alt-bab əlavə olunanda yarpaq özü
 * yarımçıq qalır və nişan **avtomatik** sönür; «kitab bitdi» sətrini ayrıca saxlasaydıq o, məzmun
 * böyüdükdən sonra da yerində durub yalan deyərdi.
 */
data class HadithCompletion(
    /** Bitmiş yarpaqlar: alt-bab slug-ları və alt-babı olmayan bab slug-ları. */
    val nodes: Set<String> = emptySet(),
    val chapters: Set<String> = emptySet(),
    val books: Set<String> = emptySet(),
    val volumes: Set<String> = emptySet(),
) {
    fun isNodeCompleted(slug: String?): Boolean = slug != null && slug in nodes
    fun isChapterCompleted(slug: String?): Boolean = slug != null && slug in chapters
    fun isBookCompleted(slug: String?): Boolean = slug != null && slug in books
    fun isVolumeCompleted(slug: String?): Boolean = slug != null && slug in volumes

    /** Ağacın bir düyünü — yalnız slug və valideyni lazımdır, ona görə Room entity-lərindən asılı deyil. */
    data class Node(val slug: String, val parent: String)

    companion object {
        val EMPTY = HadithCompletion()

        /**
         * Yarpaq dəstindən bütün səviyyələri qurur.
         *
         * **Boş valideyn bitmiş sayılmır**: uşağı olmayan bab/kitab/cild (hələ məzmun yazılmayıb)
         * `all {}` ilə `true` verərdi və istifadəçi heç açmadığı kitabda ✓ görərdi.
         */
        fun build(
            completedLeaves: Set<String>,
            books: List<Node>,
            chapters: List<Node>,
            subChapters: List<Node>,
        ): HadithCompletion {
            if (completedLeaves.isEmpty()) return EMPTY

            val subsByChapter = subChapters.groupBy { it.parent }

            val completedChapters = chapters.filter { chapter ->
                val subs = subsByChapter[chapter.slug].orEmpty()

                // Alt-babı olmayan babın yarpağı öz slug-ıdır; olanınkı bütün alt-bablarıdır.
                if (subs.isEmpty()) {
                    chapter.slug in completedLeaves
                } else {
                    subs.all { it.slug in completedLeaves }
                }
            }.mapTo(HashSet()) { it.slug }

            val chaptersByBook = chapters.groupBy { it.parent }

            val completedBooks = books.filter { book ->
                val own = chaptersByBook[book.slug].orEmpty()
                own.isNotEmpty() && own.all { it.slug in completedChapters }
            }.mapTo(HashSet()) { it.slug }

            val completedVolumes = books.groupBy { it.parent }
                .filterValues { volumeBooks ->
                    volumeBooks.isNotEmpty() && volumeBooks.all { it.slug in completedBooks }
                }
                .keys
                .toSet()

            return HadithCompletion(
                nodes = completedLeaves,
                chapters = completedChapters,
                books = completedBooks,
                volumes = completedVolumes,
            )
        }
    }
}
