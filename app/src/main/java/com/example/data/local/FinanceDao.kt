package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    // Accounts
    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY id ASC")
    fun observeActiveAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY id ASC")
    suspend fun getAllAccountsOnce(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>): List<Long>

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccountById(id: Long)

    @Query("DELETE FROM transactions WHERE accountId = :accountId OR targetAccountId = :accountId")
    suspend fun deleteTransactionsForAccount(accountId: Long)

    // Categories
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, id ASC")
    fun observeAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllCategoriesOnce(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>): List<Long>

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)

    @Query("DELETE FROM budgets WHERE categoryId = :categoryId")
    suspend fun deleteBudgetsByCategoryId(categoryId: Long)

    @Query("UPDATE transactions SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategoryFromTransactions(categoryId: Long)

    // Transactions
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    suspend fun getAllTransactionsOnce(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    // Budgets
    @Query("SELECT * FROM budgets ORDER BY id ASC")
    fun observeAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth")
    suspend fun getBudgetsForMonthOnce(yearMonth: String): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND yearMonth = :yearMonth LIMIT 1")
    suspend fun getBudgetForCategoryMonth(categoryId: Long, yearMonth: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<BudgetEntity>): List<Long>

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Long)

    // Exchange Rates
    @Query("SELECT * FROM exchange_rates ORDER BY currencyCode ASC")
    fun observeExchangeRates(): Flow<List<ExchangeRateEntity>>

    @Query("SELECT * FROM exchange_rates ORDER BY currencyCode ASC")
    suspend fun getExchangeRatesOnce(): List<ExchangeRateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExchangeRate(rate: ExchangeRateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExchangeRates(rates: List<ExchangeRateEntity>)

    // Monthly Summaries (Rollups for MoM and YoY)
    @Query("SELECT * FROM monthly_summaries ORDER BY yearMonth DESC")
    fun observeMonthlySummaries(): Flow<List<MonthlySummaryEntity>>

    @Query("SELECT * FROM monthly_summaries WHERE yearMonth = :yearMonth LIMIT 1")
    suspend fun getMonthlySummary(yearMonth: String): MonthlySummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthlySummary(summary: MonthlySummaryEntity): Long
}
