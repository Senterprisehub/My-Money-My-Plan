package com.example.data.repository

import com.example.data.local.AccountEntity
import com.example.data.local.AccountType
import com.example.data.local.BudgetEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.ExchangeRateEntity
import com.example.data.local.FinanceDao
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.security.CryptoManager
import com.example.domain.AccountModel
import com.example.domain.BudgetStatusModel
import com.example.domain.CategoryModel
import com.example.domain.ExchangeRateModel
import com.example.domain.TransactionModel
import com.example.util.CurrencyAndBfCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

/**
 * Offline encrypted repository mediating between Room SQLite entities, AES-256-GCM field encryption,
 * and clean domain models.
 */
class FinanceRepository(private val dao: FinanceDao) {

    private data class CachedDecryptedTx(
        val encryptedAmount: String,
        val encryptedTargetAmount: String?,
        val encryptedTitle: String,
        val encryptedNote: String,
        val amount: Double,
        val targetAmount: Double?,
        val title: String,
        val note: String
    )

    private data class CachedDecryptedBudget(
        val encryptedLimitAmount: String,
        val limitAmount: Double
    )

    private val decryptedTxCache = ConcurrentHashMap<Long, CachedDecryptedTx>()
    private val decryptedBudgetCache = ConcurrentHashMap<Long, CachedDecryptedBudget>()

    fun clearDecryptionCache() {
        decryptedTxCache.clear()
        decryptedBudgetCache.clear()
    }

    val accountsFlow: Flow<List<AccountModel>> = dao.observeActiveAccounts().map { list ->
        list.map { entity -> entity.toDomain() }
    }

    val categoriesFlow: Flow<List<CategoryModel>> = dao.observeAllCategories().map { list ->
        list.map { entity -> entity.toDomain() }
    }

    val transactionsFlow: Flow<List<TransactionEntity>> = dao.observeAllTransactions()

    val budgetsFlow: Flow<List<BudgetEntity>> = dao.observeAllBudgets()

    val exchangeRatesFlow: Flow<List<ExchangeRateModel>> = dao.observeExchangeRates().map { list ->
        list.map { entity ->
            ExchangeRateModel(
                currencyCode = entity.currencyCode,
                symbol = entity.symbol,
                displayName = entity.displayName,
                rateToUsd = entity.rateToUsd,
                isCustomOverride = entity.isCustomOverride,
                updatedAt = entity.updatedAt
            )
        }
    }

    private fun AccountEntity.toDomain(): AccountModel {
        val acctType = runCatching { AccountType.valueOf(accountType) }.getOrDefault(AccountType.BANK)
        val bfBalance = CryptoManager.decryptDouble(encryptedBroughtForwardBalance, 0.0)
        val rawAcctNumber = if (encryptedAccountNumber.isNotEmpty()) CryptoManager.decrypt(encryptedAccountNumber) else ""
        val displayMask = if (maskedAccountNumber.isNotEmpty()) maskedAccountNumber else maskAccountNumber(rawAcctNumber)
        return AccountModel(
            id = id,
            name = name,
            accountType = acctType,
            currencyCode = currencyCode,
            initialBroughtForwardBalance = bfBalance,
            broughtForwardDate = broughtForwardDate,
            carryOverEnabled = carryOverEnabled,
            colorHex = colorHex,
            iconKey = iconKey,
            accountNumber = rawAcctNumber,
            maskedAccountNumber = displayMask
        )
    }

    companion object {
        fun maskAccountNumber(raw: String): String {
            val clean = raw.trim()
            if (clean.length <= 6) return clean
            val maskCount = clean.length - 6
            return "x".repeat(maskCount) + clean.takeLast(6)
        }
    }

    private fun CategoryEntity.toDomain(): CategoryModel {
        val txType = runCatching { TransactionType.valueOf(type) }.getOrDefault(TransactionType.EXPENSE)
        return CategoryModel(
            id = id,
            name = name,
            type = txType,
            iconKey = iconKey,
            colorHex = colorHex,
            isCustom = isCustom
        )
    }

