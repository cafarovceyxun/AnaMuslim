package com.cafarovceyxun.anamuslim.utils.supabase

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseProvider {
    private const val SUPABASE_URL = "https://molyqwcaynvsdmixtcbc.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1vbHlxd2NheW52c2RtaXh0Y2JjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODA0MTYwOTcsImV4cCI6MjA5NTk5MjA5N30.ceK_Sof_wKibBpNpfp3nEU6535MvewPm1HSGKrRVm9M"

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
