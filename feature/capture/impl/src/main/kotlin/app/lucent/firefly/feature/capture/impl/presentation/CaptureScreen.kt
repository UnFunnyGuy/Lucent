package app.lucent.firefly.feature.capture.impl.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.lucent.firefly.core.common.base.ObserveAsEvents
import app.lucent.firefly.core.common.base.collectState
import app.lucent.firefly.core.common.model.Currency
import app.lucent.firefly.core.common.model.Money
import app.lucent.firefly.core.designsystem.theme.LucentTheme
import app.lucent.firefly.core.ui.component.FinancialKeypad
import app.lucent.firefly.core.ui.format.formatAmount
import app.lucent.firefly.data.accounts.api.model.Account
import app.lucent.firefly.data.accounts.api.model.AccountType
import app.lucent.firefly.data.transactions.api.model.TransactionType
import app.lucent.firefly.feature.capture.impl.R
import app.lucent.firefly.feature.capture.impl.presentation.viewmodel.CaptureUiAction
import app.lucent.firefly.feature.capture.impl.presentation.viewmodel.CaptureUiEvent
import app.lucent.firefly.feature.capture.impl.presentation.viewmodel.CaptureUiState
import app.lucent.firefly.feature.capture.impl.presentation.viewmodel.CaptureViewModel
import app.lucent.firefly.feature.capture.impl.presentation.viewmodel.withAccountDefaults
import org.koin.compose.viewmodel.koinViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun CaptureRoute(
    viewModel: CaptureViewModel = koinViewModel(),
    onDismiss: () -> Unit,
) {
    val state by viewModel.collectState()

    ObserveAsEvents(viewModel.uiEvent) { event ->
        when (event) {
            CaptureUiEvent.Dismiss -> onDismiss()
        }
    }

    CaptureScreen(state = state, onAction = viewModel::onAction)
}

