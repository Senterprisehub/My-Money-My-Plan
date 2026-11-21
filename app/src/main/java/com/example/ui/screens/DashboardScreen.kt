package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.TransactionType
import com.example.domain.AccountModel
import com.example.domain.DashboardWidgetType
import com.example.domain.ParsedFinancialAlert
import com.example.domain.TransactionModel
import com.example.ui.components.MonthSelectorHeader
import com.example.ui.components.PendingAutoDetectedAlertsCard
import com.example.ui.components.PeriodBroughtForwardCard
import com.example.ui.components.TransactionRowItem
import com.example.ui.components.colorFromHex
import com.example.ui.components.iconForKey
import com.example.ui.theme.BroughtForwardGold
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.FinanceUiState
import com.example.util.CurrencyAndBfCalculator

@Composable
fun DashboardScreen(
    uiState: FinanceUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetToToday: () -> Unit,
    onOpenQuickRecord: (TransactionType) -> Unit,
    onEditTransaction: (TransactionModel) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onApprovePendingAlert: (ParsedFinancialAlert) -> Unit = {},
    onEditPendingAlert: (ParsedFinancialAlert) -> Unit = {},
    onDismissPendingAlert: (String) -> Unit = {},
    onApproveAllPendingAlerts: () -> Unit = {},
    onNavigateToAccounts: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToRecords: () -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    onOpenAccountLedger: (Long) -> Unit = {},
    onOpenCreditCardExpenditure: () -> Unit = {},
    onOpenCustomExport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeWidgets = remember(uiState.preferences.orderedWidgets, uiState.preferences.enabledWidgets) {
        uiState.preferences.orderedWidgets.filter { it in uiState.preferences.enabledWidgets }
    }
    val recentTransactions = remember(uiState.periodTransactions) {
        uiState.periodTransactions.take(5)
    }
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            MonthSelectorHeader(
                displayMonthTitle = uiState.periodReconciliation.displayMonthTitle,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onResetToToday = onResetToToday
            )
        }

        if (uiState.pendingAutoAlerts.isNotEmpty()) {
            item {
                PendingAutoDetectedAlertsCard(
                    pendingAlerts = uiState.pendingAutoAlerts,
                    rates = uiState.ratesMap,
                    onApproveAlert = onApprovePendingAlert,
                    onEditAlert = onEditPendingAlert,
                    onDismissAlert = onDismissPendingAlert,
                    onApproveAll = onApproveAllPendingAlerts,
                    accounts = uiState.accounts
                )
            }
        }

        items(
            items = activeWidgets,
            key = { it.name },
            contentType = { it.name }
        ) { widgetType ->
            when (widgetType) {
                DashboardWidgetType.NET_WORTH_HERO -> {
                    NetWorthHeroWidget(
                        uiState = uiState,
                        onExportCsv = onExportCsv,
                        onExportPdf = onExportPdf,
                        onNavigateToAccounts = onNavigateToAccounts,
                        onOpenCreditCardExpenditure = onOpenCreditCardExpenditure,
                        onOpenCustomExport = onOpenCustomExport
                    )
                }
                DashboardWidgetType.BROUGHT_FORWARD_PULSE -> {
                    PeriodBroughtForwardCard(
                        summary = uiState.periodReconciliation,
                        rates = uiState.ratesMap,
                        privacyMaskEnabled = uiState.preferences.privacyMaskEnabled
                    )
                }
                DashboardWidgetType.QUICK_ACTIONS -> {
                    QuickActionBarWidget(onOpenQuickRecord = onOpenQuickRecord)
                }
                DashboardWidgetType.ACCOUNTS_CAROUSEL -> {
                    DashboardAccountsBreakdownWidget(
                        accounts = uiState.accounts,
                        baseCurrency = uiState.preferences.baseCurrency,
                        rates = uiState.ratesMap,
                        privacyMaskEnabled = uiState.preferences.privacyMaskEnabled,
                        onNavigateToAccounts = onNavigateToAccounts,
                        onOpenAccountLedger = onOpenAccountLedger,
                        onOpenCreditCardExpenditure = onOpenCreditCardExpenditure
                    )
                }
                DashboardWidgetType.BUDGET_WATCHLIST -> {
                    BudgetWatchlistWidget(
                        uiState = uiState,
                        onNavigateToBudgets = onNavigateToBudgets
                    )
                }
                DashboardWidgetType.RECENT_TRANSACTIONS -> {
                    RecentTransactionsWidget(
                        transactions = recentTransactions,
                        rates = uiState.ratesMap,
                        onEditTransaction = onEditTransaction,
                        onDeleteTransaction = onDeleteTransaction,
                        onNavigateToRecords = onNavigateToRecords
                    )
                }
            }
        }
    }
}

