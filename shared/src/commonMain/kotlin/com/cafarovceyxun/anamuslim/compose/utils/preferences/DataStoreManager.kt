package com.cafarovceyxun.anamuslim.compose.utils.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.cafarovceyxun.anamuslim.utils.preferences.createDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.Volatile

data class PrefKey<T>(
    val key: Preferences.Key<T>,
    val default: T
)

class PrefResult(private val map: Map<PrefKey<*>, Any?>) {
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: PrefKey<T>): T {
        return map[key] as T
    }

    fun toKey(): String {
        return map.values.joinToString("|") { it?.toString() ?: "" }
    }
}

object DataStoreManager {
    private lateinit var dataStore: DataStore<Preferences>

    fun init(producePath: () -> String) {
        dataStore = createDataStore(producePath)
    }

    private val dataFlow by lazy {
        dataStore.data
            .distinctUntilChanged()
            .shareIn(
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                started = SharingStarted.WhileSubscribed(5000), replay = 1
            )
    }

    /**
     * The last preferences DataStore published. Every non-suspending [read] is answered from here.
     *
     * Without it a preference lookup on the UI thread is `runBlocking` over a disk read, which parks
     * a user-interactive thread on DataStore's IO worker — a priority inversion that Xcode's Thread
     * Performance Checker reports as a hang risk, and that on a cold start is a real stall. It is
     * also what kept the iOS composition root from reading the onboarding flag without freezing
     * launch.
     *
     * [warmUp] fills it before anything composes; [publish] and the collector [warmUp] starts keep
     * it current. Null only before warm-up, where [read] still falls back to blocking.
     */
    @Volatile
    private var snapshot: Preferences? = null

    private val snapshotScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var warmedUp = false

    /**
     * Loads [snapshot] and keeps it following the store. Call once at startup, off the main thread
     * and before the first composition — Android from `QuranApp.onCreate()`, iOS from the bootstrap
     * the composition root awaits.
     */
    suspend fun warmUp() {
        if (warmedUp) return
        warmedUp = true

        snapshot = dataStore.data.first()
        // Deliberately not `dataFlow`: its WhileSubscribed sharing would drop this collector five
        // seconds after the last UI observer goes away, and the snapshot would then go stale.
        dataStore.data.onEach { snapshot = it }.launchIn(snapshotScope)
    }

    /**
     * Applies what an edit returned to [snapshot] at once. The collector gets there on its own, but
     * not before a `read()` on the very next line — which used to see the new value, because it
     * went to the store itself.
     */
    private fun publish(edited: Preferences) {
        if (warmedUp) snapshot = edited
    }

    fun <T> read(prefKey: PrefKey<T>): T {
        return read(prefKey.key, prefKey.default)
    }

    fun <T> read(key: Preferences.Key<T>, defaultValue: T): T {
        snapshot?.let { return it[key] ?: defaultValue }

        return runBlocking {
            readFirst(key, defaultValue)
        }
    }

    suspend fun <T> readFirst(prefKey: PrefKey<T>): T {
        return readFirst(prefKey.key, prefKey.default)
    }

    suspend fun <T> readFirst(key: Preferences.Key<T>, defaultValue: T): T {
        val preferences = dataStore.data.first()
        return preferences[key] ?: defaultValue
    }

    suspend fun <T> write(prefKey: PrefKey<T>, value: T) {
        write(prefKey.key, value)
    }

    suspend fun <T> write(key: Preferences.Key<T>, value: T) {
        publish(
            dataStore.edit { preferences ->
                preferences[key] = value
            }
        )
    }

    suspend fun edit(transform: suspend MutablePreferences.() -> Unit) {
        publish(dataStore.edit { it.transform() })
    }

    /**
     * Saxlanılan bütün ayarlar, ad → dəyər. Ehtiyat nüsxə faylını qurmaq üçündür
     * ([com.cafarovceyxun.anamuslim.utils.univ.PreferenceBackup]).
     *
     * Snepşotdan yox, birbaşa mağazadan oxunur: eksport nadir və istifadəçinin gözlədiyi
     * əməliyyatdır, snepşot isə warm-up-dan əvvəl boş ola bilər.
     */
    suspend fun snapshotAll(): Map<String, Any> {
        return dataStore.data.first().asMap().entries.associate { (key, value) -> key.name to value }
    }

    /**
     * [values]-i bir redaktədə yazır. Açarın tipi çağıranın məsuliyyətidir — `Preferences` dəyəri
     * açarın tipi ilə oxuyur, yanlış tiplə yazılan açar **oxunanda** `ClassCastException` verir.
     * `PreferenceBackup.decode` açarları məhz buna görə tip etiketi ilə yenidən qurur.
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun writeAll(values: Map<Preferences.Key<*>, Any>) {
        if (values.isEmpty()) return

        publish(
            dataStore.edit { preferences ->
                values.forEach { (key, value) ->
                    preferences[key as Preferences.Key<Any>] = value
                }
            }
        )
    }

    suspend fun <T> remove(prefKey: PrefKey<T>) {
        remove(prefKey.key)
    }

    suspend fun <T> remove(key: Preferences.Key<T>) {
        publish(
            dataStore.edit { preferences ->
                preferences.remove(key)
            }
        )
    }

    suspend fun removeAll(vararg keys: Preferences.Key<*>) {
        publish(
            dataStore.edit { preferences ->
                keys.forEach {
                    preferences.remove(it)
                }
            }
        )
    }

    suspend fun <T> contains(key: Preferences.Key<T>): Boolean {
        val preferences = dataStore.data.first()
        return preferences.contains(key)
    }

    fun <T> flow(prefKey: PrefKey<T>): Flow<T> {
        return flow(prefKey.key, prefKey.default)
    }

    fun <T> flow(
        key: Preferences.Key<T>,
        defaultValue: T
    ): Flow<T> {
        return dataFlow
            .map { it[key] ?: defaultValue }
            .distinctUntilChanged()
    }

    fun flowMultiple(vararg keys: PrefKey<*>): Flow<PrefResult> {
        return dataFlow
            .map { preferences ->
                val map = keys.associateWith { prefKey ->
                    preferences[prefKey.key] ?: prefKey.default
                }
                PrefResult(map)
            }
            .distinctUntilChanged()
    }

    @Composable
    fun <T> observe(prefKey: PrefKey<T>): T {
        return observe(prefKey.key, prefKey.default)
    }

    @Composable
    fun <T> observe(
        key: Preferences.Key<T>,
        defaultValue: T
    ): T {
        val flow = remember(key) {
            dataFlow
                .map { it[key] ?: defaultValue }
                .distinctUntilChanged()
        }

        return flow.collectAsState(initial = defaultValue).value
    }
}
