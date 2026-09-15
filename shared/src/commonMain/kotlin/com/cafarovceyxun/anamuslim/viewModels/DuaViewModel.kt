package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.repository.supabase.DuaRepository
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.duaMsgDeleteFailed
import com.cafarovceyxun.anamuslim.resources.duaMsgDeleted
import com.cafarovceyxun.anamuslim.repository.supabase.DuaDuplicateException
import com.cafarovceyxun.anamuslim.resources.duaMsgDuplicate
import com.cafarovceyxun.anamuslim.resources.duaMsgSaveFailed
import com.cafarovceyxun.anamuslim.resources.duaMsgSaved
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.supabase.Dua
import com.cafarovceyxun.anamuslim.utils.supabase.DuaCategory
import com.cafarovceyxun.anamuslim.utils.supabase.DuaSubcategory
import com.cafarovceyxun.anamuslim.utils.supabase.duaCategorySlug
import com.cafarovceyxun.anamuslim.utils.supabase.fallbackSlug
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/**
 * Dua məzmununun **proses boyu** versiyası — hər uğurlu yazma/silmədən sonra artır.
 *
 * Niyə instansiyadan kənarda: duanı **hədis oxucusundan** (seçim vərəqi öz `viewModel { … }`-ini
 * qurur) əlavə edirik, göstərən isə dua ekranının ayrı instansiyasıdır. Yazan tərəfin öz siyahısını
 * yeniləməsi oxuyan tərəfə heç nə demir. Android bunu gizlədir (hər Activity təzə ViewModel), iOS-da
 * isə app-scoped instansiya proses bitənə qədər yaşayır — `HadithViewModel.hadithContentRevision`
 * ilə eyni tələ, eyni həll.
 *
 * Ekranlar bunu **açar kimi** işlədir: `LaunchedEffect(revision)` yenidən oxuyur.
 */
private val duaContentRevision = MutableStateFlow(0)

/** Dualar bölməsi — başlıqlar, duaların özü, əlavə/silmə. */
class DuaViewModel : ViewModel() {

    private val repository = DuaRepository()

    private val _categories = MutableStateFlow<List<DuaCategory>>(emptyList())
    val categories: StateFlow<List<DuaCategory>> = _categories.asStateFlow()

    private val _subcategories = MutableStateFlow<List<DuaSubcategory>>(emptyList())
    val subcategories: StateFlow<List<DuaSubcategory>> = _subcategories.asStateFlow()

    private val _duas = MutableStateFlow<List<Dua>>(emptyList())
    val duas: StateFlow<List<Dua>> = _duas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Ən azı bir dəfə serverdən (və ya keşdən) cavab gəldimi — boş ekranı yükləmədən ayırır. */
    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    val revision: StateFlow<Int> = duaContentRevision.asStateFlow()

