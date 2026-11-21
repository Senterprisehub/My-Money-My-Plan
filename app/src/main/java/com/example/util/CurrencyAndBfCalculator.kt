package com.example.util

import com.example.data.local.AccountType
import com.example.data.local.TransactionType
import com.example.domain.AccountModel
import com.example.domain.CategorySpendSlice
import com.example.domain.DateRangePreset
import com.example.domain.ExchangeRateModel
import com.example.domain.MonthlyTrendPoint
import com.example.domain.PeriodReconciliationSummary
import com.example.domain.TransactionModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Deterministic multi-currency conversion & Account Brought Forward (B/F & C/F) ledger engine.
 * Works 100% offline with zero network calls.
 */
object CurrencyAndBfCalculator {

    private val defaultSymbols = mapOf(
        "USD" to "$",
        "EUR" to "€",
        "GBP" to "£",
        "JPY" to "¥",
        "INR" to "₹",
        "CAD" to "CA$",
        "AUD" to "A$",
        "CHF" to "CHF ",
        "CNY" to "¥",
        "SGD" to "S$"
    )

    fun symbolFor(currencyCode: String, rates: Map<String, ExchangeRateModel> = emptyMap()): String {
        return rates[currencyCode]?.symbol ?: defaultSymbols[currencyCode] ?: "$currencyCode "
    }

    /**
     * Converts [amount] from [fromCurrency] to [toCurrency] using the offline [rates] map
     * where each rate specifies `rateToUsd` (units per 1.00 USD).
     */
    fun convert(
        amount: Double,
        fromCurrency: String,
        toCurrency: String,
        rates: Map<String, ExchangeRateModel>
    ): Double {
        if (fromCurrency.equals(toCurrency, ignoreCase = true)) return amount
        val fromRate = rates[fromCurrency]?.rateToUsd ?: defaultRateToUsd(fromCurrency)
        val toRate = rates[toCurrency]?.rateToUsd ?: defaultRateToUsd(toCurrency)
        if (fromRate <= 0.0) return amount
        val amountInUsd = amount / fromRate
        return amountInUsd * toRate
    }

    fun defaultRateToUsd(currencyCode: String): Double = when (currencyCode.uppercase(Locale.US)) {
        "USD" -> 1.0
        "EUR" -> 0.92
        "GBP" -> 0.79
        "JPY" -> 153.50
        "INR" -> 84.10
        "CAD" -> 1.38
        "AUD" -> 1.52
        "CHF" -> 0.88
        "CNY" -> 7.24
        "SGD" -> 1.34
        else -> 1.0
    }

    private val zeroDecimalFormatter = ThreadLocal.withInitial { DecimalFormat("#,##0") }
    private val twoDecimalFormatter = ThreadLocal.withInitial { DecimalFormat("#,##0.00") }
    private val isoDateFormatter = ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    fun formatCurrency(
        amount: Double,
        currencyCode: String,
        rates: Map<String, ExchangeRateModel> = emptyMap(),
        showPlusSign: Boolean = false
    ): String {
        val symbol = symbolFor(currencyCode, rates)
        val isZeroDecimal = currencyCode.equals("JPY", ignoreCase = true)
        val formatter = (if (isZeroDecimal) zeroDecimalFormatter.get() else twoDecimalFormatter.get())
            ?: DecimalFormat(if (isZeroDecimal) "#,##0" else "#,##0.00")
        val absFormatted = formatter.format(abs(amount))
        return when {
            amount < -0.0001 -> "-$symbol$absFormatted"
            amount > 0.0001 && showPlusSign -> "+$symbol$absFormatted"
            else -> "$symbol$absFormatted"
        }
    }

    /**
     * Returns `[startEpochMillis, endEpochMillisExclusive]` for a `"YYYY-MM"` string.
     */
    fun monthBounds(yearMonth: String): Pair<Long, Long> {
        val parts = yearMonth.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val monthZeroBased = (parts.getOrNull(1)?.toIntOrNull() ?: 11) - 1

        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = (startCal.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }
        return startCal.timeInMillis to endCal.timeInMillis
    }

