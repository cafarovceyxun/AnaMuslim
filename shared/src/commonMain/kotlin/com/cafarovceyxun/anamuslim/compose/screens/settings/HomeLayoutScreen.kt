package com.cafarovceyxun.anamuslim.compose.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.IconButton
import com.cafarovceyxun.anamuslim.compose.components.common.readableWidthInset
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavigationOuterHeight
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HomePreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HomeSection
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HomeSectionState
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_down
import com.cafarovceyxun.anamuslim.resources.homeLayoutHint
import com.cafarovceyxun.anamuslim.resources.homeLayoutMoveDown
import com.cafarovceyxun.anamuslim.resources.homeLayoutMoveUp
import com.cafarovceyxun.anamuslim.resources.homeLayoutReset
import com.cafarovceyxun.anamuslim.resources.homeSectionStories
import com.cafarovceyxun.anamuslim.resources.strTitleBookmarks
import com.cafarovceyxun.anamuslim.resources.strTitleHomeLayout
import com.cafarovceyxun.anamuslim.resources.strTitleReadHistory
import com.cafarovceyxun.anamuslim.resources.strTitleReadHistoryHadith
import com.cafarovceyxun.anamuslim.resources.suggestionsTitle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Ana ekranın düzəni: hansı bölmə görünür və hansı sıradadır.
 *
 * Sürüklə-burax əvəzinə **yuxarı/aşağı düymələri**: sürükləmə hər iki platformada sürüşən siyahı
 * ilə jest münaqişəsinə düşür və əlçatanlıq üçün onsuz da düymə lazımdır. Beş bölmə üçün düymə
 * eyni işi görür.
 *
 * Vəziyyət birbaşa [HomePreferences]-dən oxunur və hər dəyişiklikdə oraya yazılır — yerli nüsxə
 * saxlasaydıq ekran bağlananda «yadda saxla» addımı lazım olardı, ana ekran isə eyni axını
 * müşahidə etdiyi üçün dəyişiklik onsuz da dərhal görünür.
 */
@Composable
fun HomeLayoutScreen() {
    val scope = rememberCoroutineScope()
    val layout = HomePreferences.observeLayout()

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            AppBar(
                title = stringResource(Res.string.strTitleHomeLayout),
                actions = {
                    TextButton(onClick = { scope.launch { HomePreferences.resetLayout() } }) {
                        Text(stringResource(Res.string.homeLayoutReset))
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                // Geniş ekranda siyahı bütün enə yayılmasın — ayarların qalan ekranları ilə eyni.
                start = readableWidthInset(),
                end = readableWidthInset(),
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + mainBottomNavigationOuterHeight() + 16.dp,
            ),
        ) {
            item {
                Text(
                    text = stringResource(Res.string.homeLayoutHint),
                    style = typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }

            items(layout, key = { it.section.key }) { state ->
                val index = layout.indexOfFirst { it.section == state.section }

                SectionRow(
                    title = stringResource(state.section.titleRes()),
                    visible = state.visible,
                    canMoveUp = index > 0,
                    canMoveDown = index < layout.lastIndex,
                    onMoveUp = { scope.launch { HomePreferences.setLayout(layout.moved(index, -1)) } },
                    onMoveDown = { scope.launch { HomePreferences.setLayout(layout.moved(index, 1)) } },
                    onVisibleChange = { visible ->
                        scope.launch {
                            HomePreferences.setLayout(
                                layout.mapIndexed { i, item ->
                                    if (i == index) item.copy(visible = visible) else item
                                }
                            )
                        }
                    },
                )
            }
        }
    }
}

/** Elementi [delta] qədər sürüşdürür; siyahının kənarından çıxan hərəkət heç nə etmir. */
private fun List<HomeSectionState>.moved(index: Int, delta: Int): List<HomeSectionState> {
    val target = index + delta
    if (index !in indices || target !in indices) return this

    return toMutableList().apply { add(target, removeAt(index)) }
}

private fun HomeSection.titleRes(): StringResource = when (this) {
    HomeSection.STORIES -> Res.string.homeSectionStories
    HomeSection.READ_HISTORY -> Res.string.strTitleReadHistory
    HomeSection.HADITH_READ_HISTORY -> Res.string.strTitleReadHistoryHadith
    HomeSection.BOOKMARKS -> Res.string.strTitleBookmarks
    HomeSection.SUGGESTIONS -> Res.string.suggestionsTitle
}

@Composable
private fun SectionRow(
    title: String,
    visible: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onVisibleChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(shapes.medium)
            .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            // Yuxarı ox ayrıca ikon deyil: eyni şevron 180° çevrilir — layihədə açılıb-yığılan
            // başlıqlar da bu üsulla işləyir, ona görə yeni resurs əlavə etmirik.
            Box(modifier = Modifier.rotate(180f)) {
                IconButton(
                    painter = painterResource(Res.drawable.dr_icon_chevron_down),
                    contentDescription = stringResource(Res.string.homeLayoutMoveUp),
                    enabled = canMoveUp,
                    small = true,
                    onClick = onMoveUp,
                )
            }

            IconButton(
                painter = painterResource(Res.drawable.dr_icon_chevron_down),
                contentDescription = stringResource(Res.string.homeLayoutMoveDown),
                enabled = canMoveDown,
                small = true,
                onClick = onMoveDown,
            )
        }

        Text(
            text = title,
            style = typography.bodyLarge,
            color = if (visible) colorScheme.onSurface else colorScheme.onSurface.alpha(0.45f),
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )

        Switch(checked = visible, onCheckedChange = onVisibleChange)
    }
}
