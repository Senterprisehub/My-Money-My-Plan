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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.TransactionType
import com.example.domain.BudgetStatusModel
import com.example.domain.CategoryModel
import com.example.ui.components.CategoryManagementSectionCard
import com.example.ui.components.CreateCategoryDialog
import com.example.ui.components.MonthSelectorHeader
import com.example.ui.components.colorFromHex
import com.example.ui.components.iconForKey
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.FinanceUiState
import com.example.util.CurrencyAndBfCalculator

@Composable
fun BudgetsScreen(
    uiState: FinanceUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetToToday: () -> Unit,
    onSaveBudget: (categoryId: Long, limitAmount: Double, currencyCode: String) -> Unit,
    onDeleteBudget: (budgetId: Long) -> Unit,
    onCopyPreviousMonthBudgets: () -> Unit = {},
    onAddCustomCategory: (name: String, type: TransactionType, iconKey: String, colorHex: String) -> Unit,
    onUpdateCategory: (id: Long, name: String, type: TransactionType, iconKey: String, colorHex: String) -> Unit = { _, n, t, i, c ->
        onAddCustomCategory(n, t, i, c)
    },
    onDeleteCategory: (categoryId: Long, categoryName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetStatusModel?>(null) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryModel?>(null) }

    val totalLimitInBase = remember(uiState.budgets, uiState.preferences.baseCurrency, uiState.ratesMap) {
        uiState.budgets.sumOf {
            CurrencyAndBfCalculator.convert(it.limitAmount, it.currencyCode, uiState.preferences.baseCurrency, uiState.ratesMap)
        }
    }
    val totalSpentInBase = remember(uiState.budgets, uiState.preferences.baseCurrency, uiState.ratesMap) {
        uiState.budgets.sumOf {
            CurrencyAndBfCalculator.convert(it.spentAmountInBudgetCurrency, it.currencyCode, uiState.preferences.baseCurrency, uiState.ratesMap)
        }
    }
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("budgets_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Non-overlapping Stacked Header + Action Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Text(
                        text = "Monthly Category Budgets",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Instant alerts when spending reaches 80% warning or 100% limit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showNewCategoryDialog = true },
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_category_button")
                    ) {
                        Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Category", maxLines = 1)
                    }
                    Button(
                        onClick = { showAddBudgetDialog = true },
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("set_budget_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Set Budget Cap", maxLines = 1)
                    }
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

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Total Monthly Budget Envelope",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        val overallPct = if (totalLimitInBase > 0) ((totalSpentInBase / totalLimitInBase) * 100.0).toInt() else 0
                        Text(
                            text = "$overallPct% Used",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Spent ${
                            CurrencyAndBfCalculator.formatCurrency(
                                totalSpentInBase,
                                uiState.preferences.baseCurrency,
                                uiState.ratesMap
                            )
                        } of ${
                            CurrencyAndBfCalculator.formatCurrency(
                                totalLimitInBase,
                                uiState.preferences.baseCurrency,
                                uiState.ratesMap
                            )
                        } across ${uiState.budgets.size} budgeted categories",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FilledTonalButton(
                        onClick = onCopyPreviousMonthBudgets,
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("copy_prev_month_budgets_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Carry Forward Budgets from Previous Month", maxLines = 1)
                    }
                }
            }
        }

        if (uiState.budgets.isEmpty()) {
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
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Monthly Budgets Set Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Set a monthly spending limit for categories like Groceries, Food & Dining, or Housing to receive 80% warning and 100% limit alerts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(
                items = uiState.budgets,
                key = { it.id },
                contentType = { "budget_category_card" }
            ) { budget ->
                BudgetCategoryCard(
                    budget = budget,
                    rates = uiState.ratesMap,
                    onEdit = { editingBudget = budget },
                    onDelete = { onDeleteBudget(budget.id) }
                )
            }
        }

        // Category Add, Edit & Delete Manager Section
        item {
            CategoryManagementSectionCard(
                categories = uiState.categories,
                onAddCategoryClick = {
                    editingCategory = null
                    showNewCategoryDialog = true
                },
                onEditCategoryClick = { cat ->
                    editingCategory = cat
                    showNewCategoryDialog = true
                },
                onDeleteCategory = onDeleteCategory
            )
        }
    }

    if (showAddBudgetDialog || editingBudget != null) {
        SetCategoryBudgetDialog(
            initialBudget = editingBudget,
            expenseCategories = uiState.categories.filter { it.type == TransactionType.EXPENSE },
            availableCurrencies = uiState.exchangeRates.map { it.currencyCode }.ifEmpty { listOf("USD", "EUR", "GBP") },
            defaultCurrency = uiState.preferences.baseCurrency,
            onDismiss = {
                showAddBudgetDialog = false
                editingBudget = null
            },
            onSave = { catId, limit, currency ->
                onSaveBudget(catId, limit, currency)
                showAddBudgetDialog = false
                editingBudget = null
            }
        )
    }

    if (showNewCategoryDialog || editingCategory != null) {
        CreateCategoryDialog(
            initialCategory = editingCategory,
            onDismiss = {
                showNewCategoryDialog = false
                editingCategory = null
            },
            onSave = { name, type, iconKey, colorHex ->
                val targetId = editingCategory?.id ?: 0L
                if (targetId > 0L) {
                    onUpdateCategory(targetId, name, type, iconKey, colorHex)
                } else {
                    onAddCustomCategory(name, type, iconKey, colorHex)
                }
                showNewCategoryDialog = false
                editingCategory = null
            }
        )
    }
}

