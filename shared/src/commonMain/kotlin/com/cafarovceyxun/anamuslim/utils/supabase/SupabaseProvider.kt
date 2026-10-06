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

    // Frankfurt (eu-central-1) — 2026-10-07-də Tokiodakı `molyqwcaynvsdmixtcbc`-dən köçürülüb.
    // Köhnə layihə köhnə build-lər yenilənənə qədər açıq qalır; bax docs/supabase/SCHEMA.md.
    private const val SUPABASE_URL = "https://vyacxuwhtqqbythsovzt.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZ5YWN4dXdodHFxYnl0aHNvdnp0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTEzMTk5NjMsImV4cCI6MjEwNjg5NTk2M30.0SNjwWKMUCEhsUZo2vkFuNNlKqeV2UByqBY0vHa4HZo"

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
