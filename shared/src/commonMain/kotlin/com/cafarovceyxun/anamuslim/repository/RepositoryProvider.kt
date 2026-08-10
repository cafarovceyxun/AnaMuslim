package com.cafarovceyxun.anamuslim.repository

import com.cafarovceyxun.anamuslim.db.searchindex.SearchIndexDatabase
import com.cafarovceyxun.anamuslim.db.ExternalQuranDatabase
import com.cafarovceyxun.anamuslim.db.HadithDatabase
import com.cafarovceyxun.anamuslim.db.translation.QuranTranslationStore

/**
 * Platform-neutral dependency-injection seam for data repositories, mirroring the existing
 * startup-provider pattern (`NetworkConfig`, `AppLocaleProvider`). Each platform builds its
 * repositories from its own `Context`/`Application` (Android) or `IosDatabaseProvider` (iOS)
 * and registers a provider here at startup; `commonMain` ViewModels then obtain repositories
 * without any `Context` coupling.
 *
 * Besides the higher-level repositories it also brokers the raw shared Room databases some
 * ViewModels access directly via their DAOs ([ExternalQuranDatabase], [HadithDatabase]).
 *
 * Registration points: Android `QuranApp.onCreate()`, iOS `initSharedForIos()`.
 *
 * Unlike the platform-subsystem seams (player, download and resource sources), the accessors here
 * **throw** when unset. The rule: a seam whose implementation exists on every platform fails loud,
 * because an unset provider can only mean a startup wiring bug; a seam that some platform has not
 * implemented yet falls back to an inert instance, so the UI degrades instead of crashing.
 */
object RepositoryProvider {

    private var quranRepositoryProvider: (() -> QuranRepository)? = null
    private var userRepositoryProvider: (() -> UserRepository)? = null
    private var externalQuranDatabaseProvider: (() -> ExternalQuranDatabase)? = null
    private var hadithDatabaseProvider: (() -> HadithDatabase)? = null
    private var searchIndexDatabaseProvider: (() -> SearchIndexDatabase)? = null
    private var quranTranslationStoreProvider: (() -> QuranTranslationStore)? = null

    /** Registers how [QuranRepository] is obtained on this platform. Call once at startup. */
    fun setQuranRepositoryProvider(provider: () -> QuranRepository) {
        quranRepositoryProvider = provider
    }

    /** Registers how [UserRepository] is obtained on this platform. Call once at startup. */
    fun setUserRepositoryProvider(provider: () -> UserRepository) {
        userRepositoryProvider = provider
    }

    /** Registers how [ExternalQuranDatabase] is obtained on this platform. Call once at startup. */
    fun setExternalQuranDatabaseProvider(provider: () -> ExternalQuranDatabase) {
        externalQuranDatabaseProvider = provider
    }

    /** Registers how [HadithDatabase] is obtained on this platform. Call once at startup. */
    fun setHadithDatabaseProvider(provider: () -> HadithDatabase) {
        hadithDatabaseProvider = provider
    }

    /** Registers how [SearchIndexDatabase] is obtained on this platform. Call once at startup. */
    fun setSearchIndexDatabaseProvider(provider: () -> SearchIndexDatabase) {
        searchIndexDatabaseProvider = provider
    }

    /** Registers how [QuranTranslationStore] is obtained on this platform. Call once at startup. */
    fun setQuranTranslationStoreProvider(provider: () -> QuranTranslationStore) {
        quranTranslationStoreProvider = provider
    }

    /**
     * The shared [QuranRepository]. The provider caches the instance on both platforms
     * (Android `DatabaseProvider`, iOS lazy holder), so repeated access is cheap.
     */
    val quranRepository: QuranRepository
        get() = quranRepositoryProvider?.invoke()
            ?: error(
                "QuranRepository provider not set — call RepositoryProvider." +
                    "setQuranRepositoryProvider(...) at app startup before any ViewModel is created."
            )

    /** The shared [UserRepository]; the provider caches the instance on both platforms. */
    val userRepository: UserRepository
        get() = userRepositoryProvider?.invoke()
            ?: error(
                "UserRepository provider not set — call RepositoryProvider." +
                    "setUserRepositoryProvider(...) at app startup before any ViewModel is created."
            )

    /** The shared [ExternalQuranDatabase]; the provider caches the instance on both platforms. */
    val externalQuranDatabase: ExternalQuranDatabase
        get() = externalQuranDatabaseProvider?.invoke()
            ?: error(
                "ExternalQuranDatabase provider not set — call RepositoryProvider." +
                    "setExternalQuranDatabaseProvider(...) at app startup before any ViewModel is created."
            )

    /** The shared [SearchIndexDatabase]; the provider caches the instance on both platforms. */
    val searchIndexDatabase: SearchIndexDatabase
        get() = searchIndexDatabaseProvider?.invoke()
            ?: error(
                "SearchIndexDatabase provider not set — call RepositoryProvider." +
                    "setSearchIndexDatabaseProvider(...) at app startup before any ViewModel is created."
            )

    /** The shared [HadithDatabase]; the provider caches the instance on both platforms. */
    val hadithDatabase: HadithDatabase
        get() = hadithDatabaseProvider?.invoke()
            ?: error(
                "HadithDatabase provider not set — call RepositoryProvider." +
                    "setHadithDatabaseProvider(...) at app startup before any ViewModel is created."
            )

    /**
     * The process-wide [QuranTranslationStore] backing `QuranTranslationFactory`; the provider
     * caches the single connection on both platforms.
     */
    val quranTranslationStore: QuranTranslationStore
        get() = quranTranslationStoreProvider?.invoke()
            ?: error(
                "QuranTranslationStore provider not set — call RepositoryProvider." +
                    "setQuranTranslationStoreProvider(...) at app startup before any translation is read."
            )
}
