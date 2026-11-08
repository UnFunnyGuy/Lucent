package app.lucent.firefly.data.transactions.impl.dto

import app.lucent.firefly.core.network.model.PageMetaDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionArrayDto(
    val data: List<TransactionReadDto> = emptyList(),
    val meta: PageMetaDto? = null,
)

@Serializable
data class TransactionSingleDto(
    val data: TransactionReadDto,
)

@Serializable
data class TransactionReadDto(
    val type: String = "transactions",
    val id: String,
    val attributes: TransactionAttributesDto,
)

@Serializable
data class TransactionAttributesDto(
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val user: String? = null,
    @SerialName("group_title") val groupTitle: String? = null,
    val transactions: List<TransactionSplitDto> = emptyList(),
)

@Serializable
data class TransactionSplitDto(
    @SerialName("transaction_journal_id") val transactionJournalId: String? = null,
    val type: String,
    val date: String,
    val amount: String,
    val description: String,
    @SerialName("source_id") val sourceId: String? = null,
    @SerialName("source_name") val sourceName: String? = null,
    @SerialName("destination_id") val destinationId: String? = null,
    @SerialName("destination_name") val destinationName: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("category_name") val categoryName: String? = null,
    @SerialName("budget_id") val budgetId: String? = null,
    @SerialName("budget_name") val budgetName: String? = null,
    @SerialName("currency_id") val currencyId: String? = null,
    @SerialName("currency_code") val currencyCode: String? = null,
    @SerialName("currency_symbol") val currencySymbol: String? = null,
    @SerialName("currency_decimal_places") val currencyDecimalPlaces: Int? = null,
    val notes: String? = null,
    val tags: List<String>? = null,
    @SerialName("external_id") val externalId: String? = null,
)

@Serializable
data class TransactionStoreDto(
    @SerialName("error_if_duplicate_hash") val errorIfDuplicateHash: Boolean = false,
    @SerialName("apply_rules") val applyRules: Boolean = true,
    @SerialName("fire_webhooks") val fireWebhooks: Boolean = true,
    @SerialName("group_title") val groupTitle: String? = null,
    val transactions: List<TransactionSplitStoreDto>,
)

@Serializable
data class TransactionSplitStoreDto(
    val type: String,
    val date: String,
    val amount: String,
    val description: String,
    @SerialName("source_id") val sourceId: String? = null,
    @SerialName("source_name") val sourceName: String? = null,
    @SerialName("destination_id") val destinationId: String? = null,
    @SerialName("destination_name") val destinationName: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("category_name") val categoryName: String? = null,
    @SerialName("budget_id") val budgetId: String? = null,
    @SerialName("budget_name") val budgetName: String? = null,
    @SerialName("currency_code") val currencyCode: String? = null,
    val notes: String? = null,
    val tags: List<String>? = null,
    @SerialName("external_id") val externalId: String? = null,
)

@Serializable
data class TransactionUpdateDto(
    @SerialName("apply_rules") val applyRules: Boolean = true,
    @SerialName("fire_webhooks") val fireWebhooks: Boolean = true,
    @SerialName("group_title") val groupTitle: String? = null,
    val transactions: List<TransactionSplitUpdateDto>,
)

/** Splits are matched by [transactionJournalId]; omitted fields keep their server value. */
@Serializable
data class TransactionSplitUpdateDto(
    @SerialName("transaction_journal_id") val transactionJournalId: String?,
    val type: String,
    val date: String,
    val amount: String,
    val description: String,
    @SerialName("source_id") val sourceId: String? = null,
    @SerialName("source_name") val sourceName: String? = null,
    @SerialName("destination_id") val destinationId: String? = null,
    @SerialName("destination_name") val destinationName: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("category_name") val categoryName: String? = null,
    @SerialName("budget_id") val budgetId: String? = null,
    @SerialName("budget_name") val budgetName: String? = null,
    @SerialName("currency_code") val currencyCode: String? = null,
    val notes: String? = null,
    val tags: List<String>? = null,
)
