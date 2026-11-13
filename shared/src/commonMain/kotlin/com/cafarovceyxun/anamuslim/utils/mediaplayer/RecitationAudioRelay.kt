package com.cafarovceyxun.anamuslim.utils.mediaplayer

import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseFailover

/**
 * quranicaudio səsini öz serverimizdən keçirir (2026-11-11).
 *
 * quranicaudio-nun CDN-i (Bunny) bir neçə IP diapazonundan cavab verir və Azərbaycanda onların bir qismi
 * ~800 bayt/s-ə qədər boğulur: `ping` normaldır, TLS əl sıxışması 5–10 s çəkir, 64 KB isə 40 saniyəyə
 * gəlmir. DNS hansı IP-ni versə, qari ona görə «işləyir» və ya «işləmir» — pleyer xəta vermir, sadəcə
 * heç vaxt başlamır. Gömülü qarilərin hamısı (`RecitationModelManager.bundledReciters`) və upstream
 * manifestindəki üçü (Əfasi, Hüsari Müəllim, Əcmi) bu hostlardadır.
 *
 * Serverimiz Frankfurtdadır, eyni CDN-ə 0.1 s-də çatır və Azərbaycandan boğulmur. Caddy orada iki yolu
 * yuxarı axına ötürür (bax `tools/supabase/project-migration/README.md`):
 *
 * - `/qari/download/<yol>.mp3` → `download.quranicaudio.com/<yol>.mp3`
 * - `/qari/mirrors/<yol>.mp3`  → `mirrors.quranicaudio.com/<yol>.mp3`
 *
 * Yan faydası: dinləyicinin IP-si quranicaudio-ya çatmır (qiblə taylları ilə eyni prinsip).
 *
 * Yalnız serverin qəbul etdiyi yollar çevrilir ([RELAYABLE_PATH] Caddy-dəki `path_regexp` ilə **eyni**
 * olmalıdır) — başqa formada ünvan toxunulmaz qalır, yoxsa keçid 404 qaytarardı və orijinal ünvanın
 * işləmək şansı da itərdi.
 */
internal object RecitationAudioRelay {

    private const val RELAY_ROOT = "https://${SupabaseFailover.PRIMARY_HOST}/qari/"

    /** Yuxarı axın kökü → keçiddəki qovluq. */
    private val UPSTREAMS = listOf(
        "https://download.quranicaudio.com/" to "download/",
        "https://mirrors.quranicaudio.com/" to "mirrors/",
    )

    private val RELAYABLE_PATH = Regex("^[A-Za-z0-9_/-]+\\.mp3$")

    fun viaRelay(url: String): String {
        for ((upstream, folder) in UPSTREAMS) {
            if (!url.startsWith(upstream)) continue

            val path = url.substring(upstream.length)
            return if (RELAYABLE_PATH.matches(path)) RELAY_ROOT + folder + path else url
        }

        return url
    }
}
