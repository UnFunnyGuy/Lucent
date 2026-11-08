package app.lucent.firefly.data.sync.impl.processor

import app.lucent.firefly.core.common.result.AppError
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.core.database.dao.SyncQueueDao
import app.lucent.firefly.core.database.model.SyncEntityType
import app.lucent.firefly.data.auth.api.repository.AuthRepository
import app.lucent.firefly.data.sync.api.handler.SyncEntityHandler
import kotlinx.coroutines.flow.firstOrNull
import org.koin.core.annotation.Factory

data class DrainResult(
    val pushedAny: Boolean,
    /** A transient error stopped the run; try again with backoff. */
    val shouldRetry: Boolean,
    /** The server rejected the token; nothing more can be pushed until the user signs in. */
    val authenticationFailed: Boolean = false,
)

/** Drains the sync queue in queue order. */
@Factory
class SyncQueueProcessor(
    private val syncQueueDao: SyncQueueDao,
    private val authRepository: AuthRepository,
    private val statusStore: SyncStatusStore,
    private val handlers: List<SyncEntityHandler>,
) {

    suspend fun drain(): DrainResult {
        val handlerMap = handlers.associateBy { it.targetType }
        // An entity whose operation failed is blocked for the rest of this run so that
        // later operations on it (e.g. DELETE after a failed CREATE) never run out of order.
        val blockedEntities: MutableSet<Pair<SyncEntityType, String>> =
            syncQueueDao.getFailed().mapTo(mutableSetOf()) { it.entityType to it.localEntityId }
        val attemptedIds = mutableSetOf<Long>()
        var pushedAny = false

        // Items enqueued while this run is in progress are picked up by re-reading the queue.
        while (true) {
            val batch = syncQueueDao.getPendingQueue().filter { it.id !in attemptedIds }
            if (batch.isEmpty()) return DrainResult(pushedAny = pushedAny, shouldRetry = false)

            for (item in batch) {
                // Signed out mid-run: the queue stays for when the same account signs back in.
                if (authRepository.activeSession.firstOrNull() == null) {
                    return DrainResult(pushedAny = pushedAny, shouldRetry = false)
                }
                attemptedIds += item.id
                val entityKey = item.entityType to item.localEntityId
                if (entityKey in blockedEntities) continue

                val handler = handlerMap[item.entityType.toTargetType()]
                if (handler == null) {
                    syncQueueDao.markFailed(item.id, "No handler registered for ${item.entityType}")
                    blockedEntities += entityKey
                    continue
                }

                syncQueueDao.markAttemptStarted(item.id)
                val error = when (val result = handler.processSync(item.toEntry())) {
                    is Result.Success -> {
                        pushedAny = true
                        continue
                    }
                    is Result.Error -> result.error
                }
                blockedEntities += entityKey
                val attempts = item.attemptCount + 1
                when (error.failureKind()) {
                    FailureKind.TRANSIENT -> {
                        if (attempts >= MAX_TRANSIENT_ATTEMPTS) {
                            syncQueueDao.markFailed(item.id, error.errorMessage)
                        } else {
                            syncQueueDao.recordError(item.id, error.errorMessage)
                            return DrainResult(pushedAny = pushedAny, shouldRetry = true)
                        }
                    }
                    FailureKind.AUTHENTICATION -> {
                        syncQueueDao.recordError(item.id, error.errorMessage)
                        statusStore.markReauthenticationRequired()
                        return DrainResult(pushedAny = pushedAny, shouldRetry = false, authenticationFailed = true)
                    }
                    FailureKind.CONFLICT -> syncQueueDao.markFailed(item.id, error.errorMessage, isConflict = true)
                    FailureKind.PERMANENT -> syncQueueDao.markFailed(item.id, error.errorMessage)
                }
            }
        }
    }

    private enum class FailureKind { TRANSIENT, AUTHENTICATION, CONFLICT, PERMANENT }

    private fun AppError.failureKind(): FailureKind = when (this) {
        AppError.Network.NO_INTERNET,
        AppError.Network.TIMEOUT,
        AppError.Network.SERVER_UNREACHABLE,
        AppError.Network.SERVER_ERROR,
        AppError.Network.UNKNOWN -> FailureKind.TRANSIENT
        AppError.ApiKeyError -> FailureKind.AUTHENTICATION
        is AppError.Conflict -> FailureKind.CONFLICT
        // Rejected payloads, untrusted certificates and unreadable responses will not fix themselves.
        else -> FailureKind.PERMANENT
    }

    private companion object {
        const val MAX_TRANSIENT_ATTEMPTS = 10
    }
}
