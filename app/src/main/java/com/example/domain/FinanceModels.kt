package com.example.domain

import androidx.compose.runtime.Immutable
import com.example.data.local.AccountType
import com.example.data.local.TransactionType

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Material Light"),
    DARK("Material Dark")
}

enum class MaterialYouPalette(
    val label: String,
    val primaryPreviewHex: String,
    val containerPreviewHex: String
) {
    OCEAN_BLUE("Ocean Blue", "#0061A4", "#D1E4FF"),
    EMERALD_MINT("Emerald Mint", "#006C4C", "#89F8C7"),
    ROYAL_VIOLET("Royal Violet", "#6750A4", "#EADDFF"),
    SUNSET_ROSE("Sunset Rose", "#984061", "#FFD9E2"),
    AMBER_GOLD("Amber Gold", "#785900", "#FFDF9E")
}

enum class AutoTrackMode(val label: String, val description: String) {
    AUTO_SAVE(
        "Auto-Save to Ledger",
        "Immediately saves detected banking, UPI & SMS alerts into your matched account"
    ),
    PENDING_REVIEW(
        "Hold in Pending Review",
        "Queues detected alerts on Dashboard & Records for 1-tap approval or editing"
    )
}

@Immutable
data class ParsedFinancialAlert(
    val id: String,
    val sourceAppLabel: String,
    val packageName: String,
    val type: TransactionType,
    val amount: Double,
    val currencyCode: String,
    val merchantOrPayee: String,
    val accountHint: String?,
    val referenceId: String?,
    val suggestedCategoryName: String,
    val rawNotificationText: String,
    val timestamp: Long
)

enum class AutoTrackSource(val label: String) {
    SUPER_MONEY("Super Money"),
    GOOGLE_PAY("Google Pay"),
    PHONEPE("PhonePe"),
    PAYTM("Paytm"),
    BHIM("BHIM UPI"),
    CRED("CRED"),
    AMAZON_PAY("Amazon Pay"),
    WHATSAPP_PAY("WhatsApp Pay"),
    BANKING_APPS("Banking Apps"),
    BANK_SMS("Bank SMS")
}

enum class RecordSortOrder(val label: String) {
    NEWEST_FIRST("Newest First"),
    OLDEST_FIRST("Oldest First"),
    HIGHEST_AMOUNT("Highest Amount"),
    LOWEST_AMOUNT("Lowest Amount")
}

enum class DateRangePreset(val label: String) {
    MONTH("Monthly"),
    LAST_7_DAYS("Last 7D"),
    LAST_30_DAYS("Last 30D"),
    LAST_90_DAYS("Last 90D"),
    ALL_TIME("All Time"),
    CUSTOM_RANGE("Custom Range")
}

enum class DashboardWidgetType(val title: String, val subtitle: String) {
    NET_WORTH_HERO("Net Worth & Multi-Currency Vault", "Consolidated balance normalized to base currency"),
    BROUGHT_FORWARD_PULSE("Period Cash Flow & Brought Forward (B/F)", "Opening B/F, monthly inflows/outflows, and Closing C/F"),
    QUICK_ACTIONS("Quick Record & Reconcile Bar", "1-tap Expense, Income, Transfer, and B/F adjustment"),
    ACCOUNTS_CAROUSEL("Multi-Currency Accounts", "Native balances with Brought Forward carry-over"),
    BUDGET_WATCHLIST("Monthly Budget Watchlist", "Category limits with 80% warning & 100% alert status"),
    RECENT_TRANSACTIONS("Recent Encrypted Records", "Latest ledger entries with running balances")
}

@Immutable
data class AccountModel(
    val id: Long,
    val name: String,
    val accountType: AccountType,
    val currencyCode: String,
    val initialBroughtForwardBalance: Double,
    val broughtForwardDate: Long,
    val carryOverEnabled: Boolean,
    val colorHex: String,
    val iconKey: String,
    val accountNumber: String = "",
    val maskedAccountNumber: String = "",
    // Computed for active period:
    val periodOpeningBroughtForward: Double = 0.0,
    val periodIncome: Double = 0.0,
    val periodExpense: Double = 0.0,
    val periodNetTransfer: Double = 0.0,
    val periodBfAdjustment: Double = 0.0,
    val periodClosingCarriedForward: Double = 0.0,
    val currentAllTimeBalance: Double = 0.0,
    val normalizedClosingInBase: Double = 0.0
)

@Immutable
data class CategoryModel(
    val id: Long,
    val name: String,
    val type: TransactionType,
    val iconKey: String,
    val colorHex: String,
    val isCustom: Boolean
)

@Immutable
data class TransactionModel(
    val id: Long,
    val accountId: Long,
    val accountName: String,
    val targetAccountId: Long?,
    val targetAccountName: String?,
    val categoryId: Long?,
    val categoryName: String,
    val categoryIconKey: String,
    val categoryColorHex: String,
    val type: TransactionType,
    val amount: Double,
    val targetAmount: Double?,
    val currencyCode: String,
    val targetCurrencyCode: String?,
    val normalizedAmountInBase: Double,
    val title: String,
    val note: String,
    val timestamp: Long,
    val runningBalanceAfter: Double = 0.0 // Running balance in the source account after this tx
)

