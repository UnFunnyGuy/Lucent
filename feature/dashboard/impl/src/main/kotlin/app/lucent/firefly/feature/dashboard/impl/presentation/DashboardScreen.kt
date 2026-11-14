package app.lucent.firefly.feature.dashboard.impl.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import app.lucent.firefly.core.common.base.ObserveAsEvents
import app.lucent.firefly.core.common.base.collectState
import app.lucent.firefly.core.common.model.Currency
import app.lucent.firefly.core.common.model.Money
import app.lucent.firefly.core.designsystem.theme.LucentTheme
import app.lucent.firefly.core.ui.component.AppScaffold
import app.lucent.firefly.core.ui.format.formatAmount
import app.lucent.firefly.data.accounts.api.model.Account
import app.lucent.firefly.data.accounts.api.model.AccountType
import app.lucent.firefly.data.sync.api.handler.SyncOperation
import app.lucent.firefly.data.sync.api.model.FailedChange
import app.lucent.firefly.data.transactions.api.model.Transaction
import app.lucent.firefly.data.transactions.api.model.TransactionSplit
import app.lucent.firefly.data.transactions.api.model.TransactionType
import app.lucent.firefly.feature.dashboard.impl.R
import app.lucent.firefly.feature.dashboard.impl.presentation.viewmodel.DashboardUiAction
import app.lucent.firefly.feature.dashboard.impl.presentation.viewmodel.DashboardUiEvent
import app.lucent.firefly.feature.dashboard.impl.presentation.viewmodel.DashboardUiState
import app.lucent.firefly.feature.dashboard.impl.presentation.viewmodel.DashboardViewModel
import app.lucent.firefly.feature.dashboard.impl.presentation.viewmodel.TransactionListItem
import kotlinx.coroutines.flow.flowOf
import org.koin.compose.viewmodel.koinViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DashboardRoute(
    onNavigateToQuickAdd: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToTransactionDetail: (String) -> Unit = {},
    onNavigateToAccountDetail: (String) -> Unit = {},
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val state by viewModel.collectState()
    val transactions = viewModel.transactions.collectAsLazyPagingItems()

    ObserveAsEvents(viewModel.uiEvent) { event ->
        when (event) {
            DashboardUiEvent.NavigateToQuickAdd -> onNavigateToQuickAdd()
            DashboardUiEvent.NavigateToLogin -> onNavigateToLogin()
            is DashboardUiEvent.NavigateToTransactionDetail -> onNavigateToTransactionDetail(event.transactionId)
            is DashboardUiEvent.NavigateToAccountDetail -> onNavigateToAccountDetail(event.accountId)
            is DashboardUiEvent.ShowSnackbar -> Unit
        }
    }

    DashboardScreen(
        state = state,
        transactions = transactions,
        onAction = viewModel::onAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    transactions: LazyPagingItems<TransactionListItem>,
    onAction: (DashboardUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val isFabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val isRefreshing = state.isRefreshing || transactions.loadState.mediator?.refresh is LoadState.Loading
    val refresh = {
        onAction(DashboardUiAction.Refresh)
        transactions.refresh()
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onAction(DashboardUiAction.DismissError)
        }
    }

    AppScaffold(
        modifier = modifier,
        topBar = {
            DashboardTopBar(
                state = state,
                onSync = refresh,
                onLogout = { onAction(DashboardUiAction.Logout) },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.feature_dashboard_action_add)) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = { onAction(DashboardUiAction.OpenQuickAdd) },
                expanded = isFabExpanded,
                containerColor = LucentTheme.colors.primary,
                contentColor = LucentTheme.colors.onPrimary,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = refresh,
            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
        ) {
            DashboardContent(
                state = state,
                transactions = transactions,
                listState = listState,
                onAction = onAction,
            )
        }
    }

    state.logoutConfirmationPendingCount?.let { count ->
        AlertDialog(
            onDismissRequest = { onAction(DashboardUiAction.CancelLogout) },
            title = { Text(stringResource(R.string.feature_dashboard_logout_dialog_title)) },
            text = {
                Text(stringResource(R.string.feature_dashboard_logout_dialog_message, count))
            },
            confirmButton = {
                TextButton(onClick = { onAction(DashboardUiAction.ConfirmLogout) }) {
                    Text(stringResource(R.string.feature_dashboard_action_logout))
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(DashboardUiAction.CancelLogout) }) {
                    Text(stringResource(R.string.feature_dashboard_action_stay))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    state: DashboardUiState,
    onSync: () -> Unit,
    onLogout: () -> Unit,
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            Column {
                Text(
                    text = stringResource(R.string.feature_dashboard_title),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.serverHost.isNotBlank()) {
                    Text(
                        text = state.serverHost,
                        style = MaterialTheme.typography.bodySmall,
                        color = LucentTheme.colors.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        },
        actions = {
            SyncStatusButton(state = state, onClick = onSync)
            Box {
                IconButton(onClick = { isMenuOpen = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.feature_dashboard_cd_more_options),
                    )
                }
                DropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.feature_dashboard_action_logout)) },
                        onClick = {
                            isMenuOpen = false
                            onLogout()
                        },
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = LucentTheme.colors.background),
    )
}

/** One glanceable icon for the sync state; tapping it syncs now. */
@Composable
private fun SyncStatusButton(
    state: DashboardUiState,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        when {
            state.isSyncing -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            state.requiresReauthentication || state.failedChanges.isNotEmpty() ->
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = stringResource(R.string.feature_dashboard_cd_sync_needs_attention),
                    tint = LucentTheme.colors.error,
                )
            !state.isOnline ->
                Icon(
                    Icons.Default.CloudOff,
                    contentDescription = stringResource(R.string.feature_dashboard_cd_offline),
                )
            state.pendingChangesCount > 0 ->
                BadgedBox(badge = { Badge { Text("${state.pendingChangesCount}") } }) {
                    Icon(
                        Icons.Default.CloudUpload,
                        contentDescription =
                            stringResource(
                                R.string.feature_dashboard_cd_changes_waiting,
                                state.pendingChangesCount,
                            ),
                    )
                }
            else ->
                Icon(
                    Icons.Default.CloudDone,
                    contentDescription = stringResource(R.string.feature_dashboard_cd_synced),
                )
        }
    }
}

