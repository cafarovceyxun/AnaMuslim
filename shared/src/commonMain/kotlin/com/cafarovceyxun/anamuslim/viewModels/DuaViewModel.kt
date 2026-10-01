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
import com.cafarovceyxun.anamuslim.resources.duaMsgSavedPartial
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
import org.jetbrains.compose.resources.StringResource
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

/**
<<<<<<< Updated upstream
 * [DuaViewModel.saveDua]-nın nəticəsi — duanın **həqiqətən** yazıldığı yer.
 *
 * Yeni başlıq/alt başlıq istənibsə slug-ları yalnız yazılanda bəlli olur; seçim ekranı onları
 * götürüb formadakı «yeni ad» sahələrini təmizləyir.
 */
data class DuaSaveResult(
    val categorySlug: String,
    val subcategorySlug: String?,
    val headId: Long?,
=======
 * Seçim ekranından gələn bir dua — hədəfi və hissələri ([parts]-ın birincisi baş sətirdir).
 *
 * Başlıq ya mövcuddur ([categorySlug]), ya da yazılanda yaradılır ([newCategoryName]); alt başlıq
 * eyni qayda ilə, amma istəyə bağlıdır. Hissələrin `category_slug`/`part_of_id` sahələri burada
 * boşdur — onları [DuaViewModel.saveDuas] yazanda qoyur.
 */
data class DuaSaveRequest(
    val categorySlug: String?,
    val newCategoryName: String?,
    val newCategoryNameAr: String?,
    val subcategorySlug: String?,
    val newSubcategoryName: String?,
    val parts: List<Dua>,
>>>>>>> Stashed changes
)

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
     * Seçim ekranının **bütün** dualarını ardıcıl yazır — hər biri öz başlığına, hissələri isə baş
     * sətrə bağlanaraq ([Dua.part_of_id]).
     *
<<<<<<< Updated upstream
     * [parts]-ın birincisi **baş sətirdir**, qalanları onun hissələri (33 + 33 + 33 + 1) — hamısı
     * eyni başlığa düşür, hər birinin öz sayı var (bax [DuaRepository.addDuaWithParts]).
     *
     * [onSaved] **həll olunmuş** hədəfi alır: seçim ekranı «Saxla və davam et»dən sonra açıq qalır və
     * növbəti zikri eyni başlığa yazır. Yeni başlıq adı formada qalsaydı ikinci yazı onu yenidən
     * yaradardı və siyahıda `x`, `x-2` kimi iki eyni adlı başlıq çıxardı.
     *
     * Hər addım uğurlu olmasa heç nə göstərilmir: yarımçıq başlıq qalsa siyahıda boş ad görünərdi.
     */
    fun saveDua(
        categorySlug: String?,
        newCategoryName: String?,
        newCategoryNameAr: String?,
        subcategorySlug: String?,
        newSubcategoryName: String?,
        parts: List<Dua>,
        savedMessage: StringResource = Res.string.duaMsgSaved,
        onSaved: (DuaSaveResult) -> Unit = {},
    ) {
        val head = parts.firstOrNull() ?: return

=======
     * Yazma ardıcıldır və ilk xətada dayanır, ona görə yazılanlar həmişə siyahının **başıdır**:
     * [onResult] onların sayını alır, ekran onları qaralamadan çıxarır və qalanını təkrar göndərmək
     * olur. Hissədə xəta olarsa baş sətir də silinir (CASCADE yazılmış hissələri də aparır) — yarımçıq
     * dua siyahıda tam dua kimi görünərdi.
     *
     * Yeni başlıq/alt başlıq **ada görə bir dəfə** yaradılır: ikinci dua birincinin hədəfini miras
     * alanda o da «yeni başlıq: X» daşıyır və hər biri öz başlığını açsaydı siyahıda iki eyni adlı
     * başlıq (`x`, `x-2`) çıxardı. Eyni səbəbdən adı **artıq olan** başlıq yenidən yaradılmır —
     * yarımçıq yazmadan sonra təkrar göndərmə də belə dublikat açardı.
     */
    fun saveDuas(requests: List<DuaSaveRequest>, onResult: (saved: Int) -> Unit = {}) {
>>>>>>> Stashed changes
        viewModelScope.launch {
            _isLoading.value = true

            val categoryMemo = HashMap<String, String>()
            val subcategoryMemo = HashMap<String, String>()
            var saved = 0
            var failure: Throwable? = null

            for (request in requests) {
                val result = saveRequest(request, categoryMemo, subcategoryMemo)
                failure = result.exceptionOrNull()
                if (failure != null) break
                saved++
            }

            if (saved > 0) {
                bumpRevision()
                refresh()
            }

<<<<<<< Updated upstream
            repository
                .addDuaWithParts(
                    head = head.copy(category_slug = slug, subcategory_slug = subSlug),
                    parts = parts.drop(1),
                )
                .onSuccess { written ->
                    bumpRevision()
                    refresh()
                    PlatformUtils.showToast(getString(savedMessage))
                    onSaved(
                        DuaSaveResult(
                            categorySlug = slug,
                            subcategorySlug = subSlug,
                            headId = written.id,
                        ),
                    )
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
=======
            // Dublikat nə icazə, nə də bağlantı problemidir — ayrıca mesaj lazımdır.
            val reason = getString(
                if (failure is DuaDuplicateException) Res.string.duaMsgDuplicate
                else Res.string.duaMsgSaveFailed,
            )
            PlatformUtils.showToast(
                when {
                    failure == null -> getString(Res.string.duaMsgSaved)
                    saved == 0 -> reason
                    else -> getString(Res.string.duaMsgSavedPartial, saved, requests.size, reason)
                },
            )
>>>>>>> Stashed changes

            _isLoading.value = false
            onResult(saved)
        }
    }

    /** Bir duanı (baş sətir + hissələri) yazır — bax [saveDuas]. */
    private suspend fun saveRequest(
        request: DuaSaveRequest,
        categoryMemo: MutableMap<String, String>,
        subcategoryMemo: MutableMap<String, String>,
    ): Result<Unit> {
        val head = request.parts.firstOrNull()
            ?: return Result.failure(IllegalArgumentException("hissə yoxdur"))

        val slug = request.categorySlug
            ?: resolveCategory(request.newCategoryName, request.newCategoryNameAr, categoryMemo)
            ?: return Result.failure(IllegalStateException("başlıq yaradılmadı"))

        // Alt başlıq **istəyə bağlıdır**: seçilməyibsə dua birbaşa başlığın altına düşür. Amma
        // yeni alt başlıq istənib yaradılmayıbsa dua yazılmır — onu səssizcə başlığın altına salmaq
        // yanlış yerə yazmaq olardı.
        val newSubName = request.newSubcategoryName?.trim()?.takeIf { it.isNotEmpty() }
        val subSlug = request.subcategorySlug
            ?: newSubName?.let {
                resolveSubcategory(slug, it, subcategoryMemo)
                    ?: return Result.failure(IllegalStateException("alt başlıq yaradılmadı"))
            }

        val written = repository
            .addDua(head.copy(category_slug = slug, subcategory_slug = subSlug, part_of_id = null))
            .getOrElse { return Result.failure(it) }

        val headId = written.id
            ?: return Result.failure(IllegalStateException("baş sətrin id-si gəlmədi"))

        request.parts.drop(1).forEachIndexed { index, part ->
            repository
                .addDua(
                    part.copy(
                        category_slug = slug,
                        subcategory_slug = subSlug,
                        part_of_id = headId,
                        part_no = index + 2,
                    ),
                )
                .onFailure { error ->
                    repository.deleteDua(headId)
                    return Result.failure(error)
                }
        }

        return Result.success(Unit)
    }

    /** Yeni başlığın slug-ı: bu yazmada artıq açılıbsa o, adı olan başlıq varsa o, yoxsa yenisi. */
    private suspend fun resolveCategory(
        name: String?,
        nameAr: String?,
        memo: MutableMap<String, String>,
    ): String? {
        val title = name?.trim().orEmpty()
        if (title.isEmpty()) return null

        val key = title.lowercase()
        memo[key]?.let { return it }

        val slug = _categories.value.firstOrNull { it.name.trim().lowercase() == key }?.slug
            ?: createCategory(title, nameAr)
            ?: return null

        memo[key] = slug
        return slug
    }

    /** Yeni alt başlığın slug-ı — qayda [resolveCategory] ilə eynidir, ad başlıq daxilində axtarılır. */
    private suspend fun resolveSubcategory(
        categorySlug: String,
        name: String,
        memo: MutableMap<String, String>,
    ): String? {
        val key = "$categorySlug/${name.lowercase()}"
        memo[key]?.let { return it }

        val slug = _subcategories.value
            .firstOrNull { it.category_slug == categorySlug && it.name.trim().lowercase() == name.lowercase() }
            ?.slug
            ?: createSubcategory(categorySlug, name, null)
            ?: return null

        memo[key] = slug
        return slug
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

    /**
     * İki duanı **bir duanın hissələri** edir, və ya hissəni geri ayırır.
     *
     * Hissə ayrıca `dua` sətri olaraq qalır (öz mənbəyi, öz sayı, öz mətni ilə) — dəyişən yalnız
     * bağlantıdır. `partOfId = null` verildikdə sətir yenidən müstəqil duaya çevrilir.
     */
    fun setDuaPart(duaId: Long, partOfId: Long?, partNo: Int, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true

            repository.setDuaPart(duaId, partOfId, partNo)
                .onSuccess {
                    bumpRevision()
                    refresh()
                    PlatformUtils.showToast(getString(Res.string.duaMsgSaved))
                    onDone()
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
        )
            .getOrNull()
<<<<<<< Updated upstream
            // Yerli siyahıya **dərhal** düşür: seçim ekranı hədəf sətrini bu siyahıdan qurur və
            // «Saxla və davam et»dən sonra [refresh] gələnə qədər «seçilməyib» göstərərdi.
=======
            // Yerli siyahıya da düşür: bir yazmada iki yeni başlıq eyni slug-a gəlsə, ikincisi
            // [refresh]-i gözləmədən `-2` alsın (bazada PK toqquşması verərdi).
>>>>>>> Stashed changes
            ?.also { created -> _categories.value = _categories.value + created }
            ?.slug
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
        )
            .getOrNull()
            ?.also { created -> _subcategories.value = _subcategories.value + created }
            ?.slug
    }

    private fun bumpRevision() {
        duaContentRevision.value = duaContentRevision.value + 1
    }
}
