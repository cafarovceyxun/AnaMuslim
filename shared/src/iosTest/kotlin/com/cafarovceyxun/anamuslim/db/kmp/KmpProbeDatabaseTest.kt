package com.cafarovceyxun.anamuslim.db.kmp

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Runtime proof that KMP Room works on iOS/native: the bundled SQLite driver opens a database,
 * runs a suspend insert/query round-trip, and returns correct data. If native linking of the
 * bundled driver or KSP codegen were broken, this would fail to build or crash at runtime.
 */
class KmpProbeDatabaseTest {
    @Test
    fun insertAndReadRoundTrip() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder<KmpProbeDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()

        val dao = db.probeDao()
        dao.insert(KmpProbeEntity(id = 1L, value = "salam"))
        dao.insert(KmpProbeEntity(id = 2L, value = "dünya"))

        val row = dao.get(1L)
        assertNotNull(row)
        assertEquals("salam", row.value)
        assertEquals(2, dao.count())

        db.close()
    }
}