@Composable
private fun DashboardContent(
    state: DashboardUiState,
    transactions: LazyPagingItems<TransactionListItem>,
    listState: LazyListState,
    onAction: (DashboardUiAction) -> Unit,
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "overview") {
            OverviewCard(
                netWorth = state.netWorth,
                monthIncome = state.monthIncome,
                monthSpending = state.monthSpending,
            )
        }

        if (state.requiresReauthentication) {
            item(key = "reauth") {
                NoticeBanner(stringResource(R.string.feature_dashboard_banner_reauth))
            }
        } else if (!state.isOnline && state.pendingChangesCount > 0) {
            item(key = "offline") {
                NoticeBanner(
                    stringResource(
                        R.string.feature_dashboard_banner_offline,
                        state.pendingChangesCount,
                    ),
                )
            }
        }

        if (state.failedChanges.isNotEmpty()) {
            item(key = "needs-attention") {
                NeedsAttentionCard(changes = state.failedChanges, onAction = onAction)
            }
        }

        item(key = "accounts") {
            AccountsSection(
                accounts = state.accounts,
                onAccountClick = { onAction(DashboardUiAction.OpenAccount(it)) },
            )
        }

        item(key = "transactions-title") {
            SectionTitle(
                stringResource(R.string.feature_dashboard_section_transactions),
                modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
            )
        }

        items(
            count = transactions.itemCount,
            key = transactions.itemKey { it.key },
            contentType = transactions.itemContentType { it::class.simpleName },
        ) { index ->
            when (val item = transactions[index]) {
                is TransactionListItem.DayHeader -> DayHeader(item.date)
                is TransactionListItem.Entry ->
                    TransactionRow(
                        transaction = item.transaction,
                        onClick = { onAction(DashboardUiAction.OpenTransaction(item.transaction.id)) },
                    )
                null -> Spacer(modifier = Modifier.height(64.dp))
            }
        }

        val loadState = transactions.loadState
        when {
            loadState.append is LoadState.Loading -> item(key = "append-loading") { LoadingRow() }
            loadState.append is LoadState.Error ->
                item(key = "append-error") {
                    RetryRow(
                        message = stringResource(R.string.feature_dashboard_error_load_older_transactions),
                        onRetry = transactions::retry,
                    )
                }
            transactions.itemCount == 0 && loadState.refresh is LoadState.Error ->
                item(key = "refresh-error") {
                    RetryRow(
                        message = stringResource(R.string.feature_dashboard_error_load_transactions),
                        onRetry = transactions::retry,
                    )
                }
            transactions.itemCount == 0 && loadState.refresh is LoadState.NotLoading ->
                item(key = "empty") {
                    EmptyTransactions(onAdd = { onAction(DashboardUiAction.OpenQuickAdd) })
                }
        }
    }
}

