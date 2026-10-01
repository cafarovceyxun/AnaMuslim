package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.RadioItem
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheet
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

/** Mündəricat görünüşünü seçən vərəq — hədis ayarlarındakı «Mündəricat görünüşü» sətri açır. */
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
        Column(modifier = Modifier.padding(12.dp)) {
            HadithIndexStyle.entries.forEach { style ->
                RadioItem(
                    title = style.title,
                    subtitle = style.description,
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
