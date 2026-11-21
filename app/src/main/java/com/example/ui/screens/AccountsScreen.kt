package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AccountType
import com.example.data.local.TransactionType
import com.example.domain.AccountModel
import com.example.domain.ExchangeRateModel
import com.example.domain.TransactionModel
import com.example.ui.components.AddOrEditAccountDialog
import com.example.ui.components.DeleteAccountConfirmationDialog
import com.example.ui.components.MonthSelectorHeader
import com.example.ui.components.colorFromHex
import com.example.ui.components.iconForKey
import com.example.ui.theme.BroughtForwardGold
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue
import com.example.ui.viewmodel.FinanceUiState
import com.example.util.CurrencyAndBfCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AccountsScreen(
    uiState: FinanceUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetToToday: () -> Unit,
    onSaveAccount: (
        id: Long,
        name: String,
        accountType: AccountType,
        currencyCode: String,
        initialBroughtForwardBalance: Double,
        carryOverEnabled: Boolean,
        colorHex: String,
        accountNumber: String
    ) -> Unit,
    onDeleteAccount: (accountId: Long, accountName: String) -> Unit,
    onUpdateAccountBf: (accountId: Long, newOpeningBf: Double, carryOverEnabled: Boolean) -> Unit,
    onReconcileAccountBalance: (account: AccountModel, desiredClosingBalance: Double, memo: String) -> Unit,
    onOpenAccountLedger: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountModel?>(null) }
    var deletingAccount by remember { mutableStateOf<AccountModel?>(null) }
    var editingBfAccount by remember { mutableStateOf<AccountModel?>(null) }
    var reconcilingAccount by remember { mutableStateOf<AccountModel?>(null) }

    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("accounts_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Accounts & Brought Forward",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Multi-currency balances with monthly B/F carry-over",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        editingAccount = null
                        showAddAccountDialog = true
                    },
                    shape = CircleShape,
                    modifier = Modifier.testTag("add_account_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Account")
                }
            }
        }

        item {
            MonthSelectorHeader(
                displayMonthTitle = uiState.periodReconciliation.displayMonthTitle,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onResetToToday = onResetToToday
            )
        }

        if (uiState.accounts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Financial Accounts Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Create your first cash, bank, or savings account and set its initial Balance Brought Forward (B/F).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                editingAccount = null
                                showAddAccountDialog = true
                            },
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create First Account")
                        }
                    }
                }
            }
        } else {
            items(
                items = uiState.accounts,
                key = { it.id },
                contentType = { "account_ledger_card" }
            ) { account ->
                AccountLedgerCard(
                    account = account,
                    baseCurrency = uiState.preferences.baseCurrency,
                    rates = uiState.ratesMap,
                    privacyMaskEnabled = uiState.preferences.privacyMaskEnabled,
                    onEditAccountClick = { editingAccount = account },
                    onDeleteAccountClick = { deletingAccount = account },
                    onEditBfClick = { editingBfAccount = account },
                    onReconcileClick = { reconcilingAccount = account },
                    onOpenAccountLedger = { onOpenAccountLedger(account.id) }
                )
            }

            item {
                val periodTransfers = remember(uiState.periodTransactions) {
                    uiState.periodTransactions.filter { it.type == TransactionType.TRANSFER }
                }
                AccountTransferBreakdownCard(
                    transfers = periodTransfers,
                    baseCurrency = uiState.preferences.baseCurrency,
                    rates = uiState.ratesMap,
                    privacyMaskEnabled = uiState.preferences.privacyMaskEnabled
                )
            }
        }
    }

    if (showAddAccountDialog || editingAccount != null) {
        AddOrEditAccountDialog(
            initialAccount = editingAccount,
            availableCurrencies = uiState.exchangeRates.map { it.currencyCode }.ifEmpty { listOf("USD", "EUR", "GBP", "INR", "JPY") },
            onDismiss = {
                showAddAccountDialog = false
                editingAccount = null
            },
            onSave = { id, name, type, currency, initialBf, carryOver, colorHex, acctNum ->
                onSaveAccount(id, name, type, currency, initialBf, carryOver, colorHex, acctNum)
                showAddAccountDialog = false
                editingAccount = null
            }
        )
    }

    if (deletingAccount != null) {
        val accToDelete = deletingAccount!!
        DeleteAccountConfirmationDialog(
            account = accToDelete,
            appLockEnabled = uiState.preferences.appLockEnabled,
            expectedPinHash = uiState.preferences.pinHash,
            onDismiss = { deletingAccount = null },
            onConfirmDelete = {
                onDeleteAccount(accToDelete.id, accToDelete.name)
                deletingAccount = null
            }
        )
    }

    if (editingBfAccount != null) {
        EditAccountBroughtForwardDialog(
            account = editingBfAccount!!,
            onDismiss = { editingBfAccount = null },
            onSave = { newBf, carryOver ->
                onUpdateAccountBf(editingBfAccount!!.id, newBf, carryOver)
                editingBfAccount = null
            }
        )
    }

    if (reconcilingAccount != null) {
        ReconcileAccountDialog(
            account = reconcilingAccount!!,
            onDismiss = { reconcilingAccount = null },
            onConfirmReconcile = { desiredClosing, memo ->
                onReconcileAccountBalance(reconcilingAccount!!, desiredClosing, memo)
                reconcilingAccount = null
            }
        )
    }
}