@Composable
private fun OverviewCard(
    netWorth: Money,
    monthIncome: Money,
    monthSpending: Money,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = LucentTheme.colors.primaryContainer,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.feature_dashboard_net_worth_label),
                style = MaterialTheme.typography.labelLarge,
                color = LucentTheme.colors.onPrimaryContainer,
            )
            Text(
                text = netWorth.formatAmount(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
                color = LucentTheme.colors.onPrimaryContainer,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MonthMetric(
                    label = stringResource(R.string.feature_dashboard_month_income_label),
                    value = monthIncome.formatAmount(showPlusSign = true),
                    color = LucentTheme.colors.income,
                    icon = Icons.Default.ArrowDownward,
                    modifier = Modifier.weight(1f),
                )
                MonthMetric(
                    label = stringResource(R.string.feature_dashboard_month_spending_label),
                    value = Money(monthSpending.amount.negate(), monthSpending.currency).formatAmount(),
                    color = LucentTheme.colors.expense,
                    icon = Icons.Default.ArrowUpward,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MonthMetric(
    label: String,
    value: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = LucentTheme.colors.surface,
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = LucentTheme.colors.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleSmall, color = color, maxLines = 1)
            }
        }
    }
}

@Composable
private fun AccountsSection(
    accounts: List<Account>,
    onAccountClick: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 24.dp)) {
        SectionTitle(stringResource(R.string.feature_dashboard_section_accounts), modifier = Modifier.padding(bottom = 8.dp))
        if (accounts.isEmpty()) {
            Text(
                text = stringResource(R.string.feature_dashboard_accounts_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = LucentTheme.colors.onSurfaceVariant,
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(accounts, key = { it.id }) { account ->
                    AccountCard(account = account, onClick = { onAccountClick(account.id) })
                }
            }
        }
    }
}

