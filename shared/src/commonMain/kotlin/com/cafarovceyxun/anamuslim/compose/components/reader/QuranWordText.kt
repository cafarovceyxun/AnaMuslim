package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.cafarovceyxun.anamuslim.db.entities.quran.AyahWordEntity
import com.cafarovceyxun.anamuslim.utils.reader.atlas.AtlasGlyphPlacement
import com.cafarovceyxun.anamuslim.utils.reader.atlas.LocalQuranAtlasBundle

@Composable
fun QuranWordText(
    modifier: Modifier = Modifier,
    word: AyahWordEntity,
    atlasPlacements: List<AtlasGlyphPlacement>?,
    style: TextStyle = TextStyle.Default,
    glyphClasses: ByteArray? = null,
    tajweedPalette: List<Color>? = null,
) {
    val bundle = LocalQuranAtlasBundle.current

    if (bundle != null && atlasPlacements != null) {
        QuranAtlasText(
            modifier = modifier,
            placements = atlasPlacements,
            bundle = bundle,
            fontSize = style.fontSize,
            lineHeight = style.lineHeight,
            color = style.color,
            glyphClasses = glyphClasses,
            tajweedPalette = tajweedPalette,
        )
    } else {
        Text(
            text = word.text,
            style = style,
            modifier = modifier,
            maxLines = 1,
            softWrap = false,
        )
    }
}
