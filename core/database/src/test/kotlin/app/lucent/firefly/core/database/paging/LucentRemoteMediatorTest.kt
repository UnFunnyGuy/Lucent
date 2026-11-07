package app.lucent.firefly.core.database.paging

import android.content.Context
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.lucent.firefly.core.common.result.AppError
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.database.LucentDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPagingApi::class)
@RunWith(RobolectricTestRunner::class)
class LucentRemoteMediatorTest {

    private lateinit var database: LucentDatabase
    private val requestedPages = mutableListOf<Int>()
    private val persisted = mutableListOf<String>()
    private var clock = 1_000_000L
    private val pages = mapOf(
        1 to RemotePage(listOf("a", "b"), hasNextPage = true),
        2 to RemotePage(listOf("c"), hasNextPage = false),
    )
    private val emptyState = PagingState<Int, String>(
        pages = emptyList(),
        anchorPosition = null,
        config = PagingConfig(pageSize = 2),
        leadingPlaceholderCount = 0,
    )

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            LucentDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun teardown() {
        database.close()
    }

    private fun mediator(fetch: suspend (Int) -> Result<RemotePage<String>> = { Result.Success(pages.getValue(it)) }) =
        LucentRemoteMediator<String, String>(
            database = database,
            remoteKeyDao = database.remoteKeyDao(),
            listKey = "items",
            fetchPage = { page -> requestedPages += page; fetch(page) },
            persistPage = { persisted += it },
            cacheTimeoutMillis = 100,
            now = { clock },
        )

    @Test
    fun refreshesOnFirstOpenAndSkipsWhileFresh() = runTest {
        val mediator = mediator()
        assertEquals(RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH, mediator.initialize())

        mediator.load(LoadType.REFRESH, emptyState)
        clock += 50
        assertEquals(RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH, mediator.initialize())

        clock += 100
        assertEquals(RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH, mediator.initialize())
    }

    @Test
    fun refreshThenAppendWalksPagesUntilTheEnd() = runTest {
        val mediator = mediator()

        val refresh = mediator.load(LoadType.REFRESH, emptyState) as RemoteMediator.MediatorResult.Success
        val append = mediator.load(LoadType.APPEND, emptyState) as RemoteMediator.MediatorResult.Success
        val afterEnd = mediator.load(LoadType.APPEND, emptyState) as RemoteMediator.MediatorResult.Success

        assertFalse(refresh.endOfPaginationReached)
        assertTrue(append.endOfPaginationReached)
        assertTrue(afterEnd.endOfPaginationReached)
        assertEquals(listOf(1, 2), requestedPages)
        assertEquals(listOf("a", "b", "c"), persisted)
    }

    @Test
    fun appendDoesNotCountAsARefresh() = runTest {
        val mediator = mediator()
        mediator.load(LoadType.REFRESH, emptyState)
        clock += 90
        mediator.load(LoadType.APPEND, emptyState)
        clock += 20

        assertEquals(RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH, mediator.initialize())
    }

    @Test
    fun prependNeverFetches() = runTest {
        val result = mediator().load(LoadType.PREPEND, emptyState) as RemoteMediator.MediatorResult.Success

        assertTrue(result.endOfPaginationReached)
        assertTrue(requestedPages.isEmpty())
    }

    @Test
    fun failedFetchReportsTypedErrorAndKeepsTheKey() = runTest {
        val mediator = mediator()
        mediator.load(LoadType.REFRESH, emptyState)

        val failing = mediator { Result.Error(AppError.Network.TIMEOUT) }
        val result = failing.load(LoadType.APPEND, emptyState) as RemoteMediator.MediatorResult.Error

        assertEquals(AppError.Network.TIMEOUT, (result.throwable as RemotePageException).error)
        assertEquals(2, database.remoteKeyDao().getKey("items", LucentRemoteMediator.LIST_ENTRY)?.nextPage)
    }
}
