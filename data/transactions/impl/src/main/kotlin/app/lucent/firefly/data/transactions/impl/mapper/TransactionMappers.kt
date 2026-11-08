package app.lucent.firefly.data.transactions.impl.mapper

import app.lucent.firefly.core.common.model.Currency
import app.lucent.firefly.core.common.model.Money
import app.lucent.firefly.core.database.entity.TransactionGroupEntity
import app.lucent.firefly.core.database.entity.TransactionSplitEntity
import app.lucent.firefly.core.database.model.SyncStatus
import app.lucent.firefly.core.database.relation.TransactionWithSplits
import app.lucent.firefly.data.transactions.api.model.Transaction
import app.lucent.firefly.data.transactions.api.model.TransactionDraft
import app.lucent.firefly.data.transactions.api.model.TransactionSplit
import app.lucent.firefly.data.transactions.api.model.TransactionType
import app.lucent.firefly.data.transactions.impl.dto.TransactionReadDto
import app.lucent.firefly.data.transactions.impl.dto.TransactionSplitDto
import app.lucent.firefly.data.transactions.impl.dto.TransactionSplitStoreDto
import app.lucent.firefly.data.transactions.impl.dto.TransactionSplitUpdateDto
import app.lucent.firefly.data.transactions.impl.dto.TransactionStoreDto
import app.lucent.firefly.data.transactions.impl.dto.TransactionUpdateDto
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

fun TransactionReadDto.toGroupEntity(existingLocalId: String? = null): TransactionGroupEntity {
    return TransactionGroupEntity(
        localId = existingLocalId ?: UUID.randomUUID().toString(),
        serverId = id,
        groupTitle = attributes.groupTitle,
        createdAt = parseIsoInstant(attributes.createdAt),
        updatedAt = parseIsoInstant(attributes.updatedAt),
    )
}

fun TransactionSplitDto.toSplitEntity(
    groupId: String,
    existingLocalId: String? = null,
): TransactionSplitEntity {
    return TransactionSplitEntity(
        localSplitId = existingLocalId ?: UUID.randomUUID().toString(),
        transactionGroupId = groupId,
        serverSplitId = transactionJournalId,
        type = type,
        date = parseIsoInstant(date),
        amount = amount,
        currencyCode = currencyCode ?: "EUR",
        currencySymbol = currencySymbol ?: "€",
        currencyDecimalPlaces = currencyDecimalPlaces ?: 2,
        description = description,
        sourceId = sourceId,
        sourceName = sourceName,
        destinationId = destinationId,
        destinationName = destinationName,
        categoryId = categoryId,
        categoryName = categoryName,
        budgetId = budgetId,
        budgetName = budgetName,
        notes = notes,
        tags = tags ?: emptyList(),
        syncStatus = SyncStatus.SYNCED,
    )
}

fun TransactionWithSplits.toDomain(): Transaction {
    return Transaction(
        id = group.localId,
        serverId = group.serverId,
        title = group.groupTitle,
        splits = splits.map { it.toDomain() },
        createdAt = group.createdAt,
        updatedAt = group.updatedAt,
    )
}

fun TransactionSplitEntity.toDomain(): TransactionSplit {
    val currency = Currency(
        code = currencyCode,
        symbol = currencySymbol,
        decimalPlaces = currencyDecimalPlaces
    )
    return TransactionSplit(
        splitId = localSplitId,
        serverSplitId = serverSplitId,
        groupId = transactionGroupId,
        type = TransactionType.fromString(type),
        date = date,
        amount = Money.of(amount, currency),
        description = description,
        sourceId = sourceId,
        sourceName = sourceName,
        destinationId = destinationId,
        destinationName = destinationName,
        categoryId = categoryId,
        categoryName = categoryName,
        budgetId = budgetId,
        budgetName = budgetName,
        notes = notes,
        tags = tags,
        isPendingSync = syncStatus != SyncStatus.SYNCED,
    )
}

fun TransactionDraft.toGroupEntity(groupId: String): TransactionGroupEntity {
    return TransactionGroupEntity(
        localId = groupId,
        serverId = null,
        groupTitle = null,
        createdAt = date,
        updatedAt = date,
    )
}

