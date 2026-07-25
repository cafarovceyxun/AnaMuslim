package com.cafarovceyxun.anamuslim.db.entities.atlas

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Import bookkeeping for a bundle's tajweed data: the schema [version] that was imported and the
 * embedded ARGB [palette]. When the shipped `tajweed.bin` header version differs from the stored
 * one, the importer rebuilds the tajweed tables.
 */
@Entity(
    tableName = "tajweed_meta",
    primaryKeys = ["bundle_key"],
)
data class TajweedMetaEntity(
    @ColumnInfo(name = "bundle_key")
    val bundleKey: String,
    @ColumnInfo(name = "version")
    val version: Int,
    /** ARGB colours, 4 little-endian bytes per class id, in class order. */
    @ColumnInfo(name = "palette", typeAffinity = ColumnInfo.BLOB)
    val palette: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TajweedMetaEntity) return false
        return bundleKey == other.bundleKey && version == other.version && palette.contentEquals(other.palette)
    }

    override fun hashCode(): Int {
        var result = bundleKey.hashCode()
        result = 31 * result + version
        result = 31 * result + palette.contentHashCode()
        return result
    }
}
