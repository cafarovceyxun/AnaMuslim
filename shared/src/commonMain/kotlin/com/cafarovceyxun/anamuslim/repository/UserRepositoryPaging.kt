package com.cafarovceyxun.anamuslim.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.cafarovceyxun.anamuslim.db.entities.user.BookmarkEntity
import com.cafarovceyxun.anamuslim.db.entities.user.HadithReadHistoryEntity
import com.cafarovceyxun.anamuslim.db.entities.user.ReadHistoryEntity
import com.cafarovceyxun.anamuslim.db.paging.OffsetPagingSource
import kotlinx.coroutines.flow.Flow

/**
 * Paging surface for [UserRepository], kept as extensions so the repository itself stays a plain
 * DAO facade. Paging 3 is multiplatform (see `shared/build.gradle.kts`), so these live in
 * commonMain alongside it.
 */

fun UserRepository.getBookmarksPagingFlow(
    pageSize: Int = 20
): Flow<PagingData<BookmarkEntity>> {
    return Pager(
        config = PagingConfig(pageSize = pageSize, enablePlaceholders = false),
        pagingSourceFactory = {
            OffsetPagingSource(userDatabase, arrayOf("user_bookmarks")) { limit, offset ->
                userDatabase.bookmarkDao().getBookmarksPaged(limit, offset)
            }
        }
    ).flow
}

fun UserRepository.getHistoriesPaginated(): Flow<PagingData<ReadHistoryEntity>> {
    return Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
        pagingSourceFactory = {
            OffsetPagingSource(userDatabase, arrayOf("read_history")) { limit, offset ->
                userDatabase.readHistoryDao().getAllPaged(limit, offset)
            }
        }
    ).flow
}

fun UserRepository.getHadithHistoriesPaginated(): Flow<PagingData<HadithReadHistoryEntity>> {
    return Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
        pagingSourceFactory = {
            OffsetPagingSource(userDatabase, arrayOf("hadith_read_history")) { limit, offset ->
                userDatabase.hadithReadHistoryDao().getAllPaged(limit, offset)
            }
        }
    ).flow
}