@Immutable
data class BudgetStatusModel(
    val id: Long,
    val categoryId: Long,
    val categoryName: String,
    val categoryIconKey: String,
    val categoryColorHex: String,
    val yearMonth: String,
    val limitAmount: Double,
    val spentAmountInBudgetCurrency: Double,
    val currencyCode: String,
    val alertAt80Percent: Boolean,
    val alertAt100Percent: Boolean
) {
    val progressRatio: Float
        get() = if (limitAmount <= 0.0) 0f else (spentAmountInBudgetCurrency / limitAmount).toFloat()
    val percentageUsed: Int
        get() = (progressRatio * 100f).toInt()
    val remainingAmount: Double
        get() = limitAmount - spentAmountInBudgetCurrency
    val isWarning: Boolean
        get() = percentageUsed in 80..99
    val isExceeded: Boolean
        get() = percentageUsed >= 100
}

@Immutable
data class ExchangeRateModel(
    val currencyCode: String,
    val symbol: String,
    val displayName: String,
    val rateToUsd: Double,
    val isCustomOverride: Boolean,
    val updatedAt: Long
)

/**
 * Represents the consolidated Period Brought Forward (B/F) & Carried Forward (C/F) reconciliation
 * across all active accounts (or a filtered single account).
 */
@Immutable
data class PeriodReconciliationSummary(
    val yearMonth: String, // "YYYY-MM"
    val displayMonthTitle: String, // e.g., "November 2026"
    val currencyCode: String,
    val openingBroughtForward: Double,
    val periodIncome: Double,
    val periodExpense: Double,
    val periodNetTransfer: Double,
    val periodBfAdjustments: Double,
    val includeBroughtForwardInNet: Boolean
) {
    val monthlyOperatingNet: Double
        get() = periodIncome - periodExpense + periodNetTransfer + periodBfAdjustments

    val closingCarriedForward: Double
        get() = (if (includeBroughtForwardInNet) openingBroughtForward else 0.0) + monthlyOperatingNet

    val trueClosingCarriedForward: Double
        get() = openingBroughtForward + monthlyOperatingNet
}

@Immutable
data class CategorySpendSlice(
    val categoryId: Long,
    val categoryName: String,
    val colorHex: String,
    val iconKey: String,
    val totalInBaseCurrency: Double,
    val sharePercentage: Float,
    val transactionCount: Int
)

@Immutable
data class MonthlyTrendPoint(
    val yearMonth: String,
    val shortLabel: String, // e.g. "Jun", "Jul"
    val openingBroughtForward: Double,
    val incomeInBase: Double,
    val expenseInBase: Double,
    val closingCarriedForward: Double
)

@Immutable
data class MerchantSpendSlice(
    val merchantName: String,
    val categoryName: String,
    val colorHex: String,
    val totalInBaseCurrency: Double,
    val sharePercentage: Float,
    val transactionCount: Int
)

@Immutable
data class ExecutiveAnalyticsSummary(
    val savingsRatePercent: Double = 0.0,
    val dailyAverageExpenseInBase: Double = 0.0,
    val largestExpenseTitle: String? = null,
    val largestExpenseAmountInBase: Double = 0.0,
    val topMerchants: List<MerchantSpendSlice> = emptyList()
)

@Immutable
data class UserPreferencesState(
    val baseCurrency: String = "USD",
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val dynamicColorEnabled: Boolean = true,
    val seedPalette: MaterialYouPalette = MaterialYouPalette.OCEAN_BLUE,
    val includeBroughtForwardInNet: Boolean = true,
    val appLockEnabled: Boolean = false,
    val pinHash: String = "",
    val notificationsEnabled: Boolean = true,
    val txAlertsEnabled: Boolean = true,
    val budgetAlertsEnabled: Boolean = true,
    val autoTrackEnabled: Boolean = true,
    val autoTrackMode: AutoTrackMode = AutoTrackMode.AUTO_SAVE,
    val enabledAutoTrackSources: Set<AutoTrackSource> = AutoTrackSource.entries.toSet(),
    val privacyMaskEnabled: Boolean = false,
    val isFxChartExpanded: Boolean = false,
    val fontStyle: String = "Default",
    val orderedWidgets: List<DashboardWidgetType> = DashboardWidgetType.entries,
    val enabledWidgets: Set<DashboardWidgetType> = DashboardWidgetType.entries.toSet()
)

@Immutable
data class CustomExportFilterParams(
    val selectedAccountIds: Set<Long> = emptySet(),
    val selectedCategoryIds: Set<Long> = emptySet(),
    val selectedTypes: Set<TransactionType> = TransactionType.entries.toSet(),
    val customStartMillis: Long? = null,
    val customEndMillis: Long? = null,
    val formatAsPdf: Boolean = true
)