fun TransactionDraft.toSplitEntity(splitId: String, groupId: String): TransactionSplitEntity {
    return TransactionSplitEntity(
        localSplitId = splitId,
        transactionGroupId = groupId,
        serverSplitId = null,
        type = type.name.lowercase(),
        date = date,
        amount = amount.amount.toPlainString(),
        currencyCode = amount.currency.code,
        currencySymbol = amount.currency.symbol,
        currencyDecimalPlaces = amount.currency.decimalPlaces,
        description = description,
        sourceId = sourceId,
        sourceName = sourceName,
        destinationId = destinationId,
        destinationName = destinationName,
        categoryId = categoryId,
        categoryName = categoryName,
        budgetId = budgetId,
        budgetName = budgetName,
        notes = notes,
        tags = tags,
        syncStatus = SyncStatus.PENDING_CREATE,
    )
}

fun TransactionDraft.toStoreDto(externalId: String): TransactionStoreDto {
    return TransactionStoreDto(
        transactions = listOf(
            TransactionSplitStoreDto(
                type = type.name.lowercase(),
                date = formatOffsetDate(date),
                amount = amount.amount.toPlainString(),
                description = description,
                sourceId = sourceId?.ifBlank { null },
                sourceName = sourceName?.ifBlank { null },
                destinationId = destinationId?.ifBlank { null },
                destinationName = destinationName?.ifBlank { null },
                categoryId = categoryId?.ifBlank { null },
                categoryName = categoryName?.ifBlank { null },
                budgetId = budgetId?.ifBlank { null },
                budgetName = budgetName?.ifBlank { null },
                currencyCode = amount.currency.code,
                notes = notes?.ifBlank { null },
                tags = tags.ifEmpty { null },
                externalId = externalId,
            )
        )
    )
}

/** Builds the create request from the current local rows; [externalId] makes retries idempotent. */
fun TransactionWithSplits.toStoreDto(externalId: String): TransactionStoreDto = TransactionStoreDto(
    groupTitle = group.groupTitle,
    transactions = splits.sortedBy { it.order }.map { split ->
        TransactionSplitStoreDto(
            type = split.type,
            date = formatOffsetDate(split.date),
            amount = split.amount,
            description = split.description,
            sourceId = split.sourceId,
            sourceName = split.sourceName,
            destinationId = split.destinationId,
            destinationName = split.destinationName,
            categoryId = split.categoryId,
            categoryName = split.categoryName,
            budgetId = split.budgetId,
            budgetName = split.budgetName,
            currencyCode = split.currencyCode,
            notes = split.notes,
            tags = split.tags.ifEmpty { null },
            externalId = externalId,
        )
    },
)

/**
 * Builds the update request from the current local rows. Notes are sent as an empty string when
 * cleared so the server removes them; other unset fields are omitted and keep their server value.
 */
fun TransactionWithSplits.toUpdateDto(): TransactionUpdateDto = TransactionUpdateDto(
    groupTitle = group.groupTitle,
    transactions = splits.sortedBy { it.order }.map { split ->
        TransactionSplitUpdateDto(
            transactionJournalId = split.serverSplitId,
            type = split.type,
            date = formatOffsetDate(split.date),
            amount = split.amount,
            description = split.description,
            sourceId = split.sourceId,
            sourceName = split.sourceName,
            destinationId = split.destinationId,
            destinationName = split.destinationName,
            categoryId = split.categoryId,
            categoryName = split.categoryName,
            budgetId = split.budgetId,
            budgetName = split.budgetName,
            currencyCode = split.currencyCode,
            notes = split.notes.orEmpty(),
            tags = split.tags,
        )
    },
)

internal fun formatOffsetDate(epochMillis: Long): String =
    DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
        OffsetDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault()),
    )

/**
 * Firefly sends offsets (`2026-10-01T00:00:00+00:00`). `Instant.parse` only accepts them from
 * Android 14, so parse through [OffsetDateTime], which handles both `Z` and numeric offsets.
 */
internal fun parseIsoInstant(dateStr: String?): Long {
    if (dateStr.isNullOrBlank()) return System.currentTimeMillis()
    return runCatching { OffsetDateTime.parse(dateStr).toInstant().toEpochMilli() }
        .getOrElse { System.currentTimeMillis() }
}
