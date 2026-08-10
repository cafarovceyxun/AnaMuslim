package com.cafarovceyxun.anamuslim.utils.reader.atlas

data class AtlasPreparedGlyph(
    val x: Float,
    val y: Float,
    val glyph: AtlasGlyphJson,
    /**
     * Index of the source [AtlasGlyphPlacement] this glyph came from. Prepared glyphs skip
     * placements whose gid is missing from the atlas, so per-glyph tajweed classes (which are
     * positionally aligned with the *original* placements) must be indexed by this, not the
     * prepared-list position.
     */
    val placementIndex: Int = 0,
)
