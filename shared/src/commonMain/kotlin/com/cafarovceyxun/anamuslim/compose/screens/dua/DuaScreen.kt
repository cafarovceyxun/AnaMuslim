package com.cafarovceyxun.anamuslim.compose.screens.dua

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.dialogs.SimpleTooltip
import com.cafarovceyxun.anamuslim.compose.components.common.FloatingTitlePill
import com.cafarovceyxun.anamuslim.compose.components.common.ModeTabStrip
import com.cafarovceyxun.anamuslim.compose.components.reader.PageTurnAnimation
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderTextZoom
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderZoomFeedback
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderZoomFeedbackOverlay
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderZoomTarget
import com.cafarovceyxun.anamuslim.compose.components.reader.pageTurnEffect
import com.cafarovceyxun.anamuslim.compose.components.reader.readerTextZoom
import com.cafarovceyxun.anamuslim.compose.components.common.ReadableWidthColumn
import com.cafarovceyxun.anamuslim.compose.components.common.readableWidthInset
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.BookmarkNoteSheet
import com.cafarovceyxun.anamuslim.repository.BookmarkAddResult
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.resources.ic_bookmark
import com.cafarovceyxun.anamuslim.resources.ic_bookmark_added
import com.cafarovceyxun.anamuslim.resources.strLabelBookmark
import com.cafarovceyxun.anamuslim.resources.strLabelRemove
import com.cafarovceyxun.anamuslim.resources.strMsgBookmarkAddedAlready
import com.cafarovceyxun.anamuslim.resources.strMsgDuaBookmarkAdded
import com.cafarovceyxun.anamuslim.resources.strMsgDuaBookmarkAddFailed
import com.cafarovceyxun.anamuslim.resources.strMsgDuaBookmarkRemoved
import com.cafarovceyxun.anamuslim.resources.strTitleBookmarkDeleteThis
import org.jetbrains.compose.resources.getString
import com.cafarovceyxun.anamuslim.resources.dr_icon_check
import com.cafarovceyxun.anamuslim.resources.duaCompletedBadge
import com.cafarovceyxun.anamuslim.db.entities.user.DuaReadHistoryEntity
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.compose.screens.hadith.FormTextField
import com.cafarovceyxun.anamuslim.compose.screens.hadith.HadithSearchNavBar
import com.cafarovceyxun.anamuslim.compose.screens.hadith.withScriptDirection
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.theme.arabicFontFamily
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.compose.utils.app.KeepScreenOnIfEnabled
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.DuaPreferences
import com.cafarovceyxun.anamuslim.utils.text.withSearchHighlight
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_left
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_right
import com.cafarovceyxun.anamuslim.resources.dr_icon_delete
import com.cafarovceyxun.anamuslim.resources.dr_icon_edit
import com.cafarovceyxun.anamuslim.resources.dr_logo_dua
import com.cafarovceyxun.anamuslim.resources.dr_icon_open
import com.cafarovceyxun.anamuslim.resources.dr_icon_translations
import com.cafarovceyxun.anamuslim.resources.dr_icon_quran_script
import com.cafarovceyxun.anamuslim.resources.dr_icon_share
import com.cafarovceyxun.anamuslim.resources.dr_icon_settings
import com.cafarovceyxun.anamuslim.resources.strTitleReaderSettings
import com.cafarovceyxun.anamuslim.resources.dr_icon_sort
import com.cafarovceyxun.anamuslim.resources.copiedToClipboard
import com.cafarovceyxun.anamuslim.resources.duaCountBadge
import com.cafarovceyxun.anamuslim.resources.duaCountLabel
import com.cafarovceyxun.anamuslim.resources.duaDeleteCategoryConfirm
import com.cafarovceyxun.anamuslim.resources.duaDeleteConfirmTitle
import com.cafarovceyxun.anamuslim.resources.duaDeleteDuaConfirm
import com.cafarovceyxun.anamuslim.resources.duaEmptyBody
import com.cafarovceyxun.anamuslim.resources.duaEmptyCategory
import com.cafarovceyxun.anamuslim.resources.duaEmptyTitle
import com.cafarovceyxun.anamuslim.resources.duaNextPage
import com.cafarovceyxun.anamuslim.resources.duaMergeNextPart
import com.cafarovceyxun.anamuslim.resources.duaOpenSource
import com.cafarovceyxun.anamuslim.resources.strLabelResumeReading
import com.cafarovceyxun.anamuslim.resources.dr_icon_history
import com.cafarovceyxun.anamuslim.resources.duaSplitLastPart
import com.cafarovceyxun.anamuslim.resources.dr_icon_add
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.duaPageIndicator
import com.cafarovceyxun.anamuslim.resources.duaPreviousPage
import com.cafarovceyxun.anamuslim.resources.duaAddCategory
import com.cafarovceyxun.anamuslim.resources.duaAddSubcategory
import com.cafarovceyxun.anamuslim.resources.duaDeleteSubcategoryConfirm
import com.cafarovceyxun.anamuslim.resources.duaDirectDuas
import com.cafarovceyxun.anamuslim.resources.duaLongPressHint
import com.cafarovceyxun.anamuslim.resources.duaRenameAction
import com.cafarovceyxun.anamuslim.resources.duaTitleOptions
import com.cafarovceyxun.anamuslim.resources.duaPickerNewTitleName
import com.cafarovceyxun.anamuslim.resources.duaPickerNewTitleNameAr
import com.cafarovceyxun.anamuslim.resources.duaPickerSave
import com.cafarovceyxun.anamuslim.resources.duaTransliterationLabel
import com.cafarovceyxun.anamuslim.resources.duaSectionTitle
import com.cafarovceyxun.anamuslim.resources.duaSortCategories
import com.cafarovceyxun.anamuslim.resources.duaSortDuas
import com.cafarovceyxun.anamuslim.resources.duaSortSubcategories
import com.cafarovceyxun.anamuslim.resources.icon_copy
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelCopy
import com.cafarovceyxun.anamuslim.resources.strLabelDelete
import com.cafarovceyxun.anamuslim.resources.strLabelEdit
import com.cafarovceyxun.anamuslim.resources.strLabelShare
import com.cafarovceyxun.anamuslim.utils.supabase.Dua
import com.cafarovceyxun.anamuslim.utils.supabase.DuaCategory
import com.cafarovceyxun.anamuslim.utils.supabase.DuaSubcategory
import com.cafarovceyxun.anamuslim.utils.supabase.DuaSourceRef
import com.cafarovceyxun.anamuslim.viewModels.AuthViewModel
import com.cafarovceyxun.anamuslim.viewModels.DuaViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * «Dua və zikr» bölməsi — **üç səviyyə**: başlıq → alt başlıq → dua.
 *
 * Alt başlıq **məcburi deyil**: başlığın birbaşa altında da dua ola bilər, o halda həmin dualar
 * alt başlıqların yanında ayrıca sətirdə toplanır (hədis ağacındakı «alt babsız bab» ilə eyni
 * qayda). Alt başlığı olmayan başlıq açılanda isə birbaşa vərəqləyiciyə keçilir — istifadəçini boş
 * bir aralıq siyahıdan keçirmək mənasızdır.
 *
 * Səviyyələr bir ekranın içindədir (naviqasiya route-u yoxdur): açılış ana ekrandan tam ekran
 * `Dialog` kimi gəlir, geri jesti bir səviyyə yuxarı, sonra çölə aparır — `HadithIndexScreen`-in öz
 * səviyyələrində gəzməsi ilə eyni forma.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DuaScreen(
    onBack: () -> Unit,
    /**
     * Açılışda birbaşa göstəriləcək dua — əlfəcinlər ekranından gələn giriş nöqtəsi.
     *
     * `null` = adi giriş (başlıqlar siyahısı). Id tapılmasa (dua silinib və ya hələ yüklənməyib)
     * ekran **sakitcə** başlıqlar siyahısında qalır: əlfəcin köhnəldi deyə boş ekran göstərmək
     * daha pisdir.
     */
    initialDuaId: Long? = null,
) {
    // ⚠️ Ad qəsdən `viewModel` deyil: yerli dəyişən `viewModel { … }` funksiyasını kölgələyir və
    // ondan sonrakı hər ViewModel qurğusu kompilyasiya olunmur.
    val duaViewModel = viewModel { DuaViewModel() }
    val authViewModel = viewModel { AuthViewModel() }

    val categories by duaViewModel.categories.collectAsStateWithLifecycle()
    val subcategories by duaViewModel.subcategories.collectAsStateWithLifecycle()
    val duas by duaViewModel.duas.collectAsStateWithLifecycle()
    val isLoaded by duaViewModel.isLoaded.collectAsStateWithLifecycle()
    val isSaving by duaViewModel.isLoading.collectAsStateWithLifecycle()
    val revision by duaViewModel.revision.collectAsStateWithLifecycle()

    val session by authViewModel.session.collectAsStateWithLifecycle()
    val isAuthorized = session != null

    // Başqa instansiyada (oxucudakı seçim ekranı) yazılan dua bu siyahıya elə buradan çatır —
    // bax `DuaViewModel`-in yanındakı sayğac.
    LaunchedEffect(revision) { if (revision > 0) duaViewModel.refresh() }

    var openedCategorySlug by remember { mutableStateOf<String?>(null) }
    var openedSubcategorySlug by remember { mutableStateOf<String?>(null) }
    /** `true` → başlığın **birbaşa** altındakı dualar açılıb (alt başlıqsızlar). */
    var openedDirect by remember { mutableStateOf(false) }

    /**
     * Hələ açılmamış giriş nöqtəsi ([initialDuaId]).
     *
     * Dualar şəbəkədən/keşdən sonradan gəlir, ona görə hədəf ilk kompozisiyada tapılmaya bilər —
     * effekt siyahı gələndə yenidən işləyir. Dəyişən vərəqləyicidən **geri qayıdanda** sıfırlanır:
     * o vaxta qədər lazımdır, çünki açılış səhifəsini (`initialIndex`) elə o təyin edir.
     */
    var pendingDuaId by remember { mutableStateOf(initialDuaId) }

    val userRepository = remember { RepositoryProvider.userRepository }

    // Bitmiş mövzular və son mövqe — hər ikisi bazadan axınla gəlir, ona görə vərəqləyicidə
    // işarələnən mövzu siyahıya **dərhal** düşür.
    val completedGroups by userRepository.getDuaReadProgressFlow()
        .collectAsStateWithLifecycle(emptyList())
    val lastRead by userRepository.getLatestDuaReadFlow().collectAsStateWithLifecycle(null)

    val completedKeys = remember(completedGroups) { completedGroups.map { it.groupKey }.toSet() }

    LaunchedEffect(duas, pendingDuaId) {
        val target = pendingDuaId?.let { id -> duas.firstOrNull { it.id == id } } ?: return@LaunchedEffect

        openedCategorySlug = target.category_slug
        openedSubcategorySlug = target.subcategory_slug
        // Alt başlığı olmayan dua başlığın **birbaşa** altındadır; bu bayraq olmasa aralıq siyahı
        // açılır və istifadəçi əlfəcindən gələn duanı yenidən əl ilə tapmalı olur.
        openedDirect = target.subcategory_slug == null
    }

    var renaming by remember { mutableStateOf<RenameTarget?>(null) }
    var pendingCategoryDelete by remember { mutableStateOf<DuaCategory?>(null) }
    var pendingSubcategoryDelete by remember { mutableStateOf<DuaSubcategory?>(null) }
    var options by remember { mutableStateOf<RenameTarget?>(null) }
    var sorting by remember { mutableStateOf<SortTarget?>(null) }

    val openedCategory = categories.firstOrNull { it.slug == openedCategorySlug }
    val openedSubcategory = subcategories.firstOrNull { it.slug == openedSubcategorySlug }

    val categorySubs = remember(subcategories, openedCategorySlug) {
        subcategories.filter { it.category_slug == openedCategorySlug }
    }

    BackHandler(enabled = openedCategorySlug != null && sorting == null) {
        // Bax vərəqləyicinin `onBack`-i: birbaşa girişdən gələn istifadəçi ağacın ortasına
        // düşməməlidir, ona görə jest də ekranı bütövlükdə bağlayır.
        if (pendingDuaId != null) {
            pendingDuaId = null
            onBack()
        } else {
            when {
                openedSubcategorySlug != null -> openedSubcategorySlug = null
                openedDirect -> openedDirect = false
                else -> openedCategorySlug = null
            }
        }
    }

    // Sıralama rejimində geri jesti **yalnız** rejimi bağlayır: səviyyə dəyişsəydi yarımçıq sıra
    // ilə birlikdə ekran da altdan sürüşərdi.
    BackHandler(enabled = sorting != null) { sorting = null }

    // ---- sıralama rejimi: hər üç səviyyə eyni ekrandan keçir
    when (val target = sorting) {
        null -> Unit

        SortTarget.Categories -> {
            DuaReorderScreen(
                title = stringResource(Res.string.duaSortCategories),
                rows = categories.map { category ->
                    ReorderRow(
                        key = category.slug,
                        title = category.name,
                        subtitle = stringResource(
                            Res.string.duaCountLabel,
                            duas.count { it.category_slug == category.slug },
                        ),
                        arabic = category.name_ar,
                    )
                },
                isSaving = isSaving,
                onSave = { order -> duaViewModel.saveCategoryOrder(order) { sorting = null } },
                onCancel = { sorting = null },
            )
            return
        }

        is SortTarget.Subcategories -> {
            DuaReorderScreen(
                title = stringResource(Res.string.duaSortSubcategories),
                rows = subcategories
                    .filter { it.category_slug == target.categorySlug }
                    .map { subcategory ->
                        ReorderRow(
                            key = subcategory.slug,
                            title = subcategory.name,
                            subtitle = stringResource(
                                Res.string.duaCountLabel,
                                duas.count { it.subcategory_slug == subcategory.slug },
                            ),
                            arabic = subcategory.name_ar,
                        )
                    },
                isSaving = isSaving,
                onSave = { order -> duaViewModel.saveSubcategoryOrder(order) { sorting = null } },
                onCancel = { sorting = null },
            )
            return
        }

        is SortTarget.Duas -> {
            // Sətirlər cari siyahıdan yenidən qurulur, rejimə girəndəki surətdən yox: dua silinsə
            // və ya mətni dəyişsə köhnə surət ekranda «kölgə sətir» kimi qalardı.
            val byId = remember(duas) { duas.mapNotNull { dua -> dua.id?.let { it to dua } }.toMap() }

            DuaReorderScreen(
                title = stringResource(Res.string.duaSortDuas),
                rows = target.ids.mapNotNull { id ->
                    byId[id]?.let { dua ->
                        ReorderRow(
                            key = id.toString(),
                            title = dua.text_az.takeIf { it.isNotBlank() }
                                ?: dua.transliteration?.takeIf { it.isNotBlank() }
                                ?: dua.text_ar,
                            subtitle = dua.source?.takeIf { it.isNotBlank() },
                            arabic = dua.text_ar,
                        )
                    }
                },
                isSaving = isSaving,
                onSave = { order ->
                    duaViewModel.saveDuaOrder(order.mapNotNull { it.toLongOrNull() }) {
                        sorting = null
                    }
                },
                onCancel = { sorting = null },
            )
            return
        }
    }

    // ---- 3-cü səviyyə: vərəqləyici
    val pagerDuas: List<Dua>? = when {
        openedCategory == null -> null
        openedSubcategory != null ->
            duas.filter { it.subcategory_slug == openedSubcategory.slug }

        openedDirect ->
            duas.filter { it.category_slug == openedCategory.slug && it.subcategory_slug == null }

        // Alt başlığı olmayan başlıq aralıq siyahı göstərmir.
        categorySubs.isEmpty() ->
            duas.filter { it.category_slug == openedCategory.slug }

        else -> null
    }

    if (openedCategory != null && pagerDuas != null) {
        // ⚠️ Vərəqləyici artıq **bütün** dualar üzərindədir, açılan qrupun süzgəci üzərində yox:
        // mövzu sərhədini keçmək üçün geri qayıdıb siyahıdan seçmək lazım gəlmir. Açılış mövqeyi
        // isə həmin qrupun ilk səhifəsidir, yəni istifadəçi üçün giriş nöqtəsi dəyişmir.
        val flatEntries = remember(categories, subcategories, duas) {
            flattenDuas(categories, subcategories, duas)
        }
        val openedGroupKey = openedSubcategory?.slug ?: openedCategory.slug

        if (flatEntries.isNotEmpty()) {
            DuaPagerScreen(
                entries = flatEntries,
                // Əlfəcindən gələndə açılış səhifəsi **həmin duadır**, qrupun ilk səhifəsi yox.
                // Hissə id-si də tapılır: əlfəcin/dərin link duanın **hər hansı** hissəsini
                // göstərə bilər, səhifə isə bütöv duadır.
                initialIndex = pendingDuaId
                    ?.let { id ->
                        flatEntries
                            .indexOfFirst { entry -> entry.parts.any { it.id == id } }
                            .takeIf { it >= 0 }
                    }
                    ?: indexOfGroup(flatEntries, openedGroupKey),
                isAuthorized = isAuthorized,
                isSaving = isSaving,
                // Sıralamağa bir dua bəs etmir; düymə görünüb heç nə etməməkdənsə ümumiyyətlə
                // çıxmasın. Sıralanan dəst **cari səhifənin qrupudur**, bütün siyahı yox.
                onSort = if (isAuthorized) {
                    { groupKey ->
                        val ids = groupDuaIds(flatEntries, groupKey)
                        if (ids.size > 1) sorting = SortTarget.Duas(ids)
                    }
                } else {
                    null
                },
                onDelete = { id -> duaViewModel.deleteDua(id) },
                onEdit = { updated -> duaViewModel.updateDua(updated) },
                onSetPart = if (isAuthorized) {
                    { duaId, partOfId, partNo -> duaViewModel.setDuaPart(duaId, partOfId, partNo) }
                } else {
                    null
                },
                onBack = {
                    if (pendingDuaId != null) {
                        // Əlfəcindən (və ya başqa birbaşa girişdən) gəlmişik: istifadəçi dua
                        // ağacından keçməyib, ona görə «geri» onu ağacın ortasına salmamalı, **çağıran
                        // ekrana** qaytarmalıdır. Sayğac burada sıfırlanır ki, ekran yenidən
                        // açılanda adi rejimdə işləsin.
                        pendingDuaId = null
                        onBack()
                    } else {
                        when {
                            openedSubcategorySlug != null -> openedSubcategorySlug = null
                            openedDirect -> openedDirect = false
                            else -> openedCategorySlug = null
                        }
                    }
                },
            )
            return
        }
    }

    // ---- 2-ci səviyyə: alt başlıqlar
    if (openedCategory != null) {
        val directCount = duas.count {
            it.category_slug == openedCategory.slug && it.subcategory_slug == null
        }

        DuaSubcategoryScreen(
            category = openedCategory,
            subcategories = categorySubs,
            completedKeys = completedKeys,
            countOf = { slug -> duas.count { it.subcategory_slug == slug } },
            directCount = directCount,
            isAuthorized = isAuthorized,
            onOpenSubcategory = { openedSubcategorySlug = it.slug },
            onOpenDirect = { openedDirect = true },
            onLongPressSubcategory = { options = RenameTarget.Subcategory(it) },
            onAdd = { name -> duaViewModel.addSubcategory(openedCategory.slug, name, null) },
            onBack = { openedCategorySlug = null },
        )

        TitleOptionsDialogs(
            options = options,
            renaming = renaming,
            pendingCategoryDelete = pendingCategoryDelete,
            pendingSubcategoryDelete = pendingSubcategoryDelete,
            isSaving = isSaving,
            sortLabel = stringResource(Res.string.duaSortSubcategories),
            onOptionsDismiss = { options = null },
            onRenameRequest = { renaming = it; options = null },
            onSortRequest = {
                options = null
                sorting = SortTarget.Subcategories(openedCategory.slug)
            },
            onDeleteRequest = {
                when (it) {
                    // «Yeni başlıq» uzun basma menyusundan gəlmir, ona görə silinəsi də yoxdur.
                    RenameTarget.NewCategory -> Unit
                    is RenameTarget.Category -> pendingCategoryDelete = it.category
                    is RenameTarget.Subcategory -> pendingSubcategoryDelete = it.subcategory
                }
                options = null
            },
            onRenameDismiss = { renaming = null },
            onRename = { target, name, nameAr ->
                renaming = null
                when (target) {
                    // Bu səviyyədə «yeni başlıq» yolu yoxdur — o, 1-ci səviyyənin düyməsidir.
                    RenameTarget.NewCategory -> Unit
                    is RenameTarget.Category ->
                        duaViewModel.renameCategory(target.category.slug, name, nameAr)

                    is RenameTarget.Subcategory ->
                        duaViewModel.renameSubcategory(target.subcategory.slug, name, nameAr)
                }
            },
            onCategoryDeleteDismiss = { pendingCategoryDelete = null },
            onSubcategoryDeleteDismiss = { pendingSubcategoryDelete = null },
            onCategoryDelete = {
                pendingCategoryDelete = null
                openedCategorySlug = null
                duaViewModel.deleteCategory(it.slug)
            },
            onSubcategoryDelete = {
                pendingSubcategoryDelete = null
                duaViewModel.deleteSubcategory(it.slug)
            },
        )
        return
    }

    // ---- 1-ci səviyyə: başlıqlar
    val allEntries = remember(categories, subcategories, duas) {
        flattenDuas(categories, subcategories, duas)
    }
    // Son qalınan duanın sətri — hissələr də nəzərə alınır (tarixçə hər hansı hissəni göstərə
    // bilər, sətir isə bütöv duadır).
    val lastReadEntry = remember(allEntries, lastRead) {
        lastRead?.let { entry ->
            allEntries.firstOrNull { row -> row.parts.any { it.id == entry.duaId } }
        }
    }

    val completedCategories = remember(allEntries, completedKeys) {
        completedDuaCategories(allEntries, completedKeys)
    }

    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(Res.string.duaSectionTitle),
                onBack = onBack,
                actions = {
                    if (isAuthorized) {
                        IconButton(onClick = { renaming = RenameTarget.NewCategory }) {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_edit),
                                contentDescription = stringResource(Res.string.duaAddCategory),
                                tint = colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                categories.isEmpty() && !isLoaded -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(28.dp),
                    color = colorScheme.primary,
                )

                categories.isEmpty() -> DuaEmptyState(
                    title = stringResource(Res.string.duaEmptyTitle),
                    body = stringResource(Res.string.duaEmptyBody),
                    modifier = Modifier.align(Alignment.Center),
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // Bax `AsmaScreen`: lazy siyahıda oxunaqlı en `contentPadding`-dən gəlir.
                    contentPadding = PaddingValues(
                        horizontal = 16.dp + readableWidthInset(),
                        vertical = 12.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (isAuthorized) {
                        item(key = "hint") {
                            Text(
                                text = stringResource(Res.string.duaLongPressHint),
                                style = typography.labelSmall.withScriptDirection(arabic = false),
                                color = colorScheme.onSurfaceVariant.alpha(0.7f),
                                modifier = Modifier.padding(bottom = 2.dp),
                            )
                        }
                    }

                    items(categories, key = { it.slug }) { category ->
                        DuaTitleCard(
                            name = category.name,
                            nameAr = category.name_ar,
                            isCompleted = category.slug in completedCategories,
                            // «Oxumağa davam et» sətrin **içindəki balaca saat nişanıdır** —
                            // Quran və hədis siyahılarındakı ilə eyni (əvvəl siyahının başında
                            // ayrıca böyük kart idi). Nişan yalnız son mövqe **hələ də mövcud**
                            // duaya işarə edəndə çıxır: silinmiş duada toxunuş heç nə etməzdi.
                            onContinueClick = lastReadEntry
                                ?.takeIf { it.category.slug == category.slug }
                                ?.let { { pendingDuaId = lastRead?.duaId } },
                            caption = stringResource(
                                Res.string.duaCountLabel,
                                duas.count { it.category_slug == category.slug },
                            ),
                            onClick = {
                                openedCategorySlug = category.slug
                                openedSubcategorySlug = null
                                openedDirect = false
                            },
                            // Uzun basma yalnız səlahiyyətli istifadəçidə nəsə edir; hər kəsdə
                            // aktiv olsaydı jest boş vədə çevrilərdi.
                            onLongClick = if (isAuthorized) {
                                { options = RenameTarget.Category(category) }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
    }

    TitleOptionsDialogs(
        options = options,
        renaming = renaming,
        pendingCategoryDelete = pendingCategoryDelete,
        pendingSubcategoryDelete = pendingSubcategoryDelete,
        isSaving = isSaving,
        sortLabel = stringResource(Res.string.duaSortCategories),
        onOptionsDismiss = { options = null },
        onRenameRequest = { renaming = it; options = null },
        onSortRequest = {
            options = null
            sorting = SortTarget.Categories
        },
        onDeleteRequest = {
            when (it) {
                RenameTarget.NewCategory -> Unit
                is RenameTarget.Category -> pendingCategoryDelete = it.category
                is RenameTarget.Subcategory -> pendingSubcategoryDelete = it.subcategory
            }
            options = null
        },
        onRenameDismiss = { renaming = null },
        onRename = { target, name, nameAr ->
            renaming = null
            when (target) {
                RenameTarget.NewCategory -> duaViewModel.addCategory(name, nameAr)
                is RenameTarget.Category ->
                    duaViewModel.renameCategory(target.category.slug, name, nameAr)

                is RenameTarget.Subcategory ->
                    duaViewModel.renameSubcategory(target.subcategory.slug, name, nameAr)
            }
        },
        onCategoryDeleteDismiss = { pendingCategoryDelete = null },
        onSubcategoryDeleteDismiss = { pendingSubcategoryDelete = null },
        onCategoryDelete = {
            pendingCategoryDelete = null
            duaViewModel.deleteCategory(it.slug)
        },
        onSubcategoryDelete = {
            pendingSubcategoryDelete = null
            duaViewModel.deleteSubcategory(it.slug)
        },
    )
}

/**
 * Sıralama rejiminin hədəfi — hansı siyahı sıralanır.
 *
 * Dualar **id-lərlə** saxlanılır, obyektlərlə yox: rejim açıq ikən siyahı yenilənə bilər (başqa
 * cihazdan gələn dəyişiklik, silinmiş dua) və köhnə surət ekranda artıq olmayan sətri göstərərdi.
 */
private sealed interface SortTarget {
    data object Categories : SortTarget
    data class Subcategories(val categorySlug: String) : SortTarget
    data class Duas(val ids: List<Long>) : SortTarget
}

/** Uzun basma menyusunun və ad formasının hədəfi. */
internal sealed interface RenameTarget {
    /** Yeni başlıq — siyahı ekranındakı «+» düyməsi. */
    data object NewCategory : RenameTarget
    data class Category(val category: DuaCategory) : RenameTarget
    data class Subcategory(val subcategory: DuaSubcategory) : RenameTarget
}

/**
 * Başlıq/alt başlıq kartı — nişan, ad, alt yazı, ərəbcə qarşılığı.
 *
 * Hər iki səviyyə eyni kartdan istifadə edir: fərq yalnız alt yazıdadır («12 dua», «3 alt başlıq»).
 */
@Composable
private fun DuaTitleCard(
    name: String,
    nameAr: String?,
    caption: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    icon: DrawableResource = Res.drawable.dr_logo_dua,
    /** Mövzu (və ya başlığın bütün mövzuları) oxunub bitib — sətirdə ✓ görünür. */
    isCompleted: Boolean = false,
    /**
     * Son qalınan dua **bu sətrin içindədir** — sətirdə balaca saat nişanı çıxır və basanda oxucu
     * həmin duada açılır (sətrin özü həmişə əvvəldən açır).
     *
     * `null` = nişan yoxdur. Görkəm Quran və hədis siyahılarındakı ilə eynidir (`ChapterCard`,
     * `HadithEntryCard`): üç bölmədə eyni şey eyni cür görünsün deyə.
     */
    onContinueClick: (() -> Unit)? = null,
) {
    Surface(
        color = colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, colorScheme.outlineVariant.alpha(0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(colorScheme.primaryContainer.alpha(0.45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        .withScriptDirection(arabic = false),
                    color = colorScheme.onSurface,
                )
                Text(
                    text = caption,
                    style = typography.labelSmall.withScriptDirection(arabic = false),
                    color = colorScheme.onSurfaceVariant.alpha(0.75f),
                )
            }

            nameAr?.takeIf { it.isNotBlank() }?.let { arabic ->
                Text(
                    text = arabic,
                    style = typography.titleMedium.withScriptDirection(
                        arabic = true,
                        arabicFontFamily = arabicFontFamily(),
                    ),
                    color = colorScheme.onSurface.alpha(0.85f),
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }

            onContinueClick?.let { onContinue ->
                val resumeLabel = stringResource(Res.string.strLabelResumeReading)

                SimpleTooltip(text = resumeLabel) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colorScheme.primaryContainer.alpha(0.45f))
                            .clickable(onClick = onContinue),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.dr_icon_history),
                            contentDescription = resumeLabel,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Spacer(Modifier.width(6.dp))
            }

            if (isCompleted) {
                // ✓ **oxun qarşısındadır**, onu əvəz etmir: sətir yenə də açılır, nişan isə yalnız
                // «bunu bitirmisən» deyir.
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_check),
                    contentDescription = stringResource(Res.string.duaCompletedBadge),
                    tint = colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
            }

            Icon(
                painter = painterResource(Res.drawable.dr_icon_chevron_right),
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant.alpha(0.5f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * **Bütün** dualar — səhifə-səhifə, sağa-sola sürüşdürməklə, mövzu sərhədlərini keçərək.
 *
 * Siyahı [flattenDuas] ilə yastılanıb: əvvəl vərəqləyici bir qrupun içində qalırdı və növbəti
 * mövzuya keçmək üçün geri qayıtmaq lazım gəlirdi. İndi sürüşdürmə fasiləsizdir, mövzu dəyişəndə
 * isə başlıq canlı yenilənir və səhifənin başında mövzu adı görünür.
 *
 * `HorizontalPager` istiqaməti mövzunun `LocalLayoutDirection`-ından alır, ona görə ərəbcə
 * interfeysdə «növbəti» təbii olaraq sola gedir; oxlar da elə pager-in öz `animateScrollToPage`-i
 * ilə işləyir, yəni iki idarə üsulu bir yerdən keçir.
 *
 * @param onSort cari səhifənin **qrup açarını** alır — sıralanan dəst ekrandakı bütün siyahı yox,
 *   həmin mövzunun dualarıdır.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DuaPagerScreen(
    entries: List<DuaFlatEntry>,
    initialIndex: Int,
    isAuthorized: Boolean,
    isSaving: Boolean,
    onSort: ((groupKey: String) -> Unit)?,
    onDelete: (Long) -> Unit,
    onEdit: (Dua) -> Unit,
    /** Hissə bağlantısını dəyişir (admin); `null` = səlahiyyət yoxdur. */
    onSetPart: ((duaId: Long, partOfId: Long?, partNo: Int) -> Unit)?,
    onBack: () -> Unit,
) {
    val safeInitialIndex = initialIndex.coerceIn(0, (entries.size - 1).coerceAtLeast(0))

    val pagerState = rememberPagerState(
        initialPage = safeInitialIndex,
        pageCount = { entries.size },
    )
    // Axan rejimlərin (ərəbcə / tərcümə) sürüşmə vəziyyəti. Vərəqləyici ilə **yanaşı** yaşayır:
    // rejim dəyişəndə mövqe bir-birinə köçürülür, yoxsa istifadəçi eyni duada qalmır.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = safeInitialIndex)
    val scope = rememberCoroutineScope()

    val viewMode = DuaPreferences.observeViewMode()

    // «Ərəbcə» və «Tərcümə» axan siyahıdır (fasiləsiz aşağı-yuxarı sürüşmə), «Qarışıq» isə
    // vərəqləyici qalır — hədis oxucusundakı bölgünün eynisi (orada da mod 0 vərəqləyici,
    // 1/2 axan siyahıdır).
    val isFlowing = viewMode != DUA_MODE_MIXED

    // Ekranın hər yerində «hansı duadayıq» sualının **tək** cavabı. Rejimdən asılı olaraq iki
    // fərqli sürüşmə vəziyyətindən oxunur; `derivedStateOf` olmadan hər kadrda yenidən qurulardı.
    val currentIndex by remember(isFlowing) {
        derivedStateOf {
            if (isFlowing) listState.firstVisibleItemIndex else pagerState.currentPage
        }
    }

    // Rejim dəyişəndə mövqe o biri vəziyyətə köçürülür.
    LaunchedEffect(isFlowing) {
        if (entries.isEmpty()) return@LaunchedEffect
        if (isFlowing) {
            listState.scrollToItem(pagerState.currentPage)
        } else {
            pagerState.scrollToPage(listState.firstVisibleItemIndex)
        }
    }

    /** Hədəfə aparır — hansı rejimdə olduğumuzdan asılı olmayaraq. */
    val goTo: suspend (index: Int, animate: Boolean) -> Unit = { index, animate ->
        val target = index.coerceIn(0, (entries.size - 1).coerceAtLeast(0))
        when {
            isFlowing && animate -> listState.animateScrollToItem(target)
            isFlowing -> listState.scrollToItem(target)
            animate -> pagerState.animateScrollToPage(target)
            else -> pagerState.scrollToPage(target)
        }
    }

    val current = entries.getOrNull(currentIndex)
    val currentTitle = current?.groupTitle.orEmpty()

    // Hissə əməliyyatları yalnız məntiqli olanda təklif olunur — düymə görünüb heç nə etməməlidir
    // (layihə qaydası: mümkün olmayan əməliyyat UI-də təklif edilmir).
    fun mergeActionFor(index: Int): (() -> Unit)? {
        val setPart = onSetPart ?: return null
        val entry = entries.getOrNull(index) ?: return null
        val next = entries.getOrNull(index + 1) ?: return null
        // Yalnız eyni mövzu daxilində: qonşu mövzunun duasını hissə etmək istifadəçinin
        // gözlədiyi şey deyil.
        if (next.groupKey != entry.groupKey) return null
        if (entry.parts.size >= MAX_DUA_PARTS) return null
        // Özü çoxhissəli duanı hissə kimi bağlamaq zəncir yaradardı — bazada da qadağandır.
        if (next.parts.size > 1) return null

        val headId = entry.dua.id ?: return null
        val nextId = next.dua.id ?: return null
        return { setPart(nextId, headId, entry.parts.size + 1) }
    }

    fun splitActionFor(index: Int): (() -> Unit)? {
        val setPart = onSetPart ?: return null
        val entry = entries.getOrNull(index) ?: return null
        if (entry.parts.size <= 1) return null

        val lastId = entry.parts.last().id ?: return null
        return { setPart(lastId, null, 1) }
    }

    // Oxuma səthi — dua əzbərlənərkən ekran sönməməlidir. Siyahı ekranlarında çağırılmır: orada
    // istifadəçi oxumur, gəzir.
    KeepScreenOnIfEnabled()

    var sourceRef by remember { mutableStateOf<DuaSourceRef?>(null) }
    var pendingDelete by remember { mutableStateOf<Dua?>(null) }
    var editing by remember { mutableStateOf<Dua?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var zoomFeedback by remember { mutableStateOf<ReaderZoomFeedback?>(null) }
    var query by remember { mutableStateOf("") }
    var sharing by remember { mutableStateOf<DuaSourceRef?>(null) }
    /** Paylaşılan duanın mövzusu — şəkil kartının üst etiketi. Vərəq açıq ikən arxada səhifə
     *  sürüşə bildiyi üçün cari səhifədən yox, **paylaşma anından** saxlanılır. */
    var sharingTitle by remember { mutableStateOf<String?>(null) }
    var showNavigator by remember { mutableStateOf(false) }
    var savingBookmark by remember { mutableStateOf<Dua?>(null) }
    var removingBookmark by remember { mutableStateOf<Dua?>(null) }

    val userRepository = remember { RepositoryProvider.userRepository }

    val bookmarkedDuaIds by userRepository.getBookmarkedDuaIdsFlow()
        .collectAsStateWithLifecycle(emptySet())

    // Oxuma vəziyyəti: harada qaldın (hər səhifədə) və nəyi bitirdin (qrupun son səhifəsində).
    //
    // ⚠️ Yazı `viewModelScope`-da deyil, **ekranın** scope-undadır, amma `snapshotFlow` səhifə
    // dayanandan sonra işlədiyi üçün sürüşdürmə boyunca onlarla yazı olmur: yalnız dayandığı
    // səhifə yazılır.
    LaunchedEffect(entries, isFlowing) {
        if (entries.isEmpty()) return@LaunchedEffect

        snapshotFlow {
            if (isFlowing) listState.firstVisibleItemIndex else pagerState.currentPage
        }.collect { page ->
            val entry = entries.getOrNull(page) ?: return@collect

            entry.dua.id?.let { duaId ->
                userRepository.saveDuaReadPosition(
                    DuaReadHistoryEntity(
                        duaId = duaId,
                        groupKey = entry.groupKey,
                        title = entry.groupTitle,
                        preview = (entry.dua.text_az.takeIf { it.isNotBlank() } ?: entry.dua.text_ar)
                            .take(120),
                    )
                )
            }

            // Mövzu **son səhifəsinə çatanda** bitmiş sayılır: hər səhifəni ayrıca saymaq eyni
            // nəticəni verir, amma yarımçıq qalan mövzunu da «bitdi» kimi göstərmək riski yaradır.
            val (position, total) = groupPositionOf(entries, page)
            if (position == total) {
                userRepository.markDuaGroupCompleted(entry.groupKey, entry.category.slug)
            }
        }
    }

    // Saxlanılmış duaya təkrar basmaq onu siyahıdan çıxarır, yenisi üçün qeyd formu açılır —
    // hədisdəki `onHadithBookmarkClick` ilə eyni jest.
    val onBookmarkClick: (Dua) -> Unit = { dua ->
        val duaId = dua.id
        if (duaId != null) {
            if (duaId in bookmarkedDuaIds) removingBookmark = dua else savingBookmark = dua
        }
    }

    // Uyğunluqlar **səhifə indeksləridir** (bax `DuaSearch.kt`). Siyahı sorğu və ya məzmun
    // dəyişəndə yenidən qurulur, hər kadrda yox.
    val matches = remember(entries, query) { duaSearchMatches(entries, query) }
    val navigatorGroupCount = remember(entries) { duaNavigatorGroups(entries).size }
    val matchPosition = duaMatchPosition(matches, currentIndex)

    val visibility = duaBlockVisibility(
        mode = viewMode,
        arabicEnabled = DuaPreferences.observeArabicEnabled(),
        transliterationEnabled = DuaPreferences.observeTransliterationEnabled(),
        translationEnabled = DuaPreferences.observeTranslationEnabled(),
    )
    val arabicMult = DuaPreferences.observeArabicSizeMultiplier()
    val translationMult = DuaPreferences.observeTranslationSizeMultiplier()
    val pageTurnAnimation = AppPreferences.observeReaderPageTurnAnimation()

    val zoomModifier = Modifier.readerTextZoom(
        enabled = AppPreferences.observeReaderPinchZoomEnabled(),
        arabicMultiplier = arabicMult,
        translationMultiplier = translationMult,
        minMultiplier = ReaderTextZoom.HADITH_MIN,
        maxMultiplier = ReaderTextZoom.HADITH_MAX,
        onZoom = { target, value ->
            // ⚠️ Ortaq jestdə iki barmaq **tərcüməyə** gedir. Tərcümə gizlidirsə (məsələn «Ərəbcə»
            // rejimi) jest görünməyən ölçünü dəyişərdi — istifadəçi barmağını açır, ekranda heç nə
            // olmur. Ona görə görünməyən hədəf görünənə yönləndirilir; ortaq komponent
            // toxunulmamış qalır, çünki jest lüğəti hər üç oxucuda eyni olmalıdır.
            val effective = when {
                target == ReaderZoomTarget.Translation && !visibility.translation &&
                    visibility.arabic -> ReaderZoomTarget.Arabic

                target == ReaderZoomTarget.Arabic && !visibility.arabic &&
                    visibility.translation -> ReaderZoomTarget.Translation

                else -> target
            }

            zoomFeedback = ReaderZoomFeedback(effective, value)
            scope.launch {
                when (effective) {
                    ReaderZoomTarget.Arabic -> DuaPreferences.setArabicSizeMultiplier(value)
                    ReaderZoomTarget.Translation ->
                        DuaPreferences.setTranslationSizeMultiplier(value)
                }
            }
        },
    )

    Scaffold(
        topBar = {
            // Barda **başlıq yoxdur**: mövzu adını üzən `FloatingTitlePill` göstərir (hədisdəki
            // kutunun eynisi) və toxunuşla mövzular vərəqini o açır — ona görə köhnə üç nöqtə
            // düyməsi də getdi. Rejim zolağı yenə barın altındadır: `titleContent` slotu axtarış
            // sahəsi ilə növbələşir, zolaq isə həmişə görünməlidir.
            Column {
                AppBar(
                    title = null,
                    onBack = onBack,
                    searchQuery = query,
                    onSearchQueryChange = { query = it },
                    actions = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_settings),
                                contentDescription = stringResource(
                                    Res.string.strTitleReaderSettings,
                                ),
                                tint = colorScheme.onSurfaceVariant,
                            )
                        }

                        // Duaların sırası **burada** dəyişir, uzun basma menyusunda yox:
                        // vərəqləyicidə uzun basmaq üçün sətir yoxdur, üstəlik sıralanan dəst elə
                        // ekrandakı dəstdir.
                        // Sıralanan dəst **cari səhifənin mövzusudur** — vərəqləyici bütün
                        // dualar üzərində olduğu üçün «ekrandakı dəst» artıq bütün siyahıdır.
                        // ⚠️ Düymə **bir duası olan mövzuda çıxmır**: sıralanacaq bir şey yoxdur,
                        // basılanda isə heç nə olmurdu (şərt əvvəl yalnız çağıranda idi).
                        val canSortGroup = current != null &&
                            entries.count { it.groupKey == current.groupKey } > 1

                        onSort?.takeIf { canSortGroup }?.let { sort ->
                            IconButton(
                                onClick = { current?.let { sort(it.groupKey) } },
                                enabled = current != null,
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.dr_icon_sort),
                                    contentDescription = stringResource(Res.string.duaSortDuas),
                                    tint = colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                )

                Surface(color = colorScheme.surfaceContainer) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        ModeTabStrip(
                            tabs = duaModeTabs(),
                            selectedIndex = viewMode.coerceIn(0, DUA_MODE_COUNT - 1),
                            onSelect = { mode ->
                                scope.launch { DuaPreferences.setViewMode(mode) }
                            },
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (entries.isEmpty()) {
                Text(
                    text = stringResource(Res.string.duaEmptyCategory),
                    style = typography.bodyMedium.withScriptDirection(arabic = false),
                    color = colorScheme.onSurfaceVariant.alpha(0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        if (isFlowing) {
                            // Axan rejim: bütün dualar **tək siyahıdadır**, mövzu sərhədi başlıqla
                            // bilinir. Zoom jesti siyahının özündədir — burada udacağı üfüqi
                            // vərəqləmə yoxdur.
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize().then(zoomModifier),
                                contentPadding = PaddingValues(
                                    top = PILL_CLEARANCE,
                                    bottom = 24.dp,
                                ),
                            ) {
                                itemsIndexed(
                                    items = entries,
                                    // Baza id-si olmayan sətir (nəzəri hal) indeksdən mənfi açar
                                    // alır ki, açarlar toqquşmasın.
                                    key = { index, entry ->
                                        entry.dua.id ?: -(index + 1).toLong()
                                    },
                                ) { index, entry ->
                                    val dua = entry.dua

                                    ReadableWidthColumn {
                                        DuaPage(
                                            parts = entry.parts,
                                            groupTitle = entry.groupTitle
                                                .takeIf { entry.isGroupStart },
                                            isAuthorized = isAuthorized,
                                            visibility = visibility,
                                            arabicSizeMult = arabicMult,
                                            translationSizeMult = translationMult,
                                            query = query,
                                            onShare = {
                                                sharing = dua
                                                sharingTitle = entry.groupTitle
                                            },
                                            isBookmarked = dua.id in bookmarkedDuaIds,
                                            onBookmark = { onBookmarkClick(dua) },
                                            // Qaynaq/redaktə/silmə **hissəyə** aiddir, əlfəcin isə
                                            // duanın özünə — ona görə birincilər parametr alır.
                                            onOpenSource = { part -> sourceRef = part },
                                            onEdit = { part -> editing = part },
                                            onDelete = { part -> pendingDelete = part },
                                            onMergeNext = mergeActionFor(index),
                                            onSplitLast = splitActionFor(index),
                                        )
                                    }

                                    if (index < entries.lastIndex) {
                                        HorizontalDivider(
                                            color = colorScheme.outlineVariant.alpha(0.5f),
                                            modifier = Modifier.padding(horizontal = 40.dp),
                                        )
                                    }
                                }
                            }
                        } else {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                            ) { page ->
                                val entry = entries[page]
                                val dua = entry.dua

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pageTurnEffect(
                                            animation = pageTurnAnimation,
                                            pagerState = pagerState,
                                            page = page,
                                            ground = colorScheme.background,
                                        )
                                        // ⚠️ Zoom jesti **şaquli sürüşən sütundadır**,
                                        // vərəqləyicinin özündə yox: pager-ə qoyulsa pinch üfüqi
                                        // sürüşməni udur və səhifə keçidi ölür.
                                        .then(zoomModifier)
                                        .verticalScroll(rememberScrollState()),
                                ) {
                                    // Üzən mövzu kutusunun altından başlasın; sürüşdürəndə mətn
                                    // onun altına girir, bu, normaldır.
                                    Spacer(Modifier.height(PILL_CLEARANCE))

                                    ReadableWidthColumn {
                                        DuaPage(
                                            parts = entry.parts,
                                            // Vərəqləyicidə mövzu adı **səhifədə yazılmır**: onu
                                            // üstdəki üzən kutu deyir və mövzu dəyişəndə özü
                                            // animasiya ilə yenilənir. İkisi birlikdə qrupun ilk
                                            // səhifəsində eyni adı iki dəfə göstərirdi.
                                            groupTitle = null,
                                            isAuthorized = isAuthorized,
                                            visibility = visibility,
                                            arabicSizeMult = arabicMult,
                                            translationSizeMult = translationMult,
                                            query = query,
                                            onShare = {
                                                sharing = dua
                                                sharingTitle = entry.groupTitle
                                            },
                                            isBookmarked = dua.id in bookmarkedDuaIds,
                                            onBookmark = { onBookmarkClick(dua) },
                                            // Qaynaq/redaktə/silmə **hissəyə** aiddir, əlfəcin isə
                                            // duanın özünə — ona görə birincilər parametr alır.
                                            onOpenSource = { part -> sourceRef = part },
                                            onEdit = { part -> editing = part },
                                            onDelete = { part -> pendingDelete = part },
                                            onMergeNext = mergeActionFor(page),
                                            onSplitLast = splitActionFor(page),
                                        )
                                    }
                                }
                            }
                        }

                        // Üzən mövzu adı — **hər üç rejimdə** və açılışdan görünür, çünki bar-da
                        // başlıq yoxdur. Toxunuş mövzular vərəqini açır; tək mövzuda vərəqin açmağa
                        // dəyər məzmunu olmadığı üçün toxunuş heç nə etmir.
                        FloatingTitlePill(
                            title = currentTitle,
                            visible = currentTitle.isNotBlank(),
                            onClick = {
                                if (navigatorGroupCount > 1) showNavigator = true
                            },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 6.dp),
                        )

                        ReaderZoomFeedbackOverlay(zoomFeedback) { zoomFeedback = null }

                        // Axtarış zolağı yalnız sorğu varkən: boş sorğuda o, oxuma sahəsindən yer
                        // oğurlayır.
                        if (query.isNotBlank()) {
                            HadithSearchNavBar(
                                query = query,
                                current = matchPosition,
                                total = matches.size,
                                onPrevious = {
                                    duaPreviousMatch(matches, currentIndex)?.let { target ->
                                        scope.launch { goTo(target, true) }
                                    }
                                },
                                onNext = {
                                    duaNextMatch(matches, currentIndex)?.let { target ->
                                        scope.launch { goTo(target, true) }
                                    }
                                },
                                onDismiss = { query = "" },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 12.dp),
                            )
                        }
                    }

                    // Alt çərçivə (oxlar, «1 / 2», nöqtələr) **vərəqləyicinin** çərçivəsidir: axan
                    // rejimdə səhifə anlayışı yoxdur, mövzunu üzən kutu deyir.
                    if (!isFlowing) {
                        val (inGroup, groupSize) = groupPositionOf(entries, currentIndex)
                        val groupStart = currentIndex - (inGroup - 1)

                        DuaPagerControls(
                            position = currentIndex,
                            total = entries.size,
                            indicator = stringResource(
                                Res.string.duaPageIndicator,
                                inGroup,
                                groupSize,
                            ),
                            // Nöqtələr mövzunun içindədir, oxlar və sürüşdürmə isə bütün siyahıda:
                            // «1 / 2» ilə nöqtələrin sayı beləcə eyni şeyi deyir.
                            dotsPosition = inGroup - 1,
                            dotsTotal = groupSize,
                            onScrubTo = { target -> scope.launch { goTo(target, false) } },
                            onDotTap = { dot -> scope.launch { goTo(groupStart + dot, true) } },
                            scrubTitle = currentTitle,
                            onPrevious = { scope.launch { goTo(currentIndex - 1, true) } },
                            onNext = { scope.launch { goTo(currentIndex + 1, true) } },
                        )
                    }
                }
            }
        }
    }

    DuaSourceSheet(ref = sourceRef, onClose = { sourceRef = null })

    DuaSettingsSheet(isOpen = showSettings, onDismiss = { showSettings = false })

    DuaNavigatorSheet(
        isOpen = showNavigator,
        entries = entries,
        currentPage = currentIndex,
        onDismiss = { showNavigator = false },
        // Keçid **ani**dir, animasiyalı deyil: 40 mövzu o tərəfə sürüşmək onlarla səhifəni
        // gözlə keçirərdi (`animateScrollToPage` aralıqdakı hər səhifəni çəkir).
        onNavigate = { index -> scope.launch { goTo(index, false) } },
    )

    DuaShareSheet(
        ref = sharing,
        // Açılışda ekranda görünən bloklar seçili gəlir — istifadəçi gördüyünü paylaşmaq istəyir.
        initialParts = DuaShareParts.visible(visibility),
        eyebrow = sharingTitle,
        onDismiss = { sharing = null },
    )

    BookmarkNoteSheet(
        subtitle = savingBookmark?.let { dua -> entries.firstOrNull { it.dua.id == dua.id }?.groupTitle },
        onDismiss = { savingBookmark = null },
        onSave = { note ->
            val dua = savingBookmark ?: return@BookmarkNoteSheet
            val duaId = dua.id ?: return@BookmarkNoteSheet
            val entry = entries.firstOrNull { it.dua.id == duaId }
            savingBookmark = null

            scope.launch {
                val result = userRepository.addDuaBookmark(
                    duaId = duaId,
                    categorySlug = dua.category_slug,
                    subcategorySlug = dua.subcategory_slug,
                    // Başlıq **ad** kimi yazılır, slug kimi yox: əlfəcinlər ekranı Supabase-ə
                    // getmədən siyahını çəkməlidir.
                    title = entry?.groupTitle.orEmpty(),
                    preview = (dua.text_az.takeIf { it.isNotBlank() } ?: dua.text_ar).take(160),
                    note = note,
                )
                PlatformUtils.showToast(
                    when (result) {
                        BookmarkAddResult.Added -> getString(Res.string.strMsgDuaBookmarkAdded)
                        BookmarkAddResult.AlreadyBookmarked ->
                            getString(Res.string.strMsgBookmarkAddedAlready)

                        BookmarkAddResult.Failed -> getString(Res.string.strMsgDuaBookmarkAddFailed)
                    }
                )
            }
        },
    )

    AlertDialog(
        isOpen = removingBookmark != null,
        onClose = { removingBookmark = null },
        title = stringResource(Res.string.strTitleBookmarkDeleteThis),
        actions = listOf(
            AlertDialogAction(
                text = stringResource(Res.string.strLabelCancel),
                onClick = { removingBookmark = null },
            ),
            AlertDialogAction(
                text = stringResource(Res.string.strLabelRemove),
                style = AlertDialogActionStyle.Danger,
                onClick = {
                    val duaId = removingBookmark?.id
                    removingBookmark = null

                    if (duaId != null) {
                        scope.launch {
                            if (userRepository.removeDuaBookmark(duaId)) {
                                PlatformUtils.showToast(
                                    getString(Res.string.strMsgDuaBookmarkRemoved)
                                )
                            }
                        }
                    }
                },
            ),
        ),
    )

    editing?.let { dua ->
        DuaEditDialog(
            dua = dua,
            isSaving = isSaving,
            onSave = { updated ->
                editing = null
                onEdit(updated)
            },
            onDismiss = { editing = null },
        )
    }

    pendingDelete?.let { dua ->
        AlertDialog(
            isOpen = true,
            onClose = { pendingDelete = null },
            title = stringResource(Res.string.duaDeleteConfirmTitle),
            actions = listOf(
                AlertDialogAction(
                    text = stringResource(Res.string.strLabelCancel),
                    onClick = { pendingDelete = null },
                ),
                AlertDialogAction(
                    text = stringResource(Res.string.strLabelDelete),
                    style = AlertDialogActionStyle.Danger,
                    onClick = {
                        pendingDelete = null
                        dua.id?.let(onDelete)
                    },
                ),
            ),
        ) {
            Text(
                text = stringResource(Res.string.duaDeleteDuaConfirm),
                style = typography.bodyMedium.withScriptDirection(arabic = false),
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
}

/** Bir duaya ən çox neçə hissə bağlana bilər — baza da eyni həddi qoyur (`part_no` 1..5). */
private const val MAX_DUA_PARTS = 5

/** Üzən mövzu kutusunun altında məzmun üçün buraxılan boşluq. */
private val PILL_CLEARANCE = 48.dp

/**
 * Bir duanın səthi — vərəqləyicidə bir səhifə, axan rejimdə siyahının bir elementi.
 *
 * Dua **bir neçə hissədən** ibarət ola bilər (1..5): hər hissə öz `dua` sətridir, öz sayı, öz
 * oxunuşu və öz mənbəyi ilə, amma ekranda bir duadır — bax [DuaFlatEntry.parts]. Əlfəcin, paylaşma
 * və kopyalama **bütöv** duaya aiddir, «qaynağa bax» və redaktə isə hissəyə.
 */
@Composable
private fun DuaPage(
    parts: List<Dua>,
    /** Mövzu adı — yalnız qrupun ilk səhifəsində verilir, qalanlarında `null`. */
    groupTitle: String?,
    isAuthorized: Boolean,
    visibility: DuaBlockVisibility,
    arabicSizeMult: Float,
    translationSizeMult: Float,
    /** Axtarış sorğusu — uyğun sözlər sarı ilə işarələnir; boş sətir = vurğu yoxdur. */
    query: String,
    onShare: () -> Unit,
    /** Dua istifadəçinin əlfəcinlərindədirmi — ikonun forması və rəngi bundan asılıdır. */
    isBookmarked: Boolean,
    onBookmark: () -> Unit,
    onOpenSource: (Dua) -> Unit,
    onEdit: (Dua) -> Unit,
    onDelete: (Dua) -> Unit,
    /**
     * Növbəti duanı bu duanın **hissəsi** edir (admin). `null` = mümkün deyil: səlahiyyət yoxdur,
     * növbəti dua başqa mövzudadır, özü çoxhissəlidir, ya da hissə həddi (5) dolub.
     */
    onMergeNext: (() -> Unit)? = null,
    /** Son hissəni ayırıb müstəqil dua edir (admin); `null` = dua tək hissəlidir. */
    onSplitLast: (() -> Unit)? = null,
) {
    // Kopyalanan mətn **ekranda görünənə** uyğundur: «Ərəbcə» rejimində tərcüməni də kopyalamaq
    // istifadəçinin istəmədiyi şeyi buferə qoyurdu (bax `DuaShareParts.visible`). Çoxhissəli duada
    // hissələr ardıcıl yazılır — oxunuş sırası elə budur.
    val copyText = remember(parts, visibility) {
        buildDuaShareText(parts, DuaShareParts.visible(visibility))
    }

    val clipboardMsg = stringResource(Res.string.copiedToClipboard)

    Column(
        modifier = Modifier
            // Uzun basmaq mətni kopyalayır — hədisdəki `rememberHadithCopyAction` ilə eyni jest.
            // `onClick` boşdur, çünki səhifədə basılacaq ayrıca element yoxdur: bütün səhifə bir
            // duadır və qısa toxunuş sürüşdürməyə mane olmamalıdır.
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    PlatformUtils.copyToClipboard(copyText)
                    PlatformUtils.showClipboardMessage(clipboardMsg)
                },
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        // 18dp deyil: bloklar arasına ayırıcı xətt düşdüyü üçün boşluq özü artıq ayırır.
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Mövzu sərhədini keçəndə istifadəçi hara düşdüyünü bilməlidir — axan rejimdə bu başlıq
        // sərhəd nişanıdır (vərəqləyicidə isə üzən kutu deyir, ona görə orada `null` gəlir).
        groupTitle?.let { heading ->
            Text(
                text = heading,
                style = typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ).withScriptDirection(arabic = false),
                color = colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        parts.forEachIndexed { index, part ->
            // Hissələr arasında qalın ayırıcı: blok ayırıcısından (`DuaBlockDivider`) fərqli olmalıdır,
            // yoxsa «duanın ikinci hissəsi» ilə «duanın tərcüməsi» eyni səviyyədə görünür.
            if (index > 0) {
                HorizontalDivider(
                    color = colorScheme.primary.alpha(0.25f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }

            DuaPartSection(
                part = part,
                isAuthorized = isAuthorized,
                visibility = visibility,
                arabicSizeMult = arabicSizeMult,
                translationSizeMult = translationSizeMult,
                query = query,
                onOpenSource = { onOpenSource(part) },
                onEdit = { onEdit(part) },
                onDelete = { onDelete(part) },
            )
        }

        // Rejim və ayar açarları birlikdə hər şeyi gizlədə bilir. Səhifəni sükutla boş buraxmaq
        // «tətbiq sınıb» kimi görünür, ona görə səbəb açıq yazılır.
        if (visibility.isEmpty) {
            Text(
                text = stringResource(Res.string.duaEmptyBody),
                style = typography.bodyMedium.withScriptDirection(arabic = false),
                color = colorScheme.onSurfaceVariant.alpha(0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            )
        }

        // Bu üçü **bütöv** duaya aiddir: kopyalanan mətn hissələri birlikdə daşıyır, əlfəcin isə
        // baş sətrin id-si ilə saxlanılır.
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val copyLabel = stringResource(Res.string.strLabelCopy)
            IconButton(onClick = { PlatformUtils.copyToClipboard(copyText) }) {
                Icon(
                    painter = painterResource(Res.drawable.icon_copy),
                    contentDescription = copyLabel,
                    tint = colorScheme.onSurfaceVariant.alpha(0.6f),
                    modifier = Modifier.size(20.dp),
                )
            }

            val shareLabel = stringResource(Res.string.strLabelShare)
            IconButton(onClick = onShare) {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_share),
                    contentDescription = shareLabel,
                    tint = colorScheme.onSurfaceVariant.alpha(0.6f),
                    modifier = Modifier.size(20.dp),
                )
            }

            IconButton(onClick = onBookmark) {
                Icon(
                    painter = painterResource(
                        if (isBookmarked) Res.drawable.ic_bookmark_added else Res.drawable.ic_bookmark
                    ),
                    contentDescription = stringResource(Res.string.strLabelBookmark),
                    // Saxlanılmış dua **rəngli** nişan alır: solğun ikon «basılmayıb» kimi oxunur.
                    tint = if (isBookmarked) colorScheme.primary else colorScheme.onSurfaceVariant.alpha(0.6f),
                    modifier = Modifier.size(20.dp),
                )
            }

            // Hissə qurma **admin işidir**: hissələr ayrı-ayrı əlavə olunur (mənbədən çıxarış
            // seçməklə), sonra burada bir duaya bağlanır. Ayrı bir «hissə əlavə et» axını
            // qurulmadı, çünki hissənin mənbəyi onsuz da seçici ekranından gəlir.
            onMergeNext?.let { merge ->
                val mergeLabel = stringResource(Res.string.duaMergeNextPart)
                SimpleTooltip(text = mergeLabel) {
                    IconButton(onClick = merge) {
                        Icon(
                            painter = painterResource(Res.drawable.dr_icon_add),
                            contentDescription = mergeLabel,
                            tint = colorScheme.primary.alpha(0.75f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            onSplitLast?.let { split ->
                val splitLabel = stringResource(Res.string.duaSplitLastPart)
                SimpleTooltip(text = splitLabel) {
                    IconButton(onClick = split) {
                        Icon(
                            painter = painterResource(Res.drawable.dr_icon_close),
                            contentDescription = splitLabel,
                            tint = colorScheme.onSurfaceVariant.alpha(0.6f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

/**
 * Duanın **bir hissəsi**: sayı, mətn blokları, mənbəsi və hissəyə aid əməliyyatlar.
 *
 * Tək hissəli duada bu, səhifənin bütün məzmunudur — yəni adi dua üçün görkəm dəyişmir.
 */
@Composable
private fun DuaPartSection(
    part: Dua,
    isAuthorized: Boolean,
    visibility: DuaBlockVisibility,
    arabicSizeMult: Float,
    translationSizeMult: Float,
    query: String,
    onOpenSource: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Zikr sayı — «33 dəfə». Yalnız yazılıbsa görünür: adi duada say anlayışı yoxdur. Hər
        // hissənin öz sayı var («3 dəfə» + «1 dəfə»), ona görə nişan hissənin başındadır.
        //
        // Mövqe nişanı («1 / 2») buradan çıxdı: o, artıq aşağıda nöqtələrin **üstündədir**.
        part.repeat_count?.takeIf { it > 0 }?.let { count ->
            Surface(
                color = colorScheme.tertiaryContainer.alpha(0.5f),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    text = stringResource(Res.string.duaCountBadge, count),
                    style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        }

        // Ərəbcə → oxunuş → tərcümə → qeyd. Bloklar əvvəlcə siyahıya yığılır, sonra aralarına
        // ayırıcı səpilir: belə olanda boş blokun (tərcüməsiz zikr, qeydsiz dua) ayırıcısı da
        // özü düşür — əks halda ekranda mətnsiz qoşa xətt qalardı.
        val blocks = buildList<@Composable () -> Unit> {
            part.text_ar.takeIf { it.isNotBlank() && visibility.arabic }?.let { arabic ->
                add {
                    Text(
                        text = arabic.withSearchHighlight(query),
                        style = typography.headlineSmall.copy(
                            fontSize = 24.sp * arabicSizeMult,
                            lineHeight = (24.sp * arabicSizeMult) * 1.95f,
                            textAlign = TextAlign.Center,
                        ).withScriptDirection(
                            arabic = true,
                            arabicFontFamily = arabicFontFamily(),
                        ),
                        color = colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Oxunuş tərcümədən **əvvəl**: oxuyan adam əvvəlcə necə deyiləcəyini, sonra nə demək
            // olduğunu axtarır. Kursiv və solğun — əsas mətnlə qarışmasın. Ölçüsü tərcümənin
            // çarpanına bağlıdır: ikisi də latın mətnidir və ayrı-ayrı böyüməsi qəribə görünür.
            part.transliteration?.takeIf { it.isNotBlank() && visibility.transliteration }
                ?.let { translit ->
                    add {
                        Text(
                            text = translit.withSearchHighlight(query),
                            style = typography.bodyMedium.copy(
                                fontSize = typography.bodyMedium.fontSize * translationSizeMult,
                                fontStyle = FontStyle.Italic,
                                textAlign = TextAlign.Center,
                            ).withLineHeightRatio(TRANSLATION_LINE_HEIGHT_RATIO)
                                .withScriptDirection(arabic = false),
                            color = colorScheme.onSurfaceVariant.alpha(0.9f),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

            // Tərcümə boş qala bilər (tək bir ilahi ad, qısa zikr) — boş `Text` ekranda izsiz
            // boşluq buraxardı. Hizalanma `Start`-dır: mərkəzlənmiş abzasın sətir sonları dişli
            // görünür və mətn axını itir.
            part.text_az.takeIf { it.isNotBlank() && visibility.translation }?.let { translation ->
                add {
                    Text(
                        text = translation.withSearchHighlight(query),
                        style = typography.bodyLarge
                            .copy(
                                fontSize = typography.bodyLarge.fontSize * translationSizeMult,
                                textAlign = TextAlign.Start,
                            )
                            .withLineHeightRatio(TRANSLATION_LINE_HEIGHT_RATIO)
                            .withScriptDirection(arabic = false),
                        color = colorScheme.onSurface.alpha(0.92f),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Qeyd tərcümənin davamıdır — tərcümə gizlədiləndə o da getməlidir, yoxsa «Ərəbcə»
            // rejimində ekranın altında azərbaycanca bir abzas qalırdı.
            part.note?.takeIf { it.isNotBlank() && visibility.translation }?.let { note ->
                add {
                    Text(
                        text = note,
                        // Qeyd tərcümədən **3sp kiçikdir** və onun çarpanı ilə böyüyüb-kiçilir:
                        // ikisi də latın mətnidir, biri sabit qalanda pinch jestindən sonra qeyd
                        // tərcümədən böyük görünürdü.
                        style = typography.bodySmall.copy(
                            fontSize = (typography.bodyLarge.fontSize.value - TRANSLATION_SUBTEXT_DROP_SP).sp *
                                translationSizeMult,
                            textAlign = TextAlign.Center,
                        ).withLineHeightRatio(TRANSLATION_LINE_HEIGHT_RATIO)
                            .withScriptDirection(arabic = false),
                        color = colorScheme.onSurfaceVariant.alpha(0.85f),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        blocks.forEachIndexed { index, block ->
            if (index > 0) DuaBlockDivider()
            block()
        }

        // Mənbə sətri tərcümə ilə birlikdə gedir — hədisdəki `showSource` qaydası ilə eyni
        // (`viewMode == 0 || viewMode == 2`): «Ərəbcə» rejimində ekranda yalnız ərəbcə qalmalıdır,
        // rəvayətçilər siyahısı isə azərbaycancadır. «Qaynağa bax» düyməsi qalır — o, mətn deyil,
        // əməliyyatdır və hər rejimdə lazım ola bilər.
        part.source?.takeIf { it.isNotBlank() && visibility.translation }?.let { source ->
            Text(
                text = "— $source",
                // Qeydlə **eyni** qayda: tərcümədən 3sp kiçik və onun çarpanı ilə böyüyüb-kiçilir
                // (bax [TRANSLATION_SUBTEXT_DROP_SP]). Sabit `labelMedium` ilə mənbə pinch-dən
                // sonra tərcümədən böyük görünürdü.
                style = typography.labelMedium.copy(
                    fontSize = (typography.bodyLarge.fontSize.value - TRANSLATION_SUBTEXT_DROP_SP).sp *
                        translationSizeMult,
                ).withLineHeightRatio(TRANSLATION_LINE_HEIGHT_RATIO)
                    .withScriptDirection(arabic = false),
                // Mənbə sətri mətnin özü deyil, arxasındakı istinaddır — tonu bir az daha boz
                // olsun ki, göz əvvəl duanı, sonra mənbəni oxusun.
                color = colorScheme.onSurfaceVariant.alpha(0.55f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Qaynaq və redaktə **hissəyə** aiddir: hissələrin mənbəyi ayrı-ayrı hədis/ayə ola bilər.
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onOpenSource) {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_open),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.duaOpenSource))
            }

            if (isAuthorized) {
                IconButton(onClick = onEdit) {
                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_edit),
                        contentDescription = stringResource(Res.string.strLabelEdit),
                        tint = colorScheme.onSurfaceVariant.alpha(0.6f),
                        modifier = Modifier.size(20.dp),
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_delete),
                        contentDescription = stringResource(Res.string.strLabelDelete),
                        tint = colorScheme.error.alpha(0.75f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/** Bölmə boş olanda göstərilən dəvət — ikon, başlıq, izah. */
@Composable
internal fun DuaEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(colorScheme.primaryContainer.alpha(0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.dr_logo_dua),
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(30.dp),
            )
        }

        Text(
            text = title,
            style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                .withScriptDirection(arabic = false),
            color = colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Text(
            text = body,
            style = typography.bodySmall.withScriptDirection(arabic = false),
            color = colorScheme.onSurfaceVariant.alpha(0.8f),
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Bir başlığın **alt başlıqları** — və varsa, başlığın birbaşa altındakı dualar.
 *
 * Alt başlıq olmayanda bu ekran ümumiyyətlə açılmır ([DuaScreen] birbaşa vərəqləyiciyə keçir):
 * tək sətirlik aralıq siyahı istifadəçidən artıq bir toxunuş istəyərdi.
 */
@Composable
private fun DuaSubcategoryScreen(
    category: DuaCategory,
    subcategories: List<DuaSubcategory>,
    /** Oxunub bitmiş mövzuların açarları — sətirdəki ✓ bunu oxuyur. */
    completedKeys: Set<String>,
    countOf: (String) -> Int,
    directCount: Int,
    isAuthorized: Boolean,
    onOpenSubcategory: (DuaSubcategory) -> Unit,
    onOpenDirect: () -> Unit,
    onLongPressSubcategory: (DuaSubcategory) -> Unit,
    onAdd: (String) -> Unit,
    onBack: () -> Unit,
) {
    var adding by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppBar(
                title = category.name,
                onBack = onBack,
                actions = {
                    if (isAuthorized) {
                        IconButton(onClick = { adding = true }) {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_edit),
                                contentDescription = stringResource(Res.string.duaAddSubcategory),
                                tint = colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(
                horizontal = 16.dp + readableWidthInset(),
                vertical = 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Başlığın birbaşa altındakı dualar — alt başlıqlarla eyni siyahıda, amma öz sətrində.
            // Gizlətmək onları əlçatmaz edərdi: heç bir alt başlığa aid deyillər.
            if (directCount > 0) {
                item(key = "direct") {
                    DuaTitleCard(
                        name = stringResource(Res.string.duaDirectDuas),
                        nameAr = null,
                        // Birbaşa duaların qrup açarı **başlığın slug-ıdır** (bax `DuaFlatEntry`).
                        isCompleted = category.slug in completedKeys,
                        caption = stringResource(Res.string.duaCountLabel, directCount),
                        onClick = onOpenDirect,
                        onLongClick = null,
                    )
                }
            }

            items(subcategories, key = { it.slug }) { subcategory ->
                DuaTitleCard(
                    name = subcategory.name,
                    nameAr = subcategory.name_ar,
                    isCompleted = subcategory.slug in completedKeys,
                    caption = stringResource(
                        Res.string.duaCountLabel,
                        countOf(subcategory.slug),
                    ),
                    onClick = { onOpenSubcategory(subcategory) },
                    onLongClick = if (isAuthorized) {
                        { onLongPressSubcategory(subcategory) }
                    } else {
                        null
                    },
                )
            }

            if (subcategories.isEmpty() && directCount == 0) {
                item(key = "empty") {
                    Text(
                        text = stringResource(Res.string.duaEmptyCategory),
                        style = typography.bodyMedium.withScriptDirection(arabic = false),
                        color = colorScheme.onSurfaceVariant.alpha(0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    )
                }
            }
        }
    }

    if (adding) {
        DuaNameForm(
            title = stringResource(Res.string.duaAddSubcategory),
            initialName = "",
            initialNameAr = "",
            showArabic = false,
            isSaving = false,
            onSave = { name, _ ->
                adding = false
                onAdd(name)
            },
            onDismiss = { adding = false },
        )
    }
}

/**
 * Uzun basma menyusu və ona bağlı formalar — hər iki səviyyədə eyni dəst.
 *
 * Bir yerdə toplanıb, çünki başlıq siyahısı ilə alt başlıq siyahısı eyni üç dialoqu göstərir;
 * ayrı-ayrı yazılsa biri dəyişəndə o birisi arxada qalardı.
 */
@Composable
private fun TitleOptionsDialogs(
    options: RenameTarget?,
    renaming: RenameTarget?,
    pendingCategoryDelete: DuaCategory?,
    pendingSubcategoryDelete: DuaSubcategory?,
    isSaving: Boolean,
    sortLabel: String,
    onOptionsDismiss: () -> Unit,
    onRenameRequest: (RenameTarget) -> Unit,
    onSortRequest: () -> Unit,
    onDeleteRequest: (RenameTarget) -> Unit,
    onRenameDismiss: () -> Unit,
    onRename: (RenameTarget, String, String?) -> Unit,
    onCategoryDeleteDismiss: () -> Unit,
    onSubcategoryDeleteDismiss: () -> Unit,
    onCategoryDelete: (DuaCategory) -> Unit,
    onSubcategoryDelete: (DuaSubcategory) -> Unit,
) {
    options?.let { target ->
        AlertDialog(
            isOpen = true,
            onClose = onOptionsDismiss,
            // Başlıq elə seçilmiş başlığın **öz adıdır**: «Başlıq əməliyyatları» kimi ümumi etiket
            // dialoqun ən görünən sətrini heç nə demədən yeyirdi.
            title = target.displayName().ifBlank { stringResource(Res.string.duaTitleOptions) },
            actions = listOf(
                AlertDialogAction(
                    text = stringResource(Res.string.strLabelCancel),
                    onClick = onOptionsDismiss,
                ),
            ),
        ) {
            // ⚠️ Əməliyyatlar **siyahıdır**, düymə sırası yox: `AlertDialog` düymələri bir sətirdə
            // bərabər paylayır, dördüncüsü (sıralama) əlavə olunanda isə dar ekranda «Adı dəyiş»
            // kəsilirdi. Siyahıda hər əməliyyatın ikonu və tam adı var.
            Column(modifier = Modifier.fillMaxWidth()) {
                OptionRow(
                    icon = Res.drawable.dr_icon_edit,
                    label = stringResource(Res.string.duaRenameAction),
                    onClick = { onRenameRequest(target) },
                )

                OptionRow(
                    icon = Res.drawable.dr_icon_sort,
                    label = sortLabel,
                    onClick = onSortRequest,
                )

                OptionRow(
                    icon = Res.drawable.dr_icon_delete,
                    label = stringResource(Res.string.strLabelDelete),
                    danger = true,
                    onClick = { onDeleteRequest(target) },
                )
            }
        }
    }

    renaming?.let { target ->
        DuaNameForm(
            title = stringResource(
                if (target == RenameTarget.NewCategory) Res.string.duaAddCategory
                else Res.string.duaRenameAction,
            ),
            initialName = if (target == RenameTarget.NewCategory) "" else target.displayName(),
            initialNameAr = target.arabicName().orEmpty(),
            showArabic = target !is RenameTarget.Subcategory,
            isSaving = isSaving,
            onSave = { name, nameAr -> onRename(target, name, nameAr) },
            onDismiss = onRenameDismiss,
        )
    }

    pendingCategoryDelete?.let { category ->
        ConfirmDelete(
            message = stringResource(Res.string.duaDeleteCategoryConfirm),
            onDismiss = onCategoryDeleteDismiss,
            onConfirm = { onCategoryDelete(category) },
        )
    }

    pendingSubcategoryDelete?.let { subcategory ->
        ConfirmDelete(
            message = stringResource(Res.string.duaDeleteSubcategoryConfirm),
            onDismiss = onSubcategoryDeleteDismiss,
            onConfirm = { onSubcategoryDelete(subcategory) },
        )
    }
}

/** Seçim dialoqunun bir sətri — ikon, ad; təhlükəli əməliyyat qırmızı. */
@Composable
private fun OptionRow(
    icon: DrawableResource,
    label: String,
    onClick: () -> Unit,
    danger: Boolean = false,
) {
    val tint = if (danger) colorScheme.error else colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint.alpha(if (danger) 0.9f else 0.7f),
            modifier = Modifier.size(20.dp),
        )

        Spacer(Modifier.width(14.dp))

        Text(
            text = label,
            style = typography.bodyLarge.withScriptDirection(arabic = false),
            color = tint,
        )
    }
}

private fun RenameTarget.displayName(): String = when (this) {
    RenameTarget.NewCategory -> ""
    is RenameTarget.Category -> category.name
    is RenameTarget.Subcategory -> subcategory.name
}

private fun RenameTarget.arabicName(): String? = when (this) {
    RenameTarget.NewCategory -> null
    is RenameTarget.Category -> category.name_ar
    is RenameTarget.Subcategory -> subcategory.name_ar
}

/** Silmə təsdiqi — mətn fərqlidir, forma eyni. */
@Composable
private fun ConfirmDelete(message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        isOpen = true,
        onClose = onDismiss,
        title = stringResource(Res.string.duaDeleteConfirmTitle),
        actions = listOf(
            AlertDialogAction(
                text = stringResource(Res.string.strLabelCancel),
                onClick = onDismiss,
            ),
            AlertDialogAction(
                text = stringResource(Res.string.strLabelDelete),
                style = AlertDialogActionStyle.Danger,
                onClick = onConfirm,
            ),
        ),
    ) {
        Text(
            text = message,
            style = typography.bodyMedium.withScriptDirection(arabic = false),
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}

/**
 * Başlıq/alt başlıq adı formasi — yeni yaratmaq və adını dəyişmək üçün eyni forma.
 *
 * ⚠️ **Slug dəyişmir**: o, duaların açarıdır, adı dəyişmək qruplaşdırmanı pozmamalıdır.
 */
@Composable
private fun DuaNameForm(
    title: String,
    initialName: String,
    initialNameAr: String,
    showArabic: Boolean,
    isSaving: Boolean,
    onSave: (name: String, nameAr: String?) -> Unit,
    onDismiss: () -> Unit,
) = Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
        dismissOnBackPress = true,
        dismissOnClickOutside = false,
        usePlatformDefaultWidth = false,
    ),
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var nameAr by remember(initialNameAr) { mutableStateOf(initialNameAr) }

    Scaffold(
        topBar = { AppBar(title = title, onBack = onDismiss) },
        bottomBar = {
            Surface(color = colorScheme.surfaceContainer, shadowElevation = 6.dp) {
                ReadableWidthColumn {
                    Button(
                        onClick = { onSave(name.trim(), nameAr.trim().takeIf { it.isNotBlank() }) },
                        enabled = name.isNotBlank() && !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(stringResource(Res.string.duaPickerSave))
                    }
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            // ⚠️ `ReadableWidthColumn` **Box**-dur — daxili `Column` məcburidir.
            ReadableWidthColumn {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Spacer(Modifier.height(8.dp))

                    FormTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = stringResource(Res.string.duaPickerNewTitleName),
                        icon = Res.drawable.dr_icon_translations,
                        onClear = { name = "" },
                    )

                    if (showArabic) {
                        FormTextField(
                            value = nameAr,
                            onValueChange = { nameAr = it },
                            label = stringResource(Res.string.duaPickerNewTitleNameAr),
                            icon = Res.drawable.dr_icon_quran_script,
                            textStyle = typography.bodyLarge.withScriptDirection(
                                arabic = true,
                                arabicFontFamily = arabicFontFamily(),
                            ),
                            onClear = { nameAr = "" },
                        )
                    }
                }
            }
        }
    }
}
