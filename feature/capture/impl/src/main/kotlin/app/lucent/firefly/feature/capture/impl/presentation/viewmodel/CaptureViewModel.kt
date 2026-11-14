package app.lucent.firefly.feature.capture.impl.presentation.viewmodel

import app.lucent.firefly.core.common.base.BaseViewModel
import app.lucent.firefly.core.common.model.Money
import app.lucent.firefly.core.common.result.Result
import app.lucent.firefly.data.accounts.api.model.Account
import app.lucent.firefly.data.accounts.api.model.AccountType
import app.lucent.firefly.data.accounts.api.usecase.ObserveAccountsUseCase
import app.lucent.firefly.data.transactions.api.model.TransactionDraft
import app.lucent.firefly.data.transactions.api.model.TransactionType
import app.lucent.firefly.data.transactions.api.usecase.CreateTransactionUseCase
import org.koin.core.annotation.KoinViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@KoinViewModel
class CaptureViewModel(
    private val observeAccountsUseCase: ObserveAccountsUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase,
) : BaseViewModel<CaptureUiState, CaptureUiEvent, CaptureUiAction>() {
    override fun initialState(): CaptureUiState = CaptureUiState()

    init {
        observeAccounts()
    }

    override fun onActionEvent(action: CaptureUiAction) {
        when (action) {
            is CaptureUiAction.SelectType -> update { copy(type = action.type).withAccountDefaults() }
            is CaptureUiAction.EnterDigit -> update { copy(amountInput = appendDigit(amountInput, action.digit)) }
            CaptureUiAction.DeleteDigit -> update { copy(amountInput = amountInput.dropLast(1).ifEmpty { "0" }) }
            CaptureUiAction.ClearAmount -> update { copy(amountInput = "0") }
            is CaptureUiAction.SelectSourceAccount -> update { selectSource(action.accountId) }
            is CaptureUiAction.SelectDestinationAccount -> update { selectDestination(action.accountId) }
            is CaptureUiAction.UpdateDescription -> update { copy(description = action.description) }
            CaptureUiAction.ShowNotes -> update { copy(isNotesVisible = true) }
            is CaptureUiAction.UpdateNotes -> update { copy(notes = action.notes) }
            is CaptureUiAction.SelectDate -> update { copy(date = action.date) }
            CaptureUiAction.Save -> save()
            CaptureUiAction.Dismiss -> sendEvent(CaptureUiEvent.Dismiss)
            CaptureUiAction.ClearError -> update { copy(errorMessage = null) }
        }
    }

    private fun observeAccounts() {
        viewmodelLaunch {
            observeAccountsUseCase.performStreaming().collect { accounts ->
                update { copy(accounts = accounts).withAccountDefaults() }
            }
        }
    }

    private fun save() {
        val state = currentState
        if (!state.canSave) return
        val currency = state.currency ?: return

        val source = state.sourceAccount.takeIf { state.needsSource }
        val destination = state.destinationAccount.takeIf { state.needsDestination }
        val draft =
            TransactionDraft(
                type = state.type,
                date = state.date.toTransactionTime(),
                amount = Money.of(state.amountInput, currency),
                description = state.description.trim(),
                // Only real account ids are sent: Firefly creates a new expense or revenue account for an
                // unknown name, and an empty counterparty uses its built-in cash account.
                sourceId = source?.serverId,
                sourceName = source?.name,
                destinationId = destination?.serverId,
                destinationName = destination?.name,
                notes = state.notes.trim().ifBlank { null },
            )

        viewmodelLaunch {
            update { copy(isSaving = true, errorMessage = null) }
            when (val result = createTransactionUseCase.perform(draft)) {
                is Result.Success -> {
                    update { copy(isSaving = false) }
                    sendEvent(CaptureUiEvent.Dismiss)
                }
                is Result.Error -> update { copy(isSaving = false, errorMessage = result.error.errorMessage) }
            }
        }
    }

    private fun CaptureUiState.selectSource(accountId: String): CaptureUiState =
        if (type == TransactionType.TRANSFER && accountId == destinationAccountId) {
            copy(sourceAccountId = accountId, destinationAccountId = sourceAccountId)
        } else {
            copy(sourceAccountId = accountId)
        }.withAccountDefaults()

    private fun CaptureUiState.selectDestination(accountId: String): CaptureUiState = copy(destinationAccountId = accountId).withAccountDefaults()

    private fun appendDigit(
        current: String,
        digit: Char,
    ): String =
        when {
            digit == '.' -> if (current.contains('.')) current else "$current."
            current.contains('.') && current.substringAfter('.').length >= MAX_DECIMALS -> current
            current == "0" -> digit.toString()
            current.length >= MAX_LENGTH -> current
            else -> current + digit
        }

    private fun LocalDate.toTransactionTime(): Long {
        val zone = ZoneId.systemDefault()
        // Today keeps the current time; other days use midday so time zones cannot shift the date.
        val time = if (this == LocalDate.now(zone)) LocalTime.now(zone) else LocalTime.NOON
        return atTime(time).atZone(zone).toInstant().toEpochMilli()
    }

    private companion object {
        const val MAX_DECIMALS = 2
        const val MAX_LENGTH = 12
    }
}

/**
 * Keeps the selected accounts valid for the current type, carrying the user's choice across type
 * changes (the account chosen for an expense becomes the account receiving income, and so on).
 */
internal fun CaptureUiState.withAccountDefaults(): CaptureUiState {
    val preferred = sourceAccountId ?: destinationAccountId
    val firstAsset = ownAccounts.firstOrNull { it.type == AccountType.ASSET } ?: ownAccounts.firstOrNull()

    fun valid(
        id: String?,
        options: List<Account>,
    ) = id?.takeIf { candidate -> options.any { it.id == candidate } }

    return when (type) {
        TransactionType.DEPOSIT ->
            copy(
                sourceAccountId = null,
                destinationAccountId = valid(destinationAccountId ?: preferred, ownAccounts) ?: firstAsset?.id,
            )
        TransactionType.TRANSFER -> {
            val source = valid(preferred, sourceOptions) ?: sourceOptions.firstOrNull()?.id
            val withSource = copy(sourceAccountId = source)
            copy(
                sourceAccountId = source,
                destinationAccountId =
                    valid(destinationAccountId, withSource.destinationOptions)
                        ?: withSource.destinationOptions.firstOrNull()?.id,
            )
        }
        else ->
            copy(
                sourceAccountId = valid(preferred, ownAccounts) ?: firstAsset?.id,
                destinationAccountId = null,
            )
    }
}
