package app.lucent.firefly.data.transactions.api.model

import app.lucent.firefly.core.common.model.Money

data class TransactionSplit(
    val splitId: String,
    val serverSplitId: String?,
    val groupId: String,
    val type: TransactionType,
    val date: Long,
    val amount: Money,
    val description: String,
    val sourceId: String?,
    val sourceName: String?,
    val destinationId: String?,
    val destinationName: String?,
    val categoryId: String?,
    val categoryName: String?,
    val budgetId: String?,
    val budgetName: String?,
    val notes: String?,
    val tags: List<String>,
    val isPendingSync: Boolean,
)
