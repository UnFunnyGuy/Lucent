package app.lucent.firefly.data.sync.api.usecase

import app.lucent.firefly.core.common.base.NoParamsUseCase
import app.lucent.firefly.data.sync.api.manager.SyncManager
import app.lucent.firefly.data.sync.api.model.SyncState
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory
class ObserveSyncStateUseCase(
    private val syncManager: SyncManager,
) : NoParamsUseCase<SyncState> {
    override fun performStreaming(): Flow<SyncState> = syncManager.syncState
}
