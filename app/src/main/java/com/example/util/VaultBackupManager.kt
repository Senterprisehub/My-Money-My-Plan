package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.local.AccountEntity
import com.example.data.local.AppDatabase
import com.example.data.local.BudgetEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.ExchangeRateEntity
import com.example.data.local.TransactionEntity
import com.example.data.security.CryptoManager
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class VaultBackupResult(
    val file: File,
    val fileName: String,
    val accountsCount: Int,
    val categoriesCount: Int,
    val transactionsCount: Int,
    val budgetsCount: Int,
    val summaryMessage: String
)

/**
 * Provides full AES-256-GCM Encrypted Vault Backup & Restore for My Money My Plan (MMMP).
 * Exports all Accounts, Categories, Transactions, Monthly Budgets, and Custom FX Rates into
 * an encrypted JSON envelope so users can back up, share, and restore their complete financial ledger.
 */
object VaultBackupManager {

    private const val BACKUP_DIR_NAME = "exports"
    private const val LATEST_BACKUP_FILENAME = "My_Money_My_Plan_MMMP_Latest_Vault_Backup.json"
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    private val humanDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    private fun ensureBackupDir(context: Context): File {
        val dir = File(context.cacheDir, BACKUP_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun hasLocalBackupSnapshot(context: Context): Boolean {
        val latest = File(ensureBackupDir(context), LATEST_BACKUP_FILENAME)
        return latest.exists() && latest.length() > 0L
    }

    suspend fun exportEncryptedBackup(context: Context): VaultBackupResult {
        val db = AppDatabase.getInstance(context)
        val dao = db.financeDao()

        val accounts = dao.getAllAccountsOnce()
        val categories = dao.getAllCategoriesOnce()
        val transactions = dao.getAllTransactionsOnce()
        val budgets = dao.observeAllBudgets().first()
        val rates = dao.getExchangeRatesOnce()

        val innerPayload = JSONObject().apply {
            put("application", StatementExporter.OFFICIAL_APP_NAME)
            put("generatedAt", System.currentTimeMillis())

            put("accounts", JSONArray().apply {
                accounts.forEach { acc ->
                    put(JSONObject().apply {
                        put("id", acc.id)
                        put("name", acc.name)
                        put("accountType", acc.accountType)
                        put("currencyCode", acc.currencyCode)
                        put("encryptedBroughtForwardBalance", acc.encryptedBroughtForwardBalance)
                        put("broughtForwardDate", acc.broughtForwardDate)
                        put("carryOverEnabled", acc.carryOverEnabled)
                        put("colorHex", acc.colorHex)
                        put("iconKey", acc.iconKey)
                        put("isArchived", acc.isArchived)
                        put("createdAt", acc.createdAt)
                    })
                }
            })

            put("categories", JSONArray().apply {
                categories.forEach { cat ->
                    put(JSONObject().apply {
                        put("id", cat.id)
                        put("name", cat.name)
                        put("type", cat.type)
                        put("iconKey", cat.iconKey)
                        put("colorHex", cat.colorHex)
                        put("isCustom", cat.isCustom)
                        put("sortOrder", cat.sortOrder)
                    })
                }
            })

            put("transactions", JSONArray().apply {
                transactions.forEach { tx ->
                    put(JSONObject().apply {
                        put("id", tx.id)
                        put("accountId", tx.accountId)
                        if (tx.targetAccountId != null) put("targetAccountId", tx.targetAccountId)
                        if (tx.categoryId != null) put("categoryId", tx.categoryId)
                        put("type", tx.type)
                        put("encryptedAmount", tx.encryptedAmount)
                        if (tx.encryptedTargetAmount != null) put("encryptedTargetAmount", tx.encryptedTargetAmount)
                        put("currencyCode", tx.currencyCode)
                        put("encryptedTitle", tx.encryptedTitle)
                        put("encryptedNote", tx.encryptedNote)
                        put("timestamp", tx.timestamp)
                    })
                }
            })

            put("budgets", JSONArray().apply {
                budgets.forEach { b ->
                    put(JSONObject().apply {
                        put("id", b.id)
                        put("categoryId", b.categoryId)
                        put("yearMonth", b.yearMonth)
                        put("encryptedLimitAmount", b.encryptedLimitAmount)
                        put("currencyCode", b.currencyCode)
                        put("alertAt80Percent", b.alertAt80Percent)
                        put("alertAt100Percent", b.alertAt100Percent)
                        put("lastNotifiedLevel", b.lastNotifiedLevel)
                    })
                }
            })

            put("exchangeRates", JSONArray().apply {
                rates.forEach { r ->
                    put(JSONObject().apply {
                        put("currencyCode", r.currencyCode)
                        put("symbol", r.symbol)
                        put("displayName", r.displayName)
                        put("rateToUsd", r.rateToUsd)
                        put("isCustomOverride", r.isCustomOverride)
                        put("updatedAt", r.updatedAt)
                    })
                }
            })
        }

        val encryptedPayload = CryptoManager.encrypt(innerPayload.toString())
        val outerEnvelope = JSONObject().apply {
            put("application", StatementExporter.OFFICIAL_APP_NAME)
            put("documentType", "AES-256-GCM Encrypted Vault Backup")
            put("version", 1)
            put("exportedAt", humanDateFormat.format(Date()))
            put("accountsCount", accounts.size)
            put("categoriesCount", categories.size)
            put("transactionsCount", transactions.size)
            put("budgetsCount", budgets.size)
            put("encryptedVaultPayload", encryptedPayload)
        }

        val dir = ensureBackupDir(context)
        val timestampedName = "My_Money_My_Plan_MMMP_Vault_Backup_${fileDateFormat.format(Date())}.json"
        val file = File(dir, timestampedName)
        val latestFile = File(dir, LATEST_BACKUP_FILENAME)

        val formattedJson = outerEnvelope.toString(2)
        file.writeText(formattedJson, Charsets.UTF_8)
        latestFile.writeText(formattedJson, Charsets.UTF_8)

        val summary = "Encrypted Backup Ready (${accounts.size} accounts, ${categories.size} categories, ${transactions.size} records, ${budgets.size} budgets)"
        return VaultBackupResult(
            file = file,
            fileName = timestampedName,
            accountsCount = accounts.size,
            categoriesCount = categories.size,
            transactionsCount = transactions.size,
            budgetsCount = budgets.size,
            summaryMessage = summary
        )
    }

    suspend fun restoreLatestLocalBackup(context: Context): String {
        val latestFile = File(ensureBackupDir(context), LATEST_BACKUP_FILENAME)
        if (!latestFile.exists() || latestFile.length() == 0L) {
            throw IllegalStateException("No local backup snapshot found yet. Tap 'Export Backup' first or select a backup file.")
        }
        return restoreFromEncryptedBackupJson(context, latestFile.readText(Charsets.UTF_8))
    }

    suspend fun restoreFromEncryptedBackupJson(context: Context, rawEnvelopeJson: String): String {
        val outer = JSONObject(rawEnvelopeJson.trim())
        val encryptedPayload = outer.optString("encryptedVaultPayload", "")
        if (encryptedPayload.isBlank()) {
            throw IllegalArgumentException("Invalid backup file: missing encryptedVaultPayload.")
        }
        val decryptedJson = CryptoManager.decrypt(encryptedPayload)
        if (decryptedJson.isBlank()) {
            throw IllegalArgumentException("Could not decrypt backup payload.")
        }
        val inner = JSONObject(decryptedJson)
        val db = AppDatabase.getInstance(context)
        val dao = db.financeDao()

        val accountsArray = inner.optJSONArray("accounts") ?: JSONArray()
        val accountEntities = mutableListOf<AccountEntity>()
        for (i in 0 until accountsArray.length()) {
            val obj = accountsArray.getJSONObject(i)
            accountEntities.add(
                AccountEntity(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    accountType = obj.getString("accountType"),
                    currencyCode = obj.getString("currencyCode"),
                    encryptedBroughtForwardBalance = obj.optString(
                        "encryptedBroughtForwardBalance",
                        obj.optString("encryptedInitialBroughtForward", "")
                    ),
                    broughtForwardDate = obj.optLong("broughtForwardDate", 0L),
                    carryOverEnabled = obj.optBoolean("carryOverEnabled", true),
                    colorHex = obj.optString("colorHex", "#0B6E4F"),
                    iconKey = obj.optString("iconKey", "AccountBalance"),
                    isArchived = obj.optBoolean("isArchived", false),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        val categoriesArray = inner.optJSONArray("categories") ?: JSONArray()
        val categoryEntities = mutableListOf<CategoryEntity>()
        for (i in 0 until categoriesArray.length()) {
            val obj = categoriesArray.getJSONObject(i)
            categoryEntities.add(
                CategoryEntity(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    type = obj.getString("type"),
                    iconKey = obj.optString("iconKey", "Category"),
                    colorHex = obj.optString("colorHex", "#1A73E8"),
                    isCustom = obj.optBoolean("isCustom", false),
                    sortOrder = obj.optInt("sortOrder", i)
                )
            )
        }

        val transactionsArray = inner.optJSONArray("transactions") ?: JSONArray()
        val transactionEntities = mutableListOf<TransactionEntity>()
        for (i in 0 until transactionsArray.length()) {
            val obj = transactionsArray.getJSONObject(i)
            transactionEntities.add(
                TransactionEntity(
                    id = obj.getLong("id"),
                    accountId = obj.getLong("accountId"),
                    targetAccountId = if (obj.has("targetAccountId") && !obj.isNull("targetAccountId")) obj.getLong("targetAccountId") else null,
                    categoryId = if (obj.has("categoryId") && !obj.isNull("categoryId")) obj.getLong("categoryId") else null,
                    type = obj.getString("type"),
                    encryptedAmount = obj.getString("encryptedAmount"),
                    encryptedTargetAmount = if (obj.has("encryptedTargetAmount") && !obj.isNull("encryptedTargetAmount")) obj.getString("encryptedTargetAmount") else null,
                    currencyCode = obj.getString("currencyCode"),
                    encryptedTitle = obj.getString("encryptedTitle"),
                    encryptedNote = obj.optString("encryptedNote", CryptoManager.encrypt("")),
                    timestamp = obj.getLong("timestamp")
                )
            )
        }

        val budgetsArray = inner.optJSONArray("budgets") ?: JSONArray()
        val budgetEntities = mutableListOf<BudgetEntity>()
        for (i in 0 until budgetsArray.length()) {
            val obj = budgetsArray.getJSONObject(i)
            budgetEntities.add(
                BudgetEntity(
                    id = obj.getLong("id"),
                    categoryId = obj.getLong("categoryId"),
                    yearMonth = obj.getString("yearMonth"),
                    encryptedLimitAmount = obj.getString("encryptedLimitAmount"),
                    currencyCode = obj.getString("currencyCode"),
                    alertAt80Percent = obj.optBoolean("alertAt80Percent", true),
                    alertAt100Percent = obj.optBoolean("alertAt100Percent", true),
                    lastNotifiedLevel = obj.optInt("lastNotifiedLevel", 0)
                )
            )
        }

        val ratesArray = inner.optJSONArray("exchangeRates") ?: JSONArray()
        val rateEntities = mutableListOf<ExchangeRateEntity>()
        for (i in 0 until ratesArray.length()) {
            val obj = ratesArray.getJSONObject(i)
            rateEntities.add(
                ExchangeRateEntity(
                    currencyCode = obj.getString("currencyCode"),
                    symbol = obj.getString("symbol"),
                    displayName = obj.getString("displayName"),
                    rateToUsd = obj.getDouble("rateToUsd"),
                    isCustomOverride = obj.optBoolean("isCustomOverride", false),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }

        if (accountEntities.isNotEmpty()) dao.insertAccounts(accountEntities)
        if (categoryEntities.isNotEmpty()) dao.insertCategories(categoryEntities)
        if (transactionEntities.isNotEmpty()) dao.insertTransactions(transactionEntities)
        if (budgetEntities.isNotEmpty()) dao.insertBudgets(budgetEntities)
        if (rateEntities.isNotEmpty()) dao.insertExchangeRates(rateEntities)

        return "Restored My Money My Plan (MMMP) Vault: ${accountEntities.size} accounts, ${categoryEntities.size} categories, ${transactionEntities.size} records, ${budgetEntities.size} budgets"
    }

    fun shareBackupFile(context: Context, backupResult: VaultBackupResult) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, backupResult.file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "My Money My Plan (MMMP) — Encrypted Vault Backup (${backupResult.fileName})")
                putExtra(Intent.EXTRA_TEXT, backupResult.summaryMessage)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Save or Share My Money My Plan (MMMP) Encrypted Backup").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Throwable) {
            // Ignored in headless / non-activity contexts
        }
    }
}