    fun decryptTransaction(
        entity: TransactionEntity,
        accountMap: Map<Long, AccountModel>,
        categoryMap: Map<Long, CategoryModel>,
        baseCurrency: String,
        rates: Map<String, ExchangeRateModel>
    ): TransactionModel {
        val txType = runCatching { TransactionType.valueOf(entity.type) }.getOrDefault(TransactionType.EXPENSE)
        val cached = decryptedTxCache[entity.id]?.takeIf {
            it.encryptedAmount == entity.encryptedAmount &&
                it.encryptedTargetAmount == entity.encryptedTargetAmount &&
                it.encryptedTitle == entity.encryptedTitle &&
                it.encryptedNote == entity.encryptedNote
        }
        val resolved = cached ?: run {
            val decAmount = CryptoManager.decryptDouble(entity.encryptedAmount, 0.0)
            val decTargetAmount = entity.encryptedTargetAmount?.let { CryptoManager.decryptDouble(it, decAmount) }
            val decTitle = CryptoManager.decrypt(entity.encryptedTitle)
            val decNote = CryptoManager.decrypt(entity.encryptedNote)
            CachedDecryptedTx(
                encryptedAmount = entity.encryptedAmount,
                encryptedTargetAmount = entity.encryptedTargetAmount,
                encryptedTitle = entity.encryptedTitle,
                encryptedNote = entity.encryptedNote,
                amount = decAmount,
                targetAmount = decTargetAmount,
                title = decTitle,
                note = decNote
            ).also {
                if (entity.id > 0L) {
                    decryptedTxCache[entity.id] = it
                }
            }
        }
        val amount = resolved.amount
        val targetAmount = resolved.targetAmount
        val title = resolved.title
        val note = resolved.note

        val sourceAccount = accountMap[entity.accountId]
        val targetAccount = entity.targetAccountId?.let { accountMap[it] }
        val category = entity.categoryId?.let { categoryMap[it] }

        val defaultCategoryName = when (txType) {
            TransactionType.TRANSFER -> "Account Transfer"
            TransactionType.BROUGHT_FORWARD -> "Balance Brought Forward (B/F)"
            TransactionType.INCOME -> "Income"
            TransactionType.EXPENSE -> "General"
        }
        val defaultIcon = when (txType) {
            TransactionType.TRANSFER -> "swap"
            TransactionType.BROUGHT_FORWARD -> "history"
            TransactionType.INCOME -> "Payments"
            TransactionType.EXPENSE -> "Receipt"
        }
        val defaultColor = when (txType) {
            TransactionType.TRANSFER -> "#3B82F6"
            TransactionType.BROUGHT_FORWARD -> "#D97706"
            TransactionType.INCOME -> "#10B981"
            TransactionType.EXPENSE -> "#0B6E4F"
        }

        val normalizedInBase = CurrencyAndBfCalculator.convert(
            amount = amount,
            fromCurrency = entity.currencyCode,
            toCurrency = baseCurrency,
            rates = rates
        )

        return TransactionModel(
            id = entity.id,
            accountId = entity.accountId,
            accountName = sourceAccount?.name ?: "Account",
            targetAccountId = entity.targetAccountId,
            targetAccountName = targetAccount?.name,
            categoryId = entity.categoryId,
            categoryName = category?.name ?: defaultCategoryName,
            categoryIconKey = category?.iconKey ?: defaultIcon,
            categoryColorHex = category?.colorHex ?: defaultColor,
            type = txType,
            amount = amount,
            targetAmount = targetAmount,
            currencyCode = entity.currencyCode,
            targetCurrencyCode = targetAccount?.currencyCode,
            normalizedAmountInBase = normalizedInBase,
            title = title.ifBlank { category?.name ?: defaultCategoryName },
            note = note,
            timestamp = entity.timestamp
        )
    }

