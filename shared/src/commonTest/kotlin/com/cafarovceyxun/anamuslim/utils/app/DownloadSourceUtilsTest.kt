package com.cafarovceyxun.anamuslim.utils.app

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * jsDelivr yol çevirməsi.
 *
 * ⚠️ Bu test bir səbəbdən var: `cdn.jsdelivr.net/gh/` **`owner/repo@ref/yol`** istəyir, kod isə
 * mirror kökünü sadəcə `owner/repo/ref/yol`-a yapışdırırdı və hər sorğu 404 qaytarırdı. Ayarlardakı
 * jsDelivr seçimi beləcə **heç vaxt işləməyib** — nə yuxarı axın atlas/WBW faylları, nə də bu
 * repodakılar. Səssiz idi, çünki yükləmə xətaları istifadəçiyə göstərilmir.
 */
class DownloadSourceUtilsTest {

    @Test
    fun putsRefBehindAtSign() {
        assertEquals(
            "AlfaazPlus/QuranAppInventory@master/wbw_v2/wbw_tr.json.gz",
            DownloadSourceUtils.toJsDelivrPath(
                "AlfaazPlus/QuranAppInventory/master/wbw_v2/wbw_tr.json.gz",
            ),
        )
    }

    @Test
    fun keepsDeepPathsIntact() {
        assertEquals(
            "cafarovceyxun/AnaMuslim@main/inventory/prayer/cities-v1.tsv.gz",
            DownloadSourceUtils.toJsDelivrPath(
                "cafarovceyxun/AnaMuslim/main/inventory/prayer/cities-v1.tsv.gz",
            ),
        )
    }

    /** Gözlənilməz forma olduğu kimi qalır — 404 verməkdənsə sorğunu dəyişdirməmək yaxşıdır. */
    @Test
    fun leavesShortPathsAlone() {
        assertEquals("owner/repo", DownloadSourceUtils.toJsDelivrPath("owner/repo"))
        assertEquals("owner/repo/main", DownloadSourceUtils.toJsDelivrPath("owner/repo/main"))
    }
}
