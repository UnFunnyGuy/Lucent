package app.lucent.firefly.data.accounts.api.model

import app.lucent.firefly.core.common.model.Currency
import app.lucent.firefly.core.common.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetWorthTest {
    private val eur = Currency(code = "EUR", symbol = "€")
    private val usd = Currency(code = "USD", symbol = "$")

    private fun account(
        id: String,
        balance: String,
        currency: Currency = eur,
        type: AccountType = AccountType.ASSET,
        active: Boolean = true,
        includeInNetWorth: Boolean = true,
        pendingDelta: String = "0",
        primaryBalance: String? = null,
    ) = Account(
        id = id,
        serverId = id,
        name = id,
        type = type,
        balance = Money.of(balance, currency),
        pendingDelta = Money.of(pendingDelta, currency),
        currency = currency,
        active = active,
        order = 0,
        updatedAt = 0L,
        includeInNetWorth = includeInNetWorth,
        primaryCurrency = eur,
        primaryCurrencyBalance = primaryBalance?.let { Money.of(it, eur) },
    )

    @Test
    fun sumsAssetsAndNegativeLiabilitiesIncludingPendingChanges() {
        val result =
            calculateNetWorth(
                listOf(
                    account("checking", "1000.00", pendingDelta = "-50.00"),
                    account("loan", "-300.00", type = AccountType.LIABILITY),
                ),
            )

        assertEquals(Money.of("650.00", eur), result)
    }

    @Test
    fun skipsExcludedInactiveAndCounterpartyAccounts() {
        val result =
            calculateNetWorth(
                listOf(
                    account("checking", "100.00"),
                    account("hidden", "999.00", includeInNetWorth = false),
                    account("closed", "999.00", active = false),
                    account("groceries", "999.00", type = AccountType.EXPENSE),
                ),
            )

        assertEquals(Money.of("100.00", eur), result)
    }

    @Test
    fun convertsForeignAccountsWithPrimaryCurrencyBalance() {
        val result =
            calculateNetWorth(
                listOf(
                    account("checking", "100.00"),
                    account("us-broker", "200.00", currency = usd, primaryBalance = "180.00"),
                ),
            )

        assertEquals(Money.of("280.00", eur), result)
    }

    @Test
    fun leavesOutForeignAccountsWithoutConversion() {
        val result =
            calculateNetWorth(
                listOf(
                    account("checking", "100.00"),
                    account("us-broker", "200.00", currency = usd),
                ),
            )

        assertEquals(Money.of("100.00", eur), result)
    }

    @Test
    fun returnsNullWithoutQualifyingAccounts() {
        assertNull(calculateNetWorth(listOf(account("groceries", "5.00", type = AccountType.EXPENSE))))
    }
}