    fun formatIsoDate(epochMillis: Long): String {
        val sdf = isoDateFormatter.get() ?: SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(epochMillis))
    }

    fun parseIsoDateStartMillis(isoDate: String): Long? {
        return runCatching {
            val parts = isoDate.trim().split("-")
            if (parts.size != 3) return null
            val year = parts[0].toInt()
            val monthZero = (parts[1].toInt() - 1).coerceIn(0, 11)
            val day = parts[2].toInt().coerceIn(1, 31)
            Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, monthZero)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }.getOrNull()
    }

    fun parseIsoDateEndExclusiveMillis(isoDate: String): Long? {
        val start = parseIsoDateStartMillis(isoDate) ?: return null
        return start + 24L * 60L * 60L * 1000L
    }

    /**
     * Resolves `[startMillis, endMillisExclusive]` and human-readable display title for the active
     * [preset], [activeYearMonth], and optional [customStartMillis]/[customEndMillis].
     */
    fun resolveRangeBounds(
        preset: DateRangePreset,
        activeYearMonth: String,
        customStartMillis: Long?,
        customEndMillis: Long?,
        nowMillis: Long = System.currentTimeMillis()
    ): Triple<Long, Long, String> {
        val dayMs = 24L * 60L * 60L * 1000L
        return when (preset) {
            DateRangePreset.MONTH -> {
                val (s, e) = monthBounds(activeYearMonth)
                Triple(s, e, formatMonthTitle(activeYearMonth))
            }
            DateRangePreset.LAST_7_DAYS -> {
                val end = nowMillis + dayMs
                val start = nowMillis - 7L * dayMs
                Triple(start, end, "Last 7 Days (${formatIsoDate(start)} – ${formatIsoDate(nowMillis)})")
            }
            DateRangePreset.LAST_30_DAYS -> {
                val end = nowMillis + dayMs
                val start = nowMillis - 30L * dayMs
                Triple(start, end, "Last 30 Days (${formatIsoDate(start)} – ${formatIsoDate(nowMillis)})")
            }
            DateRangePreset.LAST_90_DAYS -> {
                val end = nowMillis + dayMs
                val start = nowMillis - 90L * dayMs
                Triple(start, end, "Last 90 Days (${formatIsoDate(start)} – ${formatIsoDate(nowMillis)})")
            }
            DateRangePreset.ALL_TIME -> {
                Triple(0L, Long.MAX_VALUE / 2, "All-Time Ledger")
            }
            DateRangePreset.CUSTOM_RANGE -> {
                val (defaultStart, defaultEnd) = monthBounds(activeYearMonth)
                val s = customStartMillis ?: defaultStart
                val e = (customEndMillis ?: defaultEnd).coerceAtLeast(s + dayMs)
                val inclusiveEndDisplay = (e - 1000L).coerceAtLeast(s)
                Triple(s, e, "${formatIsoDate(s)} to ${formatIsoDate(inclusiveEndDisplay)}")
            }
        }
    }

    fun formatMonthTitle(yearMonth: String): String {
        val parts = yearMonth.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val month = parts.getOrNull(1)?.toIntOrNull() ?: 11
        val monthNames = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val name = monthNames.getOrElse((month - 1).coerceIn(0, 11)) { "Month" }
        return "$name $year"
    }

    fun shortMonthLabel(yearMonth: String): String {
        val parts = yearMonth.split("-")
        val month = parts.getOrNull(1)?.toIntOrNull() ?: 11
        val shortNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        return shortNames.getOrElse((month - 1).coerceIn(0, 11)) { yearMonth }
    }

    fun stepYearMonth(yearMonth: String, deltaMonths: Int): String {
        val parts = yearMonth.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val monthZeroBased = (parts.getOrNull(1)?.toIntOrNull() ?: 11) - 1
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, deltaMonths)
        }
        val newYear = cal.get(Calendar.YEAR)
        val newMonth = cal.get(Calendar.MONTH) + 1
        return "%04d-%02d".format(Locale.US, newYear, newMonth)
    }

    fun currentYearMonth(nowMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return "%04d-%02d".format(
            Locale.US,
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1
        )
    }

    /**
     * Computes how a single transaction affects an account's native balance.
     */
    fun signedEffectOnAccount(tx: TransactionModel, accountId: Long): Double {
        return when (tx.type) {
            TransactionType.INCOME -> if (tx.accountId == accountId) tx.amount else 0.0
            TransactionType.EXPENSE -> if (tx.accountId == accountId) -tx.amount else 0.0
            TransactionType.BROUGHT_FORWARD -> if (tx.accountId == accountId) tx.amount else 0.0
            TransactionType.TRANSFER -> when {
                tx.accountId == accountId && tx.targetAccountId == accountId -> 0.0
                tx.accountId == accountId -> -tx.amount
                tx.targetAccountId == accountId -> tx.targetAmount ?: tx.amount
                else -> 0.0
            }
        }
    }

    /**
     * Enriches raw accounts with exact Period Opening Brought Forward (B/F), Period Inflows,
     * Period Outflows, Net Transfers, B/F Adjustments, and Period Closing Carried Forward (C/F)
     * for [activeYearMonth].
     */
    fun computeAccountLedgersForMonth(
        rawAccounts: List<AccountModel>,
        allTransactions: List<TransactionModel>,
        activeYearMonth: String,
        baseCurrency: String,
        rates: Map<String, ExchangeRateModel>
    ): List<AccountModel> {
        val (periodStart, periodEnd) = monthBounds(activeYearMonth)
        return computeAccountLedgersForBounds(
            rawAccounts = rawAccounts,
            allTransactions = allTransactions,
            periodStart = periodStart,
            periodEnd = periodEnd,
            baseCurrency = baseCurrency,
            rates = rates
        )
    }

    private class AccountLedgerAccumulator {
        var priorNetDelta: Double = 0.0
        var income: Double = 0.0
        var expense: Double = 0.0
        var netTransfer: Double = 0.0
        var bfAdjust: Double = 0.0
        var allTimeDelta: Double = 0.0
    }

    /**
     * Enriches raw accounts with exact Opening Brought Forward (B/F) prior to [periodStart]
     * and Closing Carried Forward (C/F) at [periodEnd] in a single O(N) pass.
     */
    fun computeAccountLedgersForBounds(
        rawAccounts: List<AccountModel>,
        allTransactions: List<TransactionModel>,
        periodStart: Long,
        periodEnd: Long,
        baseCurrency: String,
        rates: Map<String, ExchangeRateModel>
    ): List<AccountModel> {
        if (rawAccounts.isEmpty()) return emptyList()

        val accumulators = HashMap<Long, AccountLedgerAccumulator>(rawAccounts.size * 2)
        for (acc in rawAccounts) {
            accumulators[acc.id] = AccountLedgerAccumulator()
        }

        for (tx in allTransactions) {
            val isPrior = tx.timestamp < periodStart
            val isInPeriod = tx.timestamp in periodStart until periodEnd

            val sourceAcc = accumulators[tx.accountId]
            if (sourceAcc != null) {
                val sourceSigned = when (tx.type) {
                    TransactionType.INCOME -> tx.amount
                    TransactionType.EXPENSE -> -tx.amount
                    TransactionType.BROUGHT_FORWARD -> tx.amount
                    TransactionType.TRANSFER -> if (tx.targetAccountId == tx.accountId) 0.0 else -tx.amount
                }
                sourceAcc.allTimeDelta += sourceSigned
                if (isPrior) {
                    sourceAcc.priorNetDelta += sourceSigned
                } else if (isInPeriod) {
                    when (tx.type) {
                        TransactionType.INCOME -> sourceAcc.income += tx.amount
                        TransactionType.EXPENSE -> sourceAcc.expense += tx.amount
                        TransactionType.BROUGHT_FORWARD -> sourceAcc.bfAdjust += tx.amount
                        TransactionType.TRANSFER -> sourceAcc.netTransfer -= tx.amount
                    }
                }
            }

            val targetId = tx.targetAccountId
            if (tx.type == TransactionType.TRANSFER && targetId != null) {
                val targetAcc = accumulators[targetId]
                if (targetAcc != null) {
                    val creditAmount = tx.targetAmount ?: tx.amount
                    if (targetId != tx.accountId) {
                        targetAcc.allTimeDelta += creditAmount
                        if (isPrior) {
                            targetAcc.priorNetDelta += creditAmount
                        }
                    }
                    if (isInPeriod) {
                        targetAcc.netTransfer += creditAmount
                    }
                }
            }
        }

        return rawAccounts.map { account ->
            val acc = accumulators[account.id] ?: AccountLedgerAccumulator()
            val priorNetDelta = if (account.carryOverEnabled) acc.priorNetDelta else 0.0
            val openingBf = (if (account.carryOverEnabled) account.initialBroughtForwardBalance else 0.0) + priorNetDelta
            val closingCf = openingBf + acc.income - acc.expense + acc.netTransfer + acc.bfAdjust
            val currentAllTime = account.initialBroughtForwardBalance + acc.allTimeDelta
            val normalizedClosing = convert(
                amount = closingCf,
                fromCurrency = account.currencyCode,
                toCurrency = baseCurrency,
                rates = rates
            )

            account.copy(
                periodOpeningBroughtForward = openingBf,
                periodIncome = acc.income,
                periodExpense = acc.expense,
                periodNetTransfer = acc.netTransfer,
                periodBfAdjustment = acc.bfAdjust,
                periodClosingCarriedForward = closingCf,
                currentAllTimeBalance = currentAllTime,
                normalizedClosingInBase = normalizedClosing
            )
        }
    }

    /**
     * Attaches the chronological running balance to each transaction based on its source account's
     * initial Brought Forward balance and all chronologically earlier transactions.
     */
    fun attachRunningBalances(
        accounts: List<AccountModel>,
        transactions: List<TransactionModel>
    ): List<TransactionModel> {
        val accountMap = accounts.associateBy { it.id }
        val runningByAccount = accounts.associate { it.id to it.initialBroughtForwardBalance }.toMutableMap()

        // Sort oldest first to accumulate running balance accurately
        val chronological = transactions.sortedWith(compareBy<TransactionModel> { it.timestamp }.thenBy { it.id })
        val runningBalanceByTxId = HashMap<Long, Double>(transactions.size)

        for (tx in chronological) {
            val sourcePrev = runningByAccount[tx.accountId] ?: accountMap[tx.accountId]?.initialBroughtForwardBalance ?: 0.0
            val sourceDelta = signedEffectOnAccount(tx, tx.accountId)
            val sourceAfter = sourcePrev + sourceDelta
            runningByAccount[tx.accountId] = sourceAfter
            runningBalanceByTxId[tx.id] = sourceAfter

            val targetId = tx.targetAccountId
            if (tx.type == TransactionType.TRANSFER && targetId != null && targetId != tx.accountId) {
                val targetPrev = runningByAccount[targetId] ?: accountMap[targetId]?.initialBroughtForwardBalance ?: 0.0
                val targetDelta = signedEffectOnAccount(tx, targetId)
                runningByAccount[targetId] = targetPrev + targetDelta
            }
        }

        return transactions.map { tx ->
            tx.copy(runningBalanceAfter = runningBalanceByTxId[tx.id] ?: 0.0)
        }
    }

    /**
     * Computes the consolidated (or single-account filtered) Period Reconciliation Summary
     * showing Opening Brought Forward (B/F), Period Income, Period Expense, and Closing Carried Forward (C/F).
     */
    fun computePeriodReconciliation(
        accountLedgers: List<AccountModel>,
        selectedAccountId: Long?,
        activeYearMonth: String,
        baseCurrency: String,
        includeBroughtForwardInNet: Boolean,
        rates: Map<String, ExchangeRateModel>,
        customTitle: String? = null
    ): PeriodReconciliationSummary {
        val targetAccounts = if (selectedAccountId != null) {
            accountLedgers.filter { it.id == selectedAccountId }
        } else {
            accountLedgers
        }
        val summaryCurrency = if (selectedAccountId != null && targetAccounts.size == 1) {
            targetAccounts.first().currencyCode
        } else {
            baseCurrency
        }

        var openingBf = 0.0
        var income = 0.0
        var expense = 0.0
        var netTransfer = 0.0
        var bfAdjustments = 0.0

        for (acc in targetAccounts) {
            openingBf += convert(acc.periodOpeningBroughtForward, acc.currencyCode, summaryCurrency, rates)
            income += convert(acc.periodIncome, acc.currencyCode, summaryCurrency, rates)
            expense += convert(acc.periodExpense, acc.currencyCode, summaryCurrency, rates)
            netTransfer += convert(acc.periodNetTransfer, acc.currencyCode, summaryCurrency, rates)
            bfAdjustments += convert(acc.periodBfAdjustment, acc.currencyCode, summaryCurrency, rates)
        }

        return PeriodReconciliationSummary(
            yearMonth = activeYearMonth,
            displayMonthTitle = customTitle ?: formatMonthTitle(activeYearMonth),
            currencyCode = summaryCurrency,
            openingBroughtForward = openingBf,
            periodIncome = income,
            periodExpense = expense,
            periodNetTransfer = netTransfer,
            periodBfAdjustments = bfAdjustments,
            includeBroughtForwardInNet = includeBroughtForwardInNet
        )
    }

    fun computeCategoryBreakdown(
        periodTransactions: List<TransactionModel>,
        expenseOnly: Boolean = true
    ): List<CategorySpendSlice> {
        val targetType = if (expenseOnly) TransactionType.EXPENSE else TransactionType.INCOME
        val filtered = periodTransactions.filter { it.type == targetType }
        val grandTotal = filtered.sumOf { abs(it.normalizedAmountInBase) }
        if (grandTotal <= 0.0) return emptyList()

        return filtered
            .groupBy { it.categoryId ?: -1L }
            .map { (catId, txs) ->
                val sample = txs.first()
                val sum = txs.sumOf { abs(it.normalizedAmountInBase) }
                CategorySpendSlice(
                    categoryId = catId,
                    categoryName = sample.categoryName,
                    colorHex = sample.categoryColorHex,
                    iconKey = sample.categoryIconKey,
                    totalInBaseCurrency = sum,
                    sharePercentage = ((sum / grandTotal) * 100.0).toFloat(),
                    transactionCount = txs.size
                )
            }
            .sortedByDescending { it.totalInBaseCurrency }
    }

    fun computeSixMonthTrend(
        rawAccounts: List<AccountModel>,
        allTransactions: List<TransactionModel>,
        endYearMonth: String,
        baseCurrency: String,
        rates: Map<String, ExchangeRateModel>
    ): List<MonthlyTrendPoint> {
        val months = (-5..0).map { offset -> stepYearMonth(endYearMonth, offset) }
        if (rawAccounts.isEmpty()) {
            return months.map { ym ->
                MonthlyTrendPoint(
                    yearMonth = ym,
                    shortLabel = shortMonthLabel(ym),
                    openingBroughtForward = 0.0,
                    incomeInBase = 0.0,
                    expenseInBase = 0.0,
                    closingCarriedForward = 0.0
                )
            }
        }

        val bounds = months.map { monthBounds(it) }
        val firstStart = bounds.first().first
        val lastEnd = bounds.last().second

        val accountCount = rawAccounts.size
        val accountIndexById = HashMap<Long, Int>(accountCount * 2)
        rawAccounts.forEachIndexed { idx, acc -> accountIndexById[acc.id] = idx }

        val priorBeforeWindow = DoubleArray(accountCount)
        val monthIncome = Array(6) { DoubleArray(accountCount) }
        val monthExpense = Array(6) { DoubleArray(accountCount) }
        val monthTransfer = Array(6) { DoubleArray(accountCount) }
        val monthBfAdjust = Array(6) { DoubleArray(accountCount) }

        for (tx in allTransactions) {
            val ts = tx.timestamp
            if (ts >= lastEnd) continue

            val srcIdx = accountIndexById[tx.accountId]
            val tgtIdx = tx.targetAccountId?.let { accountIndexById[it] }
            if (srcIdx == null && tgtIdx == null) continue

            if (ts < firstStart) {
                if (srcIdx != null) {
                    priorBeforeWindow[srcIdx] += when (tx.type) {
                        TransactionType.INCOME -> tx.amount
                        TransactionType.EXPENSE -> -tx.amount
                        TransactionType.BROUGHT_FORWARD -> tx.amount
                        TransactionType.TRANSFER -> if (tx.targetAccountId == tx.accountId) 0.0 else -tx.amount
                    }
                }
                if (tx.type == TransactionType.TRANSFER && tgtIdx != null && tx.targetAccountId != tx.accountId) {
                    priorBeforeWindow[tgtIdx] += (tx.targetAmount ?: tx.amount)
                }
            } else {
                var bucket = -1
                for (m in 0 until 6) {
                    if (ts < bounds[m].second) {
                        bucket = m
                        break
                    }
                }
                if (bucket >= 0) {
                    if (srcIdx != null) {
                        when (tx.type) {
                            TransactionType.INCOME -> monthIncome[bucket][srcIdx] += tx.amount
                            TransactionType.EXPENSE -> monthExpense[bucket][srcIdx] += tx.amount
                            TransactionType.BROUGHT_FORWARD -> monthBfAdjust[bucket][srcIdx] += tx.amount
                            TransactionType.TRANSFER -> monthTransfer[bucket][srcIdx] -= tx.amount
                        }
                    }
                    if (tx.type == TransactionType.TRANSFER && tgtIdx != null) {
                        monthTransfer[bucket][tgtIdx] += (tx.targetAmount ?: tx.amount)
                    }
                }
            }
        }

        val runningByAccount = DoubleArray(accountCount) { idx ->
            val acc = rawAccounts[idx]
            if (acc.carryOverEnabled) acc.initialBroughtForwardBalance + priorBeforeWindow[idx] else 0.0
        }

        return months.mapIndexed { m, ym ->
            var openingBfInBase = 0.0
            var incomeInBase = 0.0
            var expenseInBase = 0.0
            var netTransferInBase = 0.0
            var bfAdjustInBase = 0.0

            for (idx in 0 until accountCount) {
                val acc = rawAccounts[idx]
                val accOpening = if (acc.carryOverEnabled) runningByAccount[idx] else 0.0
                val inc = monthIncome[m][idx]
                val exp = monthExpense[m][idx]
                val trf = monthTransfer[m][idx]
                val bfAdj = monthBfAdjust[m][idx]

                if (acc.carryOverEnabled) {
                    runningByAccount[idx] = accOpening + inc - exp + trf + bfAdj
                }

                openingBfInBase += convert(accOpening, acc.currencyCode, baseCurrency, rates)
                incomeInBase += convert(inc, acc.currencyCode, baseCurrency, rates)
                expenseInBase += convert(exp, acc.currencyCode, baseCurrency, rates)
                netTransferInBase += convert(trf, acc.currencyCode, baseCurrency, rates)
                bfAdjustInBase += convert(bfAdj, acc.currencyCode, baseCurrency, rates)
            }

            val trueClosing = openingBfInBase + (incomeInBase - expenseInBase + netTransferInBase + bfAdjustInBase)
            MonthlyTrendPoint(
                yearMonth = ym,
                shortLabel = shortMonthLabel(ym),
                openingBroughtForward = openingBfInBase,
                incomeInBase = incomeInBase,
                expenseInBase = expenseInBase,
                closingCarriedForward = trueClosing
            )
        }
    }
}
