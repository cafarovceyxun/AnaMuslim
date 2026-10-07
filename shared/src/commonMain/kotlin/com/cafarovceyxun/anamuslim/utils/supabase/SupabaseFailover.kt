package com.cafarovceyxun.anamuslim.utils.supabase

import com.cafarovceyxun.anamuslim.api.NetworkClient
import com.cafarovceyxun.anamuslim.utils.AppLogger
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.encodedPath
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Əsas server (Oracle, self-hosted) ↔ ehtiyat (Supabase Frankfurt) keçidi — 2026-10-07.
 *
 * Ehtiyatda **yalnız cədvəllər** (hər gecə Oracle-dan köçürülür, `anamuslim-backup-sync.timer`) və **Edge
 * Function-lar** var; storage faylları köçürülmür. Ona görə ehtiyata yalnız **oxu** gedir: `/rest/v1/` üzərində
 * GET/HEAD və qiblə taylları. Yazma (təklif, bildiriş, admin düzəlişi) həmişə əsasa gedir — ehtiyata yazılan
 * sətir növbəti gecəki köçürmədə silinərdi, yəni səssizcə itərdi. Auth da həmişə əsasdadır: ehtiyat başqa JWT
 * sirri ilə işləyir, ona görə istifadəçi tokeni orada keçmir və sorğu ehtiyatın **anon** açarı ilə gedir.
 *
 * Ehtiyata iki yolla keçilir:
 * - **Avtomatik:** əsas cavab vermir (bağlantı xətası, vaxt aşımı, 502/503/504) → sorğu dərhal ehtiyatdan
 *   təkrarlanır və cihaz [OUTAGE_STICKY] boyunca birbaşa ehtiyatdan oxuyur (hər sorğu ölü serveri gözləməsin).
 * - **Admin düyməsi:** ehtiyatdakı `backend_switch.mode = 'backup'` → bütün istifadəçilər ehtiyatdan oxuyur
 *   (məs. Oracle işləyir, amma məzmun pozulub). Düymə əsasda saxlana bilməz — əsas çökəndə panel ora yaza
 *   bilməz. Vəziyyət prosesdə [SWITCH_TTL]-dən bir oxunur (~100 bayt).
 *
 * Plugin həm Supabase klientinə ([SupabaseProvider]), həm də paylaşılan [NetworkClient]-ə (qiblə taylları)
 * qoşulub; başqa hostlara toxunmur.
 */
object SupabaseFailover {
    internal const val PRIMARY_HOST = "anamuslim.cafarovceyxun.com"

    // Supabase-in öz Custom Domain-i pulludur — domeni Cloudflare Worker verir
    // (tools/cloudflare/backup-proxy/worker.js): ehtiyat dəyişsə tətbiq yeniləməsi lazım olmur.
    internal const val BACKUP_URL = "https://backup.cafarovceyxun.com"
    private const val BACKUP_HOST = "backup.cafarovceyxun.com"

    // Ehtiyat layihənin (vyacxuwhtqqbythsovzt) `anon` açarı — açıq açardır, RLS qoruyur.
    internal const val BACKUP_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZ5YWN4dXdodHFxYnl0aHNvdnp0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTEzMTk5NjMsImV4cCI6MjEwNjg5NTk2M30.0SNjwWKMUCEhsUZo2vkFuNNlKqeV2UByqBY0vHa4HZo"

    private const val LOG_TAG = "supabase.failover"
    private val OUTAGE_STICKY = 5.minutes
    private val SWITCH_TTL = 10.minutes
    private val SWITCH_WAIT = 1500.milliseconds
    private val OUTAGE_STATUSES = setOf(502, 503, 504)

    /** Bu cihaz hazırda ehtiyatdan niyə oxuyur (admin ekranında göstərilir). */
    enum class Route { Primary, BackupBySwitch, BackupByOutage }

    private val _route = MutableStateFlow(Route.Primary)
    val route: StateFlow<Route> = _route.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val switchMutex = Mutex()
    private var switchForced = false
    private var switchCheckedAt: TimeMark? = null
    private var switchJob: Deferred<Unit>? = null
    private var outageUntil: TimeMark? = null

