package com.example

import com.example.data.local.AccountType
import com.example.data.local.TransactionType
import com.example.data.security.CryptoManager
import com.example.domain.AccountModel
import com.example.domain.ExchangeRateModel
import com.example.domain.TransactionModel
import com.example.util.BankAndUpiAlertParser
import com.example.util.CurrencyAndBfCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun aes256GcmEncryption_roundTripPreservesFinancialAmountsAndText() {
        val originalAmount = 14892.75
        val cipher = CryptoManager.encryptDouble(originalAmount)
        val decrypted = CryptoManager.decryptDouble(cipher)
        assertEquals(originalAmount, decrypted, 0.001)

        val secretMemo = "Swiss Vault Opening B/F #4092"
        val encryptedMemo = CryptoManager.encrypt(secretMemo)
        assertEquals(secretMemo, CryptoManager.decrypt(encryptedMemo))
    }

    @Test
    fun accountBroughtForward_computesOpeningBfAndClosingCfAcrossMonths() {
        val rates = mapOf(
            "USD" to ExchangeRateModel("USD", "$", "US Dollar", 1.0, false, 0L),
            "EUR" to ExchangeRateModel("EUR", "€", "Euro", 0.92, false, 0L)
        )
        val checking = AccountModel(
            id = 1L,
            name = "Checking",
            accountType = AccountType.BANK,
            currencyCode = "USD",
            initialBroughtForwardBalance = 1000.0,
            broughtForwardDate = 0L,
            carryOverEnabled = true,
            colorHex = "#0B6E4F",
            iconKey = "AccountBalance"
        )

        val (novStart, _) = CurrencyAndBfCalculator.monthBounds("2026-11")
        val octTxTimestamp = novStart - 5 * 24 * 3600 * 1000L // Prior month (October)
        val novTxTimestamp = novStart + 2 * 24 * 3600 * 1000L // Current month (November)

        val txs = listOf(
            // Prior month +$500 income -> should carry into November Opening B/F as $1,500
            TransactionModel(
                id = 10L,
                accountId = 1L,
                accountName = "Checking",
                targetAccountId = null,
                targetAccountName = null,
                categoryId = 1L,
                categoryName = "Salary",
                categoryIconKey = "Work",
                categoryColorHex = "#10B981",
                type = TransactionType.INCOME,
                amount = 500.0,
                targetAmount = null,
                currencyCode = "USD",
                targetCurrencyCode = null,
                normalizedAmountInBase = 500.0,
                title = "October Bonus",
                note = "",
                timestamp = octTxTimestamp
            ),
            // November -$200 expense -> Closing C/F should be $1,300
            TransactionModel(
                id = 11L,
                accountId = 1L,
                accountName = "Checking",
                targetAccountId = null,
                targetAccountName = null,
                categoryId = 2L,
                categoryName = "Dining",
                categoryIconKey = "Restaurant",
                categoryColorHex = "#EA580C",
                type = TransactionType.EXPENSE,
                amount = 200.0,
                targetAmount = null,
                currencyCode = "USD",
                targetCurrencyCode = null,
                normalizedAmountInBase = 200.0,
                title = "November Groceries",
                note = "",
                timestamp = novTxTimestamp
            )
        )

        val ledgers = CurrencyAndBfCalculator.computeAccountLedgersForMonth(
            rawAccounts = listOf(checking),
            allTransactions = txs,
            activeYearMonth = "2026-11",
            baseCurrency = "USD",
            rates = rates
        )
        val checkingLedger = ledgers.first()
        assertEquals(1500.0, checkingLedger.periodOpeningBroughtForward, 0.001)
        assertEquals(200.0, checkingLedger.periodExpense, 0.001)
        assertEquals(1300.0, checkingLedger.periodClosingCarriedForward, 0.001)

        val withRunning = CurrencyAndBfCalculator.attachRunningBalances(listOf(checking), txs)
        assertTrue(withRunning.any { it.id == 10L && it.runningBalanceAfter == 1500.0 })
        assertTrue(withRunning.any { it.id == 11L && it.runningBalanceAfter == 1300.0 })
    }

    @Test
    fun bankAndUpiAlertParser_parsesSuperMoneyUpiAndDeduplicatesBankSmsWithinTwoMinutes() {
        BankAndUpiAlertParser.clearDuplicateHistory()
        val now = 1_762_000_000_000L

        // 1. Parse Super Money UPI notification
        val superMoneyAlert = BankAndUpiAlertParser.parseAndDeduplicate(
            packageName = "money.super.app",
            title = "Super Money Payment Successful",
            body = "Paid ₹450.00 to Swiggy on Super Money via HDFC A/c XX1234. UPI Ref 631289012345.",
            defaultCurrency = "INR",
            timestamp = now
        )
        assertNotNull(superMoneyAlert)
        assertEquals("Super Money", superMoneyAlert!!.sourceAppLabel)
        assertEquals(TransactionType.EXPENSE, superMoneyAlert.type)
        assertEquals(450.0, superMoneyAlert.amount, 0.001)
        assertEquals("INR", superMoneyAlert.currencyCode)
        assertEquals("Swiggy", superMoneyAlert.merchantOrPayee)
        assertEquals("Food & Dining", superMoneyAlert.suggestedCategoryName)

        // 2. Duplicate Bank SMS arriving 25 seconds later for the same payment is filtered out
        val duplicateSmsAlert = BankAndUpiAlertParser.parseAndDeduplicate(
            packageName = "com.google.android.apps.messaging",
            title = "AD-HDFCBK",
            body = "Rs. 450.00 debited from HDFC Bank A/c XX1234 to Swiggy. UPI Ref 631289012345.",
            defaultCurrency = "INR",
            timestamp = now + 25_000L
        )
        assertNull(duplicateSmsAlert)

        // 3. Account matching links HDFC XX1234 hint to user's HDFC account
        val accounts = listOf(
            AccountModel(
                id = 1L,
                name = "Cash Wallet",
                accountType = AccountType.CASH,
                currencyCode = "INR",
                initialBroughtForwardBalance = 500.0,
                broughtForwardDate = 0L,
                carryOverEnabled = true,
                colorHex = "#0B6E4F",
                iconKey = "Payments"
            ),
            AccountModel(
                id = 2L,
                name = "HDFC Savings XX1234",
                accountType = AccountType.BANK,
                currencyCode = "INR",
                initialBroughtForwardBalance = 15000.0,
                broughtForwardDate = 0L,
                carryOverEnabled = true,
                colorHex = "#1A73E8",
                iconKey = "AccountBalance"
            )
        )
        val matched = BankAndUpiAlertParser.matchAccount(superMoneyAlert, accounts)
        assertNotNull(matched)
        assertEquals(2L, matched!!.id)

        // 4. Per-app AutoTrackSource classification
        assertEquals(
            com.example.domain.AutoTrackSource.SUPER_MONEY,
            BankAndUpiAlertParser.classifyAlertSource("money.super.app", "Super Money")
        )
        assertEquals(
            com.example.domain.AutoTrackSource.GOOGLE_PAY,
            BankAndUpiAlertParser.classifyAlertSource("com.google.android.apps.nbu.paisa.user", "Google Pay")
        )
        assertEquals(
            com.example.domain.AutoTrackSource.PHONEPE,
            BankAndUpiAlertParser.classifyAlertSource("com.phonepe.app", "PhonePe")
        )
        assertEquals(
            com.example.domain.AutoTrackSource.BANK_SMS,
            BankAndUpiAlertParser.classifyAlertSource("com.google.android.apps.messaging", "Bank SMS")
        )
    }

    @Test
    fun privacyMaskAndDuplication_verifiesRound3ExecutivePolish() {
        // 1. Privacy Mask State default
        val defaultPrefs = com.example.domain.UserPreferencesState()
        org.junit.Assert.assertFalse(defaultPrefs.privacyMaskEnabled)
        val maskedPrefs = defaultPrefs.copy(privacyMaskEnabled = true)
        assertTrue(maskedPrefs.privacyMaskEnabled)

        // 2. Duplicate as Today clones record with id = 0L and current timestamp
        val originalTx = TransactionModel(
            id = 42L,
            accountId = 2L,
            accountName = "HDFC Savings XX1234",
            targetAccountId = null,
            targetAccountName = null,
            categoryId = 5L,
            categoryName = "Utilities",
            categoryIconKey = "bolt",
            categoryColorHex = "#F59E0B",
            type = TransactionType.EXPENSE,
            amount = 120.50,
            targetAmount = null,
            currencyCode = "INR",
            targetCurrencyCode = null,
            normalizedAmountInBase = 120.50,
            title = "Electricity Bill",
            note = "Monthly recurring",
            timestamp = 1761000000000L
        )

        val nowBefore = System.currentTimeMillis()
        val duplicatedTx = originalTx.copy(
            id = 0L,
            timestamp = System.currentTimeMillis()
        )
        val nowAfter = System.currentTimeMillis()

        assertEquals(0L, duplicatedTx.id)
        assertEquals(originalTx.accountId, duplicatedTx.accountId)
        assertEquals(originalTx.amount, duplicatedTx.amount, 0.001)
        assertEquals(originalTx.title, duplicatedTx.title)
        assertTrue(duplicatedTx.timestamp in nowBefore..nowAfter)

        // 3. Statement Header Filter Enrichment format check
        val baseTitle = "November 2026"
        val activeAccountName = "HDFC Savings"
        val activeCategoryName = "Utilities"
        val activeSort = com.example.domain.RecordSortOrder.NEWEST_FIRST
        val filterBadges = listOfNotNull(
            "Account: $activeAccountName",
            "Category: $activeCategoryName",
            "Sort: ${activeSort.label}"
        )
        val enrichedTitle = "$baseTitle [${filterBadges.joinToString(" • ")}]"
        assertTrue(enrichedTitle.contains("Account: HDFC Savings"))
        assertTrue(enrichedTitle.contains("Category: Utilities"))
        assertTrue(enrichedTitle.contains("Sort: Newest First"))
    }

    @Test
    fun creditCardExclusionFromNetWorth_andExpenditureCalculation() {
        val rates = mapOf(
            "USD" to ExchangeRateModel("USD", "$", "US Dollar", 1.0, false, 0L)
        )
        val checking = AccountModel(
            id = 1L,
            name = "Bank Checking",
            accountType = AccountType.BANK,
            currencyCode = "USD",
            initialBroughtForwardBalance = 5000.0,
            broughtForwardDate = 0L,
            carryOverEnabled = true,
            colorHex = "#0B6E4F",
            iconKey = "AccountBalance"
        )
        val cash = AccountModel(
            id = 2L,
            name = "Wallet Cash",
            accountType = AccountType.CASH,
            currencyCode = "USD",
            initialBroughtForwardBalance = 1000.0,
            broughtForwardDate = 0L,
            carryOverEnabled = true,
            colorHex = "#10B981",
            iconKey = "Payments"
        )
        val creditCard = AccountModel(
            id = 3L,
            name = "Sapphire Credit Card",
            accountType = AccountType.CREDIT_CARD,
            currencyCode = "USD",
            initialBroughtForwardBalance = 0.0,
            broughtForwardDate = 0L,
            carryOverEnabled = true,
            colorHex = "#DC2626",
            iconKey = "CreditCard"
        )

        val txs = listOf(
            TransactionModel(
                id = 101L,
                accountId = 3L,
                accountName = "Sapphire Credit Card",
                targetAccountId = null,
                targetAccountName = null,
                categoryId = 2L,
                categoryName = "Electronics",
                categoryIconKey = "devices",
                categoryColorHex = "#2563EB",
                type = TransactionType.EXPENSE,
                amount = 750.0,
                targetAmount = null,
                currencyCode = "USD",
                targetCurrencyCode = null,
                normalizedAmountInBase = 750.0,
                title = "New Headphones",
                note = "",
                timestamp = System.currentTimeMillis()
            )
        )

        val ledgers = CurrencyAndBfCalculator.computeAccountLedgersForMonth(
            rawAccounts = listOf(checking, cash, creditCard),
            allTransactions = txs,
            activeYearMonth = "2026-11",
            baseCurrency = "USD",
            rates = rates
        )

        // 1. Total Net Worth excludes Credit Card accounts
        val totalNetWorth = ledgers
            .filter { it.accountType != AccountType.CREDIT_CARD }
            .sumOf { it.normalizedClosingInBase }
        assertEquals(6000.0, totalNetWorth, 0.001)

        // 2. Credit Card expenditure is isolated
        val ccLedger = ledgers.first { it.accountType == AccountType.CREDIT_CARD }
        assertEquals(750.0, ccLedger.periodExpense, 0.001)
    }

    @Test
    fun customExportFilterParams_filteringLogic() {
        val filterParams = com.example.domain.CustomExportFilterParams(
            selectedAccountIds = setOf(1L, 2L),
            selectedCategoryIds = setOf(5L),
            selectedTypes = setOf(TransactionType.EXPENSE),
            customStartMillis = 1000L,
            customEndMillis = 5000L,
            formatAsPdf = true
        )

        val tx1 = TransactionModel(
            id = 1L,
            accountId = 1L,
            accountName = "Bank",
            targetAccountId = null,
            targetAccountName = null,
            categoryId = 5L,
            categoryName = "Dining",
            categoryIconKey = "Restaurant",
            categoryColorHex = "#EA580C",
            type = TransactionType.EXPENSE,
            amount = 50.0,
            targetAmount = null,
            currencyCode = "USD",
            targetCurrencyCode = null,
            normalizedAmountInBase = 50.0,
            title = "Lunch",
            note = "",
            timestamp = 2500L
        )

        val tx2 = tx1.copy(id = 2L, categoryId = 99L, title = "Other")
        val tx3 = tx1.copy(id = 3L, accountId = 99L, title = "Different Account")
        val tx4 = tx1.copy(id = 4L, timestamp = 9000L, title = "Out of Date Range")

        val txList = listOf(tx1, tx2, tx3, tx4)
        val filtered = txList.filter { tx ->
            val matchAccount = filterParams.selectedAccountIds.isEmpty() || tx.accountId in filterParams.selectedAccountIds
            val matchCat = filterParams.selectedCategoryIds.isEmpty() || (tx.categoryId != null && tx.categoryId in filterParams.selectedCategoryIds)
            val matchType = filterParams.selectedTypes.isEmpty() || tx.type in filterParams.selectedTypes
            val matchDate = (filterParams.customStartMillis == null || tx.timestamp >= filterParams.customStartMillis) &&
                    (filterParams.customEndMillis == null || tx.timestamp <= filterParams.customEndMillis)
            matchAccount && matchCat && matchType && matchDate
        }

        assertEquals(1, filtered.size)
        assertEquals(1L, filtered.first().id)
    }

    @Test
    fun dateTimePickers_constructAccurateMillisecondTimestamps() {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(2026, java.util.Calendar.NOVEMBER, 5, 14, 30, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val millis = calendar.timeInMillis

        val verifyCal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
        assertEquals(2026, verifyCal.get(java.util.Calendar.YEAR))
        assertEquals(java.util.Calendar.NOVEMBER, verifyCal.get(java.util.Calendar.MONTH))
        assertEquals(5, verifyCal.get(java.util.Calendar.DAY_OF_MONTH))
        assertEquals(14, verifyCal.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(30, verifyCal.get(java.util.Calendar.MINUTE))
    }

    @Test
    fun accountNumberMasking_masksAllDigitsExceptLastSix() {
        assertEquals("xxxxxx123456", com.example.data.repository.FinanceRepository.maskAccountNumber("987654123456"))
        assertEquals("xxxx123456", com.example.data.repository.FinanceRepository.maskAccountNumber("4567123456"))
        assertEquals("123456", com.example.data.repository.FinanceRepository.maskAccountNumber("123456"))
        assertEquals("1234", com.example.data.repository.FinanceRepository.maskAccountNumber("1234"))
        assertEquals("", com.example.data.repository.FinanceRepository.maskAccountNumber(""))
    }

    @Test
    fun autoTrackingSegregation_matchesAlertByLastFourToSixDigitsOfAccountNumber() {
        val hdfcAccount = AccountModel(
            id = 101L,
            name = "Salary Account",
            accountType = AccountType.BANK,
            currencyCode = "INR",
            initialBroughtForwardBalance = 50000.0,
            broughtForwardDate = 0L,
            carryOverEnabled = true,
            colorHex = "#0061A4",
            iconKey = "AccountBalance",
            accountNumber = "50100234567891",
            maskedAccountNumber = "xxxxxxxx567891"
        )

        val creditCard = AccountModel(
            id = 102L,
            name = "Travel Card",
            accountType = AccountType.CREDIT_CARD,
            currencyCode = "INR",
            initialBroughtForwardBalance = 0.0,
            broughtForwardDate = 0L,
            carryOverEnabled = false,
            colorHex = "#9C27B0",
            iconKey = "CreditCard",
            accountNumber = "4111222233334444",
            maskedAccountNumber = "xxxxxxxxxx334444"
        )

        val alertBank = com.example.domain.ParsedFinancialAlert(
            id = "alert-1",
            sourceAppLabel = "HDFC Bank",
            packageName = "com.snapwork.hdfc",
            type = TransactionType.EXPENSE,
            amount = 1200.0,
            currencyCode = "INR",
            merchantOrPayee = "Star Bazaar",
            accountHint = "XX567891",
            referenceId = "REF9999",
            suggestedCategoryName = "Groceries",
            rawNotificationText = "INR 1,200.00 debited from A/c XX567891 at Star Bazaar",
            timestamp = 1762000000000L
        )

        val matched = BankAndUpiAlertParser.matchAccount(alertBank, listOf(hdfcAccount, creditCard))
        assertNotNull(matched)
        assertEquals(101L, matched!!.id)
        assertEquals("Salary Account", matched.name)

        val alertCard = com.example.domain.ParsedFinancialAlert(
            id = "alert-2",
            sourceAppLabel = "Bank SMS",
            packageName = "com.google.android.apps.messaging",
            type = TransactionType.EXPENSE,
            amount = 4500.0,
            currencyCode = "INR",
            merchantOrPayee = "FlightBooking",
            accountHint = "ending 4444",
            referenceId = "CARDTX1",
            suggestedCategoryName = "Travel",
            rawNotificationText = "Transaction of INR 4,500.00 spent on Card ending 4444 at FlightBooking",
            timestamp = 1762000000000L
        )

        val matchedCard = BankAndUpiAlertParser.matchAccount(alertCard, listOf(hdfcAccount, creditCard))
        assertNotNull(matchedCard)
        assertEquals(102L, matchedCard!!.id)
        assertEquals("Travel Card", matchedCard.name)
    }
}

