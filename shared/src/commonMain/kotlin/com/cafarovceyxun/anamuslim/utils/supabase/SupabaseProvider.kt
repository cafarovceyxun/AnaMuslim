package com.cafarovceyxun.anamuslim.utils.supabase

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseProvider {
    /**
     * Storage REST çağırışları üçün açıqdır — `storage-kt` plugin-i qəsdən quraşdırılmayıb:
     * yeni asılılıq Ktor versiyasını sürüşdürə bilər və bu, yalnız iOS-da runtime-da partlayır
     * (bax CLAUDE.md, «Asılılıq versiya sürüşməsi tələsi»). Bir fayl yükləmək üçün paylaşılan
     * Ktor klienti ilə iki HTTP çağırışı kifayətdir.
     */
    internal val restUrl: String get() = SUPABASE_URL
    internal val anonKey: String get() = SUPABASE_KEY

    // Öz serverimiz: Oracle Cloud (Frankfurt) üzərində self-hosted Supabase, 2026-10-07-dən.
    // Ünvan öz domenimizdədir — server dəyişsə tətbiq yeniləməsi lazım olmur (DNS kifayətdir).
    // Açar yalnız `anon` rollu açıq açardır; bax tools/supabase/project-migration/README.md.
    private const val SUPABASE_URL = "https://anamuslim.cafarovceyxun.com"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoiYW5vbiIsImlzcyI6InN1cGFiYXNlIiwiaWF0IjoxNzkxMzQwMjY1LCJleHAiOjE5NDkwMjAyNjV9.7gtOC_c2DAwjeGkE940VSLHpIclLTTD3KFa9n0LxQQg"

    val client by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY
        ) {
            install(Postgrest)
            install(Auth)
        }
    }
}
