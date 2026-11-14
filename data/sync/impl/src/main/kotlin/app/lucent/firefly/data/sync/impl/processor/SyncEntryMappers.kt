package app.lucent.firefly.data.sync.impl.processor

import app.lucent.firefly.core.database.entity.SyncQueueEntity
import app.lucent.firefly.core.database.model.SyncAction
import app.lucent.firefly.core.database.model.SyncEntityType
import app.lucent.firefly.data.sync.api.handler.SyncEntry
import app.lucent.firefly.data.sync.api.handler.SyncOperation
import app.lucent.firefly.data.sync.api.handler.SyncTargetType

internal fun SyncQueueEntity.toEntry() =
    SyncEntry(
        queueId = id,
        operation = action.toOperation(),
        localId = localEntityId,
        serverId = serverEntityId,
        baseUpdatedAt = baseUpdatedAt,
        previousAttempts = attemptCount,
    )

internal fun SyncEntityType.toTargetType(): SyncTargetType =
    when (this) {
        SyncEntityType.TRANSACTION -> SyncTargetType.TRANSACTION
        SyncEntityType.ACCOUNT -> SyncTargetType.ACCOUNT
        SyncEntityType.CATEGORY -> SyncTargetType.CATEGORY
        SyncEntityType.BUDGET -> SyncTargetType.BUDGET
    }

internal fun SyncAction.toOperation(): SyncOperation =
    when (this) {
        SyncAction.CREATE -> SyncOperation.CREATE
        SyncAction.UPDATE -> SyncOperation.UPDATE
        SyncAction.DELETE -> SyncOperation.DELETE
    }
