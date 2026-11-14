package app.lucent.firefly.data.accounts.impl.balance

import app.lucent.firefly.core.database.entity.AccountEntity
import app.lucent.firefly.core.database.entity.TransactionSplitEntity
import app.lucent.firefly.core.database.model.SyncStatus
import java.math.BigDecimal

/**
 * Balance adjustments not yet reflected in the server-reported balance, keyed by account local id.
 *
 * Splits reference accounts by server id when known, falling back to the local id. Pending creates
 * move money out of the source and into the destination; pending deletes of rows the server already
 * holds reverse that movement. Splits in a different currency than the account are ignored.
 */
internal fun computePendingDeltas(
    accounts: List<AccountEntity>,
    unsyncedSplits: List<TransactionSplitEntity>,
): Map<String, BigDecimal> {
    if (unsyncedSplits.isEmpty()) return emptyMap()

    val accountsByRef =
        buildMap {
            for (account in accounts) {
                put(account.localId, account)
                account.serverId?.let { put(it, account) }
            }
        }
    val deltas = mutableMapOf<String, BigDecimal>()

    fun apply(
        accountRef: String?,
        split: TransactionSplitEntity,
        signed: BigDecimal,
    ) {
        val account = accountRef?.let { accountsByRef[it] } ?: return
        if (!account.currencyCode.equals(split.currencyCode, ignoreCase = true)) return
        deltas[account.localId] = (deltas[account.localId] ?: BigDecimal.ZERO) + signed
    }

    for (split in unsyncedSplits) {
        val direction =
            when (split.syncStatus) {
                SyncStatus.PENDING_CREATE -> BigDecimal.ONE
                SyncStatus.PENDING_DELETE -> if (split.serverSplitId != null) BigDecimal.ONE.negate() else continue
                else -> continue
            }
        val amount = split.amount.toBigDecimalOrNull()?.abs() ?: continue
        val moved = amount * direction
        apply(split.sourceId, split, moved.negate())
        apply(split.destinationId, split, moved)
    }
    return deltas
}
