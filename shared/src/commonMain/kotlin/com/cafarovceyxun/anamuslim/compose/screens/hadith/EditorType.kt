package com.cafarovceyxun.anamuslim.compose.screens.hadith

/**
 * What a hadith editor session is creating/editing.
 *
 * Declared here rather than inside `HadithEditorScreen` (`:app`) because the shared
 * [com.cafarovceyxun.anamuslim.viewModels.HadithViewModel] takes it as a parameter — a ViewModel
 * must not depend on a screen. The package is kept so existing `:app` callers resolve it unchanged.
 */
enum class EditorType { VOLUME, BOOK, CHAPTER, SUB_CHAPTER, HADITH }