/**
 * Clean 2x2 grid for Account Brought Forward (B/F), Closing (C/F), Inflow, and Outflow
 * to prevent any horizontal text overlap on narrow screens, plus Edit & Delete account controls.
 */
@Composable
private fun AccountLedgerCard(
    account: AccountModel,
    baseCurrency: String,
    rates: Map<String, com.example.domain.ExchangeRateModel>,
    privacyMaskEnabled: Boolean = false,
    onEditAccountClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    onEditBfClick: () -> Unit,
    onReconcileClick: () -> Unit,
    onOpenAccountLedger: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("account_card_${account.id}"),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(colorFromHex(account.colorHex).copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconForKey(account.iconKey),
                            contentDescription = account.name,
                            tint = colorFromHex(account.colorHex),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = account.currencyCode,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${account.accountType.displayLabel} • Carry-Over ${if (account.carryOverEnabled) "ON" else "OFF"}${if (account.maskedAccountNumber.isNotBlank()) " • ${account.maskedAccountNumber}" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (privacyMaskEnabled) "••••••" else CurrencyAndBfCalculator.formatCurrency(
                            account.periodClosingCarriedForward,
                            account.currencyCode,
                            rates
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!privacyMaskEnabled && !account.currencyCode.equals(baseCurrency, ignoreCase = true)) {
                        Text(
                            text = "≈ ${
                                CurrencyAndBfCalculator.formatCurrency(
                                    account.normalizedClosingInBase,
                                    baseCurrency,
                                    rates
                                )
                            }",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // 2x2 Metric Grid (Eliminates 4-column overlap)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AccountMetricBox(
                    label = "Opening B/F",
                    value = if (privacyMaskEnabled) "••••••" else CurrencyAndBfCalculator.formatCurrency(
                        account.periodOpeningBroughtForward,
                        account.currencyCode,
                        rates,
                        showPlusSign = account.periodOpeningBroughtForward > 0
                    ),
                    color = BroughtForwardGold,
                    modifier = Modifier.weight(1f)
                )
                AccountMetricBox(
                    label = "Closing C/F",
                    value = if (privacyMaskEnabled) "••••••" else CurrencyAndBfCalculator.formatCurrency(
                        account.periodClosingCarriedForward,
                        account.currencyCode,
                        rates
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AccountMetricBox(
                    label = "Period Inflow (+)",
                    value = if (privacyMaskEnabled) "••••••" else "+${
                        CurrencyAndBfCalculator.formatCurrency(
                            account.periodIncome,
                            account.currencyCode,
                            rates
                        )
                    }",
                    color = IncomeGreen,
                    modifier = Modifier.weight(1f)
                )
                AccountMetricBox(
                    label = "Period Outflow (−)",
                    value = if (privacyMaskEnabled) "••••••" else "-${
                        CurrencyAndBfCalculator.formatCurrency(
                            account.periodExpense,
                            account.currencyCode,
                            rates
                        )
                    }",
                    color = ExpenseCoral,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1-Tap Account Ledger Drill-Down
            Button(
                onClick = onOpenAccountLedger,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("view_account_ledger_${account.id}")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("View Account Ledger", maxLines = 1)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onEditBfClick,
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("edit_bf_button_${account.id}")
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Opening B/F", maxLines = 1)
                }

                FilledTonalButton(
                    onClick = onReconcileClick,
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reconcile_account_button_${account.id}")
                ) {
                    Icon(Icons.Default.Balance, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reconcile C/F", maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Account Edit & Delete Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onEditAccountClick,
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("edit_account_button_${account.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Account", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Account", maxLines = 1)
                }

                OutlinedButton(
                    onClick = onDeleteAccountClick,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, ExpenseCoral.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("delete_account_button_${account.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Account",
                        tint = ExpenseCoral,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete", color = ExpenseCoral, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun AccountMetricBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EditAccountBroughtForwardDialog(
    account: AccountModel,
    onDismiss: () -> Unit,
    onSave: (Double, Boolean) -> Unit
) {
    var bfText by rememberSaveable { mutableStateOf(account.initialBroughtForwardBalance.toString()) }
    var carryOver by rememberSaveable { mutableStateOf(account.carryOverEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Opening Brought Forward — ${account.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Set the initial opening Balance Brought Forward (${account.currencyCode}) and whether monthly balances carry forward into subsequent months.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = bfText,
                    onValueChange = { bfText = it },
                    label = { Text("Initial Opening B/F (${account.currencyCode})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_opening_bf_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable Monthly Carry-Over (B/F)",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = carryOver, onCheckedChange = { carryOver = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = bfText.trim().toDoubleOrNull() ?: account.initialBroughtForwardBalance
                    onSave(parsed, carryOver)
                },
                modifier = Modifier.testTag("save_opening_bf_button")
            ) {
                Text("Save B/F")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ReconcileAccountDialog(
    account: AccountModel,
    onDismiss: () -> Unit,
    onConfirmReconcile: (Double, String) -> Unit
) {
    var targetBalanceText by rememberSaveable {
        mutableStateOf(account.periodClosingCarriedForward.toString())
    }
    var memoText by rememberSaveable {
        mutableStateOf("Statement B/F Reconciliation")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reconcile Balance — ${account.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Current Closing Balance (C/F): ${account.currencyCode} ${"%.2f".format(account.periodClosingCarriedForward)}. Enter your actual statement balance below to record a Brought Forward (B/F) reconciliation entry.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = targetBalanceText,
                    onValueChange = { targetBalanceText = it },
                    label = { Text("Actual Statement Balance (${account.currencyCode})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reconcile_target_balance_input")
                )
                OutlinedTextField(
                    value = memoText,
                    onValueChange = { memoText = it },
                    label = { Text("Reconciliation Memo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetBalanceText.trim().toDoubleOrNull() ?: return@Button
                    onConfirmReconcile(target, memoText)
                },
                modifier = Modifier.testTag("confirm_reconcile_button")
            ) {
                Text("Reconcile")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private val transferDateFormatThreadLocal = ThreadLocal.withInitial { SimpleDateFormat("MMM d, yyyy", Locale.US) }

@Composable
private fun AccountTransferBreakdownCard(
    transfers: List<TransactionModel>,
    baseCurrency: String,
    rates: Map<String, ExchangeRateModel>,
    privacyMaskEnabled: Boolean = false
) {
    val dateFormat = transferDateFormatThreadLocal.get() ?: SimpleDateFormat("MMM d, yyyy", Locale.US)
    val totalTransferVolumeBase = remember(transfers) {
        transfers.sumOf { kotlin.math.abs(it.normalizedAmountInBase) }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("account_transfer_breakdown_card"),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                        contentDescription = null,
                        tint = TransferBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Account-to-Account Transfer Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Inter-account movement & multi-currency settlement flow",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = TransferBlue.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "${transfers.size} Transfer(s)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TransferBlue,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (transfers.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No inter-account transfers recorded in this period. Use '+ New Record → Transfer' to move funds between accounts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Period Transfer Volume ($baseCurrency)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = if (privacyMaskEnabled) "••••••" else CurrencyAndBfCalculator.formatCurrency(totalTransferVolumeBase, baseCurrency, rates),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    transfers.forEach { tx ->
                        val formattedOut = if (privacyMaskEnabled) "••••••" else CurrencyAndBfCalculator.formatCurrency(
                            tx.amount,
                            tx.currencyCode,
                            rates
                        )
                        val formattedIn = if (privacyMaskEnabled) "" else if (tx.targetAmount != null && tx.targetCurrencyCode != null && tx.targetCurrencyCode != tx.currencyCode) {
                            " → ${CurrencyAndBfCalculator.formatCurrency(tx.targetAmount, tx.targetCurrencyCode, rates)}"
                        } else {
                            ""
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("transfer_row_${tx.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${tx.accountName} → ${tx.targetAccountName ?: "Target Account"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${dateFormat.format(Date(tx.timestamp))} • ${tx.title}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$formattedOut$formattedIn",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TransferBlue
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