/**
 * Clean, dynamic-height Google Classic Net Worth Card that excludes credit cards and never clips or overlaps text.
 */
@Composable
private fun NetWorthHeroWidget(
    uiState: FinanceUiState,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onOpenCreditCardExpenditure: () -> Unit,
    onOpenCustomExport: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("net_worth_hero_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Net Worth (Excl. Credit Cards)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    shape = CircleShape
                ) {
                    Text(
                        text = "${uiState.accounts.count { it.accountType != com.example.data.local.AccountType.CREDIT_CARD }} Asset Account(s)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (uiState.preferences.privacyMaskEnabled) "••••••" else CurrencyAndBfCalculator.formatCurrency(
                    uiState.totalNetWorthInBase,
                    uiState.preferences.baseCurrency,
                    uiState.ratesMap
                ),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.testTag("total_net_worth_text")
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (uiState.preferences.privacyMaskEnabled) {
                    "Opening B/F: ••••••  •  Closing C/F: ••••••"
                } else {
                    "Opening B/F: ${
                        CurrencyAndBfCalculator.formatCurrency(
                            uiState.periodReconciliation.openingBroughtForward,
                            uiState.periodReconciliation.currencyCode,
                            uiState.ratesMap
                        )
                    }  •  Closing C/F: ${
                        CurrencyAndBfCalculator.formatCurrency(
                            uiState.periodReconciliation.trueClosingCarriedForward,
                            uiState.periodReconciliation.currencyCode,
                            uiState.ratesMap
                        )
                    }"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
            )

            if (uiState.creditCardAccounts.isNotEmpty() || uiState.totalCreditCardExpenseInBase > 0.0) {
                Spacer(modifier = Modifier.height(14.dp))
                FilledTonalButton(
                    onClick = onOpenCreditCardExpenditure,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("credit_card_spend_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Credit Card Spend: " + (if (uiState.preferences.privacyMaskEnabled) "••••••" else CurrencyAndBfCalculator.formatCurrency(
                            uiState.totalCreditCardExpenseInBase,
                            uiState.preferences.baseCurrency,
                            uiState.ratesMap
                        )) + " • View Details 💳",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (uiState.accounts.isEmpty()) {
                Button(
                    onClick = onNavigateToAccounts,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Your First Account")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onOpenCustomExport,
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("hero_custom_export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Custom Export", maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = onExportPdf,
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hero_export_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF", maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = onExportCsv,
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("hero_export_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableView,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CSV", maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionBarWidget(
    onOpenQuickRecord: (TransactionType) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quick_actions_widget"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickActionButton(
                label = "Expense",
                icon = Icons.Default.RemoveCircleOutline,
                tint = ExpenseCoral,
                onClick = { onOpenQuickRecord(TransactionType.EXPENSE) },
                testTag = "quick_add_expense"
            )
            QuickActionButton(
                label = "Income",
                icon = Icons.Default.AddCircleOutline,
                tint = IncomeGreen,
                onClick = { onOpenQuickRecord(TransactionType.INCOME) },
                testTag = "quick_add_income"
            )
            QuickActionButton(
                label = "Transfer",
                icon = Icons.AutoMirrored.Filled.CompareArrows,
                tint = TransferBlue,
                onClick = { onOpenQuickRecord(TransactionType.TRANSFER) },
                testTag = "quick_add_transfer"
            )
            QuickActionButton(
                label = "Adjust B/F",
                icon = Icons.Default.History,
                tint = BroughtForwardGold,
                onClick = { onOpenQuickRecord(TransactionType.BROUGHT_FORWARD) },
                testTag = "quick_add_bf"
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BudgetWatchlistWidget(
    uiState: FinanceUiState,
    onNavigateToBudgets: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monthly Budgets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToBudgets) {
                    Text(if (uiState.budgets.isEmpty()) "Set Budget" else "View All")
                }
            }

            if (uiState.budgets.isEmpty()) {
                Text(
                    text = "No monthly category budgets set yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.budgets.take(3).forEach { b ->
                        val barColor = when {
                            b.isExceeded -> ExpenseCoral
                            b.isWarning -> WarningAmber
                            else -> MaterialTheme.colorScheme.primary
                        }
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = b.categoryName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${
                                        CurrencyAndBfCalculator.formatCurrency(
                                            b.spentAmountInBudgetCurrency,
                                            b.currencyCode,
                                            uiState.ratesMap
                                        )
                                    } / ${
                                        CurrencyAndBfCalculator.formatCurrency(
                                            b.limitAmount,
                                            b.currencyCode,
                                            uiState.ratesMap
                                        )
                                    } (${b.percentageUsed}%)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = barColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { b.progressRatio.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = barColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionsWidget(
    transactions: List<TransactionModel>,
    rates: Map<String, com.example.domain.ExchangeRateModel>,
    onEditTransaction: (TransactionModel) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onNavigateToRecords: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Records",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onNavigateToRecords) {
                Text("View All")
            }
        }

        if (transactions.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Text(
                    text = "No transactions recorded for this month yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(18.dp)
                )
            }
        } else {
            transactions.forEach { tx ->
                TransactionRowItem(
                    transaction = tx,
                    rates = rates,
                    onEditClick = onEditTransaction,
                    onDeleteClick = onDeleteTransaction
                )
            }
        }
    }
}

@Composable
private fun DashboardAccountsBreakdownWidget(
    accounts: List<AccountModel>,
    baseCurrency: String,
    rates: Map<String, com.example.domain.ExchangeRateModel>,
    privacyMaskEnabled: Boolean = false,
    onNavigateToAccounts: () -> Unit,
    onOpenAccountLedger: (Long) -> Unit = {},
    onOpenCreditCardExpenditure: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_accounts_individual_details_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "All Accounts Individual Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isExpanded) "Real-time balances, B/F reconciliation & ledgers" else "Retracted • Tap expand to view all account details",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onNavigateToAccounts,
                        modifier = Modifier.testTag("dashboard_manage_accounts_button")
                    ) {
                        Text(if (accounts.isEmpty()) "Add Account" else "Manage (${accounts.size})")
                    }
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("expand_accounts_breakdown_button")
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse account details" else "Expand account details",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (accounts.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No accounts configured yet",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Add Bank, Cash, Savings, or Credit Card accounts with Opening Brought Forward balances.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = onNavigateToAccounts,
                                    shape = CircleShape
                                ) {
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Account")
                                }
                            }
                        }
                    } else {
                        accounts.forEach { acc ->
                            val isCreditCard = acc.accountType == com.example.data.local.AccountType.CREDIT_CARD
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                border = BorderStroke(
                                    1.dp,
                                    if (isCreditCard) MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dashboard_account_item_${acc.id}")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(colorFromHex(acc.colorHex).copy(alpha = 0.16f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = iconForKey(acc.iconKey),
                                                    contentDescription = acc.name,
                                                    tint = colorFromHex(acc.colorHex),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = acc.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Surface(
                                                        color = if (isCreditCard) MaterialTheme.colorScheme.errorContainer
                                                        else MaterialTheme.colorScheme.secondaryContainer,
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isCreditCard) "Credit Card (Excluded from Net Worth)"
                                                            else acc.accountType.name.replace("_", " "),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (isCreditCard) MaterialTheme.colorScheme.onErrorContainer
                                                            else MaterialTheme.colorScheme.onSecondaryContainer,
                                                            fontWeight = FontWeight.SemiBold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = acc.currencyCode,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Column {
                                            Text(
                                                text = if (isCreditCard) "Card Balance / Outstanding" else "Current Closing Balance",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = if (privacyMaskEnabled) "••••••" else CurrencyAndBfCalculator.formatCurrency(
                                                    acc.periodClosingCarriedForward,
                                                    acc.currencyCode,
                                                    rates
                                                ),
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCreditCard) MaterialTheme.colorScheme.error
                                                else MaterialTheme.colorScheme.primary
                                            )
                                            if (!privacyMaskEnabled && !acc.currencyCode.equals(baseCurrency, ignoreCase = true)) {
                                                Text(
                                                    text = "≈ ${
                                                        CurrencyAndBfCalculator.formatCurrency(
                                                            acc.normalizedClosingInBase,
                                                            baseCurrency,
                                                            rates
                                                        )
                                                    }",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Opening B/F",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = BroughtForwardGold
                                            )
                                            Text(
                                                text = if (privacyMaskEnabled) "••••••" else CurrencyAndBfCalculator.formatCurrency(
                                                    acc.periodOpeningBroughtForward,
                                                    acc.currencyCode,
                                                    rates
                                                ),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = BroughtForwardGold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = { onOpenAccountLedger(acc.id) },
                                            shape = CircleShape,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("dashboard_account_ledger_${acc.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("View Ledger")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