    fun decryptBudgetsForMonth(
        allBudgets: List<BudgetEntity>,
        activeYearMonth: String,
        periodTransactions: List<TransactionModel>,
        categoryMap: Map<Long, CategoryModel>,
        rates: Map<String, ExchangeRateModel>
    ): List<BudgetStatusModel> {
        val monthBudgets = allBudgets.filter { it.yearMonth == activeYearMonth }
        return monthBudgets.mapNotNull { entity ->
            val category = categoryMap[entity.categoryId] ?: return@mapNotNull null
            val cachedBudget = decryptedBudgetCache[entity.id]?.takeIf {
                it.encryptedLimitAmount == entity.encryptedLimitAmount
            }
            val limit = cachedBudget?.limitAmount ?: run {
                CryptoManager.decryptDouble(entity.encryptedLimitAmount, 0.0).also { decLimit ->
                    if (entity.id > 0L) {
                        decryptedBudgetCache[entity.id] = CachedDecryptedBudget(entity.encryptedLimitAmount, decLimit)
                    }
                }
            }
            val spentInBudgetCurrency = periodTransactions
                .filter { it.type == TransactionType.EXPENSE && it.categoryId == entity.categoryId }
                .sumOf { tx ->
                    CurrencyAndBfCalculator.convert(
                        amount = tx.amount,
                        fromCurrency = tx.currencyCode,
                        toCurrency = entity.currencyCode,
                        rates = rates
                    )
                }

            BudgetStatusModel(
                id = entity.id,
                categoryId = entity.categoryId,
                categoryName = category.name,
                categoryIconKey = category.iconKey,
                categoryColorHex = category.colorHex,
                yearMonth = entity.yearMonth,
                limitAmount = limit,
                spentAmountInBudgetCurrency = spentInBudgetCurrency,
                currencyCode = entity.currencyCode,
                alertAt80Percent = entity.alertAt80Percent,
                alertAt100Percent = entity.alertAt100Percent
            )
        }
    }

    suspend fun saveAccount(
        id: Long = 0L,
        name: String,
        accountType: AccountType,
        currencyCode: String,
        initialBroughtForwardBalance: Double,
        broughtForwardDate: Long = System.currentTimeMillis(),
        carryOverEnabled: Boolean = true,
        colorHex: String = "#0B6E4F",
        iconKey: String = "AccountBalance",
        accountNumber: String = ""
    ): Long {
        val cleanAcctNumber = accountNumber.trim()
        val masked = maskAccountNumber(cleanAcctNumber)
        val encryptedAcctNumber = if (cleanAcctNumber.isNotEmpty()) CryptoManager.encrypt(cleanAcctNumber) else ""
        val entity = AccountEntity(
            id = id,
            name = name.trim(),
            accountType = accountType.name,
            currencyCode = currencyCode.uppercase(),
            encryptedBroughtForwardBalance = CryptoManager.encryptDouble(initialBroughtForwardBalance),
            broughtForwardDate = broughtForwardDate,
            carryOverEnabled = carryOverEnabled,
            colorHex = colorHex,
            iconKey = iconKey,
            encryptedAccountNumber = encryptedAcctNumber,
            maskedAccountNumber = masked
        )
        return dao.insertAccount(entity)
    }

    suspend fun updateAccountBroughtForward(
        accountId: Long,
        newInitialBroughtForward: Double,
        carryOverEnabled: Boolean
    ) {
        val existing = dao.getAccountById(accountId) ?: return
        dao.updateAccount(
            existing.copy(
                encryptedBroughtForwardBalance = CryptoManager.encryptDouble(newInitialBroughtForward),
                carryOverEnabled = carryOverEnabled
            )
        )
    }

    suspend fun deleteAccount(accountId: Long) {
        dao.deleteTransactionsForAccount(accountId)
        dao.deleteAccountById(accountId)
    }

    suspend fun saveCategory(
        id: Long = 0L,
        name: String,
        type: TransactionType,
        iconKey: String,
        colorHex: String
    ): Long {
        val existingOrder = if (id > 0L) {
            dao.getAllCategoriesOnce().find { it.id == id }?.sortOrder ?: 50
        } else {
            50
        }
        return dao.insertCategory(
            CategoryEntity(
                id = id,
                name = name.trim(),
                type = type.name,
                iconKey = iconKey,
                colorHex = colorHex,
                isCustom = true,
                sortOrder = existingOrder
            )
        )
    }

