package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.InteractiveCategoryDonutChart
import com.example.ui.components.MonthSelectorHeader
import com.example.ui.components.PeriodBroughtForwardCard
import com.example.ui.components.SixMonthCashFlowAndBfTrendChart
import com.example.ui.components.colorFromHex
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.FinanceUiState
import com.example.util.CurrencyAndBfCalculator
import com.example.util.ExportedStatementResult
import com.example.util.StatementExporter
import kotlin.math.abs

@Composable
fun AnalyticsScreen(
    uiState: FinanceUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetToToday: () -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    onDismissExportPreview: () -> Unit,
    onDownloadStatement: (ExportedStatementResult) -> Unit = {},
    onOpenCustomExport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showExpenseBreakdown by rememberSaveable { mutableStateOf(true) }

    val activeSlices = if (showExpenseBreakdown) {
        uiState.expenseCategorySlices
    } else {
        uiState.incomeCategorySlices
    }

    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
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

        // Export CSV & PDF Action Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Export Statements (Custom Filters, CSV & PDF)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Export itemized ledger, Brought Forward (B/F) & Closing (C/F) reconciliation table with custom multi-filters, download or share.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
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
                                .testTag("analytics_custom_export_button")
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Custom Export", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onExportPdf,
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("analytics_export_pdf_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onExportCsv,
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("analytics_export_csv_button")
                        ) {
                            Icon(Icons.Default.TableView, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CSV", maxLines = 1)
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

        // Interactive Category Donut Card
        item {
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
                            text = "Category Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = showExpenseBreakdown,
                                onClick = { showExpenseBreakdown = true },
                                shape = CircleShape,
                                label = { Text("Expenses") }
                            )
                            FilterChip(
                                selected = !showExpenseBreakdown,
                                onClick = { showExpenseBreakdown = false },
                                shape = CircleShape,
                                label = { Text("Income") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    InteractiveCategoryDonutChart(
                        slices = activeSlices,
                        baseCurrency = uiState.preferences.baseCurrency,
                        rates = uiState.ratesMap
                    )
                }
            }
        }

        // 6-Month Cash Flow & Cumulative Carry-Over (C/F) Trend Chart
        item {
            SixMonthCashFlowAndBfTrendChart(
                points = uiState.sixMonthTrend,
                baseCurrency = uiState.preferences.baseCurrency,
                rates = uiState.ratesMap
            )
        }

        // Executive Spending Velocity & Top 5 Merchants / Payees Card
        item {
            ExecutiveVelocityAndTopMerchantsCard(uiState = uiState)
        }

        // Multi-Currency Account Contribution Analysis
        if (uiState.accounts.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Account Contribution (${uiState.preferences.baseCurrency})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val totalAbs = uiState.accounts.sumOf { abs(it.normalizedClosingInBase) }.coerceAtLeast(1.0)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            uiState.accounts.forEach { acc ->
                                val share = (abs(acc.normalizedClosingInBase) / totalAbs).toFloat().coerceIn(0f, 1f)
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${acc.name} (${acc.currencyCode})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = CurrencyAndBfCalculator.formatCurrency(
                                                acc.periodClosingCarriedForward,
                                                acc.currencyCode,
                                                uiState.ratesMap
                                            ),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { share },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = colorFromHex(acc.colorHex),
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.latestExportResult != null) {
        val result = uiState.latestExportResult
        AlertDialog(
            onDismissRequest = onDismissExportPreview,
            title = { Text("My Money My Plan (MMMP) — Statement Export Ready") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = result.fileName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = result.previewSummary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            onDownloadStatement(result)
                            onDismissExportPreview()
                        },
                        shape = CircleShape,
                        modifier = Modifier.testTag("analytics_download_export_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download")
                    }
                    Button(
                        onClick = {
                            StatementExporter.shareExportedFile(context, result)
                            onDismissExportPreview()
                        },
                        shape = CircleShape,
                        modifier = Modifier.testTag("analytics_share_export_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissExportPreview) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun ExecutiveVelocityAndTopMerchantsCard(uiState: FinanceUiState) {
    val exec = uiState.executiveSummary
    val baseCurrency = uiState.preferences.baseCurrency
    val savingsColor = if (exec.savingsRatePercent >= 0.0) IncomeGreen else ExpenseCoral

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("executive_analytics_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Executive Velocity & Top Merchants ($baseCurrency)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Net savings rate, daily spend pace, and top 5 payees in ${uiState.periodReconciliation.displayMonthTitle}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Net Savings Rate",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${"%.1f".format(exec.savingsRatePercent)}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = savingsColor
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Daily Avg Spend",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyAndBfCalculator.formatCurrency(
                                exec.dailyAverageExpenseInBase,
                                baseCurrency,
                                uiState.ratesMap
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseCoral
                        )
                    }
                }
            }

            if (!exec.largestExpenseTitle.isNullOrBlank() && exec.largestExpenseAmountInBase > 0.0) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
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
                                text = "Largest Single Expense",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = exec.largestExpenseTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = CurrencyAndBfCalculator.formatCurrency(
                                exec.largestExpenseAmountInBase,
                                baseCurrency,
                                uiState.ratesMap
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseCoral
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Text(
                text = "Top 5 Merchants / Payees",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (exec.topMerchants.isEmpty()) {
                Text(
                    text = "No expense merchants recorded in ${uiState.periodReconciliation.displayMonthTitle}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    exec.topMerchants.forEachIndexed { idx, slice ->
                        val ratio = (slice.sharePercentage / 100f).coerceIn(0f, 1f)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${idx + 1}. ${slice.merchantName} (${slice.transactionCount}x)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${
                                        CurrencyAndBfCalculator.formatCurrency(
                                            slice.totalInBaseCurrency,
                                            baseCurrency,
                                            uiState.ratesMap
                                        )
                                    } (${"%.0f".format(slice.sharePercentage)}%)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = colorFromHex(slice.colorHex),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

