package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.platform.LocalContext
import com.example.data.repository.FinanceRepository
import com.example.data.security.CryptoManager
import com.example.domain.CustomExportFilterParams
import java.util.Calendar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AccountType
import com.example.data.local.TransactionType
import com.example.domain.AccountModel
import com.example.domain.CategoryModel
import com.example.domain.ExchangeRateModel
import com.example.domain.ParsedFinancialAlert
import com.example.domain.PeriodReconciliationSummary
import com.example.domain.TransactionModel
import com.example.ui.theme.BroughtForwardGold
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue
import com.example.util.BankAndUpiAlertParser
import com.example.util.CurrencyAndBfCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

fun iconForKey(key: String): ImageVector = when (key.lowercase(Locale.US)) {
    "restaurant" -> Icons.Default.Restaurant
    "shoppingcart" -> Icons.Default.ShoppingCart
    "home" -> Icons.Default.Home
    "directionscar" -> Icons.Default.DirectionsCar
    "shoppingbag" -> Icons.Default.ShoppingBag
    "bolt" -> Icons.Default.Bolt
    "medicalservices" -> Icons.Default.MedicalServices
    "flight" -> Icons.Default.Flight
    "movie" -> Icons.Default.Movie
    "work" -> Icons.Default.Work
    "trendingup" -> Icons.AutoMirrored.Filled.TrendingUp
    "savings" -> Icons.Default.Savings
    "history" -> Icons.Default.History
    "swap" -> Icons.AutoMirrored.Filled.CompareArrows
    "wallet" -> Icons.Default.AccountBalanceWallet
    "accountbalance" -> Icons.Default.AccountBalance
    "creditcard" -> Icons.Default.CreditCard
    "payments" -> Icons.Default.Payments
    "receipt" -> Icons.Default.Receipt
    else -> Icons.Default.Category
}

private val parsedHexColorCache = ConcurrentHashMap<String, Color>()
private val rowDateFormatThreadLocal = ThreadLocal.withInitial { SimpleDateFormat("MMM d • HH:mm", Locale.US) }

fun colorFromHex(hex: String, fallback: Color = Color(0xFF1A73E8)): Color {
    if (hex.isBlank()) return fallback
    parsedHexColorCache[hex]?.let { return it }
    return try {
        val clean = hex.removePrefix("#")
        val value = clean.toLong(16)
        val color = if (clean.length == 6) {
            Color(0xFF000000 or value)
        } else {
            Color(value)
        }
        if (parsedHexColorCache.size < 256) {
            parsedHexColorCache[hex] = color
        }
        color
    } catch (_: Throwable) {
        fallback
    }
}

