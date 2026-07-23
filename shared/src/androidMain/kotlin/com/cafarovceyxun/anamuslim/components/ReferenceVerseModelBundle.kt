package com.cafarovceyxun.anamuslim.components

import android.os.Bundle
import com.cafarovceyxun.anamuslim.components.ReferenceVerseModelKeys as K

/**
 * Android `Bundle` codec for [ReferenceVerseModel]. Kept as extensions here (a companion extension
 * for the decoder) so `ReferenceVerseModel.fromBundle(...)` call sites read unchanged after the
 * data class moved to commonMain — same split as `RecitationServiceState`.
 */
fun ReferenceVerseModel.toBundle(): Bundle {
    return Bundle().apply {
        putString(K.TITLE, title)
        putString(K.DESC, desc)
        putStringArrayList(K.TRANSL_SLUGS, ArrayList(translSlugs))
        putIntegerArrayList(K.CHAPTERS, ArrayList(chapters))
        putStringArrayList(K.VERSES, ArrayList(verses))

        when (thumbnail) {
            is ReferenceThumbnail.RemoteUrl -> putString(K.THUMBNAIL_URL, thumbnail.url)
            is ReferenceThumbnail.ResourceId -> putInt(K.THUMBNAIL_RES_ID, thumbnail.id)
            null -> Unit
        }
    }
}

fun ReferenceVerseModel.Companion.fromBundle(bundle: Bundle?): ReferenceVerseModel? {
    if (bundle == null) return null

    val thumbnailRes = bundle.getInt(K.THUMBNAIL_RES_ID, 0)
    val thumbnailUrl = bundle.getString(K.THUMBNAIL_URL)

    return ReferenceVerseModel(
        title = bundle.getString(K.TITLE) ?: "",
        desc = bundle.getString(K.DESC),
        // NOTE: `getStringArray` (not `getStringArrayList`) is what the pre-migration code read
        // here, so this always yields an empty set for bundles written by `toBundle`. Preserved
        // verbatim on purpose — `ReferenceScreen` then falls back to the user's saved
        // translations, which is the behaviour shipping today.
        translSlugs = bundle.getStringArray(K.TRANSL_SLUGS)?.toSet() ?: emptySet(),
        chapters = bundle.getIntegerArrayList(K.CHAPTERS)?.toSet() ?: emptySet(),
        verses = bundle.getStringArrayList(K.VERSES)?.toSet() ?: emptySet(),
        thumbnail = when {
            thumbnailRes != 0 -> ReferenceThumbnail.ResourceId(thumbnailRes)
            thumbnailUrl != null -> ReferenceThumbnail.RemoteUrl(thumbnailUrl)
            else -> null
        },
    )
}