    init {
        // Keş dərhal göstərilir, şəbəkə cavabı onun üstünə gəlir: bölmə oflayn da, soyuq açılışda da
        // boş qalmır.
        _categories.value = repository.cachedCategories()
        _subcategories.value = repository.cachedSubcategories()
        _duas.value = repository.cachedDuas()
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _categories.value = repository.fetchCategories()
            _subcategories.value = repository.fetchSubcategories()
            _duas.value = repository.fetchDuas()
            _isLoaded.value = true
            _isLoading.value = false
        }
    }

    /** Bir başlığın **bütün** duaları (alt başlıqdakılar daxil) — siyahıdakı say nişanı üçün. */
    fun duasOf(categorySlug: String): List<Dua> =
        _duas.value.filter { it.category_slug == categorySlug }

    /** Başlıq üzrə dua sayı — siyahıdakı nişan. */
    fun countOf(categorySlug: String): Int = duasOf(categorySlug).size

    /**
     * Yeni başlıq yaradır (istifadəçi «Dua və zikr» ekranından əlavə edəndə).
     *
     * Seçim ekranındakı yol [saveDua]-dan keçir; bu, siyahı ekranının öz düyməsi üçündür.
     */
    fun addCategory(name: String, nameAr: String?, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true

            if (createCategory(name, nameAr) == null) {
                PlatformUtils.showToast(getString(Res.string.duaMsgSaveFailed))
            } else {
                bumpRevision()
                refresh()
                PlatformUtils.showToast(getString(Res.string.duaMsgSaved))
                onSaved()
            }

            _isLoading.value = false
        }
    }

    /** Yeni alt başlıq. */
    fun addSubcategory(
        categorySlug: String,
        name: String,
        nameAr: String?,
        onSaved: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            if (createSubcategory(categorySlug, name, nameAr) == null) {
                PlatformUtils.showToast(getString(Res.string.duaMsgSaveFailed))
            } else {
                bumpRevision()
                refresh()
                PlatformUtils.showToast(getString(Res.string.duaMsgSaved))
                onSaved()
            }

            _isLoading.value = false
        }
    }

    /**
     * Başlığın adını dəyişir. **Slug toxunulmur** — o, duaların açarıdır; adı dəyişmək
     * qruplaşdırmanı pozmamalıdır.
     */
    fun renameCategory(slug: String, name: String, nameAr: String?, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true

            repository.renameCategory(slug, name.trim(), nameAr?.trim()?.takeIf { it.isNotBlank() })
                .onSuccess {
                    bumpRevision()
                    refresh()
                    PlatformUtils.showToast(getString(Res.string.duaMsgSaved))
                    onSaved()
                }
                .onFailure { PlatformUtils.showToast(getString(Res.string.duaMsgSaveFailed)) }

            _isLoading.value = false
        }
    }

    /** Alt başlığın adını dəyişir — bax [renameCategory]. */
    fun renameSubcategory(slug: String, name: String, nameAr: String?, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true

            repository.renameSubcategory(
                slug,
                name.trim(),
                nameAr?.trim()?.takeIf { it.isNotBlank() },
            )
                .onSuccess {
                    bumpRevision()
                    refresh()
                    PlatformUtils.showToast(getString(Res.string.duaMsgSaved))
                    onSaved()
                }
                .onFailure { PlatformUtils.showToast(getString(Res.string.duaMsgSaveFailed)) }

            _isLoading.value = false
        }
    }

    /**
     * Alt başlığı silir — içindəki dualar **itmir**, başlığın birbaşa altına qalxır
     * (bazada `on delete set null`).
     */
    fun deleteSubcategory(slug: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true

            repository.deleteSubcategory(slug)
                .onSuccess {
                    bumpRevision()
                    refresh()
                    PlatformUtils.showToast(getString(Res.string.duaMsgDeleted))
                    onDeleted()
                }
                .onFailure { PlatformUtils.showToast(getString(Res.string.duaMsgDeleteFailed)) }

            _isLoading.value = false
        }
    }

    /**
     * Duanı yazır; [categorySlug] null olanda əvvəlcə [newCategoryName] adı ilə yeni başlıq açır.
     *
     * Hər iki addım uğurlu olmasa heç nə göstərilmir: yarımçıq başlıq qalsa siyahıda boş ad
     * görünərdi.
     */
    fun saveDua(
        categorySlug: String?,
        newCategoryName: String?,
        newCategoryNameAr: String?,
        subcategorySlug: String?,
        newSubcategoryName: String?,
        dua: Dua,
        onSaved: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            val slug = categorySlug ?: createCategory(newCategoryName, newCategoryNameAr)

            if (slug == null) {
                PlatformUtils.showToast(getString(Res.string.duaMsgSaveFailed))
                _isLoading.value = false
                return@launch
            }

            // Alt başlıq **istəyə bağlıdır**: seçilməyibsə dua birbaşa başlığın altına düşür.
            val subSlug = subcategorySlug
                ?: newSubcategoryName?.takeIf { it.isNotBlank() }
                    ?.let { createSubcategory(slug, it, null) }

            repository.addDua(dua.copy(category_slug = slug, subcategory_slug = subSlug))
                .onSuccess {
                    bumpRevision()
                    refresh()
                    PlatformUtils.showToast(getString(Res.string.duaMsgSaved))
                    onSaved()
                }
                .onFailure { error ->
                    // Dublikat nə icazə, nə də bağlantı problemidir — ayrıca mesaj lazımdır.
                    PlatformUtils.showToast(
                        getString(
                            if (error is DuaDuplicateException) Res.string.duaMsgDuplicate
                            else Res.string.duaMsgSaveFailed,
                        ),
                    )
                }

            _isLoading.value = false
        }
    }

    /** Mövcud duanı yeniləyir — admin panelindəki «redaktə» axını. */
    fun updateDua(dua: Dua, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true

            repository.updateDua(dua)
                .onSuccess {
                    bumpRevision()
                    refresh()
                    PlatformUtils.showToast(getString(Res.string.duaMsgSaved))
                    onSaved()
                }
                .onFailure {
                    PlatformUtils.showToast(getString(Res.string.duaMsgSaveFailed))
                }

            _isLoading.value = false
        }
    }

    fun deleteDua(id: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true

            repository.deleteDua(id)
                .onSuccess {
                    bumpRevision()
                    refresh()
                    PlatformUtils.showToast(getString(Res.string.duaMsgDeleted))
                    onDeleted()
                }
                .onFailure {
                    PlatformUtils.showToast(getString(Res.string.duaMsgDeleteFailed))
                }

            _isLoading.value = false
        }
    }

    fun deleteCategory(slug: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true

            repository.deleteCategory(slug)
                .onSuccess {
                    bumpRevision()
                    refresh()
                    PlatformUtils.showToast(getString(Res.string.duaMsgDeleted))
                    onDeleted()
                }
                .onFailure {
                    PlatformUtils.showToast(getString(Res.string.duaMsgDeleteFailed))
                }

            _isLoading.value = false
        }
    }

    /**
     * Başlıqların yeni sırasını yazır — [slugs] ekrandakı **son** sıradır.
     *
     * Siyahı əvvəlcə yerli axında dəyişir (ekran dərhal yeni sıranı göstərir), sonra yalnız
     * **dəyişən** sətirlər serverə gedir. Yazma alınmasa [refresh] həqiqəti geri qaytarır: RLS
     * başqasının başlığını bloklaya bilər və o halda ekranda qalan sıra yalan olardı.
     */
    fun saveCategoryOrder(slugs: List<String>, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true

            val rank = slugs.withIndex().associate { (index, slug) -> slug to index }
            val changes = _categories.value
                .mapNotNull { category ->
                    val target = rank[category.slug] ?: return@mapNotNull null
                    if (category.sort_no == target) null else category.slug to target
                }

            _categories.value = _categories.value
                .map { it.copy(sort_no = rank[it.slug] ?: it.sort_no) }
                .sortedWith(compareBy({ it.sort_no }, { it.name }))

            commitOrder(changes.isEmpty(), onSaved) { repository.updateCategoryOrder(changes) }

            _isLoading.value = false
        }
    }

    /** Bir başlığın alt başlıqlarının sırası — bax [saveCategoryOrder]. */
    fun saveSubcategoryOrder(slugs: List<String>, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true

            val rank = slugs.withIndex().associate { (index, slug) -> slug to index }
            val changes = _subcategories.value
                .mapNotNull { subcategory ->
                    val target = rank[subcategory.slug] ?: return@mapNotNull null
                    if (subcategory.sort_no == target) null else subcategory.slug to target
                }

            _subcategories.value = _subcategories.value
                .map { it.copy(sort_no = rank[it.slug] ?: it.sort_no) }
                .sortedWith(compareBy({ it.sort_no }, { it.name }))

            commitOrder(changes.isEmpty(), onSaved) { repository.updateSubcategoryOrder(changes) }

            _isLoading.value = false
        }
    }

    /**
     * Bir qrupun duaları — bax [saveCategoryOrder].
     *
     * [ids] yalnız ekranda göstərilən qrupdur (bir alt başlıq, ya da başlığın birbaşa altındakılar),
     * ona görə nömrələmə həmin qrupun içində 0-dan gedir.
     */
    fun saveDuaOrder(ids: List<Long>, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true

            val rank = ids.withIndex().associate { (index, id) -> id to index }
            val changes = _duas.value
                .mapNotNull { dua ->
                    val id = dua.id ?: return@mapNotNull null
                    val target = rank[id] ?: return@mapNotNull null
                    if (dua.sort_no == target) null else id to target
                }

            _duas.value = _duas.value
                .map { dua -> dua.copy(sort_no = dua.id?.let { rank[it] } ?: dua.sort_no) }
                .sortedWith(compareBy({ it.sort_no }, { it.id ?: 0L }))

            commitOrder(changes.isEmpty(), onSaved) { repository.updateDuaOrder(changes) }

            _isLoading.value = false
        }
    }

    /**
     * Sıra yazmasının ortaq quyruğu: heç nə dəyişməyibsə şəbəkəyə çıxmır, uğurda sayğacı artırır,
     * uğursuzluqda isə **mütləq** [refresh] edir — yerli optimist sıra ekranda yalan qalmasın.
     */
    private suspend fun commitOrder(
        unchanged: Boolean,
        onSaved: () -> Unit,
        write: suspend () -> Result<Unit>,
    ) {
        if (unchanged) {
            onSaved()
            return
        }

        write()
            .onSuccess {
                bumpRevision()
                refresh()
                PlatformUtils.showToast(getString(Res.string.duaMsgSaved))
                onSaved()
            }
            .onFailure {
                PlatformUtils.showToast(getString(Res.string.duaMsgSaveFailed))
                refresh()
            }
    }

    /**
     * Yeni başlıq yaradıb slug-ını qaytarır, alınmasa null.
     *
     * Slug addan qurulur; ad latın hərfi daşımırsa (məsələn tam ərəbcə yazılıbsa) vaxt damğalı
     * ehtiyat sluga keçir. Eyni slug artıq varsa `-2`, `-3` … sınanır: baza PK toqquşması verir və
     * onsuz da ikinci cəhd lazım olur.
     */
    private suspend fun createCategory(name: String?, nameAr: String?): String? {
        val title = name?.trim().orEmpty()
        if (title.isEmpty()) return null

        val base = duaCategorySlug(title).ifEmpty { fallbackSlug(currentEpochMillis()) }
        val taken = _categories.value.mapTo(HashSet()) { it.slug }

        var slug = base
        var suffix = 2
        while (slug in taken) {
            slug = "${base.take(70)}-$suffix"
            suffix++
        }

        val nextSort = (_categories.value.maxOfOrNull { it.sort_no } ?: 0) + 1

        return repository.createCategory(
            DuaCategory(
                slug = slug,
                name = title,
                name_ar = nameAr?.trim()?.takeIf { it.isNotBlank() },
                sort_no = nextSort,
            ),
        ).getOrNull()?.slug
    }

    /** Yeni alt başlıq yaradıb slug-ını qaytarır, alınmasa null. Qayda [createCategory] ilə eynidir. */
    private suspend fun createSubcategory(
        categorySlug: String,
        name: String?,
        nameAr: String?,
    ): String? {
        val title = name?.trim().orEmpty()
        if (title.isEmpty()) return null

        val base = duaCategorySlug(title).ifEmpty { fallbackSlug(currentEpochMillis()) }
        // Slug bütün alt başlıqlar arasında unikaldır (PK), ona görə süzgəc başlığa görə deyil.
        val taken = _subcategories.value.mapTo(HashSet()) { it.slug }

        var slug = base
        var suffix = 2
        while (slug in taken) {
            slug = "${base.take(70)}-$suffix"
            suffix++
        }

        val nextSort = (_subcategories.value
            .filter { it.category_slug == categorySlug }
            .maxOfOrNull { it.sort_no } ?: 0) + 1

        return repository.createSubcategory(
            DuaSubcategory(
                slug = slug,
                category_slug = categorySlug,
                name = title,
                name_ar = nameAr?.trim()?.takeIf { it.isNotBlank() },
                sort_no = nextSort,
            ),
        ).getOrNull()?.slug
    }

    private fun bumpRevision() {
        duaContentRevision.value = duaContentRevision.value + 1
    }
}
