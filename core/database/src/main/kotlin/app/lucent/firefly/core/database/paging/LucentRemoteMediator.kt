package app.lucent.firefly.core.database.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import app.lucent.firefly.core.common.result.AppError
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.database.dao.RemoteKeyDao
import app.lucent.firefly.core.database.entity.RemoteKeyEntity
import java.util.concurrent.TimeUnit

/** One page from a page-numbered API. */
data class RemotePage<T>(
    val items: List<T>,
    val hasNextPage: Boolean,
)

/** Carries the typed [AppError] of a failed page load through Paging's `LoadState.Error`. */
class RemotePageException(
    val error: AppError,
) : Exception(error.errorMessage)

/**
 * Pages a page-numbered remote list into Room, which stays the only source the UI reads.
 *
 * Any list plugs in with two functions: [fetchPage] (call the API for page N) and [persistPage]
 * (write those items into Room). The next page is kept per [listKey] in `remote_keys`.
 *
 * Unlike the common "clear and reload" pattern, REFRESH updates rows in place: the table also holds
 * changes that have not been uploaded yet, so it must never be wiped. [persistPage] is expected to
 * upsert and to leave rows with local changes alone; removing rows deleted on the server is the job
 * of the sync reconciler, not of paging.
 *
 * @param cacheTimeoutMillis how long a previous refresh stays fresh; within it, opening the list
 * shows cached rows without a network call.
 */
@OptIn(ExperimentalPagingApi::class)
class LucentRemoteMediator<Value : Any, Dto>(
    private val database: RoomDatabase,
    private val remoteKeyDao: RemoteKeyDao,
    private val listKey: String,
    private val fetchPage: suspend (page: Int) -> Result<RemotePage<Dto>>,
    private val persistPage: suspend (items: List<Dto>) -> Unit,
    private val cacheTimeoutMillis: Long = DEFAULT_CACHE_TIMEOUT_MILLIS,
    private val now: () -> Long = System::currentTimeMillis,
) : RemoteMediator<Int, Value>() {
    override suspend fun initialize(): InitializeAction {
        val lastRefresh =
            remoteKeyDao.getKey(listKey, LIST_ENTRY)?.lastUpdated
                ?: return InitializeAction.LAUNCH_INITIAL_REFRESH
        return if (now() - lastRefresh <= cacheTimeoutMillis) {
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Value>,
    ): MediatorResult {
        val page =
            when (loadType) {
                LoadType.REFRESH -> FIRST_PAGE
                // The list starts at the newest item; REFRESH always covers the top.
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    // No key yet means no refresh has completed; start from the first page.
                    val key = remoteKeyDao.getKey(listKey, LIST_ENTRY)
                    when {
                        key == null -> FIRST_PAGE
                        key.nextPage == null -> return MediatorResult.Success(endOfPaginationReached = true)
                        else -> key.nextPage
                    }
                }
            }

        val remotePage =
            when (val result = fetchPage(page)) {
                is Result.Success -> result.data
                is Result.Error -> return MediatorResult.Error(RemotePageException(result.error))
            }

        database.withTransaction {
            val previous = remoteKeyDao.getKey(listKey, LIST_ENTRY)
            remoteKeyDao.insertOrReplace(
                RemoteKeyEntity(
                    entityType = listKey,
                    entityId = LIST_ENTRY,
                    prevPage = null,
                    nextPage = if (remotePage.hasNextPage) page + 1 else null,
                    // lastUpdated marks the last REFRESH, which is what initialize() checks.
                    lastUpdated = if (loadType == LoadType.REFRESH) now() else previous?.lastUpdated ?: now(),
                ),
            )
            persistPage(remotePage.items)
        }
        return MediatorResult.Success(endOfPaginationReached = !remotePage.hasNextPage)
    }

    companion object {
        const val FIRST_PAGE = 1

        /** `remote_keys` stores one row per list; this is its entity id. */
        const val LIST_ENTRY = "list"

        val DEFAULT_CACHE_TIMEOUT_MILLIS: Long = TimeUnit.MINUTES.toMillis(15)
    }
}
