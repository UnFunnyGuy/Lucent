package app.lucent.firefly.data.accounts.impl.balance

import app.lucent.firefly.core.database.entity.AccountEntity
import app.lucent.firefly.core.database.entity.TransactionSplitEntity
import app.lucent.firefly.core.database.model.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class PendingBalanceDeltasTest {
    private val checking = account(localId = "local-checking", serverId = "1")
    private val savings = account(localId = "local-savings", serverId = "2")

    @Test
    fun pendingWithdrawalReducesSourceAccount() {
        val deltas =
            computePendingDeltas(
                listOf(checking),
                listOf(split(SyncStatus.PENDING_CREATE, amount = "14.50", sourceId = "1", destinationId = null)),
            )
        assertEquals(BigDecimal("-14.50"), deltas["local-checking"])
    }

    @Test
    fun pendingDepositIncreasesDestinationAccount() {
        val deltas =
            computePendingDeltas(
                listOf(checking),
                listOf(split(SyncStatus.PENDING_CREATE, amount = "100.00", sourceId = null, destinationId = "1")),
            )
        assertEquals(BigDecimal("100.00"), deltas["local-checking"])
    }

    @Test
    fun pendingTransferMovesBetweenAccounts() {
        val deltas =
            computePendingDeltas(
                listOf(checking, savings),
                listOf(split(SyncStatus.PENDING_CREATE, amount = "50.00", sourceId = "1", destinationId = "2")),
            )
        assertEquals(BigDecimal("-50.00"), deltas["local-checking"])
        assertEquals(BigDecimal("50.00"), deltas["local-savings"])
    }

    @Test
    fun pendingDeleteOfServerTransactionReversesIt() {
        val deltas =
            computePendingDeltas(
                listOf(checking),
                listOf(split(SyncStatus.PENDING_DELETE, amount = "20.00", sourceId = "1", destinationId = null, serverSplitId = "j-1")),
            )
        assertEquals(BigDecimal("20.00"), deltas["local-checking"])
    }

    @Test
    fun matchesAccountsByLocalIdWhenServerIdUnknown() {
        val deltas =
            computePendingDeltas(
                listOf(checking),
                listOf(split(SyncStatus.PENDING_CREATE, amount = "3.00", sourceId = "local-checking", destinationId = null)),
            )
        assertEquals(BigDecimal("-3.00"), deltas["local-checking"])
    }

    @Test
    fun ignoresSplitsInOtherCurrency() {
        val deltas =
            computePendingDeltas(
                listOf(checking),
                listOf(split(SyncStatus.PENDING_CREATE, amount = "3.00", sourceId = "1", destinationId = null, currency = "USD")),
            )
        assertTrue(deltas.isEmpty())
    }

    private fun account(
        localId: String,
        serverId: String,
    ) = AccountEntity(
        localId = localId,
        serverId = serverId,
        name = localId,
        type = "asset",
        currentBalance = "1000.00",
        currencyId = null,
        currencyCode = "EUR",
        currencySymbol = "€",
        currencyDecimalPlaces = 2,
        active = true,
        order = 0,
        syncStatus = SyncStatus.SYNCED,
    )

    private fun split(
        status: SyncStatus,
        amount: String,
        sourceId: String?,
        destinationId: String?,
        serverSplitId: String? = null,
        currency: String = "EUR",
    ) = TransactionSplitEntity(
        localSplitId = "s-$amount-$status",
        transactionGroupId = "g",
        serverSplitId = serverSplitId,
        type = "withdrawal",
        date = 0L,
        amount = amount,
        currencyCode = currency,
        currencySymbol = "€",
        currencyDecimalPlaces = 2,
        description = "",
        sourceId = sourceId,
        sourceName = null,
        destinationId = destinationId,
        destinationName = null,
        categoryId = null,
        categoryName = null,
        budgetId = null,
        budgetName = null,
        notes = null,
        tags = emptyList(),
        syncStatus = status,
    )
}
