package com.cafarovceyxun.anamuslim.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.cafarovceyxun.anamuslim.db.converters.QuranConverters
import com.cafarovceyxun.anamuslim.db.dao.ArabicSearchDao
import com.cafarovceyxun.anamuslim.db.dao.AyahDao
import com.cafarovceyxun.anamuslim.db.dao.AyahWordDao
import com.cafarovceyxun.anamuslim.db.dao.ExtrasDao
import com.cafarovceyxun.anamuslim.db.dao.MushafDao
import com.cafarovceyxun.anamuslim.db.dao.NavigationDao
import com.cafarovceyxun.anamuslim.db.dao.SurahDao
import com.cafarovceyxun.anamuslim.db.dao.SurahSearchDao
import com.cafarovceyxun.anamuslim.db.entities.extras.MutashabihatPhraseAyahEntity
import com.cafarovceyxun.anamuslim.db.entities.extras.MutashabihatPhraseEntity
import com.cafarovceyxun.anamuslim.db.entities.extras.SimilarVerseEntity
import com.cafarovceyxun.anamuslim.db.entities.quran.AyahEntity
import com.cafarovceyxun.anamuslim.db.entities.quran.AyahWordEntity
import com.cafarovceyxun.anamuslim.db.entities.quran.MushafEntity
import com.cafarovceyxun.anamuslim.db.entities.quran.MushafMapEntity
import com.cafarovceyxun.anamuslim.db.entities.quran.NavigationRangeEntity
import com.cafarovceyxun.anamuslim.db.entities.quran.ScriptEntity
import com.cafarovceyxun.anamuslim.db.entities.quran.SurahAliasFtsEntity
import com.cafarovceyxun.anamuslim.db.entities.quran.SurahEntity
import com.cafarovceyxun.anamuslim.db.entities.quran.SurahLocalizationEntity
import com.cafarovceyxun.anamuslim.db.entities.quran.SurahSearchAliasEntity

@Database(
    entities = [
        SurahEntity::class,
        SurahLocalizationEntity::class,
        SurahSearchAliasEntity::class,
        SurahAliasFtsEntity::class,
        AyahEntity::class,
        ScriptEntity::class,
        AyahWordEntity::class,
        NavigationRangeEntity::class,
        MushafEntity::class,
        MushafMapEntity::class,
        SimilarVerseEntity::class,
        MutashabihatPhraseEntity::class,
        MutashabihatPhraseAyahEntity::class,
    ],
    // v9: `arabic_search` dropped from the managed entities (standalone FTS4 breaks the Room
    // compiler on native; it is queried via @RawQuery now). The identity hash changed, so the
    // bump makes existing installs re-copy the asset through fallbackToDestructiveMigration.
    version = 9,
    exportSchema = false
)
@TypeConverters(QuranConverters::class)
@ConstructedBy(QuranDatabaseConstructor::class)
abstract class QuranDatabase : RoomDatabase() {
    abstract fun arabicSearchDao(): ArabicSearchDao
    abstract fun surahDao(): SurahDao
    abstract fun surahSearchDao(): SurahSearchDao
    abstract fun ayahDao(): AyahDao
    abstract fun ayahWordDao(): AyahWordDao
    abstract fun navigationDao(): NavigationDao
    abstract fun mushafDao(): MushafDao
    abstract fun extrasDao(): ExtrasDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpectedDeclaration")
expect object QuranDatabaseConstructor : RoomDatabaseConstructor<QuranDatabase> {
    override fun initialize(): QuranDatabase
}
