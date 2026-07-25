package com.cafarovceyxun.anamuslim.db.entities.atlas

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Context-free tajweed base for a unique atlas word text: one class byte (0..7) per glyph, in
 * glyph order (see `tools/tajweed/FORMAT.md`). Keyed by word text, mirroring
 * [AtlasWordShapeEntity] — the atlas layout is deduplicated by word text, so the base is too.
 */
@Entity(
    tableName = "tajweed_word_colors",
    primaryKeys = ["bundle_key", "word"],
    indices = [
        Index(value = ["bundle_key"], name = "idx_tajweed_word_colors_bundle"),
    ],
)
data class TajweedWordColorEntity(
    @ColumnInfo(name = "bundle_key")
    val bundleKey: String,
    @ColumnInfo(name = "word")
    val word: String,
    /** One byte per glyph: the tajweed class id, positionally aligned with the atlas placements. */
    @ColumnInfo(name = "classes", typeAffinity = ColumnInfo.BLOB)
    val classes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TajweedWordColorEntity) return false
        return bundleKey == other.bundleKey && word == other.word && classes.contentEquals(other.classes)
    }

    override fun hashCode(): Int {
        var result = bundleKey.hashCode()
        result = 31 * result + word.hashCode()
        result = 31 * result + classes.contentHashCode()
        return result
    }
}
