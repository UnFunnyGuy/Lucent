package app.lucent.firefly.feature.capture.impl.presentation.viewmodel

import app.lucent.firefly.core.common.base.UIState
import app.lucent.firefly.core.common.model.Currency
import app.lucent.firefly.data.accounts.api.model.Account
import app.lucent.firefly.data.accounts.api.model.AccountType
import app.lucent.firefly.data.transactions.api.model.TransactionType
import java.time.LocalDate

data class CaptureUiState(
    val type: TransactionType = TransactionType.WITHDRAWAL,
    val amountInput: String = "0",
    val accounts: List<Account> = emptyList(),
    val sourceAccountId: String? = null,
    val destinationAccountId: String? = null,
    val description: String = "",
    val notes: String = "",
    val isNotesVisible: Boolean = false,
    val date: LocalDate = LocalDate.now(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) : UIState {

    /** Accounts money can come from or go to: what the user owns or owes. */
    val ownAccounts: List<Account>
        get() = accounts.filter { it.active && (it.type == AccountType.ASSET || it.type == AccountType.LIABILITY) }

    val sourceOptions: List<Account>
        get() = if (type == TransactionType.TRANSFER) ownAccounts.filter { it.type == AccountType.ASSET } else ownAccounts

    val destinationOptions: List<Account>
        get() = if (type == TransactionType.TRANSFER) {
            ownAccounts.filter { it.type == AccountType.ASSET && it.id != sourceAccountId }
        } else {
            ownAccounts
        }

    val sourceAccount: Account? get() = accounts.firstOrNull { it.id == sourceAccountId }
    val destinationAccount: Account? get() = accounts.firstOrNull { it.id == destinationAccountId }

    val needsSource: Boolean get() = type != TransactionType.DEPOSIT
    val needsDestination: Boolean get() = type != TransactionType.WITHDRAWAL

    /** The amount is entered in the currency of the account the money moves out of (or into, for income). */
    val currency: Currency?
        get() = (if (type == TransactionType.DEPOSIT) destinationAccount else sourceAccount)?.currency
            ?: ownAccounts.firstOrNull()?.currency

    val canSave: Boolean
        get() = !isSaving &&
            (amountInput.toBigDecimalOrNull()?.signum() ?: 0) > 0 &&
            description.isNotBlank() &&
            (!needsSource || sourceAccount != null) &&
            (!needsDestination || destinationAccount != null) &&
            (type != TransactionType.TRANSFER || sourceAccountId != destinationAccountId)
}
