package app.lucent.firefly.data.transactions.api.model

import app.lucent.firefly.core.common.model.Money

data class Transaction(
    val id: String,
    val serverId: String?,
    val title: String?,
    val splits: List<TransactionSplit>,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val primarySplit: TransactionSplit get() = splits.first()
    val totalAmount: Money get() = splits.map { it.amount }.reduce { acc, money -> acc + money }
    val isPendingSync: Boolean get() = splits.any { it.isPendingSync }
}
