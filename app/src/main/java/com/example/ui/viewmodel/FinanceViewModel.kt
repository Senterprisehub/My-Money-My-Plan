package com.example.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AccountType
import com.example.data.local.BudgetEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.repository.FinanceRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.data.security.CryptoManager
import com.example.domain.AccountModel
import com.example.domain.AutoTrackMode
import com.example.domain.AutoTrackSource
import com.example.domain.BudgetStatusModel
import com.example.domain.CategoryModel
import com.example.domain.CategorySpendSlice
import com.example.domain.CustomExportFilterParams
import com.example.domain.DashboardWidgetType
import com.example.domain.DateRangePreset
import com.example.domain.ExchangeRateModel
import com.example.domain.ExecutiveAnalyticsSummary
import com.example.domain.MaterialYouPalette
import com.example.domain.MerchantSpendSlice
import com.example.domain.MonthlyTrendPoint
import com.example.domain.ParsedFinancialAlert
import com.example.domain.PeriodReconciliationSummary
import com.example.domain.RecordSortOrder
import com.example.domain.ThemeMode
import com.example.domain.TransactionModel
import com.example.domain.UserPreferencesState
import com.example.util.AutoTrackedAlertManager
import com.example.util.BankAndUpiAlertParser
import com.example.util.CurrencyAndBfCalculator
import com.example.util.ExportedStatementResult
import com.example.util.NotificationHelper
import com.example.util.StatementExporter
import com.example.util.VaultBackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

@Immutable
data class FilterState(
    val activeYearMonth: String = CurrencyAndBfCalculator.currentYearMonth(),
    val dateRangePreset: DateRangePreset = DateRangePreset.MONTH,
    val customStartMillis: Long? = null,
    val customEndMillis: Long? = null,
    val selectedAccountId: Long? = null,
    val selectedType: TransactionType? = null,
    val selectedCategoryId: Long? = null,
    val sortOrder: RecordSortOrder = RecordSortOrder.NEWEST_FIRST,
    val searchQuery: String = ""
)

@Immutable
data class FinanceUiState(
    val isLoading: Boolean = true,
    val isVaultLocked: Boolean = false,
    val pinError: String? = null,
    val statusBannerMessage: String? = null,
    val latestExportResult: ExportedStatementResult? = null,
    val preferences: UserPreferencesState = UserPreferencesState(),
    val filter: FilterState = FilterState(),
    val accounts: List<AccountModel> = emptyList(),
    val categories: List<CategoryModel> = emptyList(),
    val periodTransactions: List<TransactionModel> = emptyList(),
    val filteredTransactions: List<TransactionModel> = emptyList(),
    val allTransactions: List<TransactionModel> = emptyList(),
    val budgets: List<BudgetStatusModel> = emptyList(),
    val exchangeRates: List<ExchangeRateModel> = emptyList(),
    val ratesMap: Map<String, ExchangeRateModel> = emptyMap(),
    val periodReconciliation: PeriodReconciliationSummary = PeriodReconciliationSummary(
        yearMonth = CurrencyAndBfCalculator.currentYearMonth(),
        displayMonthTitle = CurrencyAndBfCalculator.formatMonthTitle(CurrencyAndBfCalculator.currentYearMonth()),
        currencyCode = "USD",
        openingBroughtForward = 0.0,
        periodIncome = 0.0,
        periodExpense = 0.0,
        periodNetTransfer = 0.0,
        periodBfAdjustments = 0.0,
        includeBroughtForwardInNet = true
    ),
    val totalNetWorthInBase: Double = 0.0,
    val totalCreditCardExpenseInBase: Double = 0.0,
    val creditCardAccounts: List<AccountModel> = emptyList(),
    val expenseCategorySlices: List<CategorySpendSlice> = emptyList(),
    val incomeCategorySlices: List<CategorySpendSlice> = emptyList(),
    val sixMonthTrend: List<MonthlyTrendPoint> = emptyList(),
    val executiveSummary: ExecutiveAnalyticsSummary = ExecutiveAnalyticsSummary(),
    val pendingAutoAlerts: List<ParsedFinancialAlert> = emptyList()
)

private data class RawDataBundle(
    val accounts: List<AccountModel>,
    val categories: List<CategoryModel>,
    val txEntities: List<TransactionEntity>,
    val budgetEntities: List<BudgetEntity>,
    val rates: List<ExchangeRateModel>
)

private data class UiControlState(
    val filter: FilterState = FilterState(),
    val sessionUnlocked: Boolean = false,
    val manualLockRequested: Boolean = false,
    val pinError: String? = null,
    val statusBannerMessage: String? = null,
    val latestExportResult: ExportedStatementResult? = null
)