private val CAPTURE_TYPES = listOf(TransactionType.WITHDRAWAL, TransactionType.DEPOSIT, TransactionType.TRANSFER)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    state: CaptureUiState,
    onAction: (CaptureUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isDatePickerOpen by rememberSaveable { mutableStateOf(false) }
    val accent = state.type.accentColor()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onAction(CaptureUiAction.Dismiss) }) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.feature_capture_cd_close))
            }
            Text(
                text = stringResource(R.string.feature_capture_title_new),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            CAPTURE_TYPES.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = state.type == type,
                    onClick = { onAction(CaptureUiAction.SelectType(type)) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = CAPTURE_TYPES.size),
                    label = { Text(type.label()) },
                )
            }
        }

        AmountDisplay(
            amountInput = state.amountInput,
            currency = state.currency,
            color = accent,
            modifier = Modifier.padding(vertical = 16.dp),
        )

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.ownAccounts.isEmpty()) {
                Text(
                    text = stringResource(R.string.feature_capture_no_accounts),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LucentTheme.colors.onSurfaceVariant,
                )
            }
            if (state.needsSource) {
                AccountPicker(
                    label = stringResource(R.string.feature_capture_picker_from),
                    accounts = state.sourceOptions,
                    selectedId = state.sourceAccountId,
                    accent = accent,
                    onSelect = { onAction(CaptureUiAction.SelectSourceAccount(it)) },
                )
            }
            if (state.needsDestination) {
                AccountPicker(
                    label = stringResource(R.string.feature_capture_picker_to),
                    accounts = state.destinationOptions,
                    selectedId = state.destinationAccountId,
                    accent = accent,
                    onSelect = { onAction(CaptureUiAction.SelectDestinationAccount(it)) },
                )
            }

            DescriptionField(
                value = state.description,
                type = state.type,
                onValueChange = { onAction(CaptureUiAction.UpdateDescription(it)) },
            )

            DateChips(
                date = state.date,
                onSelect = { onAction(CaptureUiAction.SelectDate(it)) },
                onPickDate = { isDatePickerOpen = true },
            )

            if (state.isNotesVisible) {
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = { onAction(CaptureUiAction.UpdateNotes(it)) },
                    label = { Text(stringResource(R.string.feature_capture_notes_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
            } else {
                TextButton(onClick = { onAction(CaptureUiAction.ShowNotes) }) {
                    Text(stringResource(R.string.feature_capture_action_add_note))
                }
            }

            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = LucentTheme.colors.error,
                )
            }
        }

        FinancialKeypad(
            onDigitClick = { onAction(CaptureUiAction.EnterDigit(it)) },
            onBackspaceClick = { onAction(CaptureUiAction.DeleteDigit) },
            modifier = Modifier.padding(vertical = 8.dp),
        )

        Button(
            onClick = { onAction(CaptureUiAction.Save) },
            enabled = state.canSave,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent),
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    text = stringResource(R.string.feature_capture_save_button, state.type.label().lowercase()),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    if (isDatePickerOpen) {
        val pickerState =
            rememberDatePickerState(
                initialSelectedDateMillis =
                    state.date
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli(),
            )
        DatePickerDialog(
            onDismissRequest = { isDatePickerOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            // The picker reports the chosen day as midnight UTC.
                            onAction(CaptureUiAction.SelectDate(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()))
                        }
                        isDatePickerOpen = false
                    },
                ) { Text(stringResource(R.string.feature_capture_dialog_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { isDatePickerOpen = false }) {
                    Text(stringResource(R.string.feature_capture_dialog_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun AmountDisplay(
    amountInput: String,
    currency: Currency?,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "${currency?.symbol.orEmpty()}$amountInput",
        style = MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun AccountPicker(
    label: String,
    accounts: List<Account>,
    selectedId: String?,
    accent: Color,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = LucentTheme.colors.onSurfaceVariant,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(accounts, key = { it.id }) { account ->
                val selected = account.id == selectedId
                Surface(
                    onClick = { onSelect(account.id) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) accent.copy(alpha = 0.12f) else LucentTheme.colors.surfaceVariant,
                    border = if (selected) BorderStroke(2.dp, accent) else null,
                    modifier = Modifier.width(148.dp),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = account.displayBalance.formatAmount(),
                            style = MaterialTheme.typography.bodySmall,
                            color = LucentTheme.colors.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DescriptionField(
    value: String,
    type: TransactionType,
    onValueChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.feature_capture_description_label)) },
        placeholder = {
            Text(
                when (type) {
                    TransactionType.DEPOSIT -> stringResource(R.string.feature_capture_description_placeholder_income)
                    TransactionType.TRANSFER -> stringResource(R.string.feature_capture_description_placeholder_transfer)
                    else -> stringResource(R.string.feature_capture_description_placeholder_expense)
                },
            )
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
    )
}

@Composable
private fun DateChips(
    date: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onPickDate: () -> Unit,
) {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    val isOtherDay = date != today && date != yesterday
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = date == today,
            onClick = { onSelect(today) },
            label = { Text(stringResource(R.string.feature_capture_date_today)) },
        )
        FilterChip(
            selected = date == yesterday,
            onClick = { onSelect(yesterday) },
            label = { Text(stringResource(R.string.feature_capture_date_yesterday)) },
        )
        FilterChip(
            selected = isOtherDay,
            onClick = onPickDate,
            label = {
                Text(
                    if (isOtherDay) {
                        date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
                    } else {
                        stringResource(R.string.feature_capture_date_pick)
                    },
                )
            },
            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp)) },
        )
    }
}

@Composable
private fun TransactionType.label(): String =
    when (this) {
        TransactionType.DEPOSIT -> stringResource(R.string.feature_capture_type_income)
        TransactionType.TRANSFER -> stringResource(R.string.feature_capture_type_transfer)
        else -> stringResource(R.string.feature_capture_type_expense)
    }

@Composable
private fun TransactionType.accentColor(): Color =
    when (this) {
        TransactionType.DEPOSIT -> LucentTheme.colors.income
        TransactionType.TRANSFER -> LucentTheme.colors.transfer
        else -> LucentTheme.colors.expense
    }

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun CaptureScreenPreview() {
    val eur = Currency(code = "EUR", symbol = "€")

    fun account(
        id: String,
        name: String,
        balance: String,
    ) = Account(
        id = id,
        serverId = id,
        name = name,
        type = AccountType.ASSET,
        balance = Money.of(balance, eur),
        pendingDelta = Money.zero(eur),
        currency = eur,
        active = true,
        order = 0,
        updatedAt = 0L,
    )
    LucentTheme {
        CaptureScreen(
            state =
                CaptureUiState(
                    amountInput = "24.50",
                    accounts = listOf(account("1", "Checking", "1240.00"), account("2", "Savings", "8000.00")),
                    description = "Groceries",
                ).withAccountDefaults(),
            onAction = {},
        )
    }
}
