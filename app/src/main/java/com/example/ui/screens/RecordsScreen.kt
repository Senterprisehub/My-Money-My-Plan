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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.example.data.local.TransactionType
import com.example.domain.DateRangePreset
import com.example.domain.ParsedFinancialAlert
import com.example.domain.RecordSortOrder
import com.example.domain.TransactionModel
import com.example.ui.components.MonthSelectorHeader
import com.example.ui.components.PendingAutoDetectedAlertsCard
import com.example.ui.components.PeriodBroughtForwardCard
import com.example.ui.components.TransactionRowItem
import com.example.ui.theme.BroughtForwardGold
import com.example.ui.viewmodel.FinanceUiState
import com.example.util.CurrencyAndBfCalculator

@Composable
fun RecordsScreen(
    uiState: FinanceUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetToToday: () -> Unit,
    onSelectDateRangePreset: (DateRangePreset) -> Unit,
    onSetCustomDateRange: (startIsoDate: String, endIsoDate: String) -> Unit,
    onSelectAccountFilter: (Long?) -> Unit,
    onSelectTypeFilter: (TransactionType?) -> Unit,
    onSelectCategoryFilter: (Long?) -> Unit = {},
    onSelectSortOrder: (RecordSortOrder) -> Unit = {},
    onSearchQueryChange: (String) -> Unit,
    onEditTransaction: (TransactionModel) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onApprovePendingAlert: (ParsedFinancialAlert) -> Unit = {},
    onEditPendingAlert: (ParsedFinancialAlert) -> Unit = {},
    onDismissPendingAlert: (String) -> Unit = {},
    onApproveAllPendingAlerts: () -> Unit = {},
    onExportFilteredCsv: () -> Unit,
    onExportFilteredPdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomRangeDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("records_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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

        // Date Range Filter Presets & Custom Start–End Range Bar
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("date_range_filter_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Date Range Filter",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${uiState.filteredTransactions.size} matching record(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DateRangePreset.entries.forEach { preset ->
                            FilterChip(
                                selected = uiState.filter.dateRangePreset == preset,
                                onClick = {
                                    if (preset == DateRangePreset.CUSTOM_RANGE) {
                                        showCustomRangeDialog = true
                                    } else {
                                        onSelectDateRangePreset(preset)
                                    }
                                },
                                shape = CircleShape,
                                label = { Text(preset.label) },
                                modifier = Modifier.testTag("date_preset_chip_${preset.name}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Search & Filtered Range Statement Export Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onExportFilteredCsv,
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("records_export_csv_button")
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Search CSV", maxLines = 1)
                        }
                        Button(
                            onClick = onExportFilteredPdf,
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("records_export_pdf_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Search PDF", maxLines = 1)
                        }
                    }
                }
            }
        }

        item {
            PeriodBroughtForwardCard(
                summary = uiState.periodReconciliation,
                rates = uiState.ratesMap
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.filter.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search records by title, category, or note...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (uiState.filter.searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("records_search_input")
                )

                if (uiState.accounts.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.filter.selectedAccountId == null,
                            onClick = { onSelectAccountFilter(null) },
                            shape = CircleShape,
                            label = { Text("All Accounts") }
                        )
                        uiState.accounts.forEach { acc ->
                            FilterChip(
                                selected = uiState.filter.selectedAccountId == acc.id,
                                onClick = { onSelectAccountFilter(acc.id) },
                                shape = CircleShape,
                                label = { Text("${acc.name} (${acc.currencyCode})") }
                            )
                        }
                    }
                }

                // Compact 3-item row of dropdown options: All Types, All Categories, Sort Order
                var typeDropdownExpanded by remember { mutableStateOf(false) }
                var categoryDropdownExpanded by remember { mutableStateOf(false) }
                var sortDropdownExpanded by remember { mutableStateOf(false) }

                val currentCategory = uiState.categories.find { it.id == uiState.filter.selectedCategoryId }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Type Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { typeDropdownExpanded = true },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("records_type_dropdown")
                        ) {
                            Text(
                                text = uiState.filter.selectedType?.displayLabel ?: "All Types",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = typeDropdownExpanded,
                            onDismissRequest = { typeDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Types") },
                                onClick = {
                                    onSelectTypeFilter(null)
                                    typeDropdownExpanded = false
                                },
                                modifier = Modifier.testTag("type_item_all")
                            )
                            TransactionType.entries.forEach { tType ->
                                DropdownMenuItem(
                                    text = { Text(tType.displayLabel) },
                                    onClick = {
                                        onSelectTypeFilter(tType)
                                        typeDropdownExpanded = false
                                    },
                                    modifier = Modifier.testTag("type_item_${tType.name}")
                                )
                            }
                        }
                    }

                    // 2. Category Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { categoryDropdownExpanded = true },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("records_category_dropdown")
                        ) {
                            Text(
                                text = currentCategory?.name ?: "All Categories",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Categories") },
                                onClick = {
                                    onSelectCategoryFilter(null)
                                    categoryDropdownExpanded = false
                                },
                                modifier = Modifier.testTag("records_category_item_all")
                            )
                            uiState.categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        onSelectCategoryFilter(cat.id)
                                        categoryDropdownExpanded = false
                                    },
                                    modifier = Modifier.testTag("records_category_item_${cat.id}")
                                )
                            }
                        }
                    }

                    // 3. Sort Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { sortDropdownExpanded = true },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("records_sort_dropdown")
                        ) {
                            Text(
                                text = uiState.filter.sortOrder.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = sortDropdownExpanded,
                            onDismissRequest = { sortDropdownExpanded = false }
                        ) {
                            RecordSortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.label) },
                                    onClick = {
                                        onSelectSortOrder(order)
                                        sortDropdownExpanded = false
                                    },
                                    modifier = Modifier.testTag("records_sort_item_${order.name}")
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.filteredTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Text(
                        text = "No transactions found for ${uiState.periodReconciliation.displayMonthTitle}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(
                items = uiState.filteredTransactions,
                key = { it.id },
                contentType = { "transaction_row" }
            ) { tx ->
                TransactionRowItem(
                    transaction = tx,
                    rates = uiState.ratesMap,
                    onEditClick = onEditTransaction,
                    onDeleteClick = onDeleteTransaction
                )
            }
        }

        // Opening Balance Brought Forward (B/F) Baseline Anchor Entry at start of active period
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("opening_bf_ledger_footer"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BroughtForwardGold.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Balance Brought Forward",
                                tint = BroughtForwardGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Opening Balance Brought Forward (B/F)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Carried over into ${uiState.periodReconciliation.displayMonthTitle}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = CurrencyAndBfCalculator.formatCurrency(
                            uiState.periodReconciliation.openingBroughtForward,
                            uiState.periodReconciliation.currencyCode,
                            uiState.ratesMap,
                            showPlusSign = uiState.periodReconciliation.openingBroughtForward > 0
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BroughtForwardGold
                    )
                }
            }
        }
    }

    if (showCustomRangeDialog) {
        val defaultStart = uiState.filter.customStartMillis?.let { CurrencyAndBfCalculator.formatIsoDate(it) }
            ?: "${uiState.filter.activeYearMonth}-01"
        val defaultEnd = uiState.filter.customEndMillis?.let {
            CurrencyAndBfCalculator.formatIsoDate((it - 1000L).coerceAtLeast(0L))
        } ?: CurrencyAndBfCalculator.formatIsoDate(System.currentTimeMillis())

        var startIso by rememberSaveable { mutableStateOf(defaultStart) }
        var endIso by rememberSaveable { mutableStateOf(defaultEnd) }

        AlertDialog(
            onDismissRequest = { showCustomRangeDialog = false },
            title = { Text("Custom Date Range Filter") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter Start and End dates in YYYY-MM-DD format. Opening Brought Forward (B/F) prior to Start Date and Closing C/F at End Date will be calculated automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = startIso,
                        onValueChange = { startIso = it },
                        label = { Text("Start Date (YYYY-MM-DD)") },
                        placeholder = { Text("2026-10-01") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_range_start_input")
                    )
                    OutlinedTextField(
                        value = endIso,
                        onValueChange = { endIso = it },
                        label = { Text("End Date (YYYY-MM-DD)") },
                        placeholder = { Text("2026-11-01") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_range_end_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSetCustomDateRange(startIso.trim(), endIso.trim())
                        showCustomRangeDialog = false
                    },
                    modifier = Modifier.testTag("apply_custom_range_button")
                ) {
                    Text("Apply Range")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCustomRangeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