class FinanceViewModel(
    private val appContext: Context,
    private val repository: FinanceRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val controlState = MutableStateFlow(UiControlState())

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            NotificationHelper.ensureChannels(appContext)
        }
        viewModelScope.launch {
            AutoTrackedAlertManager.lastAutoSavedMessageFlow.collect { msg ->
                if (!msg.isNullOrBlank()) {
                    postStatusMessage(msg)
                    AutoTrackedAlertManager.consumeLastAutoSavedMessage()
                }
            }
        }
    }

    private data class DecryptedLedgerSnapshot(
        val rawAccounts: List<AccountModel>,
        val categories: List<CategoryModel>,
        val categoryMap: Map<Long, CategoryModel>,
        val budgetEntities: List<BudgetEntity>,
        val rates: List<ExchangeRateModel>,
        val ratesMap: Map<String, ExchangeRateModel>,
        val allTransactions: List<TransactionModel>
    )

    private data class PeriodCalcKey(
        val baseCurrency: String,
        val includeBroughtForwardInNet: Boolean,
        val activeYearMonth: String,
        val dateRangePreset: DateRangePreset,
        val customStartMillis: Long?,
        val customEndMillis: Long?,
        val selectedAccountId: Long?
    )

    private data class PeriodAnalyticsSnapshot(
        val accounts: List<AccountModel>,
        val categories: List<CategoryModel>,
        val periodTransactions: List<TransactionModel>,
        val allTransactions: List<TransactionModel>,
        val budgets: List<BudgetStatusModel>,
        val exchangeRates: List<ExchangeRateModel>,
        val ratesMap: Map<String, ExchangeRateModel>,
        val periodReconciliation: PeriodReconciliationSummary,
        val totalNetWorthInBase: Double,
        val totalCreditCardExpenseInBase: Double,
        val creditCardAccounts: List<AccountModel>,
        val expenseCategorySlices: List<CategorySpendSlice>,
        val incomeCategorySlices: List<CategorySpendSlice>,
        val sixMonthTrend: List<MonthlyTrendPoint>,
        val executiveSummary: ExecutiveAnalyticsSummary
    )

    private data class RecordsQueryKey(
        val selectedAccountId: Long?,
        val selectedType: TransactionType?,
        val selectedCategoryId: Long?,
        val sortOrder: RecordSortOrder,
        val searchQuery: String
    )

    private val rawDataFlow = combine(
        repository.accountsFlow,
        repository.categoriesFlow,
        repository.transactionsFlow,
        repository.budgetsFlow,
        repository.exchangeRatesFlow
    ) { accounts, categories, txEntities, budgetEntities, rates ->
        RawDataBundle(
            accounts = accounts,
            categories = categories,
            txEntities = txEntities,
            budgetEntities = budgetEntities,
            rates = rates
        )
    }

    private val baseCurrencyFlow = preferencesRepository.preferencesFlow
        .map { it.baseCurrency }
        .distinctUntilChanged()

    // Stage 1: Decrypt transactions once per DB emission (with entity-keyed plaintext cache) & attach running balances off main thread
    private val decryptedLedgerFlow = combine(
        rawDataFlow,
        baseCurrencyFlow
    ) { raw, baseCurrency ->
        val ratesMap = raw.rates.associateBy { it.currencyCode }
        val rawAccountMap = raw.accounts.associateBy { it.id }
        val categoryMap = raw.categories.associateBy { it.id }

        val decryptedTxs = raw.txEntities.map { entity ->
            repository.decryptTransaction(
                entity = entity,
                accountMap = rawAccountMap,
                categoryMap = categoryMap,
                baseCurrency = baseCurrency,
                rates = ratesMap
            )
        }
        val txsWithRunningBalance = CurrencyAndBfCalculator.attachRunningBalances(
            accounts = raw.accounts,
            transactions = decryptedTxs
        )

        DecryptedLedgerSnapshot(
            rawAccounts = raw.accounts,
            categories = raw.categories,
            categoryMap = categoryMap,
            budgetEntities = raw.budgetEntities,
            rates = raw.rates,
            ratesMap = ratesMap,
            allTransactions = txsWithRunningBalance
        )
    }.flowOn(Dispatchers.Default)

    private val periodCalcKeyFlow = combine(
        preferencesRepository.preferencesFlow
            .map { it.baseCurrency to it.includeBroughtForwardInNet }
            .distinctUntilChanged(),
        controlState
            .map { it.filter }
            .map { f ->
                PeriodCalcKey(
                    baseCurrency = "",
                    includeBroughtForwardInNet = true,
                    activeYearMonth = f.activeYearMonth,
                    dateRangePreset = f.dateRangePreset,
                    customStartMillis = f.customStartMillis,
                    customEndMillis = f.customEndMillis,
                    selectedAccountId = f.selectedAccountId
                )
            }
            .distinctUntilChanged()
    ) { (baseCurrency, includeBf), filterPart ->
        filterPart.copy(
            baseCurrency = baseCurrency,
            includeBroughtForwardInNet = includeBf
        )
    }.distinctUntilChanged()

    // Stage 2: Single-pass O(N) B/F ledgers, budgets, category slices, executive summary & 6-month trend off main thread
    private val periodAnalyticsFlow = combine(
        decryptedLedgerFlow,
        periodCalcKeyFlow
    ) { ledger, key ->
        val (rangeStart, rangeEnd, rangeDisplayTitle) = CurrencyAndBfCalculator.resolveRangeBounds(
            preset = key.dateRangePreset,
            activeYearMonth = key.activeYearMonth,
            customStartMillis = key.customStartMillis,
            customEndMillis = key.customEndMillis
        )

        val enrichedAccounts = CurrencyAndBfCalculator.computeAccountLedgersForBounds(
            rawAccounts = ledger.rawAccounts,
            allTransactions = ledger.allTransactions,
            periodStart = rangeStart,
            periodEnd = rangeEnd,
            baseCurrency = key.baseCurrency,
            rates = ledger.ratesMap
        )

        val periodTxs = ledger.allTransactions.filter { it.timestamp in rangeStart until rangeEnd }

        val periodReconciliation = CurrencyAndBfCalculator.computePeriodReconciliation(
            accountLedgers = enrichedAccounts,
            selectedAccountId = key.selectedAccountId,
            activeYearMonth = key.activeYearMonth,
            baseCurrency = key.baseCurrency,
            includeBroughtForwardInNet = key.includeBroughtForwardInNet,
            rates = ledger.ratesMap,
            customTitle = rangeDisplayTitle
        )

        val totalNetWorthInBase = enrichedAccounts
            .asSequence()
            .filter { it.accountType != AccountType.CREDIT_CARD }
            .sumOf { acc ->
                CurrencyAndBfCalculator.convert(
                    amount = acc.currentAllTimeBalance,
                    fromCurrency = acc.currencyCode,
                    toCurrency = key.baseCurrency,
                    rates = ledger.ratesMap
                )
            }

        val creditCardAccounts = enrichedAccounts.filter { it.accountType == AccountType.CREDIT_CARD }
        val creditCardAccountIds = creditCardAccounts.mapTo(HashSet(creditCardAccounts.size * 2)) { it.id }
        val totalCreditCardExpenseInBase = periodTxs
            .asSequence()
            .filter { it.accountId in creditCardAccountIds && it.type == TransactionType.EXPENSE }
            .sumOf { abs(it.normalizedAmountInBase) }

        val budgets = repository.decryptBudgetsForMonth(
            allBudgets = ledger.budgetEntities,
            activeYearMonth = key.activeYearMonth,
            periodTransactions = periodTxs,
            categoryMap = ledger.categoryMap,
            rates = ledger.ratesMap
        )

        val expenseSlices = CurrencyAndBfCalculator.computeCategoryBreakdown(
            periodTransactions = periodTxs,
            expenseOnly = true
        )
        val incomeSlices = CurrencyAndBfCalculator.computeCategoryBreakdown(
            periodTransactions = periodTxs,
            expenseOnly = false
        )
        val sixMonthTrend = CurrencyAndBfCalculator.computeSixMonthTrend(
            rawAccounts = ledger.rawAccounts,
            allTransactions = ledger.allTransactions,
            endYearMonth = key.activeYearMonth,
            baseCurrency = key.baseCurrency,
            rates = ledger.ratesMap
        )

        val expenseTxs = periodTxs.filter { it.type == TransactionType.EXPENSE }
        val totalPeriodExpenseInBase = periodReconciliation.periodExpense.coerceAtLeast(0.0)
        val savingsRate = if (periodReconciliation.periodIncome > 0.0) {
            ((periodReconciliation.periodIncome - periodReconciliation.periodExpense) / periodReconciliation.periodIncome) * 100.0
        } else {
            0.0
        }
        val daysInRange = ((rangeEnd - rangeStart) / (24L * 60L * 60L * 1000L)).toInt().coerceIn(1, 365)
        val dailyAvgExpense = totalPeriodExpenseInBase / daysInRange
        val largestExpenseTx = expenseTxs.maxByOrNull { abs(it.normalizedAmountInBase) }

        val topMerchants = expenseTxs
            .groupBy { tx -> tx.title.trim().ifBlank { tx.categoryName } }
            .map { (merchantName, txs) ->
                val sumBase = txs.sumOf { abs(it.normalizedAmountInBase) }
                val firstTx = txs.first()
                val share = if (totalPeriodExpenseInBase > 0.0) {
                    ((sumBase / totalPeriodExpenseInBase) * 100.0).toFloat().coerceIn(0f, 100f)
                } else {
                    0f
                }
                MerchantSpendSlice(
                    merchantName = merchantName,
                    categoryName = firstTx.categoryName,
                    colorHex = firstTx.categoryColorHex,
                    totalInBaseCurrency = sumBase,
                    sharePercentage = share,
                    transactionCount = txs.size
                )
            }
            .sortedByDescending { it.totalInBaseCurrency }
            .take(5)

        val executiveSummary = ExecutiveAnalyticsSummary(
            savingsRatePercent = savingsRate,
            dailyAverageExpenseInBase = dailyAvgExpense,
            largestExpenseTitle = largestExpenseTx?.title,
            largestExpenseAmountInBase = largestExpenseTx?.let { abs(it.normalizedAmountInBase) } ?: 0.0,
            topMerchants = topMerchants
        )

        PeriodAnalyticsSnapshot(
            accounts = enrichedAccounts,
            categories = ledger.categories,
            periodTransactions = periodTxs,
            allTransactions = ledger.allTransactions,
            budgets = budgets,
            exchangeRates = ledger.rates,
            ratesMap = ledger.ratesMap,
            periodReconciliation = periodReconciliation,
            totalNetWorthInBase = totalNetWorthInBase,
            totalCreditCardExpenseInBase = totalCreditCardExpenseInBase,
            creditCardAccounts = creditCardAccounts,
            expenseCategorySlices = expenseSlices,
            incomeCategorySlices = incomeSlices,
            sixMonthTrend = sixMonthTrend,
            executiveSummary = executiveSummary
        )
    }.flowOn(Dispatchers.Default)

    private val recordsQueryKeyFlow = controlState
        .map { ctrl ->
            val f = ctrl.filter
            RecordsQueryKey(
                selectedAccountId = f.selectedAccountId,
                selectedType = f.selectedType,
                selectedCategoryId = f.selectedCategoryId,
                sortOrder = f.sortOrder,
                searchQuery = f.searchQuery
            )
        }
        .distinctUntilChanged()

    // Stage 3: Fast search/filter/sort projection over already-decrypted periodTransactions
    private val filteredTransactionsFlow = combine(
        periodAnalyticsFlow.map { it.periodTransactions },
        recordsQueryKeyFlow
    ) { periodTxs, query ->
        val q = query.searchQuery.trim()
        val filtered = periodTxs.filter { tx ->
            val matchesAccount = query.selectedAccountId == null ||
                tx.accountId == query.selectedAccountId ||
                tx.targetAccountId == query.selectedAccountId
            val matchesType = query.selectedType == null || tx.type == query.selectedType
            val matchesCategory = query.selectedCategoryId == null ||
                tx.categoryId == query.selectedCategoryId
            val matchesQuery = q.isEmpty() ||
                tx.title.contains(q, ignoreCase = true) ||
                tx.categoryName.contains(q, ignoreCase = true) ||
                tx.accountName.contains(q, ignoreCase = true) ||
                tx.note.contains(q, ignoreCase = true)
            matchesAccount && matchesType && matchesCategory && matchesQuery
        }
        when (query.sortOrder) {
            RecordSortOrder.NEWEST_FIRST -> filtered.sortedByDescending { it.timestamp }
            RecordSortOrder.OLDEST_FIRST -> filtered.sortedBy { it.timestamp }
            RecordSortOrder.HIGHEST_AMOUNT -> filtered.sortedByDescending { abs(it.normalizedAmountInBase) }
            RecordSortOrder.LOWEST_AMOUNT -> filtered.sortedBy { abs(it.normalizedAmountInBase) }
        }
    }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<FinanceUiState> = combine(
        periodAnalyticsFlow,
        filteredTransactionsFlow,
        preferencesRepository.preferencesFlow,
        controlState,
        AutoTrackedAlertManager.pendingAlertsFlow
    ) { snapshot, filteredTxs, prefs, ctrl, pendingAlerts ->
        val isLocked = ctrl.manualLockRequested || (prefs.appLockEnabled && !ctrl.sessionUnlocked)

        FinanceUiState(
            isLoading = false,
            isVaultLocked = isLocked,
            pinError = ctrl.pinError,
            statusBannerMessage = ctrl.statusBannerMessage,
            latestExportResult = ctrl.latestExportResult,
            preferences = prefs,
            filter = ctrl.filter,
            accounts = snapshot.accounts,
            categories = snapshot.categories,
            periodTransactions = snapshot.periodTransactions,
            filteredTransactions = filteredTxs,
            allTransactions = snapshot.allTransactions,
            budgets = snapshot.budgets,
            exchangeRates = snapshot.exchangeRates,
            ratesMap = snapshot.ratesMap,
            periodReconciliation = snapshot.periodReconciliation,
            totalNetWorthInBase = snapshot.totalNetWorthInBase,
            totalCreditCardExpenseInBase = snapshot.totalCreditCardExpenseInBase,
            creditCardAccounts = snapshot.creditCardAccounts,
            expenseCategorySlices = snapshot.expenseCategorySlices,
            incomeCategorySlices = snapshot.incomeCategorySlices,
            sixMonthTrend = snapshot.sixMonthTrend,
            executiveSummary = snapshot.executiveSummary,
            pendingAutoAlerts = pendingAlerts
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinanceUiState()
    )

    // Period & Filter Navigation
    fun previousMonth() {
        controlState.update { state ->
            state.copy(
                filter = state.filter.copy(
                    dateRangePreset = DateRangePreset.MONTH,
                    activeYearMonth = CurrencyAndBfCalculator.stepYearMonth(state.filter.activeYearMonth, -1)
                )
            )
        }
    }

    fun nextMonth() {
        controlState.update { state ->
            state.copy(
                filter = state.filter.copy(
                    dateRangePreset = DateRangePreset.MONTH,
                    activeYearMonth = CurrencyAndBfCalculator.stepYearMonth(state.filter.activeYearMonth, 1)
                )
            )
        }
    }

    fun resetToCurrentMonth() {
        controlState.update { state ->
            state.copy(
                filter = state.filter.copy(
                    dateRangePreset = DateRangePreset.MONTH,
                    activeYearMonth = CurrencyAndBfCalculator.currentYearMonth()
                )
            )
        }
    }

    fun selectDateRangePreset(preset: DateRangePreset) {
        controlState.update { state ->
            state.copy(filter = state.filter.copy(dateRangePreset = preset))
        }
    }

    fun setCustomDateRange(startIsoDate: String, endIsoDate: String) {
        val startMs = CurrencyAndBfCalculator.parseIsoDateStartMillis(startIsoDate)
        val endMs = CurrencyAndBfCalculator.parseIsoDateEndExclusiveMillis(endIsoDate)
        if (startMs == null || endMs == null || endMs <= startMs) {
            postStatusMessage("Invalid date range. Use YYYY-MM-DD with End Date >= Start Date.")
            return
        }
        controlState.update { state ->
            state.copy(
                filter = state.filter.copy(
                    dateRangePreset = DateRangePreset.CUSTOM_RANGE,
                    customStartMillis = startMs,
                    customEndMillis = endMs
                ),
                statusBannerMessage = "Applied custom date filter: $startIsoDate to $endIsoDate"
            )
        }
    }

    fun selectAccountFilter(accountId: Long?) {
        controlState.update { state ->
            state.copy(filter = state.filter.copy(selectedAccountId = accountId))
        }
    }

    fun selectTypeFilter(type: TransactionType?) {
        controlState.update { state ->
            state.copy(filter = state.filter.copy(selectedType = type))
        }
    }

    fun selectCategoryFilter(categoryId: Long?) {
        controlState.update { state ->
            state.copy(filter = state.filter.copy(selectedCategoryId = categoryId))
        }
    }

    fun selectSortOrder(sortOrder: RecordSortOrder) {
        controlState.update { state ->
            state.copy(filter = state.filter.copy(sortOrder = sortOrder))
        }
    }

    fun updateSearchQuery(query: String) {
        controlState.update { state ->
            state.copy(filter = state.filter.copy(searchQuery = query))
        }
    }

    fun clearBannerMessage() {
        controlState.update { it.copy(statusBannerMessage = null) }
    }

    private fun postStatusMessage(msg: String) {
        controlState.update { it.copy(statusBannerMessage = msg) }
    }

    // Security & Biometric / PIN Vault Controls
    fun unlockWithBiometric() {
        controlState.update {
            it.copy(
                sessionUnlocked = true,
                manualLockRequested = false,
                pinError = null
            )
        }
    }

    fun unlockWithPin(pin: String): Boolean {
        val currentPinHash = uiState.value.preferences.pinHash
        val isValid = CryptoManager.verifyPin(pin, currentPinHash) || pin == "1234" && currentPinHash.isEmpty()
        if (isValid) {
            controlState.update {
                it.copy(
                    sessionUnlocked = true,
                    manualLockRequested = false,
                    pinError = null
                )
            }
            return true
        } else {
            controlState.update {
                it.copy(pinError = "Incorrect 4-digit PIN. Default PIN is 1234.")
            }
            return false
        }
    }

    fun lockVaultNow() {
        CryptoManager.clearSessionKeyCache()
        repository.clearDecryptionCache()
        controlState.update {
            it.copy(
                sessionUnlocked = false,
                manualLockRequested = true,
                pinError = null
            )
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAppLockEnabled(enabled)
            if (enabled) {
                controlState.update { it.copy(sessionUnlocked = true) }
                postStatusMessage("Biometric & PIN App Lock enabled (Default PIN: 1234)")
            } else {
                controlState.update { it.copy(manualLockRequested = false) }
                postStatusMessage("App Lock disabled")
            }
        }
    }

    fun updateVaultPin(newPin: String) {
        viewModelScope.launch {
            preferencesRepository.updatePin(newPin)
            postStatusMessage("Encrypted 4-digit Vault PIN updated")
        }
    }

    // Brought Forward & Preferences Controls
    fun setIncludeBroughtForwardInNet(include: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setIncludeBroughtForwardInNet(include)
            val modeText = if (include) {
                "Balance Brought Forward (B/F) included in closing monthly balance"
            } else {
                "Showing isolated monthly net flow (excluding B/F)"
            }
            postStatusMessage(modeText)
        }
    }

    fun togglePrivacyMask() {
        viewModelScope.launch {
            val next = !uiState.value.preferences.privacyMaskEnabled
            preferencesRepository.setPrivacyMaskEnabled(next)
            val msg = if (next) "Executive Privacy Mask Enabled (Balances Hidden)" else "Privacy Mask Disabled (Balances Visible)"
            postStatusMessage(msg)
        }
    }

    fun toggleFxChartExpanded() {
        viewModelScope.launch {
            val next = !uiState.value.preferences.isFxChartExpanded
            preferencesRepository.setFxChartExpanded(next)
        }
    }

    fun setFontStyle(style: String) {
        viewModelScope.launch {
            preferencesRepository.setFontStyle(style)
            postStatusMessage("Display font style set to $style")
        }
    }

    fun setBaseCurrency(currencyCode: String) {
        viewModelScope.launch {
            preferencesRepository.setBaseCurrency(currencyCode)
            postStatusMessage("Primary base currency set to $currencyCode")
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDynamicColorEnabled(enabled)
            val msg = if (enabled) {
                "Material You Dynamic Wallpaper Color enabled"
            } else {
                "Using custom Material You tonal seed palette (${uiState.value.preferences.seedPalette.label})"
            }
            postStatusMessage(msg)
        }
    }

    fun setSeedPalette(palette: MaterialYouPalette) {
        viewModelScope.launch {
            preferencesRepository.setSeedPalette(palette)
            if (uiState.value.preferences.dynamicColorEnabled) {
                preferencesRepository.setDynamicColorEnabled(false)
            }
            postStatusMessage("Applied Material You '${palette.label}' tonal seed palette")
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setNotificationsEnabled(enabled)
        }
    }

    fun setTxAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setTxAlertsEnabled(enabled)
        }
    }

    fun setBudgetAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBudgetAlertsEnabled(enabled)
        }
    }

    fun setAutoTrackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutoTrackEnabled(enabled)
            val status = if (enabled) {
                "Enabled Auto-Track for Banking, UPI (Super Money, GPay, PhonePe, etc.) & SMS Alerts"
            } else {
                "Paused Auto-Track for Banking, UPI & SMS Alerts"
            }
            postStatusMessage(status)
        }
    }

    fun setAutoTrackMode(mode: AutoTrackMode) {
        viewModelScope.launch {
            preferencesRepository.setAutoTrackMode(mode)
            postStatusMessage("Auto-Track Recording Mode: ${mode.label}")
        }
    }

    fun setAutoTrackSourceEnabled(source: AutoTrackSource, enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutoTrackSourceEnabled(source, enabled)
            val stateVerb = if (enabled) "Enabled" else "Muted"
            postStatusMessage("$stateVerb live auto-track for ${source.label}")
        }
    }

    fun approvePendingAlert(alert: ParsedFinancialAlert) {
        viewModelScope.launch {
            val saved = AutoTrackedAlertManager.saveParsedAlertToLedger(
                context = appContext,
                alert = alert,
                sendPushNotification = false
            )
            AutoTrackedAlertManager.removePendingAlert(alert.id)
            if (saved) {
                postStatusMessage("Approved & saved ${alert.sourceAppLabel} alert: ${alert.merchantOrPayee}")
            }
        }
    }

    fun dismissPendingAlert(alertId: String) {
        AutoTrackedAlertManager.removePendingAlert(alertId)
        postStatusMessage("Dismissed pending notification alert")
    }

    fun approveAllPendingAlerts() {
        viewModelScope.launch {
            val currentList = AutoTrackedAlertManager.pendingAlertsFlow.value
            val accounts = uiState.value.accounts
            var savedCount = 0
            var heldUnmatchedCount = 0
            for (alert in currentList) {
                val canAutoApprove = accounts.size <= 1 ||
                    BankAndUpiAlertParser.matchAccountStrict(alert, accounts) != null
                if (!canAutoApprove) {
                    heldUnmatchedCount++
                    continue
                }
                val ok = AutoTrackedAlertManager.saveParsedAlertToLedger(
                    context = appContext,
                    alert = alert,
                    sendPushNotification = false
                )
                if (ok) {
                    AutoTrackedAlertManager.removePendingAlert(alert.id)
                    savedCount++
                }
            }
            when {
                savedCount > 0 && heldUnmatchedCount > 0 -> postStatusMessage(
                    "Saved $savedCount matched alert(s); $heldUnmatchedCount alert(s) held to assign A/c"
                )
                savedCount > 0 -> postStatusMessage(
                    "Approved & saved $savedCount A/c-matched alert(s) to ledger"
                )
                heldUnmatchedCount > 0 -> postStatusMessage(
                    "$heldUnmatchedCount alert(s) need A/c selection — tap Assign A/c"
                )
            }
        }
    }

    fun triggerTestNotificationAlert() {
        val state = uiState.value
        val formattedNetWorth = CurrencyAndBfCalculator.formatCurrency(
            state.totalNetWorthInBase,
            state.preferences.baseCurrency,
            state.ratesMap
        )
        val formattedBf = CurrencyAndBfCalculator.formatCurrency(
            state.periodReconciliation.openingBroughtForward,
            state.periodReconciliation.currencyCode,
            state.ratesMap
        )
        NotificationHelper.sendTransactionAlert(
            context = appContext,
            title = "My Money My Plan (MMMP) Real-Time Spending Alert",
            body = "Active Opening B/F: $formattedBf | Total Encrypted Net Worth: $formattedNetWorth"
        )
        postStatusMessage("Dispatched real-time spending alert notification")
    }

    // Dashboard Widget Customization
    fun setWidgetEnabled(widget: DashboardWidgetType, enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setWidgetEnabled(widget, enabled)
        }
    }

    fun moveWidget(widget: DashboardWidgetType, moveUp: Boolean) {
        viewModelScope.launch {
            preferencesRepository.moveWidget(widget, moveUp)
        }
    }

    // Transactions & Brought Forward Adjustments
    fun addOrUpdateTransaction(
        id: Long = 0L,
        accountId: Long,
        targetAccountId: Long? = null,
        categoryId: Long? = null,
        type: TransactionType,
        amount: Double,
        title: String,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (amount == 0.0) return
        viewModelScope.launch {
            val state = uiState.value
            val sourceAccount = state.accounts.find { it.id == accountId } ?: return@launch
            val targetAccount = targetAccountId?.let { tid -> state.accounts.find { it.id == tid } }

            val convertedTargetAmount = if (type == TransactionType.TRANSFER && targetAccount != null) {
                CurrencyAndBfCalculator.convert(
                    amount = amount,
                    fromCurrency = sourceAccount.currencyCode,
                    toCurrency = targetAccount.currencyCode,
                    rates = state.ratesMap
                )
            } else {
                null
            }

            val resolvedCategoryId = when (type) {
                TransactionType.BROUGHT_FORWARD -> {
                    state.categories.find { it.type == TransactionType.BROUGHT_FORWARD }?.id ?: categoryId
                }
                TransactionType.TRANSFER -> null
                else -> categoryId ?: state.categories.firstOrNull { it.type == type }?.id
            }

            repository.saveTransaction(
                id = id,
                accountId = accountId,
                targetAccountId = targetAccountId,
                categoryId = resolvedCategoryId,
                type = type,
                amount = amount,
                targetAmount = convertedTargetAmount,
                currencyCode = sourceAccount.currencyCode,
                title = title,
                note = note,
                timestamp = timestamp
            )

            val formattedAmt = CurrencyAndBfCalculator.formatCurrency(
                amount,
                sourceAccount.currencyCode,
                state.ratesMap
            )
            postStatusMessage("Saved encrypted ${type.displayLabel}: $formattedAmt in ${sourceAccount.name}")

            // Real-time local push notifications
            if (state.preferences.notificationsEnabled && state.preferences.txAlertsEnabled) {
                NotificationHelper.sendTransactionAlert(
                    context = appContext,
                    title = "${type.displayLabel} Recorded: $formattedAmt",
                    body = "${title.ifBlank { type.displayLabel }} on ${sourceAccount.name}"
                )
            }

            // Check if any monthly category budget crossed 80% or 100%
            if (type == TransactionType.EXPENSE && resolvedCategoryId != null &&
                state.preferences.notificationsEnabled && state.preferences.budgetAlertsEnabled
            ) {
                val budget = state.budgets.find { it.categoryId == resolvedCategoryId }
                if (budget != null) {
                    val addInBudgetCurrency = CurrencyAndBfCalculator.convert(
                        amount = amount,
                        fromCurrency = sourceAccount.currencyCode,
                        toCurrency = budget.currencyCode,
                        rates = state.ratesMap
                    )
                    val newSpent = budget.spentAmountInBudgetCurrency + addInBudgetCurrency
                    val newPct = if (budget.limitAmount > 0) ((newSpent / budget.limitAmount) * 100.0).toInt() else 0
                    if ((newPct >= 100 && budget.alertAt100Percent) || (newPct in 80..99 && budget.alertAt80Percent)) {
                        NotificationHelper.sendBudgetThresholdAlert(
                            context = appContext,
                            categoryName = budget.categoryName,
                            percentUsed = newPct,
                            formattedSpent = CurrencyAndBfCalculator.formatCurrency(newSpent, budget.currencyCode, state.ratesMap),
                            formattedLimit = CurrencyAndBfCalculator.formatCurrency(budget.limitAmount, budget.currencyCode, state.ratesMap)
                        )
                    }
                }
            }
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            postStatusMessage("Deleted record from encrypted ledger")
        }
    }

    // Accounts & Account Brought Forward (B/F) Management
    fun saveAccount(
        id: Long = 0L,
        name: String,
        accountType: AccountType,
        currencyCode: String,
        initialBroughtForwardBalance: Double,
        carryOverEnabled: Boolean,
        colorHex: String,
        accountNumber: String = ""
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val iconKey = when (accountType) {
                AccountType.CASH -> "Wallet"
                AccountType.BANK -> "AccountBalance"
                AccountType.SAVINGS -> "Savings"
                AccountType.CREDIT_CARD -> "CreditCard"
                AccountType.INVESTMENT -> "TrendingUp"
            }
            repository.saveAccount(
                id = id,
                name = name,
                accountType = accountType,
                currencyCode = currencyCode,
                initialBroughtForwardBalance = initialBroughtForwardBalance,
                carryOverEnabled = carryOverEnabled,
                colorHex = colorHex,
                iconKey = iconKey,
                accountNumber = accountNumber
            )
            val verb = if (id == 0L) "Created" else "Updated"
            postStatusMessage("$verb account '${name.trim()}' ($currencyCode)")
        }
    }

    fun deleteAccount(accountId: Long, accountName: String) {
        viewModelScope.launch {
            repository.deleteAccount(accountId)
            controlState.update { state ->
                val clearedFilter = if (state.filter.selectedAccountId == accountId) {
                    state.filter.copy(selectedAccountId = null)
                } else {
                    state.filter
                }
                state.copy(
                    filter = clearedFilter,
                    statusBannerMessage = "Deleted account '$accountName' and its associated records"
                )
            }
        }
    }

    fun updateAccountBroughtForwardSettings(
        accountId: Long,
        newOpeningBroughtForward: Double,
        carryOverEnabled: Boolean
    ) {
        viewModelScope.launch {
            repository.updateAccountBroughtForward(
                accountId = accountId,
                newInitialBroughtForward = newOpeningBroughtForward,
                carryOverEnabled = carryOverEnabled
            )
            postStatusMessage("Updated Account Opening Brought Forward (B/F) balance")
        }
    }

    /**
     * Reconciles an account's current Closing Carried Forward (C/F) balance to [desiredClosingBalance]
     * by recording an explicit `BROUGHT_FORWARD` adjustment transaction in the active period.
     */
    fun reconcileAccountBalanceWithBfAdjustment(
        account: AccountModel,
        desiredClosingBalance: Double,
        memo: String
    ) {
        val delta = desiredClosingBalance - account.periodClosingCarriedForward
        if (kotlin.math.abs(delta) < 0.005) {
            postStatusMessage("${account.name} is already reconciled at that balance")
            return
        }
        val (monthStart, monthEnd) = CurrencyAndBfCalculator.monthBounds(uiState.value.filter.activeYearMonth)
        val now = System.currentTimeMillis()
        val effectiveTimestamp = if (now in monthStart until monthEnd) now else (monthEnd - 60_000L)

        addOrUpdateTransaction(
            accountId = account.id,
            type = TransactionType.BROUGHT_FORWARD,
            amount = delta,
            title = "Balance Brought Forward (B/F) Reconciliation",
            note = memo.ifBlank { "Reconciled closing balance to ${account.currencyCode} $desiredClosingBalance" },
            timestamp = effectiveTimestamp
        )
    }

    // Categories & Budgets
    fun addCustomCategory(
        name: String,
        type: TransactionType,
        iconKey: String = "Category",
        colorHex: String = "#1A73E8"
    ) {
        saveOrUpdateCategory(
            id = 0L,
            name = name,
            type = type,
            iconKey = iconKey,
            colorHex = colorHex
        )
    }

    fun saveOrUpdateCategory(
        id: Long = 0L,
        name: String,
        type: TransactionType,
        iconKey: String = "Category",
        colorHex: String = "#1A73E8"
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.saveCategory(
                id = id,
                name = name,
                type = type,
                iconKey = iconKey,
                colorHex = colorHex
            )
            val action = if (id == 0L) "Added" else "Updated"
            postStatusMessage("$action category '${name.trim()}' (${type.displayLabel})")
        }
    }

    fun deleteCategory(categoryId: Long, categoryName: String) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
            postStatusMessage("Deleted category '$categoryName'")
        }
    }

    fun saveMonthlyBudget(
        categoryId: Long,
        limitAmount: Double,
        currencyCode: String
    ) {
        if (limitAmount <= 0.0) return
        viewModelScope.launch {
            val ym = uiState.value.filter.activeYearMonth
            repository.saveBudget(
                categoryId = categoryId,
                yearMonth = ym,
                limitAmount = limitAmount,
                currencyCode = currencyCode
            )
            postStatusMessage("Updated monthly category budget for ${CurrencyAndBfCalculator.formatMonthTitle(ym)}")
        }
    }

    fun deleteBudget(budgetId: Long) {
        viewModelScope.launch {
            repository.deleteBudget(budgetId)
            postStatusMessage("Removed monthly budget cap")
        }
    }

    fun copyPreviousMonthBudgets() {
        viewModelScope.launch {
            val activeYm = uiState.value.filter.activeYearMonth
            val prevYm = CurrencyAndBfCalculator.stepYearMonth(activeYm, -1)
            val copiedCount = repository.copyBudgetsFromMonth(
                sourceYearMonth = prevYm,
                targetYearMonth = activeYm
            )
            if (copiedCount > 0) {
                postStatusMessage(
                    "Carried forward $copiedCount budget(s) from ${CurrencyAndBfCalculator.formatMonthTitle(prevYm)} to ${CurrencyAndBfCalculator.formatMonthTitle(activeYm)}"
                )
            } else {
                postStatusMessage(
                    "No new budgets to copy from ${CurrencyAndBfCalculator.formatMonthTitle(prevYm)} (or already set)"
                )
            }
        }
    }

    // Offline Multi-Currency Exchange Rates
    fun updateCustomExchangeRate(currencyCode: String, newRateToUsd: Double) {
        if (newRateToUsd <= 0.0) return
        viewModelScope.launch {
            repository.updateExchangeRate(currencyCode, newRateToUsd, isCustomOverride = true)
            postStatusMessage("Updated offline FX rate: 1 USD = $newRateToUsd $currencyCode")
        }
    }

    fun resetExchangeRates() {
        viewModelScope.launch {
            repository.resetExchangeRatesToDefault()
            postStatusMessage("Restored default built-in offline exchange rates")
        }
    }

    // Encrypted Vault Backup & Restore
    fun exportEncryptedVaultBackup(shareImmediately: Boolean = true) {
        viewModelScope.launch {
            try {
                val backupResult = withContext(Dispatchers.IO) {
                    VaultBackupManager.exportEncryptedBackup(appContext)
                }
                postStatusMessage("My Money My Plan (MMMP) — ${backupResult.summaryMessage}")
                if (shareImmediately) {
                    VaultBackupManager.shareBackupFile(appContext, backupResult)
                }
            } catch (t: Throwable) {
                postStatusMessage("Backup failed: ${t.message ?: "Unknown error"}")
            }
        }
    }

    fun restoreLatestVaultBackup() {
        viewModelScope.launch {
            try {
                val msg = withContext(Dispatchers.IO) {
                    VaultBackupManager.restoreLatestLocalBackup(appContext)
                }
                postStatusMessage(msg)
            } catch (t: Throwable) {
                postStatusMessage(t.message ?: "No backup snapshot found to restore")
            }
        }
    }

    fun restoreVaultBackupFromJson(rawEnvelopeJson: String) {
        viewModelScope.launch {
            try {
                val msg = withContext(Dispatchers.IO) {
                    VaultBackupManager.restoreFromEncryptedBackupJson(appContext, rawEnvelopeJson)
                }
                postStatusMessage(msg)
            } catch (t: Throwable) {
                postStatusMessage("Restore failed: ${t.message ?: "Invalid encrypted backup file"}")
            }
        }
    }

    // CSV & PDF Statement Exports
    fun exportStatement(asPdf: Boolean, shareImmediately: Boolean = true) {
        viewModelScope.launch {
            val state = uiState.value
            val filterBadges = buildList {
                val accName = state.filter.selectedAccountId?.let { id -> state.accounts.find { it.id == id }?.name }
                if (accName != null) add("Account: $accName")
                val catName = state.filter.selectedCategoryId?.let { id -> state.categories.find { it.id == id }?.name }
                if (catName != null) add("Category: $catName")
                if (state.filter.selectedType != null) add("Type: ${state.filter.selectedType.displayLabel}")
                if (state.filter.searchQuery.isNotBlank()) add("Query: \"${state.filter.searchQuery.trim()}\"")
                add("Sort: ${state.filter.sortOrder.label}")
            }
            val enrichedTitle = if (filterBadges.isNotEmpty()) {
                "${state.periodReconciliation.displayMonthTitle} (${filterBadges.joinToString(" • ")})"
            } else {
                state.periodReconciliation.displayMonthTitle
            }
            val enrichedPeriodSummary = state.periodReconciliation.copy(displayMonthTitle = enrichedTitle)

            val result = withContext(Dispatchers.IO) {
                if (asPdf) {
                    StatementExporter.exportPdfStatement(
                        context = appContext,
                        periodSummary = enrichedPeriodSummary,
                        accountLedgers = state.accounts,
                        transactions = state.filteredTransactions,
                        budgets = state.budgets,
                        rates = state.ratesMap
                    )
                } else {
                    StatementExporter.exportCsvStatement(
                        context = appContext,
                        periodSummary = enrichedPeriodSummary,
                        accountLedgers = state.accounts,
                        transactions = state.filteredTransactions,
                        budgets = state.budgets,
                        rates = state.ratesMap
                    )
                }
            }
            controlState.update {
                it.copy(
                    latestExportResult = result,
                    statusBannerMessage = "Generated ${if (asPdf) "PDF" else "CSV"} Statement: ${result.fileName}"
                )
            }
            if (shareImmediately) {
                StatementExporter.shareExportedFile(appContext, result)
            }
        }
    }

    fun downloadStatement(result: ExportedStatementResult) {
        viewModelScope.launch {
            val destination = StatementExporter.saveToDownloads(appContext, result)
            postStatusMessage(destination)
        }
    }

    fun exportCustomFilteredStatement(params: CustomExportFilterParams, downloadOnly: Boolean) {
        viewModelScope.launch {
            val state = uiState.value
            val targetTxs = state.allTransactions.filter { tx ->
                val matchesAccount = params.selectedAccountIds.isEmpty() || tx.accountId in params.selectedAccountIds || (tx.targetAccountId != null && tx.targetAccountId in params.selectedAccountIds)
                val matchesCategory = params.selectedCategoryIds.isEmpty() || (tx.categoryId != null && tx.categoryId in params.selectedCategoryIds)
                val matchesType = tx.type in params.selectedTypes
                val matchesStart = params.customStartMillis == null || tx.timestamp >= params.customStartMillis
                val matchesEnd = params.customEndMillis == null || tx.timestamp < params.customEndMillis
                matchesAccount && matchesCategory && matchesType && matchesStart && matchesEnd
            }

            val targetAccounts = if (params.selectedAccountIds.isNotEmpty()) {
                state.accounts.filter { it.id in params.selectedAccountIds }
            } else {
                state.accounts
            }

            val filterBadges = buildList {
                if (params.selectedAccountIds.isNotEmpty()) {
                    val names = targetAccounts.map { it.name }.take(3).joinToString(", ") + if (targetAccounts.size > 3) " +${targetAccounts.size - 3}" else ""
                    add("Accounts: $names")
                }
                if (params.selectedCategoryIds.isNotEmpty()) {
                    val catNames = state.categories.filter { it.id in params.selectedCategoryIds }.map { it.name }.take(3).joinToString(", ")
                    add("Categories: $catNames")
                }
                if (params.selectedTypes.size < TransactionType.entries.size) {
                    add("Types: ${params.selectedTypes.joinToString("/") { it.displayLabel }}")
                }
                if (params.customStartMillis != null && params.customEndMillis != null) {
                    add("Range: ${CurrencyAndBfCalculator.formatIsoDate(params.customStartMillis)} to ${CurrencyAndBfCalculator.formatIsoDate(params.customEndMillis - 1)}")
                }
            }

            val customTitle = if (filterBadges.isNotEmpty()) {
                "Custom Audit Report (${filterBadges.joinToString(" • ")})"
            } else {
                "Custom Full Financial Audit Report"
            }

            val enrichedPeriodSummary = state.periodReconciliation.copy(
                displayMonthTitle = customTitle
            )

            val result = withContext(Dispatchers.IO) {
                if (params.formatAsPdf) {
                    StatementExporter.exportPdfStatement(
                        context = appContext,
                        periodSummary = enrichedPeriodSummary,
                        accountLedgers = targetAccounts,
                        transactions = targetTxs,
                        budgets = state.budgets,
                        rates = state.ratesMap
                    )
                } else {
                    StatementExporter.exportCsvStatement(
                        context = appContext,
                        periodSummary = enrichedPeriodSummary,
                        accountLedgers = targetAccounts,
                        transactions = targetTxs,
                        budgets = state.budgets,
                        rates = state.ratesMap
                    )
                }
            }

            controlState.update {
                it.copy(latestExportResult = result)
            }

            if (downloadOnly) {
                val dest = StatementExporter.saveToDownloads(appContext, result)
                postStatusMessage(dest)
            } else {
                StatementExporter.shareExportedFile(appContext, result)
                postStatusMessage("Prepared ${result.fileName} for sharing")
            }
        }
    }

    fun dismissExportPreview() {
        controlState.update { it.copy(latestExportResult = null) }
    }

    companion object {
        fun provideFactory(
            context: Context,
            repository: FinanceRepository,
            preferencesRepository: UserPreferencesRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FinanceViewModel(
                    appContext = context.applicationContext,
                    repository = repository,
                    preferencesRepository = preferencesRepository
                ) as T
            }
        }
    }
}
