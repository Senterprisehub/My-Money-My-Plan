package com.example.util

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.repository.FinanceRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.domain.AutoTrackMode
import com.example.domain.ParsedFinancialAlert
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Coordinates real-time notifications captured by [com.example.service.FinancialNotificationListenerService],
 * respects the user's Master On/Off Toggle (`autoTrackEnabled`) and Recording Mode (`AUTO_SAVE` vs `PENDING_REVIEW`),
 * and either saves directly to the encrypted Room ledger or queues alerts for 1-tap user review.
 */
object AutoTrackedAlertManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _pendingAlertsFlow = MutableStateFlow<List<ParsedFinancialAlert>>(emptyList())
    val pendingAlertsFlow: StateFlow<List<ParsedFinancialAlert>> = _pendingAlertsFlow.asStateFlow()

    private val _lastAutoSavedMessageFlow = MutableStateFlow<String?>(null)
    val lastAutoSavedMessageFlow: StateFlow<String?> = _lastAutoSavedMessageFlow.asStateFlow()

    fun handleNotificationPosted(
        context: Context,
        packageName: String,
        title: String,
        body: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        scope.launch {
            processIncomingAlertSuspend(
                context = context.applicationContext,
                packageName = packageName,
                title = title,
                body = body,
                timestamp = timestamp
            )
        }
    }

    suspend fun processIncomingAlertSuspend(
        context: Context,
        packageName: String,
        title: String,
        body: String,
        timestamp: Long = System.currentTimeMillis()
    ): ParsedFinancialAlert? {
        val prefsRepo = UserPreferencesRepository(context)
        val prefs = prefsRepo.preferencesFlow.first()

        // Strictly respect the Master On/Off Toggle in Settings
        if (!prefs.autoTrackEnabled) {
            return null
        }

        // Check Per-App Source Filter Chips before deduplicating/recording
        val preliminaryLabel = BankAndUpiAlertParser.resolveSourceAppLabel(packageName, title, body)
        val alertSource = BankAndUpiAlertParser.classifyAlertSource(packageName, preliminaryLabel)
        if (alertSource !in prefs.enabledAutoTrackSources) {
            return null
        }

        val parsed = BankAndUpiAlertParser.parseAndDeduplicate(
            packageName = packageName,
            title = title,
            body = body,
            defaultCurrency = prefs.baseCurrency,
            timestamp = timestamp
        ) ?: return null

        if (prefs.autoTrackMode == AutoTrackMode.AUTO_SAVE) {
            val db = AppDatabase.getInstance(context)
            val repository = FinanceRepository(db.financeDao())
            repository.ensureSeeded()
            val accounts = repository.accountsFlow.first()
            val strictMatch = BankAndUpiAlertParser.matchAccountStrict(parsed, accounts)
            if (strictMatch != null) {
                saveParsedAlertToLedger(context, parsed, prefs.notificationsEnabled && prefs.txAlertsEnabled)
            } else {
                _pendingAlertsFlow.update { current ->
                    listOf(parsed) + current.filterNot { it.id == parsed.id }
                }
                val formatted = CurrencyAndBfCalculator.formatCurrency(parsed.amount, parsed.currencyCode, emptyMap())
                _lastAutoSavedMessageFlow.value =
                    "Held ${parsed.sourceAppLabel} ${parsed.type.displayLabel} ($formatted) in Pending Review — confirm A/c"
            }
        } else {
            _pendingAlertsFlow.update { current ->
                listOf(parsed) + current.filterNot { it.id == parsed.id }
            }
        }
        return parsed
    }

    suspend fun saveParsedAlertToLedger(
        context: Context,
        alert: ParsedFinancialAlert,
        sendPushNotification: Boolean = true
    ): Boolean {
        val db = AppDatabase.getInstance(context)
        val repository = FinanceRepository(db.financeDao())
        repository.ensureSeeded()

        val accounts = repository.accountsFlow.first()
        val categories = repository.categoriesFlow.first()
        val rates = repository.exchangeRatesFlow.first().associateBy { it.currencyCode }

        val matchedAccount = BankAndUpiAlertParser.matchAccount(alert, accounts) ?: return false
        val matchedCategory = BankAndUpiAlertParser.matchCategory(alert, categories)

        // Convert alert amount into matched account's native currency if different
        val finalAmount = if (alert.currencyCode.equals(matchedAccount.currencyCode, ignoreCase = true)) {
            alert.amount
        } else {
            CurrencyAndBfCalculator.convert(
                amount = alert.amount,
                fromCurrency = alert.currencyCode,
                toCurrency = matchedAccount.currencyCode,
                rates = rates
            )
        }

        val noteBuilder = buildString {
            append("Auto-tracked via ${alert.sourceAppLabel}")
            if (!alert.accountHint.isNullOrBlank()) {
                append(" (${alert.accountHint})")
            }
            if (!alert.referenceId.isNullOrBlank()) {
                append(" • Ref: ${alert.referenceId}")
            }
        }

        repository.saveTransaction(
            id = 0L,
            accountId = matchedAccount.id,
            targetAccountId = null,
            categoryId = matchedCategory?.id,
            type = alert.type,
            amount = finalAmount,
            targetAmount = null,
            currencyCode = matchedAccount.currencyCode,
            title = alert.merchantOrPayee,
            note = noteBuilder,
            timestamp = alert.timestamp
        )

        val formatted = CurrencyAndBfCalculator.formatCurrency(finalAmount, matchedAccount.currencyCode, rates)
        val msg = "Auto-saved ${alert.sourceAppLabel} ${alert.type.displayLabel}: $formatted (${alert.merchantOrPayee}) → ${matchedAccount.name}"
        _lastAutoSavedMessageFlow.value = msg

        if (sendPushNotification) {
            NotificationHelper.sendTransactionAlert(
                context = context,
                title = "My Money My Plan (MMMP) Auto-Tracked (${alert.sourceAppLabel})",
                body = "${alert.merchantOrPayee}: $formatted recorded in ${matchedAccount.name}"
            )
        }
        return true
    }

    fun removePendingAlert(alertId: String) {
        _pendingAlertsFlow.update { list -> list.filterNot { it.id == alertId } }
    }

    fun clearAllPendingAlerts() {
        _pendingAlertsFlow.value = emptyList()
    }

    fun consumeLastAutoSavedMessage() {
        _lastAutoSavedMessageFlow.value = null
    }
}
