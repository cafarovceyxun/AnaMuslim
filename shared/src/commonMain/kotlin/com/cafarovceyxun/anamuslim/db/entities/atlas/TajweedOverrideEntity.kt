package com.cafarovceyxun.anamuslim.db.entities.atlas

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Per-occurrence tajweed override for one word at `(ayah_id, word_index)`. Stored as the raw packed
 * diff bytes from `tajweed.bin` — each byte is `(glyph_index << 3) | class` — applied on top of the
 * word's context-free base ([TajweedWordColorEntity]). Only words whose tajweed differs from the
 * base at a word boundary carry a row here.
 */
@Entity(
    tableName = "tajweed_overrides",
    primaryKeys = ["ayah_id", "word_index"],
    indices = [
        Index(value = ["ayah_id"], name = "idx_tajweed_overrides_ayah"),
    ],
)
data class TajweedOverrideEntity(
    @ColumnInfo(name = "ayah_id")
    val ayahId: Int,
    @ColumnInfo(name = "word_index")
    val wordIndex: Int,
    @ColumnInfo(name = "diffs", typeAffinity = ColumnInfo.BLOB)
    val diffs: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TajweedOverrideEntity) return false
        return ayahId == other.ayahId && wordIndex == other.wordIndex && diffs.contentEquals(other.diffs)
    }

    override fun hashCode(): Int {
        var result = ayahId
        result = 31 * result + wordIndex
        result = 31 * result + diffs.contentHashCode()
        return result
    }
}
