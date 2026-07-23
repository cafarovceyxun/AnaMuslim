package com.cafarovceyxun.anamuslim.utils.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.cafarovceyxun.anamuslim.R
import com.cafarovceyxun.anamuslim.activities.ActivitySettings
import com.cafarovceyxun.anamuslim.api.GithubApi
import com.cafarovceyxun.anamuslim.api.models.translation.TranslationBookInfoModel
import io.ktor.utils.io.jvm.javaio.toInputStream
import com.cafarovceyxun.anamuslim.compose.navigation.SettingRoutes
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.search.SearchIndexScheduler
import com.cafarovceyxun.anamuslim.utils.Logger
import com.cafarovceyxun.anamuslim.utils.app.AppActions
import com.cafarovceyxun.anamuslim.utils.app.NotificationUtils
import com.cafarovceyxun.anamuslim.utils.app.NotificationUtils.createForegroundInfoFallback
import com.cafarovceyxun.anamuslim.utils.reader.factory.QuranTranslationFactory
import com.cafarovceyxun.anamuslim.utils.sharedPrefs.SPAppActions.removeFromPendingAction
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseTranslation
import com.cafarovceyxun.anamuslim.utils.univ.Keys
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.io.File

class TranslationDownloadWorker(
    val ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val bookInfoJson = inputData.getString("bookInfo")
            ?: return createForegroundInfoFallback(ctx)
        val bookInfo = Json.decodeFromString<TranslationBookInfoModel>(bookInfoJson)


        return createForegroundInfo(bookInfo, 0)
    }

    override suspend fun doWork(): Result {
        val bookInfoJson = inputData.getString("bookInfo") ?: return Result.failure()
        val bookInfo = Json.decodeFromString<TranslationBookInfoModel>(bookInfoJson)

        setForeground(createForegroundInfo(bookInfo, 0))

        return try {
            val translData = if (bookInfo.slug == "az") {
                downloadFromSupabase(bookInfo)
            } else {
                downloadFromGithub(bookInfo)
            }

            QuranTranslationFactory().use {
                it.store.storeTranslation(bookInfo, translData)
            }

            SearchIndexScheduler.enqueueSlug(ctx.applicationContext, bookInfo.slug)

            removeFromPendingAction(ctx, AppActions.APP_ACTION_TRANSL_UPDATE, bookInfo.slug)
            val savedTranslations = ReaderPreferences.getTranslations().toMutableSet()
            if (savedTranslations.remove(bookInfo.slug)) {
                ReaderPreferences.setTranslations(savedTranslations)
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private suspend fun downloadFromSupabase(bookInfo: TranslationBookInfoModel): String {
        val allRows = mutableListOf<SupabaseTranslation>()
        var offset = 0
        val pageSize = 1000
        
        try {
            while (offset < 7000) { // Quran ayə sayından bir az artıq limit qoyuruq
                val response = SupabaseProvider.client.from("translations").select {
                    filter {
                        // Həm az, həm də azv2 üçün eyni "az" sətirlərini çəkirik
                        eq("slug", "az")
                    }
                    order("id", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                    range(offset.toLong(), (offset + pageSize - 1).toLong())
                }.decodeList<SupabaseTranslation>()

                if (response.isEmpty()) break
                
                allRows.addAll(response)
                if (response.size < pageSize) break
                offset += pageSize
            }
        } catch (error: Exception) {
            Logger.d("Supabase yükləmə xətası ($offset): ${error.message}")
            if (allRows.isEmpty()) throw error
        }

        if (allRows.isEmpty()) throw Exception("Supabase-də məlumat tapılmadı")
        
        Logger.d("Supabase-dən cəmi ${allRows.size} ayə yükləndi.")

        val json = JSONObject()
        allRows.forEach { row ->
            val verseObj = JSONObject()
            val content = row.text
            val note = row.note
            verseObj.put("t", content ?: "")
            if (!note.isNullOrEmpty()) {
                verseObj.put("n", note)
            }
            json.put("${row.chapter_no}:${row.verse_no}", verseObj)
        }

        return json.toString()
    }

    private suspend fun downloadFromGithub(
        bookInfo: TranslationBookInfoModel
    ): String = withContext(Dispatchers.IO) {
        val tmpFile = File.createTempFile(
            bookInfo.slug,
            ".json",
            ctx.cacheDir
        )

        GithubApi.getTranslation(bookInfo.downloadPath) { scope ->
            if (!scope.isSuccessful) throw Exception("HTTP ${scope.statusCode}")
            val totalBytes = scope.contentLength

            scope.channel.toInputStream().use { inS ->
                tmpFile.outputStream().buffered().use { outS ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = 0L

                    while (true) {
                        ensureActive()

                        if (isStopped) break

                        val bytes = inS.read(buffer)

                        if (bytes <= 0) break

                        outS.write(buffer, 0, bytes)
                        downloaded += bytes

                        val progress =
                            if (totalBytes > 0) ((downloaded * 100) / totalBytes).toInt() else null
                        setProgressAsync(workDataOf("progress" to progress))
                        setForeground(createForegroundInfo(bookInfo, progress))
                    }

                    outS.flush()
                }
            }
        }

        tmpFile.readText()
    }

    private suspend fun mockDownloadFile(
        bookInfo: TranslationBookInfoModel
    ) {
        for (progress in 0..100 step 10) {
            if (isStopped) break

            Logger.d("Mock downloading ${bookInfo.slug}: $progress%")

            setProgressAsync(workDataOf("progress" to progress))
            setForeground(createForegroundInfo(bookInfo, progress))

            kotlinx.coroutines.delay(1000)
        }
    }

    private fun createForegroundInfo(
        bookInfo: TranslationBookInfoModel,
        progress: Int?
    ): ForegroundInfo {
        val channelId = NotificationUtils.CHANNEL_ID_DOWNLOADS
        val builder = NotificationCompat.Builder(ctx, channelId).apply {
            setAutoCancel(false)
            setOngoing(true)
            setShowWhen(false)
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentTitle(ctx.getString(R.string.textDownloading))
            setContentText(bookInfo.bookName)
            setCategory(NotificationCompat.CATEGORY_PROGRESS)
            setProgress(100, progress ?: 0, progress == null)
        }

        var flag = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flag = flag or PendingIntent.FLAG_IMMUTABLE
        }

        val activityIntent = Intent(ctx, ActivitySettings::class.java).apply {
            putExtra(
                Keys.NAV_DESTINATION,
                SettingRoutes.TRANSLATIONS
            )
        }
        val pendingIntent = PendingIntent.getActivity(
            ctx,
            bookInfo.slug.hashCode(),
            activityIntent,
            flag
        )
        builder.setContentIntent(pendingIntent)

        val cancelIntent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(id)

        builder.addAction(
            R.drawable.dr_icon_close,
            ctx.getString(R.string.strLabelCancel),
            cancelIntent
        )

        val notificationId = bookInfo.slug.hashCode()
        val notification = builder.build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }
}