@Composable
fun MonthSelectorHeader(
    displayMonthTitle: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetToToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = CircleShape,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier.testTag("prev_month_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous month",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onResetToToday)
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = displayMonthTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tap to return to current month",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onNextMonth,
                modifier = Modifier.testTag("next_month_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next month",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Clean Google Classic 2x2 Grid for Account Brought Forward (B/F) & Closing Carried Forward (C/F)
 * reconciliation without any overlapping columns or inline settings clutter.
 */
@Composable
fun PeriodBroughtForwardCard(
    summary: PeriodReconciliationSummary,
    rates: Map<String, ExchangeRateModel>,
    privacyMaskEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("period_brought_forward_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BroughtForwardGold.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Account Brought Forward",
                            tint = BroughtForwardGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Account Brought Forward (B/F) Summary",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${summary.displayMonthTitle} • ${summary.currencyCode} • Carry-Over ${if (summary.includeBroughtForwardInNet) "Active" else "Excluded"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Row 1 of 2x2 Grid: Opening B/F & Closing C/F
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ReconciliationGridBox(
                    label = "Opening Brought Forward (B/F)",
                    amountText = if (privacyMaskEnabled) "••••••" else CurrencyAndBfCalculator.formatCurrency(
                        summary.openingBroughtForward,
                        summary.currencyCode,
                        rates,
                        showPlusSign = summary.openingBroughtForward > 0
                    ),
                    accentColor = BroughtForwardGold,
                    modifier = Modifier.weight(1f)
                )
                ReconciliationGridBox(
                    label = if (summary.includeBroughtForwardInNet) {
                        "Closing Carried Forward (C/F)"
                    } else {
                        "Monthly Net (Excl. B/F)"
                    },
                    amountText = if (privacyMaskEnabled) "••••••" else CurrencyAndBfCalculator.formatCurrency(
                        summary.closingCarriedForward,
                        summary.currencyCode,
                        rates,
                        showPlusSign = summary.closingCarriedForward > 0
                    ),
                    accentColor = if (summary.closingCarriedForward >= 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        ExpenseCoral
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("closing_cf_value_text")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2 of 2x2 Grid: Period Income & Period Expense
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ReconciliationGridBox(
                    label = "Period Income (+)",
                    amountText = if (privacyMaskEnabled) "••••••" else CurrencyAndBfCalculator.formatCurrency(
                        summary.periodIncome,
                        summary.currencyCode,
                        rates,
                        showPlusSign = true
                    ),
                    accentColor = IncomeGreen,
                    modifier = Modifier.weight(1f)
                )
                ReconciliationGridBox(
                    label = "Period Expense (−)",
                    amountText = if (privacyMaskEnabled) "••••••" else "-${
                        CurrencyAndBfCalculator.formatCurrency(
                            summary.periodExpense,
                            summary.currencyCode,
                            rates
                        )
                    }",
                    accentColor = ExpenseCoral,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ReconciliationGridBox(
    label: String,
    amountText: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amountText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TransactionRowItem(
    transaction: TransactionModel,
    rates: Map<String, ExchangeRateModel>,
    onEditClick: (TransactionModel) -> Unit,
    onDeleteClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = rowDateFormatThreadLocal.get() ?: SimpleDateFormat("MMM d • HH:mm", Locale.US)
    val badgeColor = when (transaction.type) {
        TransactionType.INCOME -> IncomeGreen
        TransactionType.EXPENSE -> ExpenseCoral
        TransactionType.TRANSFER -> TransferBlue
        TransactionType.BROUGHT_FORWARD -> BroughtForwardGold
    }
    val signedDisplay = when (transaction.type) {
        TransactionType.INCOME -> "+${CurrencyAndBfCalculator.formatCurrency(transaction.amount, transaction.currencyCode, rates)}"
        TransactionType.EXPENSE -> "-${CurrencyAndBfCalculator.formatCurrency(transaction.amount, transaction.currencyCode, rates)}"
        TransactionType.TRANSFER -> CurrencyAndBfCalculator.formatCurrency(transaction.amount, transaction.currencyCode, rates)
        TransactionType.BROUGHT_FORWARD -> CurrencyAndBfCalculator.formatCurrency(
            transaction.amount,
            transaction.currencyCode,
            rates,
            showPlusSign = true
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEditClick(transaction) }
            .testTag("tx_item_${transaction.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colorFromHex(transaction.categoryColorHex, badgeColor).copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconForKey(transaction.categoryIconKey),
                    contentDescription = transaction.categoryName,
                    tint = colorFromHex(transaction.categoryColorHex, badgeColor),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val accountLine = if (transaction.type == TransactionType.TRANSFER && transaction.targetAccountName != null) {
                    "${transaction.accountName} → ${transaction.targetAccountName}"
                } else {
                    "${transaction.categoryName} • ${transaction.accountName}"
                }
                Text(
                    text = accountLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dateFormat.format(Date(transaction.timestamp)) +
                        if (transaction.note.isNotBlank()) " • ${transaction.note}" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = signedDisplay,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    maxLines = 1
                )
                Text(
                    text = "Bal: ${
                        CurrencyAndBfCalculator.formatCurrency(
                            transaction.runningBalanceAfter,
                            transaction.currencyCode,
                            rates
                        )
                    }",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = { onDeleteClick(transaction.id) },
                modifier = Modifier
                    .size(36.dp)
                    .testTag("delete_tx_${transaction.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete record",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AddOrEditTransactionDialog(
    initialTransaction: TransactionModel? = null,
    initialType: TransactionType = TransactionType.EXPENSE,
    accounts: List<AccountModel>,
    categories: List<CategoryModel>,
    onDismiss: () -> Unit,
    onNavigateToAccounts: (() -> Unit)? = null,
    onAddCategory: ((name: String, type: TransactionType, iconKey: String, colorHex: String) -> Unit)? = null,
    onDeleteCategory: ((categoryId: Long, categoryName: String) -> Unit)? = null,
    onSave: (
        id: Long,
        accountId: Long,
        targetAccountId: Long?,
        categoryId: Long?,
        type: TransactionType,
        amount: Double,
        title: String,
        note: String,
        timestamp: Long
    ) -> Unit
) {
    if (accounts.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Create an Account First") },
            text = { Text("You don't have any accounts yet. Create an account (with an optional Opening Balance Brought Forward) before recording transactions.") },
            confirmButton = {
                if (onNavigateToAccounts != null) {
                    Button(
                        onClick = {
                            onDismiss()
                            onNavigateToAccounts()
                        }
                    ) {
                        Text("Go to Accounts")
                    }
                } else {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            },
            dismissButton = {
                if (onNavigateToAccounts != null) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            }
        )
        return
    }

    val context = LocalContext.current
    val dateDisplayFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }
    val timeDisplayFormat = remember { SimpleDateFormat("hh:mm a", Locale.US) }

    var selectedTimestampMillis by rememberSaveable {
        mutableStateOf(initialTransaction?.timestamp ?: System.currentTimeMillis())
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    fun openDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedTimestampMillis }
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    timeInMillis = selectedTimestampMillis
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                selectedTimestampMillis = newCal.timeInMillis
                validationError = null
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun openTimePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedTimestampMillis }
        android.app.TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val newCal = Calendar.getInstance().apply {
                    timeInMillis = selectedTimestampMillis
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }
                selectedTimestampMillis = newCal.timeInMillis
                validationError = null
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            false
        ).show()
    }

    var selectedType by rememberSaveable {
        mutableStateOf(initialTransaction?.type ?: initialType)
    }
    var selectedAccountId by rememberSaveable {
        mutableStateOf(initialTransaction?.accountId ?: accounts.first().id)
    }
    var selectedTargetAccountId by rememberSaveable {
        mutableStateOf(
            initialTransaction?.targetAccountId
                ?: accounts.getOrNull(1)?.id
                ?: accounts.first().id
        )
    }
    val matchingCategories = categories.filter {
        when (selectedType) {
            TransactionType.EXPENSE -> it.type == TransactionType.EXPENSE
            TransactionType.INCOME -> it.type == TransactionType.INCOME
            TransactionType.BROUGHT_FORWARD -> it.type == TransactionType.BROUGHT_FORWARD
            TransactionType.TRANSFER -> false
        }
    }
    var selectedCategoryId by rememberSaveable(selectedType, matchingCategories.size) {
        mutableStateOf(
            initialTransaction?.categoryId?.takeIf { id -> matchingCategories.any { it.id == id } }
                ?: matchingCategories.firstOrNull()?.id
        )
    }
    var amountText by rememberSaveable {
        mutableStateOf(initialTransaction?.amount?.toString() ?: "")
    }
    var titleText by rememberSaveable {
        mutableStateOf(initialTransaction?.title ?: "")
    }
    var noteText by rememberSaveable {
        mutableStateOf(initialTransaction?.note ?: "")
    }
    var showInlineCreateCategoryDialog by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val activeAccount = accounts.find { it.id == selectedAccountId } ?: accounts.first()
    val selectedCategoryObj = matchingCategories.find { it.id == selectedCategoryId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialTransaction == null || initialTransaction.id == 0L) "Add Record" else "Edit Record",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TransactionType.entries.forEach { tType ->
                        FilterChip(
                            selected = selectedType == tType,
                            onClick = { selectedType = tType },
                            shape = CircleShape,
                            label = { Text(tType.displayLabel) },
                            leadingIcon = if (selectedType == tType) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.testTag("tx_type_chip_${tType.name}")
                        )
                    }
                }

                // Rebuilt Date with Time Picker
                Text(
                    text = "Transaction Date & Time",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Date Picker Action
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { openDatePicker() }
                                .padding(vertical = 4.dp)
                                .testTag("tx_date_picker_trigger")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Pick Date",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = dateDisplayFormat.format(Date(selectedTimestampMillis)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )

                        // Time Picker Action
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { openTimePicker() }
                                .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                                .testTag("tx_time_picker_trigger")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Pick Time",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = timeDisplayFormat.format(Date(selectedTimestampMillis)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        validationError = null
                    },
                    label = {
                        Text(
                            if (selectedType == TransactionType.BROUGHT_FORWARD) {
                                "Brought Forward Adjustment (${activeAccount.currencyCode}, +/-)"
                            } else {
                                "Amount (${activeAccount.currencyCode})"
                            }
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_amount_input")
                )

                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Title / Payee") },
                    placeholder = {
                        Text(
                            when (selectedType) {
                                TransactionType.BROUGHT_FORWARD -> "Opening / Period Balance Brought Forward"
                                TransactionType.TRANSFER -> "Account Transfer"
                                else -> "Enter description"
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_title_input")
                )

                Text(
                    text = if (selectedType == TransactionType.TRANSFER) "From Account" else "Account",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    accounts.forEach { acc ->
                        FilterChip(
                            selected = selectedAccountId == acc.id,
                            onClick = { selectedAccountId = acc.id },
                            shape = CircleShape,
                            label = { Text("${acc.name} (${acc.currencyCode})") }
                        )
                    }
                }

                if (selectedType == TransactionType.TRANSFER) {
                    Text(
                        text = "To Target Account",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        accounts.filter { it.id != selectedAccountId }.forEach { acc ->
                            FilterChip(
                                selected = selectedTargetAccountId == acc.id,
                                onClick = { selectedTargetAccountId = acc.id },
                                shape = CircleShape,
                                label = { Text("${acc.name} (${acc.currencyCode})") }
                            )
                        }
                    }
                } else if (selectedType == TransactionType.EXPENSE || selectedType == TransactionType.INCOME) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (onDeleteCategory != null && selectedCategoryObj != null) {
                                TextButton(
                                    onClick = {
                                        onDeleteCategory(selectedCategoryObj.id, selectedCategoryObj.name)
                                    },
                                    modifier = Modifier.testTag("tx_dialog_delete_category_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete selected category",
                                        tint = ExpenseCoral,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete", color = ExpenseCoral, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            if (onAddCategory != null) {
                                TextButton(
                                    onClick = { showInlineCreateCategoryDialog = true },
                                    modifier = Modifier.testTag("tx_dialog_add_category_button")
                                ) {
                                    Text("+ Add Category", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    if (matchingCategories.isEmpty()) {
                        Text(
                            text = "No categories yet. Tap '+ Add Category' to create one.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { categoryDropdownExpanded = true }
                                    .testTag("tx_dialog_category_dropdown_trigger")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (selectedCategoryObj != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(colorFromHex(selectedCategoryObj.colorHex).copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = iconForKey(selectedCategoryObj.iconKey),
                                                    contentDescription = null,
                                                    tint = colorFromHex(selectedCategoryObj.colorHex),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = selectedCategoryObj.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        } else {
                                            Text(
                                                text = "Select Category...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Expand Category Dropdown",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = categoryDropdownExpanded,
                                onDismissRequest = { categoryDropdownExpanded = false }
                            ) {
                                matchingCategories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .clip(CircleShape)
                                                        .background(colorFromHex(cat.colorHex).copy(alpha = 0.2f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = iconForKey(cat.iconKey),
                                                        contentDescription = null,
                                                        tint = colorFromHex(cat.colorHex),
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = cat.name,
                                                    fontWeight = if (selectedCategoryId == cat.id) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedCategoryId = cat.id
                                            categoryDropdownExpanded = false
                                        },
                                        trailingIcon = if (selectedCategoryId == cat.id) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        } else null,
                                        modifier = Modifier.testTag("tx_dialog_cat_item_${cat.id}")
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note (Optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_note_input")
                )

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = ExpenseCoral
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = amountText.trim().toDoubleOrNull()
                    if (parsed == null || parsed == 0.0) {
                        validationError = "Please enter a valid non-zero numeric amount."
                        return@Button
                    }
                    val finalAmount = if (selectedType == TransactionType.BROUGHT_FORWARD) {
                        parsed
                    } else {
                        kotlin.math.abs(parsed)
                    }
                    onSave(
                        initialTransaction?.id ?: 0L,
                        selectedAccountId,
                        if (selectedType == TransactionType.TRANSFER) selectedTargetAccountId else null,
                        selectedCategoryId,
                        selectedType,
                        finalAmount,
                        titleText.ifBlank { selectedType.displayLabel },
                        noteText,
                        selectedTimestampMillis
                    )
                    onDismiss()
                },
                modifier = Modifier.testTag("save_tx_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (initialTransaction != null && initialTransaction.id > 0L) {
                    OutlinedButton(
                        onClick = {
                            val parsed = amountText.trim().toDoubleOrNull()
                            if (parsed == null || parsed == 0.0) {
                                validationError = "Please enter a valid non-zero numeric amount."
                                return@OutlinedButton
                            }
                            val finalAmount = if (selectedType == TransactionType.BROUGHT_FORWARD) {
                                parsed
                            } else {
                                kotlin.math.abs(parsed)
                            }
                            onSave(
                                0L,
                                selectedAccountId,
                                if (selectedType == TransactionType.TRANSFER) selectedTargetAccountId else null,
                                selectedCategoryId,
                                selectedType,
                                finalAmount,
                                titleText.ifBlank { selectedType.displayLabel },
                                noteText,
                                System.currentTimeMillis()
                            )
                            onDismiss()
                        },
                        modifier = Modifier.testTag("tx_duplicate_today_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Duplicate as Today")
                    }
                }
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )

    if (showInlineCreateCategoryDialog && onAddCategory != null) {
        CreateCategoryDialog(
            initialType = if (selectedType == TransactionType.INCOME) TransactionType.INCOME else TransactionType.EXPENSE,
            onDismiss = { showInlineCreateCategoryDialog = false },
            onSave = { name, type, iconKey, colorHex ->
                onAddCategory(name, type, iconKey, colorHex)
                showInlineCreateCategoryDialog = false
            }
        )
    }
}

@Composable
fun CreateCategoryDialog(
    initialCategory: CategoryModel? = null,
    initialType: TransactionType = TransactionType.EXPENSE,
    onDismiss: () -> Unit,
    onSave: (name: String, type: TransactionType, iconKey: String, colorHex: String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialCategory?.name ?: "") }
    var type by rememberSaveable {
        mutableStateOf(
            initialCategory?.type
                ?: if (initialType == TransactionType.INCOME) TransactionType.INCOME else TransactionType.EXPENSE
        )
    }
    val iconChoices = remember {
        listOf(
            "Category" to "Default",
            "Restaurant" to "Food",
            "ShoppingCart" to "Grocery",
            "Home" to "Home",
            "DirectionsCar" to "Auto",
            "ShoppingBag" to "Shop",
            "Bolt" to "Bills",
            "MedicalServices" to "Health",
            "Flight" to "Travel",
            "Movie" to "Fun",
            "Work" to "Work",
            "Savings" to "Save"
        )
    }
    val colorChoices = remember {
        listOf(
            "#1A73E8",
            "#1E8E3E",
            "#D93025",
            "#E37400",
            "#9334E6",
            "#12B5CB",
            "#E52592"
        )
    }
    var selectedIconKey by rememberSaveable { mutableStateOf(initialCategory?.iconKey ?: "Category") }
    var selectedColorHex by rememberSaveable { mutableStateOf(initialCategory?.colorHex ?: "#1A73E8") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialCategory == null) "Add Category" else "Edit Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g., Coffee, Education, Bonus") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_name_input")
                )

                Text("Category Type", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE },
                        label = { Text("Expense") },
                        modifier = Modifier.testTag("category_type_expense_chip")
                    )
                    FilterChip(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME },
                        label = { Text("Income") },
                        modifier = Modifier.testTag("category_type_income_chip")
                    )
                }

                Text("Category Icon", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    iconChoices.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedIconKey == key,
                            onClick = { selectedIconKey = key },
                            leadingIcon = {
                                Icon(
                                    imageVector = iconForKey(key),
                                    contentDescription = label,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(label) }
                        )
                    }
                }

                Text("Badge Color", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorChoices.forEach { hex ->
                        FilterChip(
                            selected = selectedColorHex == hex,
                            onClick = { selectedColorHex = hex },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(colorFromHex(hex))
                                )
                            },
                            label = { Text(hex) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), type, selectedIconKey, selectedColorHex)
                    }
                },
                modifier = Modifier.testTag("confirm_create_category_button")
            ) {
                Text(if (initialCategory == null) "Add Category" else "Save Changes")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Executive Card for adding, editing, and deleting Expense & Income Categories.
 */
@Composable
fun CategoryManagementSectionCard(
    categories: List<CategoryModel>,
    onAddCategoryClick: () -> Unit,
    onEditCategoryClick: ((CategoryModel) -> Unit)? = null,
    onDeleteCategory: (categoryId: Long, categoryName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabType by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
    val filteredCategories = categories.filter { it.type == selectedTabType }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_management_card"),
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
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Categories (Add, Edit & Delete)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Customize Expense & Income category names, icons, and colors",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = onAddCategoryClick,
                    shape = CircleShape,
                    modifier = Modifier.testTag("card_add_category_button")
                ) {
                    Text("+ Add")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedTabType == TransactionType.EXPENSE,
                    onClick = { selectedTabType = TransactionType.EXPENSE },
                    shape = CircleShape,
                    label = {
                        val expCount = categories.count { it.type == TransactionType.EXPENSE }
                        Text("Expense ($expCount)")
                    },
                    modifier = Modifier.testTag("manage_cat_expense_tab")
                )
                FilterChip(
                    selected = selectedTabType == TransactionType.INCOME,
                    onClick = { selectedTabType = TransactionType.INCOME },
                    shape = CircleShape,
                    label = {
                        val incCount = categories.count { it.type == TransactionType.INCOME }
                        Text("Income ($incCount)")
                    },
                    modifier = Modifier.testTag("manage_cat_income_tab")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredCategories.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No ${selectedTabType.displayLabel.lowercase(Locale.US)} categories. Tap '+ Add' to create one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredCategories.forEach { cat ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("category_row_${cat.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(colorFromHex(cat.colorHex).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = iconForKey(cat.iconKey),
                                            contentDescription = cat.name,
                                            tint = colorFromHex(cat.colorHex),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = cat.type.displayLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (onEditCategoryClick != null) {
                                        IconButton(
                                            onClick = { onEditCategoryClick(cat) },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .testTag("edit_category_${cat.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit category ${cat.name}",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { onDeleteCategory(cat.id, cat.name) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("delete_category_${cat.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete category ${cat.name}",
                                            tint = ExpenseCoral,
                                            modifier = Modifier.size(18.dp)
                                        )
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

@Composable
fun AddOrEditAccountDialog(
    initialAccount: AccountModel? = null,
    availableCurrencies: List<String>,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        name: String,
        accountType: AccountType,
        currencyCode: String,
        initialBf: Double,
        carryOver: Boolean,
        colorHex: String,
        accountNumber: String
    ) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialAccount?.name ?: "") }
    var accountNumberText by rememberSaveable { mutableStateOf(initialAccount?.accountNumber ?: "") }
    var selectedType by rememberSaveable { mutableStateOf(initialAccount?.accountType ?: AccountType.BANK) }
    var selectedCurrency by rememberSaveable {
        mutableStateOf(initialAccount?.currencyCode ?: availableCurrencies.firstOrNull() ?: "USD")
    }
    var openingBfText by rememberSaveable {
        mutableStateOf(
            if (initialAccount != null) {
                initialAccount.initialBroughtForwardBalance.toString()
            } else {
                "0.00"
            }
        )
    }
    var carryOverEnabled by rememberSaveable {
        mutableStateOf(initialAccount?.carryOverEnabled ?: true)
    }
    val colorChoices = remember {
        listOf(
            "#1A73E8",
            "#1E8E3E",
            "#D93025",
            "#E37400",
            "#9334E6",
            "#12B5CB"
        )
    }
    var selectedColorHex by rememberSaveable {
        mutableStateOf(initialAccount?.colorHex ?: "#1A73E8")
    }

    val isCard = selectedType == AccountType.CREDIT_CARD
    val maskedPreview = remember(accountNumberText) {
        FinanceRepository.maskAccountNumber(accountNumberText)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialAccount == null) "Create Account" else "Edit Account — ${initialAccount.name}")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    placeholder = { Text("e.g., Main Checking, Cash Wallet") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_account_name_input")
                )

                OutlinedTextField(
                    value = accountNumberText,
                    onValueChange = { accountNumberText = it },
                    label = { Text(if (isCard) "Card Number" else "Account Number") },
                    placeholder = {
                        Text(if (isCard) "e.g., 4111222233334444" else "e.g., 10 to 18 digits or suffix")
                    },
                    supportingText = {
                        if (accountNumberText.isNotBlank()) {
                            Text(
                                text = "Masked: $maskedPreview (Last 6 digits visible, prior hidden with 'x')",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        } else {
                            Text(
                                text = "Used for pinpoint auto-tracking segregation by account/card",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_account_number_input")
                )

                Text("Account Type", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AccountType.entries.forEach { aType ->
                        FilterChip(
                            selected = selectedType == aType,
                            onClick = { selectedType = aType },
                            label = { Text(aType.displayLabel) }
                        )
                    }
                }

                Text("Currency", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableCurrencies.forEach { code ->
                        FilterChip(
                            selected = selectedCurrency == code,
                            onClick = { selectedCurrency = code },
                            label = { Text(code) }
                        )
                    }
                }

                OutlinedTextField(
                    value = openingBfText,
                    onValueChange = { openingBfText = it },
                    label = { Text("Opening Brought Forward (B/F) in $selectedCurrency") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_account_bf_input")
                )

                Text("Badge Color", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorChoices.forEach { hex ->
                        FilterChip(
                            selected = selectedColorHex == hex,
                            onClick = { selectedColorHex = hex },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(colorFromHex(hex))
                                )
                            },
                            label = { Text(hex) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Carry Over Balance Month-to-Month",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = carryOverEnabled,
                        onCheckedChange = { carryOverEnabled = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val bf = openingBfText.trim().toDoubleOrNull() ?: 0.0
                        onSave(
                            initialAccount?.id ?: 0L,
                            name.trim(),
                            selectedType,
                            selectedCurrency,
                            bf,
                            carryOverEnabled,
                            selectedColorHex,
                            accountNumberText.trim()
                        )
                    }
                },
                modifier = Modifier.testTag("confirm_save_account_button")
            ) {
                Text(if (initialAccount == null) "Create" else "Save Changes")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DeleteAccountConfirmationDialog(
    account: AccountModel,
    appLockEnabled: Boolean = false,
    expectedPinHash: String = "",
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    var enteredPin by rememberSaveable { mutableStateOf("") }
    val pinValid = !appLockEnabled || CryptoManager.hashPin(enteredPin) == expectedPinHash

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Account — ${account.name}?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Deleting '${account.name}' (${account.currencyCode}) will permanently remove this account and all transactions/transfers recorded under it from your encrypted ledger.",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (appLockEnabled) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "App Lock / Biometric Vault security is enabled. Enter your 4-digit Vault PIN to confirm account deletion:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = { enteredPin = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text("4-Digit PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_account_pin_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                enabled = pinValid,
                colors = ButtonDefaults.buttonColors(containerColor = ExpenseCoral),
                modifier = Modifier.testTag("confirm_delete_account_button")
            ) {
                Text("Delete Account")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DisableAppLockConfirmationDialog(
    expectedPinHash: String,
    onDismiss: () -> Unit,
    onConfirmDisable: () -> Unit
) {
    var enteredPin by rememberSaveable { mutableStateOf("") }
    val pinValid = CryptoManager.hashPin(enteredPin) == expectedPinHash

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Disable App Lock / Vault Security?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Turning off App Lock and biometric protection will remove Vault security from app startup. Enter your 4-digit Vault PIN to confirm:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = { enteredPin = it.filter { ch -> ch.isDigit() }.take(4) },
                    label = { Text("4-Digit PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("disable_app_lock_pin_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDisable,
                enabled = pinValid,
                modifier = Modifier.testTag("confirm_disable_app_lock_button")
            ) {
                Text("Turn Off Lock")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Google Classic Card for adding, editing, and deleting Accounts inside Settings.
 */
@Composable
fun AccountManagementSectionCard(
    accounts: List<AccountModel>,
    rates: Map<String, ExchangeRateModel>,
    onAddAccountClick: () -> Unit,
    onEditAccountClick: (AccountModel) -> Unit,
    onDeleteAccountClick: (AccountModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("account_management_card"),
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
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Accounts (Add, Edit & Delete)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Manage multi-currency accounts & Brought Forward settings",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = onAddAccountClick,
                    shape = CircleShape,
                    modifier = Modifier.testTag("settings_add_account_button")
                ) {
                    Text("+ Add")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (accounts.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No accounts created yet. Tap '+ Add' to create your first account.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    accounts.forEach { acc ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_account_row_${acc.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(colorFromHex(acc.colorHex).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = iconForKey(acc.iconKey),
                                            contentDescription = acc.name,
                                            tint = colorFromHex(acc.colorHex),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "${acc.name} (${acc.currencyCode})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${acc.accountType.displayLabel} • Bal: ${
                                                CurrencyAndBfCalculator.formatCurrency(
                                                    acc.currentAllTimeBalance,
                                                    acc.currencyCode,
                                                    rates
                                                )
                                            }",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onEditAccountClick(acc) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("settings_edit_account_${acc.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit account ${acc.name}",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteAccountClick(acc) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("settings_delete_account_${acc.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete account ${acc.name}",
                                            tint = ExpenseCoral,
                                            modifier = Modifier.size(18.dp)
                                        )
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

/**
 * Material You Card displaying queued Banking, UPI (Super Money, GPay, PhonePe, Paytm, BHIM, etc.)
 * and Bank SMS notifications when the user selects "Hold in Pending Review" mode.
 */
@Composable
fun PendingAutoDetectedAlertsCard(
    pendingAlerts: List<ParsedFinancialAlert>,
    rates: Map<String, ExchangeRateModel>,
    onApproveAlert: (ParsedFinancialAlert) -> Unit,
    onEditAlert: ((ParsedFinancialAlert) -> Unit)? = null,
    onDismissAlert: (String) -> Unit,
    onApproveAll: () -> Unit,
    modifier: Modifier = Modifier,
    accounts: List<AccountModel> = emptyList()
) {
    if (pendingAlerts.isEmpty()) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pending_auto_alerts_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Pending Debit & Credit Alerts (${pendingAlerts.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Strict Debit/Credit & A/c-wise matching ready for review",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                if (pendingAlerts.size > 1) {
                    TextButton(
                        onClick = onApproveAll,
                        modifier = Modifier.testTag("approve_all_pending_alerts_button")
                    ) {
                        Text("Approve All", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pendingAlerts.forEach { alert ->
                    val amountColor = if (alert.type == TransactionType.EXPENSE) ExpenseCoral else IncomeGreen
                    val prefix = if (alert.type == TransactionType.EXPENSE) "-" else "+"
                    val formattedAmt = CurrencyAndBfCalculator.formatCurrency(
                        alert.amount,
                        alert.currencyCode,
                        rates
                    )
                    val matchedStrict = remember(alert.id, accounts) {
                        BankAndUpiAlertParser.matchAccountStrict(alert, accounts)
                    }
                    val needsAccountSelection = matchedStrict == null && accounts.size > 1 && onEditAlert != null

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pending_alert_item_${alert.id}")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = alert.sourceAppLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = alert.merchantOrPayee,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = "$prefix$formattedAmt",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = amountColor
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            val accountBadgeText = when {
                                matchedStrict != null -> "A/c Matched: ${matchedStrict.name}${if (matchedStrict.maskedAccountNumber.isNotBlank()) " (${matchedStrict.maskedAccountNumber})" else ""}"
                                accounts.size > 1 -> "A/c Unmatched — Select Account"
                                accounts.size == 1 -> "Target A/c: ${accounts.first().name}"
                                else -> "Category: ${alert.suggestedCategoryName}"
                            }

                            val metaLine = buildString {
                                append(accountBadgeText)
                                append(" • ${alert.suggestedCategoryName}")
                                if (!alert.accountHint.isNullOrBlank()) {
                                    append(" • Hint: ${alert.accountHint}")
                                }
                                if (!alert.referenceId.isNullOrBlank()) {
                                    append(" • Ref: ${alert.referenceId}")
                                }
                            }
                            Text(
                                text = metaLine,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (needsAccountSelection) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (needsAccountSelection) {
                                            onEditAlert?.invoke(alert)
                                        } else {
                                            onApproveAlert(alert)
                                        }
                                    },
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .testTag("approve_pending_alert_${alert.id}")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (needsAccountSelection) "Assign A/c" else "Approve", maxLines = 1)
                                }
                                if (onEditAlert != null) {
                                    OutlinedButton(
                                        onClick = { onEditAlert(alert) },
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("edit_pending_alert_${alert.id}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Edit", maxLines = 1)
                                    }
                                }
                                OutlinedButton(
                                    onClick = { onDismissAlert(alert.id) },
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("dismiss_pending_alert_${alert.id}")
                                ) {
                                    Text("Dismiss", maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Confirmation dialog before permanently deleting a financial transaction record from the ledger.
 */
@Composable
fun DeleteTransactionConfirmationDialog(
    transaction: TransactionModel,
    rates: Map<String, ExchangeRateModel>,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    val formattedAmount = CurrencyAndBfCalculator.formatCurrency(
        transaction.amount,
        transaction.currencyCode,
        rates
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = ExpenseCoral
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Ledger Record?")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Are you sure you want to delete '${transaction.title}' ($formattedAmount)?",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Account: ${transaction.accountName} • Category: ${transaction.categoryName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Deleting this record will recalculate ${transaction.accountName}'s running balance and Brought Forward (B/F) carry-over.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = ExpenseCoral),
                modifier = Modifier.testTag("confirm_delete_tx_button")
            ) {
                Text("Delete Record")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_delete_tx_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Enterprise Custom Data Export Dialog supporting multi-select filters across
 * Accounts, Categories, Transaction Types, and Date Bounds with Download & Share actions.
 */
@Composable
fun DataExportCustomFilterDialog(
    accounts: List<AccountModel>,
    categories: List<CategoryModel>,
    baseCurrency: String,
    onDismiss: () -> Unit,
    onExportAndDownload: (CustomExportFilterParams) -> Unit,
    onExportAndShare: (CustomExportFilterParams) -> Unit
) {
    val context = LocalContext.current
    var selectedAccountIds by rememberSaveable { mutableStateOf<Set<Long>>(emptySet()) }
    var selectedCategoryIds by rememberSaveable { mutableStateOf<Set<Long>>(emptySet()) }
    var selectedTypes by rememberSaveable { mutableStateOf(TransactionType.entries.toSet()) }
    var formatAsPdf by rememberSaveable { mutableStateOf(true) }

    val (curMonthStart, curMonthEnd) = remember {
        CurrencyAndBfCalculator.monthBounds(CurrencyAndBfCalculator.currentYearMonth())
    }
    var rangeMode by rememberSaveable { mutableStateOf("MONTH") } // "MONTH", "30D", "ALL", "CUSTOM"
    var customStartMillis by rememberSaveable { mutableStateOf(curMonthStart) }
    var customEndMillis by rememberSaveable { mutableStateOf(curMonthEnd) }
    val dateDisplayFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }

    fun openStartDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = customStartMillis }
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                customStartMillis = newCal.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun openEndDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = customEndMillis }
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                customEndMillis = newCal.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun buildParams(): CustomExportFilterParams {
        val (start, end) = when (rangeMode) {
            "MONTH" -> curMonthStart to curMonthEnd
            "30D" -> (System.currentTimeMillis() - 30L * 24L * 3600L * 1000L) to System.currentTimeMillis()
            "ALL" -> null to null
            else -> customStartMillis to customEndMillis
        }
        return CustomExportFilterParams(
            selectedAccountIds = selectedAccountIds,
            selectedCategoryIds = selectedCategoryIds,
            selectedTypes = selectedTypes,
            customStartMillis = start,
            customEndMillis = end,
            formatAsPdf = formatAsPdf
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Custom Data Export", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Select custom accounts, categories, types, and date ranges. Then download directly or share.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 1. Export Format
                Text("Export Format", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = formatAsPdf,
                        onClick = { formatAsPdf = true },
                        shape = CircleShape,
                        label = { Text("PDF Statement") },
                        modifier = Modifier.testTag("export_format_pdf")
                    )
                    FilterChip(
                        selected = !formatAsPdf,
                        onClick = { formatAsPdf = false },
                        shape = CircleShape,
                        label = { Text("CSV Spreadsheet") },
                        modifier = Modifier.testTag("export_format_csv")
                    )
                }

                // 2. Date Range
                Text("Date Period", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = rangeMode == "MONTH",
                        onClick = { rangeMode = "MONTH" },
                        shape = CircleShape,
                        label = { Text("This Month") }
                    )
                    FilterChip(
                        selected = rangeMode == "30D",
                        onClick = { rangeMode = "30D" },
                        shape = CircleShape,
                        label = { Text("Last 30 Days") }
                    )
                    FilterChip(
                        selected = rangeMode == "ALL",
                        onClick = { rangeMode = "ALL" },
                        shape = CircleShape,
                        label = { Text("All Time") }
                    )
                    FilterChip(
                        selected = rangeMode == "CUSTOM",
                        onClick = { rangeMode = "CUSTOM" },
                        shape = CircleShape,
                        label = { Text("Custom Dates") }
                    )
                }

                if (rangeMode == "CUSTOM") {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { openStartDatePicker() }
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("From", style = MaterialTheme.typography.labelSmall)
                                    Text(dateDisplayFormat.format(Date(customStartMillis)), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { openEndDatePicker() }
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("To", style = MaterialTheme.typography.labelSmall)
                                    Text(dateDisplayFormat.format(Date(customEndMillis)), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 3. Accounts Filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Accounts Filter", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = {
                        selectedAccountIds = if (selectedAccountIds.isEmpty()) accounts.map { it.id }.toSet() else emptySet()
                    }) {
                        Text(if (selectedAccountIds.isEmpty()) "Select Specific" else "All Accounts", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedAccountIds.isEmpty(),
                        onClick = { selectedAccountIds = emptySet() },
                        shape = CircleShape,
                        label = { Text("All Accounts") }
                    )
                    accounts.forEach { acc ->
                        val isSelected = acc.id in selectedAccountIds
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedAccountIds = if (isSelected) {
                                    selectedAccountIds - acc.id
                                } else {
                                    selectedAccountIds + acc.id
                                }
                            },
                            shape = CircleShape,
                            label = { Text(acc.name) }
                        )
                    }
                }

                // 4. Categories Filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Categories Filter", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = {
                        selectedCategoryIds = if (selectedCategoryIds.isEmpty()) categories.map { it.id }.toSet() else emptySet()
                    }) {
                        Text(if (selectedCategoryIds.isEmpty()) "Select Specific" else "All Categories", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedCategoryIds.isEmpty(),
                        onClick = { selectedCategoryIds = emptySet() },
                        shape = CircleShape,
                        label = { Text("All Categories") }
                    )
                    categories.forEach { cat ->
                        val isSelected = cat.id in selectedCategoryIds
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategoryIds = if (isSelected) {
                                    selectedCategoryIds - cat.id
                                } else {
                                    selectedCategoryIds + cat.id
                                }
                            },
                            shape = CircleShape,
                            label = { Text(cat.name) }
                        )
                    }
                }

                // 5. Transaction Types
                Text("Transaction Types", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TransactionType.entries.forEach { tType ->
                        val isSelected = tType in selectedTypes
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedTypes = if (isSelected) {
                                    if (selectedTypes.size > 1) selectedTypes - tType else selectedTypes
                                } else {
                                    selectedTypes + tType
                                }
                            },
                            shape = CircleShape,
                            label = { Text(tType.displayLabel) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        onExportAndDownload(buildParams())
                        onDismiss()
                    },
                    shape = CircleShape,
                    modifier = Modifier.testTag("export_dialog_download_button")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download")
                }
                Button(
                    onClick = {
                        onExportAndShare(buildParams())
                        onDismiss()
                    },
                    shape = CircleShape,
                    modifier = Modifier.testTag("export_dialog_share_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dedicated Credit Card Total Expenditure View Card & Dialog.
 * Breaks down revolving card spending isolated from asset Net Worth.
 */
@Composable
fun CreditCardExpenditureDialog(
    creditCardAccounts: List<AccountModel>,
    totalExpenditureInBase: Double,
    baseCurrency: String,
    rates: Map<String, ExchangeRateModel>,
    recentCreditCardTransactions: List<TransactionModel>,
    privacyMaskEnabled: Boolean,
    onDismiss: () -> Unit,
    onViewCreditCardRecords: (accountId: Long?) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.US) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = ExpenseCoral
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Credit Card Expenditure", style = MaterialTheme.typography.titleLarge)
                    Text("Isolated from Asset Net Worth", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Hero Spend Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Period Credit Card Spend ($baseCurrency)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (privacyMaskEnabled) "••••••" else CurrencyAndBfCalculator.formatCurrency(
                                totalExpenditureInBase,
                                baseCurrency,
                                rates
                            ),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.testTag("cc_total_spend_text")
                        )
                        Text(
                            text = "Excluded from Total Net Worth to preserve liquid asset equity.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.82f)
                        )
                    }
                }

                if (creditCardAccounts.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No Credit Card accounts configured yet. Go to Accounts to add a card with type 'Credit Card'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    Text("Credit Card Accounts (${creditCardAccounts.size})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    creditCardAccounts.forEach { ccAcc ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ccAcc.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Currency: ${ccAcc.currencyCode} • Closing: ${if (privacyMaskEnabled) "••••••" else CurrencyAndBfCalculator.formatCurrency(ccAcc.periodClosingCarriedForward, ccAcc.currencyCode, rates)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = if (privacyMaskEnabled) "••••••" else "-${CurrencyAndBfCalculator.formatCurrency(ccAcc.periodExpense, ccAcc.currencyCode, rates)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseCoral
                                )
                            }
                        }
                    }

                    if (recentCreditCardTransactions.isNotEmpty()) {
                        Text("Recent Credit Card Expenses", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        recentCreditCardTransactions.take(5).forEach { tx ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tx.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${dateFormat.format(Date(tx.timestamp))} • ${tx.accountName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = if (privacyMaskEnabled) "••••••" else "-${CurrencyAndBfCalculator.formatCurrency(tx.amount, tx.currencyCode, rates)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseCoral
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onViewCreditCardRecords(creditCardAccounts.firstOrNull()?.id)
                    onDismiss()
                },
                shape = CircleShape,
                modifier = Modifier.testTag("view_cc_records_button")
            ) {
                Text("View Credit Card Ledger")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = CircleShape) {
                Text("Close")
            }
        }
    )
}





