package com.cafarovceyxun.anamuslim.compose.screens.dua

import androidx.compose.runtime.Composable
import com.cafarovceyxun.anamuslim.compose.components.common.ModeTab
import com.cafarovceyxun.anamuslim.compose.components.common.ModeTabIcon
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.arabicLabel
import com.cafarovceyxun.anamuslim.resources.ic_mode_mushaf
import com.cafarovceyxun.anamuslim.resources.ic_mode_translation
import com.cafarovceyxun.anamuslim.resources.ic_mode_verse
import com.cafarovceyxun.anamuslim.resources.labelTranslation
import com.cafarovceyxun.anamuslim.resources.strLabelMixed
import org.jetbrains.compose.resources.stringResource

/**
 * Dua səhifəsindəki üç mətn blokunun görünüşü.
 *
 * İki müstəqil mənbədən hesablanır və onlar **kəsişir**:
 * - **rejim zolağı** (`dua.v_mode`) — ani seçim: qarışıq / ərəbcə / tərcümə;
 * - **ayar açarları** (`dua.*_enabled`) — davamlı seçim: «bu bloku heç vaxt göstərmə».
 *
 * Rejim açarları ləğv etmir: ərəbcəni ayarda söndürmüş adam «Ərəbcə» rejiminə keçəndə onu geri
 * almaq istəmir. Nəticədə hər üç blokun gizlənə biləcəyi hal yaranır — [isEmpty] elə bunun üçündür,
 * UI həmin halda boş səhifə yox, izahlı mesaj göstərməlidir.
 */
internal data class DuaBlockVisibility(
    val arabic: Boolean,
    val transliteration: Boolean,
    val translation: Boolean,
) {
    /** Üç blokun hamısı gizlidir — səhifədə göstəriləsi mətn qalmır. */
    val isEmpty: Boolean get() = !arabic && !transliteration && !translation
}

/** Qarışıq — hər üç blok. */
internal const val DUA_MODE_MIXED = 0

/** Ərəbcə — yalnız ərəbcə mətn; oxunuş da, tərcümə də gizlənir. */
internal const val DUA_MODE_ARABIC = 1

/**
 * Tərcümə — tərcümə və **oxunuş**.
 *
 * Oxunuş burada qalır, ərəbcə rejimində yox: o, latın hərfləri ilə yazılıb və ərəbcə oxuya
 * bilməyən üçün tərcümənin yoldaşıdır, ərəbcə oxuyan üçün isə artıqdır.
 */
internal const val DUA_MODE_TRANSLATION = 2

/** Rejim zolağındakı tab sayı. */
internal const val DUA_MODE_COUNT = 3

/**
 * Effektiv görünüş — `rejim ∧ ayar`.
 *
 * @param mode [DUA_MODE_MIXED], [DUA_MODE_ARABIC] və ya [DUA_MODE_TRANSLATION]. Tanınmayan dəyər
 *   qarışıq sayılır: `dua.v_mode` DataStore-dandır və köhnə/zədələnmiş dəyər ekranı boş qoymamalıdır.
 */
internal fun duaBlockVisibility(
    mode: Int,
    arabicEnabled: Boolean,
    transliterationEnabled: Boolean,
    translationEnabled: Boolean,
): DuaBlockVisibility = when (mode) {
    DUA_MODE_ARABIC -> DuaBlockVisibility(
        arabic = arabicEnabled,
        transliteration = false,
        translation = false,
    )

    DUA_MODE_TRANSLATION -> DuaBlockVisibility(
        arabic = false,
        transliteration = transliterationEnabled,
        translation = translationEnabled,
    )

    else -> DuaBlockVisibility(
        arabic = arabicEnabled,
        transliteration = transliterationEnabled,
        translation = translationEnabled,
    )
}

/**
 * Rejim zolağının tabları — hədisdəki ilə **eyni nişanlar və eyni sıra**.
 *
 * Eyniliyi qəsdəndir: istifadəçi hədisdən duaya keçəndə zolağın mənasını yenidən öyrənməməlidir.
 */
@Composable
internal fun duaModeTabs(): List<ModeTab> = listOf(
    ModeTab(
        icon = ModeTabIcon.Painted(Res.drawable.ic_mode_verse),
        label = stringResource(Res.string.strLabelMixed),
    ),
    ModeTab(
        icon = ModeTabIcon.Painted(Res.drawable.ic_mode_mushaf),
        label = stringResource(Res.string.arabicLabel),
    ),
    ModeTab(
        icon = ModeTabIcon.Painted(Res.drawable.ic_mode_translation),
        label = stringResource(Res.string.labelTranslation),
    ),
)
