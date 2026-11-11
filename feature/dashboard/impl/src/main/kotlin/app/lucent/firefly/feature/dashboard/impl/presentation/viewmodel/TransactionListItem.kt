package app.lucent.firefly.feature.dashboard.impl.presentation.viewmodel

import app.lucent.firefly.data.transactions.api.model.Transaction
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Rows of the paged transaction list: day headers between transactions of different days. */
sealed interface TransactionListItem {
    val key: String

    data class DayHeader(val date: LocalDate) : TransactionListItem {
        override val key: String = "day-$date"
    }

    data class Entry(val transaction: Transaction) : TransactionListItem {
        override val key: String = transaction.id
        val date: LocalDate = transaction.localDate()
    }
}

internal fun Transaction.localDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(splits.firstOrNull()?.date ?: createdAt).atZone(zone).toLocalDate()