    val plugin = createClientPlugin("SupabaseFailover") {
        on(Send) { request ->
            if (!request.isBackupEligible()) return@on proceed(request)

            awaitSwitch()
            updateRoute()
            if (switchForced || isInOutage()) {
                return@on proceed(request.copyForBackup())
            }

            val failure: String = try {
                val call = proceed(request)
                val status = call.response.status.value
                if (status !in OUTAGE_STATUSES) return@on call
                "HTTP $status"
            } catch (e: CancellationException) {
                // Sorğunun öz vaxt aşımı (HttpTimeout) icra kontekstini ləğv edir — o, xətadır. Çağıranın
                // ləğvi isə (ekran bağlandı) ötürülməlidir.
                if (!currentCoroutineContext().isActive) throw e
                e.message ?: "cancelled"
            } catch (e: Exception) {
                e.message ?: e::class.simpleName ?: "error"
            }

            outageUntil = TimeSource.Monotonic.markNow() + OUTAGE_STICKY
            updateRoute()
            AppLogger.d(LOG_TAG, "əsas cavab vermədi ($failure) — ehtiyatdan oxunur: ${request.url.encodedPath}")
            proceed(request.copyForBackup())
        }
    }

    /** Admin düyməsi dəyişəndən sonra keşi yeniləmək üçün. */
    fun invalidateSwitch() {
        switchCheckedAt = null
    }

    private fun isInOutage(): Boolean = outageUntil?.hasNotPassedNow() == true

    private fun updateRoute() {
        _route.value = when {
            switchForced -> Route.BackupBySwitch
            isInOutage() -> Route.BackupByOutage
            else -> Route.Primary
        }
    }

    private fun HttpRequestBuilder.isBackupEligible(): Boolean {
        if (url.host != PRIMARY_HOST) return false
        val path = url.encodedPath
        return when {
            path.startsWith("/rest/v1/") -> method == HttpMethod.Get || method == HttpMethod.Head
            path.startsWith("/functions/v1/qibla-tiles/") -> method == HttpMethod.Get
            else -> false
        }
    }

    private fun HttpRequestBuilder.copyForBackup(): HttpRequestBuilder =
        HttpRequestBuilder().takeFrom(this).apply {
            url.host = BACKUP_HOST
            headers.remove("apikey")
            headers.remove(HttpHeaders.Authorization)
            header("apikey", BACKUP_KEY)
            header(HttpHeaders.Authorization, "Bearer $BACKUP_KEY")
        }

    /**
     * Düymənin vəziyyəti köhnədirsə fonda oxuyur və ən çox [SWITCH_WAIT] gözləyir — ehtiyat yavaşdırsa
     * əsas sorğu ləngiməsin (bu halda əvvəlki vəziyyət qüvvədə qalır).
     */
    private suspend fun awaitSwitch() {
        val job = switchMutex.withLock {
            val fresh = switchCheckedAt?.let { it.elapsedNow() < SWITCH_TTL } == true
            if (fresh) return
            switchJob?.takeIf { it.isActive } ?: scope.async { fetchSwitch() }.also { switchJob = it }
        }
        withTimeoutOrNull(SWITCH_WAIT) { job.await() }
    }

    private suspend fun fetchSwitch() {
        try {
            val response = NetworkClient.client.get("$BACKUP_URL/rest/v1/backend_switch?select=mode") {
                header("apikey", BACKUP_KEY)
            }
            if (response.status.isSuccess()) {
                switchForced = response.bodyAsText().contains("\"mode\":\"backup\"")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.d(LOG_TAG, "keçid vəziyyəti oxunmadı: ${e.message}")
        } finally {
            // Uğursuzluqda da işarələ: ehtiyat əlçatmazdırsa hər sorğu onu yenidən soruşmasın.
            switchCheckedAt = TimeSource.Monotonic.markNow()
            updateRoute()
        }
    }
}