    suspend fun deleteCategory(categoryId: Long) {
        dao.clearCategoryFromTransactions(categoryId)
        dao.deleteBudgetsByCategoryId(categoryId)
        dao.deleteCategoryById(categoryId)
    }

    suspend fun saveTransaction(
        id: Long = 0L,
        accountId: Long,
        targetAccountId: Long? = null,
        categoryId: Long? = null,
        type: TransactionType,
        amount: Double,
        targetAmount: Double? = null,
        currencyCode: String,
        title: String,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ): Long {
        val entity = TransactionEntity(
            id = id,
            accountId = accountId,
            targetAccountId = if (type == TransactionType.TRANSFER) targetAccountId else null,
            categoryId = categoryId,
            type = type.name,
            encryptedAmount = CryptoManager.encryptDouble(amount),
            encryptedTargetAmount = targetAmount?.let { CryptoManager.encryptDouble(it) },
            currencyCode = currencyCode,
            encryptedTitle = CryptoManager.encrypt(title.trim()),
            encryptedNote = CryptoManager.encrypt(note.trim()),
            timestamp = timestamp
        )
        return dao.insertTransaction(entity)
    }

    suspend fun deleteTransaction(id: Long) {
        dao.deleteTransactionById(id)
    }

    suspend fun saveBudget(
        id: Long = 0L,
        categoryId: Long,
        yearMonth: String,
        limitAmount: Double,
        currencyCode: String,
        alertAt80Percent: Boolean = true,
        alertAt100Percent: Boolean = true
    ): Long {
        val existing = dao.getBudgetForCategoryMonth(categoryId, yearMonth)
        val entity = BudgetEntity(
            id = existing?.id ?: id,
            categoryId = categoryId,
            yearMonth = yearMonth,
            encryptedLimitAmount = CryptoManager.encryptDouble(limitAmount),
            currencyCode = currencyCode,
            alertAt80Percent = alertAt80Percent,
            alertAt100Percent = alertAt100Percent,
            lastNotifiedLevel = existing?.lastNotifiedLevel ?: 0
        )
        return dao.insertBudget(entity)
    }

    suspend fun deleteBudget(id: Long) {
        dao.deleteBudgetById(id)
    }

    /**
     * Copies all category budgets from [sourceYearMonth] into [targetYearMonth] for categories
     * that do not yet have a budget in [targetYearMonth]. Returns the number of budgets copied.
     */
    suspend fun copyBudgetsFromMonth(
        sourceYearMonth: String,
        targetYearMonth: String
    ): Int {
        val allBudgets = dao.observeAllBudgets().first()
        val sourceBudgets = allBudgets.filter { it.yearMonth == sourceYearMonth }
        if (sourceBudgets.isEmpty()) return 0

        var copiedCount = 0
        for (source in sourceBudgets) {
            val existingInTarget = dao.getBudgetForCategoryMonth(source.categoryId, targetYearMonth)
            if (existingInTarget == null) {
                dao.insertBudget(
                    BudgetEntity(
                        id = 0L,
                        categoryId = source.categoryId,
                        yearMonth = targetYearMonth,
                        encryptedLimitAmount = source.encryptedLimitAmount,
                        currencyCode = source.currencyCode,
                        alertAt80Percent = source.alertAt80Percent,
                        alertAt100Percent = source.alertAt100Percent,
                        lastNotifiedLevel = 0
                    )
                )
                copiedCount++
            }
        }
        return copiedCount
    }

