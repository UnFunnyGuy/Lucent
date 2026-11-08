package app.lucent.firefly.data.transactions.api.model

import app.lucent.firefly.core.common.model.Money

data class TransactionDraft(
    val type: TransactionType,
    val date: Long = System.currentTimeMillis(),
    val amount: Money,
    val description: String,
    val sourceId: String? = null,
    val sourceName: String? = null,
    val destinationId: String? = null,
    val destinationName: String? = null,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val budgetId: String? = null,
    val budgetName: String? = null,
    val notes: String? = null,
    val tags: List<String> = emptyList(),
)
