package app.lucent.firefly.data.sync.api.usecase

import app.lucent.firefly.core.common.base.BaseUseCase
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.data.sync.api.manager.SyncManager
import org.koin.core.annotation.Factory

@Factory
class RequestSyncUseCase(
    private val syncManager: SyncManager,
) : BaseUseCase<Boolean, Result<Unit>> {
    override suspend fun perform(params: Boolean): Result<Unit> {
        syncManager.requestSync(expedited = params)
        return Result.Success(Unit)
    }
}
