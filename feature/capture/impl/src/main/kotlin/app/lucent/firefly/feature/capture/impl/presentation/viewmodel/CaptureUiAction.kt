package app.lucent.firefly.feature.capture.impl.presentation.viewmodel

import app.lucent.firefly.core.common.base.UIAction
import app.lucent.firefly.data.transactions.api.model.TransactionType
import java.time.LocalDate

sealed interface CaptureUiAction : UIAction {
    data class SelectType(
        val type: TransactionType,
    ) : CaptureUiAction

    data class EnterDigit(
        val digit: Char,
    ) : CaptureUiAction

    data object DeleteDigit : CaptureUiAction

    data object ClearAmount : CaptureUiAction

    data class SelectSourceAccount(
        val accountId: String,
    ) : CaptureUiAction

    data class SelectDestinationAccount(
        val accountId: String,
    ) : CaptureUiAction

    data class UpdateDescription(
        val description: String,
    ) : CaptureUiAction

    data object ShowNotes : CaptureUiAction

    data class UpdateNotes(
        val notes: String,
    ) : CaptureUiAction

    data class SelectDate(
        val date: LocalDate,
    ) : CaptureUiAction

    data object Save : CaptureUiAction

    data object Dismiss : CaptureUiAction

    data object ClearError : CaptureUiAction
}
