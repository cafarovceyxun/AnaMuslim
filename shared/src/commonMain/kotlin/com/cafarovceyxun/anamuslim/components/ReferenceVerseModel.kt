package com.cafarovceyxun.anamuslim.components

/**
 * Hero image for a reference collection. [ResourceId] carries a platform drawable id, so only the
 * platform image renderer (`PlatformImage`) knows how to resolve it.
 */
sealed class ReferenceThumbnail {
    data class RemoteUrl(val url: String) : ReferenceThumbnail()
    data class ResourceId(val id: Int) : ReferenceThumbnail()
}

/**
 * A curated set of verses ("reference") rendered by `ReferenceScreen`. Pure data, so it lives in
 * commonMain; the Android `Bundle` codec is an extension in androidMain
 * (`ReferenceVerseModelBundle.kt`), following the `RecitationServiceState` split.
 */
data class ReferenceVerseModel(
    val title: String,
    val desc: String?,
    val translSlugs: Set<String> = emptySet(),
    val chapters: Set<Int>,
    val verses: Set<String>,
    val thumbnail: ReferenceThumbnail? = null,
) {
    companion object
}

/** Bundle keys, kept here so the Android codec and any future platform codec agree. */
internal object ReferenceVerseModelKeys {
    const val TITLE = "title"
    const val DESC = "desc"
    const val TRANSL_SLUGS = "translSlugs"
    const val CHAPTERS = "chapters"
    const val VERSES = "verses"
    const val THUMBNAIL_URL = "thumbnail_url"
    const val THUMBNAIL_RES_ID = "thumbnail_res_id"
}
