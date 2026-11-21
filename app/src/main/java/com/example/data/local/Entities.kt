package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AccountType(val displayLabel: String) {
    CASH("Cash Wallet"),
    BANK("Checking / Bank"),
    SAVINGS("Savings Vault"),
    CREDIT_CARD("Credit Card"),
    INVESTMENT("Investment")
}

enum class TransactionType(val displayLabel: String) {
    EXPENSE("Expense"),
    INCOME("Income"),
    TRANSFER("Transfer"),
    BROUGHT_FORWARD("Brought Forward (B/F)")
}

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val accountType: String, // AccountType.name
    val currencyCode: String, // e.g., USD, EUR, GBP, INR, JPY
    val encryptedBroughtForwardBalance: String, // AES-256-GCM encrypted Double
    val broughtForwardDate: Long, // Effective opening B/F timestamp
    val carryOverEnabled: Boolean = true, // Include prior months' balance as B/F
    val colorHex: String,
    val iconKey: String,
    val encryptedAccountNumber: String = "", // AES-256-GCM encrypted account/card number
    val maskedAccountNumber: String = "",    // Disguised representation (e.g. xxxxxx123456)
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: String, // EXPENSE, INCOME, BROUGHT_FORWARD
    val iconKey: String,
    val colorHex: String,
    val isCustom: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["targetAccountId"]),
        Index(value = ["categoryId"]),
        Index(value = ["timestamp"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val accountId: Long,
    val targetAccountId: Long? = null, // Used when type == TRANSFER
    val categoryId: Long? = null,
    val type: String, // TransactionType.name
    val encryptedAmount: String, // AES-256-GCM encrypted Double in source account currency
    val encryptedTargetAmount: String? = null, // AES-256-GCM encrypted Double in target account currency
    val currencyCode: String,
    val encryptedTitle: String, // AES-256-GCM encrypted title / payee
    val encryptedNote: String, // AES-256-GCM encrypted memo
    val timestamp: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["categoryId", "yearMonth"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val categoryId: Long,
    val yearMonth: String, // Format: "YYYY-MM"
    val encryptedLimitAmount: String, // AES-256-GCM encrypted Double
    val currencyCode: String,
    val alertAt80Percent: Boolean = true,
    val alertAt100Percent: Boolean = true,
    val lastNotifiedLevel: Int = 0 // 0 = none, 80 = warning sent, 100 = exceeded sent
)

@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey val currencyCode: String, // ISO 4217 code
    val symbol: String,
    val displayName: String,
    val rateToUsd: Double, // How many units of currencyCode equal 1.00 USD
    val isCustomOverride: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "monthly_summaries",
    indices = [Index(value = ["yearMonth"], unique = true)]
)
data class MonthlySummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val yearMonth: String, // "YYYY-MM"
    val totalIncomeInBase: Double,
    val totalExpenseInBase: Double,
    val netSavingsInBase: Double,
    val currencyCode: String,
    val updatedAt: Long = System.currentTimeMillis()
)
