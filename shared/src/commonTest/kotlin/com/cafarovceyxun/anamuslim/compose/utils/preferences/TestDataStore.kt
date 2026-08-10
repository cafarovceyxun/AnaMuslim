package com.cafarovceyxun.anamuslim.compose.utils.preferences

import okio.FileSystem
import kotlin.random.Random

/**
 * Points [DataStoreManager] at a throwaway file so preference-backed logic can be exercised in a
 * unit test.
 *
 * `DataStoreManager.dataStore` is a process-wide `lateinit`, and the flow it exposes is built once
 * lazily — so this initialises exactly once per test process, and every test that needs preferences
 * shares that one store. Tests must therefore set the values they depend on rather than assume a
 * pristine store.
 *
 * Works on both JVM and native because the DataStore factory takes a plain okio path on both; the
 * file lives under the system temp directory, well away from [com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem],
 * whose root needs an Android `Context`.
 */
object TestDataStore {

    private var initialized = false

    fun ensureInitialized() {
        if (initialized) return
        initialized = true

        val path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY /
                "anamuslim_test_${Random.nextLong()}.preferences_pb"
        DataStoreManager.init { path.toString() }
    }
}
