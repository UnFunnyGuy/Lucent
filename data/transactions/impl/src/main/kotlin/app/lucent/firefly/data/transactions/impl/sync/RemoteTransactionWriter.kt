package app.lucent.firefly.data.transactions.impl.sync

import app.lucent.firefly.core.database.dao.SyncQueueDao
import app.lucent.firefly.core.database.dao.TransactionDao
import app.lucent.firefly.core.database.model.SyncEntityType
import app.lucent.firefly.core.database.model.SyncStatus
import app.lucent.firefly.data.transactions.impl.dto.TransactionReadDto
import app.lucent.firefly.data.transactions.impl.mapper.parseIsoInstant
import app.lucent.firefly.data.transactions.impl.mapper.toGroupEntity
import app.lucent.firefly.data.transactions.impl.mapper.toSplitEntity
import org.koin.core.annotation.Factory

/** Writes server transactions into Room. Callers run it inside a database transaction. */
@Factory
class RemoteTransactionWriter(
    private val transactionDao: TransactionDao,
    private val syncQueueDao: SyncQueueDao,
) {
    /**
     * Upserts [dto], reusing the local ids of a group already linked to it (by server id, or by the
     * `external_id` it was created with). A group with unsynced local changes is left alone unless
     * [overwriteLocalChanges] is set; a group whose server `updated_at` is unchanged is skipped.
     *
     * @return the local id of the group.
     */
    suspend fun upsert(
        dto: TransactionReadDto,
        overwriteLocalChanges: Boolean = false,
    ): String {
        val externalIds =
            dto.attributes.transactions
                .mapNotNull { it.externalId }
                .toSet()
        val existing =
            transactionDao
                .getGroupByServerId(dto.id)
                ?.localId
                ?.let { transactionDao.getTransactionWithSplits(it) }
                ?: externalIds.firstNotNullOfOrNull { transactionDao.getTransactionWithSplits(it) }

        val serverUpdatedAt = parseIsoInstant(dto.attributes.updatedAt)
        if (existing != null && !overwriteLocalChanges) {
            val hasLocalChanges =
                existing.splits.any { it.syncStatus != SyncStatus.SYNCED } ||
                    syncQueueDao
                        .getForEntity(SyncEntityType.TRANSACTION, existing.group.localId)
                        .isNotEmpty()
            val unchanged =
                existing.group.serverId == dto.id &&
                    existing.group.updatedAt == serverUpdatedAt &&
                    existing.splits.size == dto.attributes.transactions.size
            if (hasLocalChanges || unchanged) return existing.group.localId
        }

        val groupEntity = dto.toGroupEntity(existingLocalId = existing?.group?.localId)
        val localSplitIdsByServerId =
            existing
                ?.splits
                ?.mapNotNull { split -> split.serverSplitId?.let { it to split.localSplitId } }
                ?.toMap()
                .orEmpty()
        val splitEntities =
            dto.attributes.transactions.mapIndexed { index, splitDto ->
                splitDto
                    .toSplitEntity(
                        groupId = groupEntity.localId,
                        existingLocalId = splitDto.transactionJournalId?.let { localSplitIdsByServerId[it] },
                    ).copy(order = index)
            }

        if (existing != null) {
            transactionDao.deleteSplitsByGroupId(existing.group.localId)
        }
        transactionDao.insertGroup(groupEntity)
        transactionDao.insertSplits(splitEntities)
        return groupEntity.localId
    }
}
