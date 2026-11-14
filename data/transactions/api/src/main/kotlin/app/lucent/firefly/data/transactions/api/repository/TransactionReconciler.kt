package app.lucent.firefly.data.transactions.api.repository

import app.lucent.firefly.core.common.result.Result

interface TransactionReconciler {
    /** Reconciles the recent window (start of last month until today) with the server. */
    suspend fun syncRecentTransactions(): Result<Unit>

    /** Reconciles a longer window to catch edits and deletions of older transactions. */
    suspend fun reconcileTransactions(monthsBack: Int): Result<Unit>
}
