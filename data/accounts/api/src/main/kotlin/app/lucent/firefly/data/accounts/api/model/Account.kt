package app.lucent.firefly.data.accounts.api.model

import app.lucent.firefly.core.common.model.Currency
import app.lucent.firefly.core.common.model.Money

data class Account(
    val id: String,
    val serverId: String?,
    val name: String,
    val type: AccountType,
    val balance: Money,
    val pendingDelta: Money,
    val currency: Currency,
    val active: Boolean,
    val order: Int,
    val updatedAt: Long,
    val includeInNetWorth: Boolean = true,
    /** The user's primary (reporting) currency on the server. */
    val primaryCurrency: Currency? = null,
    /** [balance] converted to [primaryCurrency]; null when the server does not convert. */
    val primaryCurrencyBalance: Money? = null,
) {
    val displayBalance: Money get() = balance + pendingDelta
}
