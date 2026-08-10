package com.cafarovceyxun.anamuslim.db.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import com.cafarovceyxun.anamuslim.concurrent.ReentrantLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import com.cafarovceyxun.anamuslim.concurrent.withLock

/**
 * Offset-based [PagingSource] backed by suspend `LIMIT/OFFSET` queries on a commonMain Room DAO.
 *
 * Room-KMP does not generate `PagingSource` return types, so the previous `room-paging` types were
 * dropped. This restores the same behaviour, including live updates: it observes the database
 * [InvalidationTracker] for the given [tables] and calls [invalidate] on any change, so
 * inserts/deletes refresh the list exactly as Room's own paging did.
 *
 * Placeholders are not supported (callers use `enablePlaceholders = false`).
 */
class OffsetPagingSource<T : Any>(
    private val db: RoomDatabase,
    tables: Array<String>,
    private val load: suspend (limit: Int, offset: Int) -> List<T>,
) : PagingSource<Int, T>() {

    private val tables = tables

    // Room-KMP's common InvalidationTracker exposes `createFlow`, not the JVM `Observer` API, so
    // invalidation is observed from a scope owned by this source. `load` can be called
    // concurrently, so the one-time subscription is guarded by the shared lock seam
    // (`AtomicBoolean.compareAndSet` is JVM-only).
    private val observerLock = ReentrantLock()
    private var observerRegistered = false
    private val observerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // A PagingSource is single-use: once invalidated Paging builds a new one, so the scope can
        // be torn down with it.
        registerInvalidatedCallback { observerScope.cancel() }
    }

    override fun getRefreshKey(state: PagingState<Int, T>): Int? =
        state.anchorPosition?.let { anchor ->
            val page = state.closestPageToPosition(anchor)
            page?.prevKey?.plus(state.config.pageSize)
                ?: page?.nextKey?.minus(state.config.pageSize)
        }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        val shouldRegister = observerLock.withLock {
            if (observerRegistered) false else { observerRegistered = true; true }
        }
        if (shouldRegister) {
            observerScope.launch {
                db.invalidationTracker
                    .createFlow(*tables, emitInitialState = false)
                    .collect { invalidate() }
            }
        }

        val offset = params.key ?: 0
        val limit = params.loadSize

        return try {
            val data = load(limit, offset)
            LoadResult.Page(
                data = data,
                prevKey = if (offset <= 0) null else maxOf(0, offset - limit),
                nextKey = if (data.size < limit) null else offset + limit,
            )
        } catch (t: Throwable) {
            LoadResult.Error(t)
        }
    }
}