@Composable
private fun BudgetCategoryCard(
    budget: BudgetStatusModel,
    rates: Map<String, com.example.domain.ExchangeRateModel>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when {
        budget.isExceeded -> ExpenseCoral
        budget.isWarning -> WarningAmber
        else -> IncomeGreen
    }
    val statusLabel = when {
        budget.isExceeded -> "EXCEEDED (${budget.percentageUsed}%)"
        budget.isWarning -> "80%+ WARNING (${budget.percentageUsed}%)"
        else -> "ON TRACK (${budget.percentageUsed}%)"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("budget_card_${budget.categoryId}"),
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colorFromHex(budget.categoryColorHex).copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconForKey(budget.categoryIconKey),
                            contentDescription = budget.categoryName,
                            tint = colorFromHex(budget.categoryColorHex),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = budget.categoryName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = statusColor.copy(alpha = 0.14f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_budget_${budget.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit budget limit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_budget_${budget.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete budget",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { budget.progressRatio.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Spent: ${
                        CurrencyAndBfCalculator.formatCurrency(
                            budget.spentAmountInBudgetCurrency,
                            budget.currencyCode,
                            rates
                        )
                    }",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Limit: ${
                        CurrencyAndBfCalculator.formatCurrency(
                            budget.limitAmount,
                            budget.currencyCode,
                            rates
                        )
                    }",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SetCategoryBudgetDialog(
    initialBudget: BudgetStatusModel? = null,
    expenseCategories: List<CategoryModel>,
    availableCurrencies: List<String>,
    defaultCurrency: String,
    onDismiss: () -> Unit,
    onSave: (Long, Double, String) -> Unit
) {
    if (expenseCategories.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("No Expense Categories") },
            text = { Text("Add an expense category first.") },
            confirmButton = { OutlinedButton(onClick = onDismiss) { Text("Close") } }
        )
        return
    }
    var selectedCatId by rememberSaveable {
        mutableStateOf(initialBudget?.categoryId ?: expenseCategories.first().id)
    }
    var limitText by rememberSaveable {
        mutableStateOf(
            if (initialBudget != null) "%.2f".format(java.util.Locale.US, initialBudget.limitAmount) else "400.00"
        )
    }
    var selectedCurrency by rememberSaveable {
        mutableStateOf(initialBudget?.currencyCode ?: defaultCurrency)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialBudget == null) "Set Monthly Budget" else "Edit Budget Limit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select Category", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    expenseCategories.forEach { cat ->
                        FilterChip(
                            selected = selectedCatId == cat.id,
                            onClick = { selectedCatId = cat.id },
                            label = { Text(cat.name) }
                        )
                    }
                }

                Text("Budget Currency", style = MaterialTheme.typography.labelMedium)
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
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Monthly Limit ($selectedCurrency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_limit_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = limitText.trim().toDoubleOrNull() ?: return@Button
                    if (limit > 0) onSave(selectedCatId, limit, selectedCurrency)
                },
                modifier = Modifier.testTag("confirm_save_budget_button")
            ) {
                Text("Save Budget")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun NewCategoryDialog(
    onDismiss: () -> Unit,
    onSave: (String, TransactionType) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Custom Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g., Coffee & Co-working") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE },
                        label = { Text("Expense") }
                    )
                    FilterChip(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME },
                        label = { Text("Income") }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onSave(name, type) }) {
                Text("Create")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