    suspend fun updateExchangeRate(
        currencyCode: String,
        newRateToUsd: Double,
        isCustomOverride: Boolean = true
    ) {
        val current = dao.getExchangeRatesOnce().find { it.currencyCode == currencyCode }
        if (current != null && newRateToUsd > 0.0) {
            dao.insertExchangeRate(
                current.copy(
                    rateToUsd = newRateToUsd,
                    isCustomOverride = isCustomOverride,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun resetExchangeRatesToDefault() {
        dao.insertExchangeRates(defaultExchangeRates())
    }

    private fun defaultExchangeRates(): List<ExchangeRateEntity> = listOf(
        ExchangeRateEntity("USD", "$", "US Dollar", 1.00, false),
        ExchangeRateEntity("EUR", "€", "Euro", 0.92, false),
        ExchangeRateEntity("GBP", "£", "British Pound", 0.79, false),
        ExchangeRateEntity("INR", "₹", "Indian Rupee", 84.10, false),
        ExchangeRateEntity("JPY", "¥", "Japanese Yen", 153.50, false),
        ExchangeRateEntity("CAD", "CA$", "Canadian Dollar", 1.38, false),
        ExchangeRateEntity("AUD", "A$", "Australian Dollar", 1.52, false),
        ExchangeRateEntity("CHF", "CHF ", "Swiss Franc", 0.88, false),
        ExchangeRateEntity("CNY", "¥", "Chinese Yuan", 7.24, false),
        ExchangeRateEntity("SGD", "S$", "Singapore Dollar", 1.34, false)
    )

    /**
     * Initializes only offline currency definitions and standard transaction category definitions.
     * Does NOT pre-fill any demo accounts, transactions, or budgets so the user starts with a clean ledger.
     */
    suspend fun ensureSeeded() {
        if (dao.getExchangeRatesOnce().isEmpty()) {
            dao.insertExchangeRates(defaultExchangeRates())
        }

        if (dao.getAllCategoriesOnce().isEmpty()) {
            val seedCats = listOf(
                CategoryEntity(name = "Food & Dining", type = TransactionType.EXPENSE.name, iconKey = "Restaurant", colorHex = "#E37400", sortOrder = 1),
                CategoryEntity(name = "Groceries", type = TransactionType.EXPENSE.name, iconKey = "ShoppingCart", colorHex = "#1E8E3E", sortOrder = 2),
                CategoryEntity(name = "Housing & Rent", type = TransactionType.EXPENSE.name, iconKey = "Home", colorHex = "#1A73E8", sortOrder = 3),
                CategoryEntity(name = "Transport & Fuel", type = TransactionType.EXPENSE.name, iconKey = "DirectionsCar", colorHex = "#1967D2", sortOrder = 4),
                CategoryEntity(name = "Shopping & Lifestyle", type = TransactionType.EXPENSE.name, iconKey = "ShoppingBag", colorHex = "#9334E6", sortOrder = 5),
                CategoryEntity(name = "Bills & Utilities", type = TransactionType.EXPENSE.name, iconKey = "Bolt", colorHex = "#F9AB00", sortOrder = 6),
                CategoryEntity(name = "Health & Wellness", type = TransactionType.EXPENSE.name, iconKey = "MedicalServices", colorHex = "#D93025", sortOrder = 7),
                CategoryEntity(name = "Travel & FX", type = TransactionType.EXPENSE.name, iconKey = "Flight", colorHex = "#12B5CB", sortOrder = 8),
                CategoryEntity(name = "Entertainment", type = TransactionType.EXPENSE.name, iconKey = "Movie", colorHex = "#E52592", sortOrder = 9),
                CategoryEntity(name = "Salary & Payroll", type = TransactionType.INCOME.name, iconKey = "Work", colorHex = "#1E8E3E", sortOrder = 10),
                CategoryEntity(name = "Freelance & Consulting", type = TransactionType.INCOME.name, iconKey = "TrendingUp", colorHex = "#137333", sortOrder = 11),
                CategoryEntity(name = "Dividends & Yield", type = TransactionType.INCOME.name, iconKey = "Savings", colorHex = "#1A73E8", sortOrder = 12),
                CategoryEntity(name = "Balance Brought Forward", type = TransactionType.BROUGHT_FORWARD.name, iconKey = "History", colorHex = "#E37400", sortOrder = 13)
            )
            dao.insertCategories(seedCats)
        }
    }
}
