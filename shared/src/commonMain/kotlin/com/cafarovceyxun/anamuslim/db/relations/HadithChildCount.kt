package com.cafarovceyxun.anamuslim.db.relations

/**
 * One `parent slug → number of children` row, produced by the grouped `COUNT(*)` queries in
 * [com.cafarovceyxun.anamuslim.db.dao.HadithDao] that feed the counters on the hadith index cards.
 *
 * Counting in SQL rather than loading the children keeps the hadith levels cheap: a bab only needs
 * to know how many hadiths it holds, not their text.
 */
data class HadithChildCount(
    val parentSlug: String,
    val childCount: Int,
)
