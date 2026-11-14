package app.lucent.firefly.data.accounts.impl.mapper

import app.lucent.firefly.core.common.model.Currency
import app.lucent.firefly.core.common.model.Money
import app.lucent.firefly.core.database.entity.AccountEntity
import app.lucent.firefly.core.database.model.SyncStatus
import app.lucent.firefly.data.accounts.api.model.Account
import app.lucent.firefly.data.accounts.api.model.AccountType
import app.lucent.firefly.data.accounts.impl.dto.AccountReadDto
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

fun AccountReadDto.toEntity(existingLocalId: String? = null): AccountEntity =
    AccountEntity(
        localId = existingLocalId ?: UUID.randomUUID().toString(),
        serverId = id,
        name = attributes.name,
        type = attributes.type,
        currentBalance = attributes.currentBalance ?: "0.00",
        currencyId = attributes.currencyId,
        currencyCode = attributes.currencyCode ?: "EUR",
        currencySymbol = attributes.currencySymbol ?: "€",
        currencyDecimalPlaces = attributes.currencyDecimalPlaces ?: 2,
        active = attributes.active ?: true,
        order = attributes.order ?: 0,
        includeNetWorth = attributes.includeNetWorth ?: true,
        primaryCurrencyBalance = attributes.primaryCurrencyBalance,
        primaryCurrencyCode = attributes.primaryCurrencyCode,
        primaryCurrencySymbol = attributes.primaryCurrencySymbol,
        primaryCurrencyDecimalPlaces = attributes.primaryCurrencyDecimalPlaces,
        syncStatus = SyncStatus.SYNCED,
        updatedAt = System.currentTimeMillis(),
    )

fun AccountEntity.toDomain(pendingDeltaAmount: BigDecimal = BigDecimal.ZERO): Account {
    val currency =
        Currency(
            id = currencyId,
            code = currencyCode,
            symbol = currencySymbol,
            decimalPlaces = currencyDecimalPlaces,
        )
    val primaryCurrency =
        primaryCurrencyCode?.let { code ->
            Currency(
                code = code,
                symbol = primaryCurrencySymbol ?: code,
                decimalPlaces = primaryCurrencyDecimalPlaces ?: 2,
            )
        }
    return Account(
        id = localId,
        serverId = serverId,
        name = name,
        type = AccountType.fromString(type),
        balance = Money.of(currentBalance, currency),
        pendingDelta =
            Money(
                pendingDeltaAmount.setScale(currency.decimalPlaces, RoundingMode.HALF_UP),
                currency,
            ),
        currency = currency,
        active = active,
        order = order ?: 0,
        updatedAt = updatedAt,
        includeInNetWorth = includeNetWorth,
        primaryCurrency = primaryCurrency,
        primaryCurrencyBalance =
            primaryCurrency?.let { currency ->
                primaryCurrencyBalance?.let { Money.of(it, currency) }
            },
    )
}
