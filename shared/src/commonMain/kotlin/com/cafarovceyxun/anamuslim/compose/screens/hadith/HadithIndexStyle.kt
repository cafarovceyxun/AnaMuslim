package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheet
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HadithPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_menu
import com.cafarovceyxun.anamuslim.resources.hadithIndexStyle
import com.cafarovceyxun.anamuslim.resources.hadithIndexStyleAccordion
import com.cafarovceyxun.anamuslim.resources.hadithIndexStyleAccordionDesc
import com.cafarovceyxun.anamuslim.resources.hadithIndexStyleBookStrip
import com.cafarovceyxun.anamuslim.resources.hadithIndexStyleBookStripDesc
import com.cafarovceyxun.anamuslim.resources.hadithIndexStylePaged
import com.cafarovceyxun.anamuslim.resources.hadithIndexStylePagedDesc
import com.cafarovceyxun.anamuslim.resources.hadithIndexStyleSingleBook
import com.cafarovceyxun.anamuslim.resources.hadithIndexStyleSingleBookDesc
import com.cafarovceyxun.anamuslim.resources.hadithIndexStyleTree
import com.cafarovceyxun.anamuslim.resources.hadithIndexStyleTreeDesc
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Cildin içindəki mündəricatın (kitab → bab → alt bab) görünüşü.
 *
 * [PAGED] köhnə üç ekranlı axındır; qalanları bütün mündəricatı bir siyahıda göstərir
 * ([HadithVolumeContentsScreen]) və dua siyahısının akkordeon dilini işlədir. Hər variant yalnız
 * mündəricatı dəyişir — hədis oxucusu, axtarış və «davam et» hamısında eynidir.
 *
 * ⚠️ Ayarda enum-un **adı** saxlanılır ([HadithPreferences.INDEX_STYLE]) — sabiti yenidən
 * adlandırmaq istifadəçinin seçimini pilləliyə qaytarar.
 */
enum class HadithIndexStyle(val title: StringResource, val description: StringResource) {
    PAGED(Res.string.hadithIndexStylePaged, Res.string.hadithIndexStylePagedDesc),
    ACCORDION(Res.string.hadithIndexStyleAccordion, Res.string.hadithIndexStyleAccordionDesc),
    SINGLE_BOOK(Res.string.hadithIndexStyleSingleBook, Res.string.hadithIndexStyleSingleBookDesc),
    BOOK_STRIP(Res.string.hadithIndexStyleBookStrip, Res.string.hadithIndexStyleBookStripDesc),
    TREE(Res.string.hadithIndexStyleTree, Res.string.hadithIndexStyleTreeDesc);

    companion object {
        fun fromKey(key: String?): HadithIndexStyle = entries.firstOrNull { it.name == key } ?: PAGED
    }
}

@Composable
fun observeHadithIndexStyle(): HadithIndexStyle =
    HadithIndexStyle.fromKey(HadithPreferences.observeIndexStyle())

/**
 * Mündəricat görünüşünü seçən vərəq — hədis ayarlarındakı «Mündəricat görünüşü» sətri və kitab
 * ekranının bar düyməsi açır.
 *
 * Hər seçim kartdır və yanında həmin görünüşün kiçik eskizi var: adlar («Kitab zolağı», «Kompakt
 * ağac») tək başına nə görəcəyini demir, eskiz isə bir baxışda deyir.
 */
@Composable
fun HadithIndexStyleSheet(isOpen: Boolean, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val selected = observeHadithIndexStyle()

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss,
        icon = Res.drawable.dr_icon_menu,
        title = stringResource(Res.string.hadithIndexStyle),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HadithIndexStyle.entries.forEach { style ->
                IndexStyleOptionCard(
                    style = style,
                    selected = selected == style,
                    onClick = {
                        onDismiss()
                        scope.launch { HadithPreferences.setIndexStyle(style.name) }
                    },
                )
            }
        }
    }
}

@Composable
private fun IndexStyleOptionCard(style: HadithIndexStyle, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) colorScheme.primaryContainer.alpha(0.35f) else colorScheme.surfaceContainerLow
            )
            .border(
                width = if (selected) 1.5.dp else 0.5.dp,
                color = if (selected) colorScheme.primary else colorScheme.outlineVariant.alpha(0.6f),
                shape = shape,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(start = 8.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Seçim bütün kartdadır (`selectable`); düymə yalnız göstəricidir.
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(horizontal = 8.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(style.title),
                style = typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
            )
            Text(
                text = stringResource(style.description),
                style = typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        IndexStyleThumbnail(style, selected)
    }
}

/** Görünüşün 56×76 eskizi — maketdəki kimi, mövzu rəngləri ilə (qaranlıq rejimdə də oxunur). */
@Composable
private fun IndexStyleThumbnail(style: HadithIndexStyle, selected: Boolean) {
    val bar = colorScheme.onSurfaceVariant.alpha(0.28f)
    val soft = colorScheme.onSurfaceVariant.alpha(0.16f)
    val accent = colorScheme.primary.alpha(if (selected) 0.85f else 0.55f)
    val number = colorScheme.tertiary.alpha(0.5f)

    @Composable
    fun Line(height: Int, color: Color, start: Int = 0, width: Float = 1f) {
        Box(
            Modifier
                .padding(start = start.dp)
                .fillMaxWidth(width)
                .height(height.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
    }

    Column(
        modifier = Modifier
            .size(width = 56.dp, height = 76.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colorScheme.surface)
            .border(0.5.dp, colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when (style) {
            HadithIndexStyle.PAGED -> repeat(4) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
                    Spacer(Modifier.width(3.dp))
                    Line(9, bar)
                }
            }

            HadithIndexStyle.ACCORDION -> {
                Line(9, bar)
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(soft).padding(3.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Line(5, bar)
                    Line(4, accent, start = 5)
                    Line(4, accent, start = 5)
                    Line(5, bar)
                }
                Line(9, bar)
            }

            HadithIndexStyle.SINGLE_BOOK -> {
                Line(11, accent)
                repeat(4) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(RoundedCornerShape(1.5.dp)).background(number))
                        Spacer(Modifier.width(3.dp))
                        Line(6, bar)
                    }
                }
            }

            HadithIndexStyle.BOOK_STRIP -> {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box(Modifier.size(width = 15.dp, height = 7.dp).clip(CircleShape).background(accent))
                    Box(Modifier.size(width = 12.dp, height = 7.dp).clip(CircleShape).border(1.dp, bar, CircleShape))
                    Box(Modifier.size(width = 10.dp, height = 7.dp).clip(CircleShape).border(1.dp, bar, CircleShape))
                }
                repeat(3) { Line(10, bar) }
            }

            HadithIndexStyle.TREE -> {
                Line(5, bar.copy(alpha = bar.alpha * 1.6f))
                Line(4, bar, start = 5)
                Line(4, bar, start = 5)
                Line(3, accent, start = 10)
                Line(3, accent, start = 10)
                Line(4, bar, start = 5)
                Line(5, bar.copy(alpha = bar.alpha * 1.6f))
                Line(4, bar, start = 5)
            }
        }
    }
}
