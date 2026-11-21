package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AccountType
import com.example.data.local.TransactionType
import com.example.data.security.CryptoManager
import com.example.domain.AccountModel
import com.example.domain.PeriodReconciliationSummary
import com.example.domain.TransactionModel
import com.example.util.BankAndUpiAlertParser
import com.example.util.CurrencyAndBfCalculator
import com.example.util.StatementExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExampleRobolectricTest {

    @Test
    fun `read app_name and verify CSV and PDF statement export feature My Money My Plan (MMMP)`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("My Money My Plan (MMMP)", appName)

        val summary = PeriodReconciliationSummary(
            yearMonth = "2026-11",
            displayMonthTitle = "November 2026",
            currencyCode = "USD",
            openingBroughtForward = 4250.0,
            periodIncome = 5400.0,
            periodExpense = 2100.0,
            periodNetTransfer = 0.0,
            periodBfAdjustments = 0.0,
            includeBroughtForwardInNet = true
        )
        val account = AccountModel(
            id = 1L,
            name = "Premier Checking",
            accountType = AccountType.BANK,
            currencyCode = "USD",
            initialBroughtForwardBalance = 3850.0,
            broughtForwardDate = 0L,
            carryOverEnabled = true,
            colorHex = "#0B6E4F",
            iconKey = "AccountBalance",
            periodOpeningBroughtForward = 4250.0,
            periodIncome = 5400.0,
            periodExpense = 2100.0,
            periodClosingCarriedForward = 7550.0,
            currentAllTimeBalance = 7550.0,
            normalizedClosingInBase = 7550.0
        )

        val csvResult = StatementExporter.exportCsvStatement(
            context = context,
            periodSummary = summary,
            accountLedgers = listOf(account),
            transactions = emptyList(),
            budgets = emptyList(),
            rates = emptyMap()
        )
        assertTrue(csvResult.file.exists())
        assertTrue(csvResult.fileName.contains("MMMP"))
        assertTrue(csvResult.previewSummary.contains("My Money My Plan (MMMP)"))
        val csvContent = csvResult.file.readText()
        assertTrue(csvContent.contains("My Money My Plan (MMMP)"))
        assertTrue(csvContent.contains("MY MONEY MY PLAN (MMMP)"))
        assertTrue(csvContent.contains("Opening Brought Forward (B/F)"))
        assertTrue(csvContent.contains("Closing Carried Forward (C/F)"))

        val pdfResult = StatementExporter.exportPdfStatement(
            context = context,
            periodSummary = summary,
            accountLedgers = listOf(account),
            transactions = emptyList(),
            budgets = emptyList(),
            rates = emptyMap()
        )
        assertTrue(pdfResult.file.exists())
        assertTrue(pdfResult.file.length() > 100)
        assertTrue(pdfResult.fileName.contains("MMMP"))
        assertTrue(pdfResult.previewSummary.contains("My Money My Plan (MMMP)"))
    }

    @Test
    fun `test strict debit credit filter rejects offers promos rewards and otps`() {
        BankAndUpiAlertParser.clearDuplicateHistory()
        val now = System.currentTimeMillis()

        val promoOffer = BankAndUpiAlertParser.parseNotification(
            packageName = "com.phonepe.app",
            title = "PhonePe Cashback Offer!",
            body = "Get flat Rs. 150 cashback on your next recharge. Limited time offer!",
            timestamp = now
        )
        assertNull(promoOffer)

        val superCoins = BankAndUpiAlertParser.parseNotification(
            packageName = "com.paytm",
            title = "Paytm Rewards",
            body = "100 SuperCoins credited to your reward points balance. Claim now!",
            timestamp = now
        )
        assertNull(superCoins)

        val scratchCard = BankAndUpiAlertParser.parseNotification(
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Google Pay",
            body = "You won a scratch card worth Rs. 500! Spin & win exclusive deals.",
            timestamp = now
        )
        assertNull(scratchCard)

        val otpAlert = BankAndUpiAlertParser.parseNotification(
            packageName = "com.android.messaging",
            title = "HDFC Bank",
            body = "OTP 482910 for txn of INR 2,500.00 on Card XX1234. Do not share.",
            timestamp = now
        )
        assertNull(otpAlert)

        val collectRequest = BankAndUpiAlertParser.parseNotification(
            packageName = "com.phonepe.app",
            title = "PhonePe",
            body = "Rahul has requested Rs. 750.00. Approve to pay via UPI.",
            timestamp = now
        )
        assertNull(collectRequest)

        val failedTxn = BankAndUpiAlertParser.parseNotification(
            packageName = "com.sbi.lotusintouch",
            title = "SBI Alert",
            body = "Transaction of Rs. 999.00 on A/c XX1234 failed due to network error.",
            timestamp = now
        )
        assertNull(failedTxn)
    }

    @Test
    fun `test exact debit credit amount extraction ignores available balance and matches ac strictly`() {
        BankAndUpiAlertParser.clearDuplicateHistory()
        val now = System.currentTimeMillis()

        // Available balance appears BEFORE and AFTER actual debited amount
        val debitWithBalance = BankAndUpiAlertParser.parseNotification(
            packageName = "com.android.messaging",
            title = "HDFC Bank",
            body = "Avl Bal: INR 45,200.00. A/c XX4321 debited for Rs. 850.50 at Swiggy via UPI Ref 512345678901.",
            timestamp = now
        )
        assertNotNull(debitWithBalance)
        assertEquals(TransactionType.EXPENSE, debitWithBalance!!.type)
        assertEquals(850.50, debitWithBalance.amount, 0.01)
        assertEquals("INR", debitWithBalance.currencyCode)
        assertEquals("512345678901", debitWithBalance.referenceId)

        val creditWithBalance = BankAndUpiAlertParser.parseNotification(
            packageName = "com.android.messaging",
            title = "ICICI Bank",
            body = "Dear Customer, INR 12,500.00 has been credited to your A/c XX9876 on 18-Nov. Clear Bal: INR 88,450.25.",
            timestamp = now + 1000L
        )
        assertNotNull(creditWithBalance)
        assertEquals(TransactionType.INCOME, creditWithBalance!!.type)
        assertEquals(12500.00, creditWithBalance.amount, 0.01)

        val hdfcAccount = AccountModel(
            id = 10L,
            name = "HDFC Salary",
            accountType = AccountType.BANK,
            currencyCode = "INR",
            initialBroughtForwardBalance = 0.0,
            broughtForwardDate = 0L,
            carryOverEnabled = true,
            colorHex = "#0061A4",
            iconKey = "AccountBalance",
            accountNumber = "50100012344321",
            maskedAccountNumber = "XX4321"
        )
        val iciciAccount = AccountModel(
            id = 11L,
            name = "ICICI Savings",
            accountType = AccountType.SAVINGS,
            currencyCode = "INR",
            initialBroughtForwardBalance = 0.0,
            broughtForwardDate = 0L,
            carryOverEnabled = true,
            colorHex = "#006C4C",
            iconKey = "Savings",
            accountNumber = "000201559876",
            maskedAccountNumber = "XX9876"
        )
        val accounts = listOf(hdfcAccount, iciciAccount)

        assertEquals(10L, BankAndUpiAlertParser.matchAccountStrict(debitWithBalance, accounts)?.id)
        assertEquals(11L, BankAndUpiAlertParser.matchAccountStrict(creditWithBalance, accounts)?.id)

        // Alert without account digits must return null in strict matching (held in Pending Review)
        val noDigitsAlert = BankAndUpiAlertParser.parseNotification(
            packageName = "com.google.android.apps.wallet",
            title = "Google Pay",
            body = "Paid Amt 1,250.00 to Coffee Shop successfully.",
            timestamp = now + 2000L
        )
        assertNotNull(noDigitsAlert)
        assertEquals(1250.0, noDigitsAlert!!.amount, 0.01)
        assertNull(BankAndUpiAlertParser.matchAccountStrict(noDigitsAlert, accounts))

        // Alert for unknown account digits must also return null in strict matching
        val unknownAcctAlert = BankAndUpiAlertParser.parseNotification(
            packageName = "com.sbi.lotusintouch",
            title = "SBI Alert",
            body = "A/c *7777 debited by Rs. 300.00 towards Metro Card",
            timestamp = now + 3000L
        )
        assertNotNull(unknownAcctAlert)
        assertNull(BankAndUpiAlertParser.matchAccountStrict(unknownAcctAlert!!, accounts))
    }

    @Test
    fun `test smart duplicate prevention across same account vs distinct accounts`() {
        BankAndUpiAlertParser.clearDuplicateHistory()
        val now = System.currentTimeMillis()

        // Concurrent UPI + Bank SMS for same payment (one has no digits, one has *1234) -> deduplicated
        val alertApp = BankAndUpiAlertParser.parseAndDeduplicate(
            packageName = "com.phonepe.app",
            title = "PhonePe",
            body = "Paid Rs. 500.00 to Grocery Mart",
            timestamp = now
        )
        assertNotNull(alertApp)
        assertEquals(500.0, alertApp!!.amount, 0.01)

        val alertSms = BankAndUpiAlertParser.parseAndDeduplicate(
            packageName = "com.sbi.lotusintouch",
            title = "SBI Alert",
            body = "A/c *1234 debited by Rs. 500.00 towards Grocery Mart",
            timestamp = now + 10_000L
        )
        assertNull(alertSms)

        // Concurrent Bank SMS for a DIFFERENT account (*5678) with same amount -> NOT falsely deduplicated
        val alertOtherAccount = BankAndUpiAlertParser.parseAndDeduplicate(
            packageName = "com.hdfc.mobile",
            title = "HDFC Alert",
            body = "A/c XX5678 debited by Rs. 500.00 towards Grocery Mart",
            timestamp = now + 15_000L
        )
        assertNotNull(alertOtherAccount)
        assertEquals(500.0, alertOtherAccount!!.amount, 0.01)
    }

    @Test
    fun `test single pass ledgers six month trend and crypto cache lifecycle`() {
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
        val noCarryWallet = AccountModel(
            id = 2L,
            name = "Cash",
            accountType = AccountType.CASH,
            currencyCode = "USD",
            initialBroughtForwardBalance = 200.0,
            broughtForwardDate = 0L,
            carryOverEnabled = false,
            colorHex = "#1A73E8",
            iconKey = "Wallet"
        )

        val (octStart, _) = CurrencyAndBfCalculator.monthBounds("2026-10")
        val (novStart, novEnd) = CurrencyAndBfCalculator.monthBounds("2026-11")

        val txs = listOf(
            TransactionModel(
                id = 1L, accountId = 1L, accountName = "Checking",
                targetAccountId = null, targetAccountName = null, categoryId = 1L,
                categoryName = "Salary", categoryIconKey = "Work", categoryColorHex = "#1E8E3E",
                type = TransactionType.INCOME, amount = 500.0, targetAmount = null,
                currencyCode = "USD", targetCurrencyCode = null, normalizedAmountInBase = 500.0,
                title = "Oct Bonus", note = "", timestamp = octStart + 1000L
            ),
            TransactionModel(
                id = 2L, accountId = 1L, accountName = "Checking",
                targetAccountId = null, targetAccountName = null, categoryId = 2L,
                categoryName = "Food", categoryIconKey = "Restaurant", categoryColorHex = "#E37400",
                type = TransactionType.EXPENSE, amount = 150.0, targetAmount = null,
                currencyCode = "USD", targetCurrencyCode = null, normalizedAmountInBase = 150.0,
                title = "Nov Dinner", note = "", timestamp = novStart + 1000L
            ),
            TransactionModel(
                id = 3L, accountId = 2L, accountName = "Cash",
                targetAccountId = null, targetAccountName = null, categoryId = 2L,
                categoryName = "Food", categoryIconKey = "Restaurant", categoryColorHex = "#E37400",
                type = TransactionType.EXPENSE, amount = 40.0, targetAmount = null,
                currencyCode = "USD", targetCurrencyCode = null, normalizedAmountInBase = 40.0,
                title = "Nov Coffee", note = "", timestamp = novStart + 2000L
            )
        )

        val ledgers = CurrencyAndBfCalculator.computeAccountLedgersForBounds(
            rawAccounts = listOf(checking, noCarryWallet),
            allTransactions = txs,
            periodStart = novStart,
            periodEnd = novEnd,
            baseCurrency = "USD",
            rates = emptyMap()
        )
        assertEquals(2, ledgers.size)
        // Checking: initial 1000 + Oct 500 = 1500 opening B/F; -150 expense = 1350 closing C/F
        assertEquals(1500.0, ledgers[0].periodOpeningBroughtForward, 0.01)
        assertEquals(1350.0, ledgers[0].periodClosingCarriedForward, 0.01)
        // Cash (carryOverEnabled = false): 0 opening B/F; -40 closing C/F for period
        assertEquals(0.0, ledgers[1].periodOpeningBroughtForward, 0.01)
        assertEquals(-40.0, ledgers[1].periodClosingCarriedForward, 0.01)

        val trend = CurrencyAndBfCalculator.computeSixMonthTrend(
            rawAccounts = listOf(checking, noCarryWallet),
            allTransactions = txs,
            endYearMonth = "2026-11",
            baseCurrency = "USD",
            rates = emptyMap()
        )
        assertEquals(6, trend.size)
        val octPoint = trend[4]
        val novPoint = trend[5]
        assertEquals("2026-10", octPoint.yearMonth)
        assertEquals(1000.0, octPoint.openingBroughtForward, 0.01)
        assertEquals(500.0, octPoint.incomeInBase, 0.01)
        assertEquals(1500.0, octPoint.closingCarriedForward, 0.01)
        assertEquals("2026-11", novPoint.yearMonth)
        assertEquals(1500.0, novPoint.openingBroughtForward, 0.01)
        assertEquals(190.0, novPoint.expenseInBase, 0.01)
        assertEquals(1310.0, novPoint.closingCarriedForward, 0.01)

        // Verify CryptoManager encrypt/decrypt continues working cleanly before and after cache clear
        val cipher = CryptoManager.encrypt("Sensitive Ledger Note")
        assertEquals("Sensitive Ledger Note", CryptoManager.decrypt(cipher))
        CryptoManager.clearSessionKeyCache()
        assertEquals("Sensitive Ledger Note", CryptoManager.decrypt(cipher))
    }
}


