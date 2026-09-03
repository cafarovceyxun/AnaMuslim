package com.cafarovceyxun.anamuslim.compose.screens

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.IndexMenuActions
import com.cafarovceyxun.anamuslim.compose.components.LocalIndexMenuActions
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavContentPaddingWithPlayer
import com.cafarovceyxun.anamuslim.compose.components.common.ReadableWidthColumn
import com.cafarovceyxun.anamuslim.compose.components.homepage.AppUpdateBanner
import com.cafarovceyxun.anamuslim.compose.components.homepage.FeatureStoriesRow
import com.cafarovceyxun.anamuslim.compose.components.homepage.HomeActions
import com.cafarovceyxun.anamuslim.compose.components.homepage.HomeSectionBookmarks
import com.cafarovceyxun.anamuslim.compose.components.homepage.HomeSectionGettingStarted
import com.cafarovceyxun.anamuslim.compose.components.homepage.HomeSectionHadithReadHistory
import com.cafarovceyxun.anamuslim.compose.components.homepage.HomeSectionPrayer
import com.cafarovceyxun.anamuslim.compose.components.homepage.HomeSectionReadHistory
import com.cafarovceyxun.anamuslim.compose.components.homepage.HomeSectionSuggestions
import com.cafarovceyxun.anamuslim.compose.components.homepage.LocalHomeActions
import com.cafarovceyxun.anamuslim.compose.components.homepage.ReorderableHomeSection
import com.cafarovceyxun.anamuslim.compose.components.homepage.rememberHomeReorderState
import com.cafarovceyxun.anamuslim.compose.navigation.MainTab
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HomePreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HomeSection
import com.cafarovceyxun.anamuslim.compose.navigation.TabReselectState
import kotlinx.coroutines.launch

/**
 * The homepage. Both action seams are parameters rather than being built here: on Android they are
 * `Intent` launchers over the hosting Activity, on iOS they are `AppNavHost` destinations — the
 * host decides, the screen stays platform-neutral.
 */
@Composable
fun HomeScreen(
    modifier: Modifier,
    homeActions: HomeActions,
    indexMenuActions: IndexMenuActions,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val layout = HomePreferences.observeLayout()

    // Hoisted out of the `verticalScroll` call below so re-tapping the Home tab can drive it.
    val scrollState = rememberScrollState()

    TabReselectState.OnTabReselect(MainTab.HOME) {
        scope.launch { scrollState.animateScrollTo(0) }
    }

    // Bölmənin üstünə basılı saxlayıb sürükləmək. Buraxanda düzən dərhal yazılır — ekran onsuz da
    // eyni axını müşahidə etdiyi üçün nəticə özü qayıdır.
    val reorder = rememberHomeReorderState { HomePreferences.setLayout(it) }

    // Sürükləmə gedərkən sıra işlək nüsxədən oxunur; qalan vaxt saxlanılan düzəndən.
    val sections = if (reorder.dragging != null) reorder.order else layout

    val autoScrollEdge = with(density) { 96.dp.toPx() }
    val autoScrollStep = with(density) { 14.dp.toPx() }

    // Kənara çatanda səhifə özü sürüşür — uzun ana ekranda kartı ekranın altından yuxarı aparmaq
    // başqa cür mümkün olmazdı.
    LaunchedEffect(reorder.dragging) {
        if (reorder.dragging == null) return@LaunchedEffect

        while (true) {
            withFrameNanos { }

            val delta = reorder.autoScrollDelta(autoScrollStep)
            if (delta != 0f) reorder.scrolled(scrollState.scrollBy(delta))
        }
    }

    CompositionLocalProvider(
        LocalIndexMenuActions provides indexMenuActions,
        LocalHomeActions provides homeActions,
    ) {
        // Başlıq barı yoxdur: səhifə birbaşa hekayə zolağı ilə başlayır. `Scaffold` bar olmayanda
        // status zolağının boşluğunu `paddingValues`-ə özü qoyur, ona görə məzmun kəsikin altında
        // qalmır.
        Scaffold(
            containerColor = Color.Transparent,
            modifier = modifier
        ) { paddingValues ->
            // Görünüş sahəsini sürüşən sütun deyil, onu saxlayan qutu ölçür: sürüşən sütunun öz
            // sərhədləri məzmunla birlikdə hərəkət edir, avto-sürüşmə isə **ekrandakı** kənarı
            // bilməlidir.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .onGloballyPositioned {
                        reorder.setViewport(
                            topInRoot = it.positionInRoot().y,
                            height = it.size.height.toFloat(),
                            edge = autoScrollEdge,
                        )
                    },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        // Sürükləmə gedərkən səhifə jestlə sürüşmür: kart barmağın altındadır, sürüşmə
                        // isə yalnız avto-sürüşmə ilə, proqramla baş verir.
                        .verticalScroll(scrollState, enabled = reorder.dragging == null)
                        .padding(bottom = mainBottomNavContentPaddingWithPlayer()),
                ) {
                    // Capped inside the full-width scroll container: every section header carries a
                    // trailing "see all" chip, and left to the window width those two ended up ~900dp
                    // apart on a 13" iPad. A no-op on phones, where the window is narrower than the cap.
                    ReadableWidthColumn {
                        Column {
                            // Düzəndən kənardadır: bu, məzmun bölməsi deyil, güncəlləmə xəbərdarlığıdır
                            // və onsuz da yalnız yeni buraxılış olanda görünür. Gizlədilə bilsəydi
                            // istifadəçi güncəlləməni səssizcə itirərdi.
                            AppUpdateBanner()

                            // Bölmələrin sırası və görünüşü Ayarlar → «Ana ekranı düzənlə»-dən gəlir.
                            // Hər bölmə onsuz da boş olanda özünü çəkmir; buradakı seçim isə **dolu**
                            // bölməni də gizlədə bilir.
                            sections.forEach { state ->
                                if (!state.visible) return@forEach

                                // `key` olmasa sıra dəyişəndə Compose bölmələri **yerinə görə**
                                // uyğunlaşdırır: yerini dəyişən iki bölmə bir-birinin vəziyyətini
                                // (zolaqların sürüşmə mövqeyi, açılmış hekayə) miras alır.
                                key(state.section) {
                                    ReorderableHomeSection(reorder, state.section, sections) {
                                        when (state.section) {
                                            HomeSection.PRAYER -> HomeSectionPrayer()
                                            // Günün ayəsi/hədisi və əlavə olunmuş funksiyaların hekayə
                                            // zolağı. Ayrıca «Günün Ayəsi» kartı yoxdur — eyni məzmun
                                            // hekayədədir.
                                            HomeSection.STORIES -> FeatureStoriesRow()
                                            HomeSection.READ_HISTORY -> HomeSectionReadHistory()
                                            HomeSection.HADITH_READ_HISTORY -> HomeSectionHadithReadHistory()
                                            HomeSection.BOOKMARKS -> HomeSectionBookmarks()
                                            HomeSection.SUGGESTIONS -> HomeSectionSuggestions()
                                        }
                                    }
                                }
                            }

                            // Last on purpose: it renders only while all of the sections above are
                            // empty, so on a fresh install it is the whole page, and it disappears once
                            // anything is read or saved.
                            HomeSectionGettingStarted()
                        }
                    }
                }
            }
        }
    }
}
