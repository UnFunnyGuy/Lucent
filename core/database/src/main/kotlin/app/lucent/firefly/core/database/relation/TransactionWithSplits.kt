package app.lucent.firefly.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import app.lucent.firefly.core.database.entity.TransactionGroupEntity
import app.lucent.firefly.core.database.entity.TransactionSplitEntity

data class TransactionWithSplits(
    @Embedded val group: TransactionGroupEntity,
    @Relation(
        parentColumn = "localId",
        entityColumn = "transactionGroupId",
    )
    val splits: List<TransactionSplitEntity>,
)
