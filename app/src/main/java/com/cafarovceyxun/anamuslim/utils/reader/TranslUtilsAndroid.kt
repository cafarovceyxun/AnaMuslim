package com.cafarovceyxun.anamuslim.utils.reader

import android.content.Context
import android.util.Pair
import com.cafarovceyxun.anamuslim.R
import com.cafarovceyxun.anamuslim.api.models.translation.TranslationBookInfoModel
import com.cafarovceyxun.anamuslim.utils.Log
import com.cafarovceyxun.anamuslim.utils.Logger
import com.cafarovceyxun.anamuslim.utils.app.AppUtils
import com.cafarovceyxun.anamuslim.utils.univ.FileUtils
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException

object TranslUtilsAndroid {
    val DIR_NAME = FileUtils.createPath(AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR, "translations")
    val DIR_NAME_4_AVAILABLE_DOWNLOADS = FileUtils.createPath(AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR, "available_translation_downloads")

    fun isPrebuilt(slug: String): Boolean = false

    @Throws(Exception::class)
    fun getTranslInfosAndFilesForMigration(fileUtils: FileUtils, translDir: File): List<Pair<TranslationBookInfoModel, File>>? {
        val dirsOfLangCodes = translDir.listFiles()
        if (dirsOfLangCodes == null || dirsOfLangCodes.isEmpty()) {
            Log.d("Nothing was found, deleting root translation directory: " + translDir.name)
            fileUtils.deleteRecursively(translDir)
            return null
        }

        val translInfosAndFiles = ArrayList<Pair<TranslationBookInfoModel, File>>()
        for (langCodeDir in dirsOfLangCodes) {
            val translFiles = langCodeDir.listFiles()
            if (translFiles == null || translFiles.isEmpty()) {
                Log.d("Deleting language directory with its contents: " + langCodeDir.name)
                fileUtils.deleteRecursively(langCodeDir)
                continue
            }

            for (singleTranslDir in translFiles) {
                if (!singleTranslDir.isDirectory) {
                    fileUtils.deleteRecursively(singleTranslDir)
                    continue
                }

                try {
                    val infoJSONFile = File(singleTranslDir, TranslUtils.TRANSL_INFO_FILE_NAME)
                    val pair = readTranslInfoFromJSONFile(fileUtils, infoJSONFile)
                    if (pair == null) {
                        Logger.print("Deleting translation directory with its manifest and data files: " + singleTranslDir.name)
                        fileUtils.deleteRecursively(singleTranslDir)
                        continue
                    }
                    translInfosAndFiles.add(pair)
                } catch (e: Exception) {
                    Logger.print("Error occurred, deleting translation directory with its manifest and data files dire: " + singleTranslDir.name)
                    fileUtils.deleteRecursively(singleTranslDir)
                    e.printStackTrace()
                }
            }
        }
        return translInfosAndFiles
    }

    @Throws(JSONException::class, IOException::class)
    private fun readTranslInfoFromJSONFile(fileUtils: FileUtils, infoJSONFile: File): Pair<TranslationBookInfoModel, File>? {
        if (!infoJSONFile.isFile) return null
        val json = fileUtils.readText(infoJSONFile)
        val jsonObject = JSONObject(json)
        val slug = jsonObject.optString("slug", "")
        if (isPrebuilt(slug)) return null
        val id = jsonObject.optInt("id")
        val langCode = jsonObject.optString("lang-code", "")
        val translFile = fileUtils.getSingleTranslationFile(id, langCode, slug)
        if (!translFile.exists() || translFile.length() == 0L) {
            Log.d("Translation file does not exist or is empty.")
            return null
        }
        val bookInfo = TranslationBookInfoModel(slug).apply {
            this.langCode = langCode
            bookName = jsonObject.optString("book", "")
            authorName = jsonObject.optString("author", "")
            this.langName = jsonObject.optString("lang-name", "")
            displayName = jsonObject.optString("display-name", "")
            lastUpdated = 1636309799000L
        }
        return Pair(bookInfo, translFile)
    }
}