@Composable
private fun AccountCard(
    account: Account,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = LucentTheme.colors.surfaceVariant,
        modifier = Modifier.width(176.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = if (account.type == AccountType.LIABILITY) Icons.Default.CreditCard else Icons.Default.AccountBalance,
                contentDescription = null,
                tint = LucentTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = account.name,
                style = MaterialTheme.typography.bodyMedium,
                color = LucentTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = account.displayBalance.formatAmount(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            if (!account.pendingDelta.isZero) {
                Text(
                    text =
                        stringResource(
                            R.string.feature_dashboard_pending_delta,
                            account.pendingDelta.formatAmount(showPlusSign = true),
                        ),
                    style = MaterialTheme.typography.labelSmall,
                    color = LucentTheme.colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier,
    )
}

@Composable
private fun DayHeader(date: LocalDate) {
    val today = LocalDate.now()
    val label =
        when {
            date == today -> stringResource(R.string.feature_dashboard_day_today)
            date == today.minusDays(1) -> stringResource(R.string.feature_dashboard_day_yesterday)
            date.year == today.year -> date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
            else -> date.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
        }
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = LucentTheme.colors.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun TransactionRow(
    transaction: Transaction,
    onClick: () -> Unit,
) {
    val split = transaction.splits.firstOrNull() ?: return
    val color = split.type.color()
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(split.type.icon(), contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.title ?: split.description,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = split.route(),
                style = MaterialTheme.typography.bodySmall,
                color = LucentTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = transaction.signedAmount().formatAmount(showPlusSign = split.type == TransactionType.DEPOSIT),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (split.type == TransactionType.TRANSFER) LucentTheme.colors.onSurfaceVariant else color,
            )
            if (transaction.isPendingSync) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = LucentTheme.colors.onSurfaceVariant,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.feature_dashboard_pending_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = LucentTheme.colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun NoticeBanner(text: String) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = LucentTheme.colors.error.copy(alpha = 0.12f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = LucentTheme.colors.error,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun NeedsAttentionCard(
    changes: List<FailedChange>,
    onAction: (DashboardUiAction) -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = LucentTheme.colors.error.copy(alpha = 0.08f),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.feature_dashboard_needs_attention_title),
                style = MaterialTheme.typography.titleSmall,
                color = LucentTheme.colors.error,
            )
            changes.forEach { change ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text =
                        stringResource(
                            R.string.feature_dashboard_needs_attention_message,
                            change.operation.describe(),
                            change.message,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                )
                Row {
                    if (change.isConflict) {
                        TextButton(onClick = { onAction(DashboardUiAction.KeepLocalChange(change.id)) }) {
                            Text(stringResource(R.string.feature_dashboard_action_keep_mine))
                        }
                    } else {
                        TextButton(onClick = { onAction(DashboardUiAction.RetryChange(change.id)) }) {
                            Text(stringResource(R.string.feature_dashboard_action_retry))
                        }
                    }
                    TextButton(onClick = { onAction(DashboardUiAction.DiscardChange(change.id)) }) {
                        Text(
                            stringResource(
                                if (change.isConflict) {
                                    R.string.feature_dashboard_action_use_server_version
                                } else {
                                    R.string.feature_dashboard_action_discard
                                },
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingRow() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun RetryRow(
    message: String,
    onRetry: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = LucentTheme.colors.onSurfaceVariant)
        OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.feature_dashboard_action_retry)) }
    }
}

@Composable
private fun EmptyTransactions(onAdd: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ReceiptLong,
            contentDescription = null,
            tint = LucentTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.feature_dashboard_empty_transactions_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.feature_dashboard_empty_transactions_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = LucentTheme.colors.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onAdd) { Text(stringResource(R.string.feature_dashboard_action_add_transaction)) }
    }
}

/** Where the money went: "Checking → Groceries", or just the account that moved. */
private fun TransactionSplit.route(): String {
    val from = sourceName?.takeIf { it.isNotBlank() }
    val to = destinationName?.takeIf { it.isNotBlank() }
    return when {
        from != null && to != null -> "$from → $to"
        else -> from ?: to.orEmpty()
    }
}

/** Expenses are shown negative, income positive, transfers unsigned. Mixed-currency splits use the first currency. */
private fun Transaction.signedAmount(): Money {
    val first = splits.first()
    val total =
        splits
            .filter { it.amount.currency.code == first.amount.currency.code }
            .fold(Money.zero(first.amount.currency)) { sum, split -> Money(sum.amount + split.amount.amount.abs(), sum.currency) }
    return if (first.type == TransactionType.WITHDRAWAL) Money(total.amount.negate(), total.currency) else total
}

@Composable
private fun TransactionType.color(): Color =
    when (this) {
        TransactionType.DEPOSIT -> LucentTheme.colors.income
        TransactionType.WITHDRAWAL -> LucentTheme.colors.expense
        else -> LucentTheme.colors.transfer
    }

private fun TransactionType.icon(): ImageVector =
    when (this) {
        TransactionType.DEPOSIT -> Icons.Default.ArrowDownward
        TransactionType.WITHDRAWAL -> Icons.Default.ArrowUpward
        else -> Icons.Default.SwapHoriz
    }

@Composable
private fun SyncOperation.describe(): String =
    when (this) {
        SyncOperation.CREATE -> stringResource(R.string.feature_dashboard_op_create)
        SyncOperation.UPDATE -> stringResource(R.string.feature_dashboard_op_update)
        SyncOperation.DELETE -> stringResource(R.string.feature_dashboard_op_delete)
    }

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun DashboardScreenPreview() {
    val eur = Currency(code = "EUR", symbol = "€")
    val checking =
        Account(
            id = "1",
            serverId = "1",
            name = "Checking",
            type = AccountType.ASSET,
            balance = Money.of("1240.00", eur),
            pendingDelta = Money.of("-14.50", eur),
            currency = eur,
            active = true,
            order = 0,
            updatedAt = 0L,
        )
    val coffee =
        Transaction(
            id = "t1",
            serverId = null,
            title = null,
            splits =
                listOf(
                    TransactionSplit(
                        splitId = "s1",
                        serverSplitId = null,
                        groupId = "t1",
                        type = TransactionType.WITHDRAWAL,
                        date = System.currentTimeMillis(),
                        amount = Money.of("14.50", eur),
                        description = "Coffee",
                        sourceId = "1",
                        sourceName = "Checking",
                        destinationId = null,
                        destinationName = null,
                        categoryId = null,
                        categoryName = null,
                        budgetId = null,
                        budgetName = null,
                        notes = null,
                        tags = emptyList(),
                        isPendingSync = true,
                    ),
                ),
            createdAt = 0L,
            updatedAt = 0L,
        )
    val items =
        flowOf(
            PagingData.from(
                listOf(TransactionListItem.DayHeader(LocalDate.now()), TransactionListItem.Entry(coffee)),
            ),
        ).collectAsLazyPagingItems()

    LucentTheme {
        DashboardScreen(
            state =
                DashboardUiState(
                    serverHost = "firefly.home.lan",
                    netWorth = Money.of("12450.80", eur),
                    monthIncome = Money.of("3200.00", eur),
                    monthSpending = Money.of("1180.25", eur),
                    accounts = listOf(checking),
                    pendingChangesCount = 1,
                ),
            transactions = items,
            onAction = {},
        )
    }
}
