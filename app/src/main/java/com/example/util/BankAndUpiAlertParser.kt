package com.example.util

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.example.data.local.TransactionType
import com.example.domain.AccountModel
import com.example.domain.AutoTrackSource
import com.example.domain.CategoryModel
import com.example.domain.ParsedFinancialAlert
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

/**
 * On-device financial regex & keyword parser and 2-minute duplicate filter for
 * strict Debited & Credited alerts from UPI apps, Banking apps, and Bank SMS notifications.
 *
 * Strictly rejects offers, promotions, cashback ads, OTPs, and reminders,
 * masks available balance clauses to extract the exact transaction amount,
 * and supports Account-Wise (last 4–6 digits) matching with Pending Review fallback.
 */
object BankAndUpiAlertParser {

    const val DEDUP_WINDOW_MS = 2 * 60 * 1000L // 2 minutes smart duplicate filter window

    private data class RecentAlertSignature(
        val type: TransactionType,
        val amount: Double,
        val currencyCode: String,
        val referenceId: String?,
        val accountDigits: Set<String>,
        val timestamp: Long
    )

    private val recentSignatures = mutableListOf<RecentAlertSignature>()

    @Synchronized
    fun clearDuplicateHistory() {
        recentSignatures.clear()
    }

    /**
     * Checks whether Android Notification Listener access is granted to My Money My Plan (MMMP).
     */
    fun isNotificationListenerEnabled(context: Context): Boolean {
        return try {
            val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
            if (enabledPackages.contains(context.packageName)) {
                return true
            }
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            flat?.contains(context.packageName) == true
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Launches Android's system Notification Access settings screen.
     */
    fun openNotificationListenerSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Throwable) {
            try {
                val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            } catch (_: Throwable) {
                // Ignored in headless / non-activity contexts
            }
        }
    }

    /**
     * Resolves a human-friendly source app label from package name and notification title.
     */
    fun resolveSourceAppLabel(packageName: String, title: String, body: String): String {
        val pkg = packageName.lowercase(Locale.US)
        val combined = "$title $body".lowercase(Locale.US)
        return when {
            pkg.contains("super") || pkg.contains("money.super") || combined.contains("super.money") || combined.contains("super money") -> "Super Money"
            pkg.contains("nbu.paisa") || pkg.contains("google.android.apps.wallet") || combined.contains("google pay") || combined.contains("gpay") -> "Google Pay"
            pkg.contains("phonepe") || combined.contains("phonepe") -> "PhonePe"
            pkg.contains("paytm") || pkg.contains("one97") || combined.contains("paytm") -> "Paytm"
            pkg.contains("npci") || pkg.contains("bhim") || combined.contains("bhim") -> "BHIM UPI"
            pkg.contains("dreamplug") || pkg.contains("cred") || combined.contains("cred") -> "CRED"
            pkg.contains("amazon") || combined.contains("amazon pay") -> "Amazon Pay"
            pkg.contains("navi") || combined.contains("navi upi") -> "Navi UPI"
            pkg.contains("whatsapp") -> "WhatsApp Pay"
            pkg.contains("mobikwik") -> "MobiKwik"
            pkg.contains("jupiter") -> "Jupiter"
            pkg.contains("slice") -> "Slice"
            pkg.contains("messaging") || pkg.contains("mms") || pkg.contains("sms") -> "Bank SMS"
            else -> "Banking App"
        }
    }

    /**
     * Maps a resolved source app label or package name to a canonical [AutoTrackSource] filter chip.
     */
    fun classifyAlertSource(packageName: String, sourceAppLabel: String): AutoTrackSource {
        val label = sourceAppLabel.lowercase(Locale.US)
        val pkg = packageName.lowercase(Locale.US)
        return when {
            label.contains("super money") || pkg.contains("money.super") -> AutoTrackSource.SUPER_MONEY
            label.contains("google pay") || pkg.contains("nbu.paisa") -> AutoTrackSource.GOOGLE_PAY
            label.contains("phonepe") || pkg.contains("phonepe") -> AutoTrackSource.PHONEPE
            label.contains("paytm") || pkg.contains("paytm") || pkg.contains("one97") -> AutoTrackSource.PAYTM
            label.contains("bhim") || pkg.contains("npci") || pkg.contains("bhim") -> AutoTrackSource.BHIM
            label.contains("cred") || pkg.contains("dreamplug") -> AutoTrackSource.CRED
            label.contains("amazon") || pkg.contains("amazon") -> AutoTrackSource.AMAZON_PAY
            label.contains("whatsapp") || pkg.contains("whatsapp") -> AutoTrackSource.WHATSAPP_PAY
            label.contains("sms") || pkg.contains("messaging") || pkg.contains("mms") -> AutoTrackSource.BANK_SMS
            else -> AutoTrackSource.BANKING_APPS
        }
    }

    private val trustedPackageKeywords = listOf(
        "super", "money.super", "nbu.paisa", "google.android.apps.wallet", "phonepe",
        "paytm", "one97", "npci", "bhim", "dreamplug", "cred", "amazon", "navi",
        "whatsapp", "mobikwik", "jupiter", "slice", "freecharge", "bharatpe", "razorpay",
        "messaging", "mms", "sms", "truecaller", "bank", "sbi", "hdfc", "icici", "axis",
        "kotak", "pnb", "bob", "idfc", "indusind", "yesbank", "canara", "unionbank",
        "federal", "chase", "citi", "hsbc", "barclays", "revolut", "wise", "amex",
        "yono", "imobile", "payzapp"
    )

    private val nonTransactionalIgnoreKeywords = listOf(
        "otp", "one time password", "verification code", "is due", "will be debited",
        "apply now", "pre-approved", "loan offer", "reminder:", "bill is generated",
        "minimum amount due", "statement is ready", "emi due", "auto-debit scheduled",
        // Promotional, marketing, reward, and offer keywords
        "offer", "promo", "promotion", "discount", "coupon", "voucher", "deal",
        "cashback offer", "get cashback", "win cashback", "upto", "up to", "flat ",
        "scratch card", "reward points", "rewards", "supercoins", "loyalty points",
        "coins credited", "points credited", "spin & win", "spin and win",
        "eligible for", "claim now", "claim your", "hurry", "limited time", "exclusive",
        "expires", "expiring", "valid till", "valid until", "use code", "promo code",
        "refer & earn", "refer and earn", "invite friends", "credit limit increase",
        "missed call", "failed", "declined", "unsuccessful", "reversed",
        "collect request", "payment request", "has requested", "approve to pay", "mandate"
    )

    private val promoActionRegexes = listOf(
        Regex("""(?i)\b(?:win|earn|get|grab|avail|enjoy|save|claim)\s+(?:up\s*to\s*)?(?:INR|Rs\.?|₹|USD|\$|EUR|€|GBP|£)?\s*[0-9]+"""),
        Regex("""(?i)\b(?:cashback|bonus|discount|off|reward)\s+(?:of|worth|up\s*to)\b"""),
        Regex("""(?i)\b[0-9]{1,3}\s*%\s*off\b"""),
        Regex("""(?i)\bworth\s+(?:INR|Rs\.?|₹|USD|\$|EUR|€|GBP|£)\s*[0-9]+""")
    )

    // Strict Debit action phrases (real financial outflows only)
    private val strictDebitActionRegex = Regex(
        """(?i)(?:\b(?:is|has\s+been|was|got)?\s*debited\b|\bpaid\s+(?:to|for|via|through|INR|Rs\.?|₹|USD|\$|EUR|€|GBP|£|amt|amount|[0-9])|(?:INR|Rs\.?|₹|USD|\$|EUR|€|GBP|£)\s*[0-9][0-9,]*(?:\.[0-9]{1,2})?\s*(?:/-)?\s*(?:is|has\s+been|was)?\s*paid\b|\bsent\s+(?:to|via|INR|Rs\.?|₹|USD|\$|EUR|€|GBP|£|[0-9])|\bspent\s+(?:on|at|via|INR|Rs\.?|₹|USD|\$|EUR|€|GBP|£|[0-9])|\bwithdrawn\b|\bdeducted\b|\bcharged\s+(?:to|on|for|with|INR|Rs\.?|₹|USD|\$|EUR|€|GBP|£|[0-9])|\bpayment\s+of\s+(?:INR|Rs\.?|₹|USD|\$|EUR|€|GBP|£|[0-9])|\bdr\.?\s+(?:a/c|acct|from)\b)"""
    )

    // Strict Credit action phrases (real financial inflows only)
    private val strictCreditActionRegex = Regex(
        """(?i)(?:\b(?:is|has\s+been|was|got)?\s*credited\b|\breceived\s+(?:from|via|in|to|into|INR|Rs\.?|₹|USD|\$|EUR|€|GBP|£|[0-9])|(?:INR|Rs\.?|₹|USD|\$|EUR|€|GBP|£)\s*[0-9][0-9,]*(?:\.[0-9]{1,2})?\s*(?:/-)?\s*(?:is|has\s+been|was)?\s*received\b|\bmoney\s+received\b|\bdeposited\s+(?:in|to|into|via|by|INR|Rs\.?|₹|USD|\$|EUR|€|GBP|£|[0-9])|\brefund(?:ed)?\s+(?:of|for|to|into|INR|Rs\.?|₹|USD|\$|EUR|€|GBP|£|[0-9])|\bsalary\s+(?:of|credited|deposited)\b|\badded\s+to\s+your\s+(?:a/c|acct|account|bank|wallet)\b|\bcr\.?\s+(?:to|a/c|acct)\b)"""
    )

    // Strips available/current balance or limit clauses so they are never mistaken for transaction amount
    private val balanceClauseRegex = Regex(
        """(?i)\b(?:avl\.?|avail\.?|available|clr\.?|clear|curr\.?|current|eff\.?|effective|closing|opening|total|net|updated|remaining|rem\.?)?\s*(?:bal(?:ance)?|lmt|limit)\b\s*(?:is|of|:|\-|as\s+on)?\s*(?:INR|Rs\.?|₹|USD|\$|EUR|€|GBP|£|AED|SGD|CAD|AUD|JPY)?\s*[0-9][0-9,]*(?:\.[0-9]{1,2})?(?:\s*/-)?(?:\s*(?:INR|Rs\.?|₹|USD|\$|EUR|€|GBP|£|AED|SGD|CAD|AUD|JPY))?"""
    )

    // Matches explicit currency + amount directly adjacent to a debit/credit verb
    private val verbAdjacentAmountRegex = Regex(
        """(?i)(?:(?:debited|credited|paid|spent|sent|received|withdrawn|deducted|deposited|charged|refunded)\s+(?:by|for|with|of|to|amt|amount)?\s*(?:(INR|Rs\.?|₹|USD|\$|EUR|€|GBP|£|AED|SGD|CAD|AUD|JPY)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?))|(?:(INR|Rs\.?|₹|USD|\$|EUR|€|GBP|£|AED|SGD|CAD|AUD|JPY)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s*(?:/-)?\s*(?:has\s+been|is|was)?\s*(?:debited|credited|paid|spent|sent|received|withdrawn|deducted|deposited|charged|refunded)))"""
    )

    // Matches currency + amount patterns like "Rs. 1,250.50", "INR 450", "₹899.00", "$45.20", "USD 120.00", "EUR 85", "GBP 30"
    private val currencyAmountRegex = Regex(
        """(?i)(?:(INR|Rs\.?|₹|USD|\$|EUR|€|GBP|£|AED|SGD|CAD|AUD|JPY)\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)(?:\s*/-)?)|(?:([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)(?:\s*/-)?\s*(INR|Rs\.?|₹|USD|\$|EUR|€|GBP|£|AED|SGD|CAD|AUD|JPY))"""
    )

    // Extracts account masks like "A/c XX1234", "Acct *1234", "ending 5678", "Card XX9012", or standalone "XX1234"
    private val accountMaskRegex = Regex(
        """(?i)(?:(?:a/c|ac|acct|account|card)\s*(?:no\.?|num\.?|number|ending(?:\s+(?:in|with))?|with|from|to)?\s*[:\-]?\s*([xX*#.\-]*[0-9]{4,6}))|(?:\b(?:[xX*]{2,}[.\-]*|\*{2,})([0-9]{4,6})\b)"""
    )

    // Extracts UPI Ref / UTR / Reference numbers (6-16 alphanumeric/digits)
    private val upiRefRegex = Regex(
        """(?i)(?:upi\s*ref(?:\s*no\.?)?|ref(?:\s*no\.?)?|utr|txn(?:\s*id)?)\s*[:\-]?\s*([0-9A-Za-z]{6,16})"""
    )

    // Extracts merchant/payee after "to", "at", "from", "towards"
    private val merchantRegex = Regex(
        """(?i)\b(?:paid\s+to|sent\s+to|transferred\s+to|to|at|from|towards)\s+([A-Za-z0-9@._&\-\s]{2,30}?)(?:\s+(?:on|via|using|ref|upi|from|a/c|acct|for|at|\.|,|$))"""
    )

    private val knownBankKeywords = listOf(
        "HDFC", "SBI", "ICICI", "Axis", "Kotak", "PNB", "Bank of Baroda",
        "IDFC", "IndusInd", "Yes Bank", "Canara", "Union Bank", "Federal Bank",
        "Chase", "Citibank", "Citi", "HSBC", "Barclays", "Revolut", "Wise", "Premier"
    )

    private fun isTrustedSource(packageName: String, fullText: String): Boolean {
        val pkg = packageName.lowercase(Locale.US)
        if (pkg.isNotBlank() && trustedPackageKeywords.any { pkg.contains(it) }) {
            return true
        }
        // If package is empty (e.g., direct parser calls) or an unlisted banking app,
        // strictly require an explicit bank/account/UPI marker in the message text.
        val lower = fullText.lowercase(Locale.US)
        return lower.contains("a/c") ||
            lower.contains("acct") ||
            lower.contains("account") ||
            lower.contains("card") ||
            lower.contains("upi") ||
            lower.contains("utr") ||
            knownBankKeywords.any { lower.contains(it.lowercase(Locale.US)) }
    }

    /**
     * Parses a notification title & text into a [ParsedFinancialAlert], or returns null
     * if the notification is promotional, non-transactional, or not a strict Debit/Credit transaction.
     */
    fun parseNotification(
        packageName: String,
        title: String,
        body: String,
        defaultCurrency: String = "INR",
        timestamp: Long = System.currentTimeMillis()
    ): ParsedFinancialAlert? {
        val fullText = "$title. $body".trim()
        if (fullText.length < 8) return null

        if (!isTrustedSource(packageName, fullText)) return null

        val lower = fullText.lowercase(Locale.US)
        if (nonTransactionalIgnoreKeywords.any { lower.contains(it) }) {
            return null
        }
        if (promoActionRegexes.any { it.containsMatchIn(fullText) }) {
            return null
        }

        val isDebit = strictDebitActionRegex.containsMatchIn(fullText)
        val isCredit = strictCreditActionRegex.containsMatchIn(fullText)

        if (!isDebit && !isCredit) return null

        // Determine transaction type (prioritize explicit debit/paid if both appear like "debited from X and credited to Y")
        val txType = when {
            lower.contains("debited") || lower.contains("paid to") || lower.contains("sent to") || lower.contains("spent") -> TransactionType.EXPENSE
            lower.contains("credited to your") || lower.contains("received from") || lower.contains("money received") || lower.contains("refund") -> TransactionType.INCOME
            isDebit -> TransactionType.EXPENSE
            else -> TransactionType.INCOME
        }

        // Extract exact transaction amount and currency (excluding balance clauses, refs, and account numbers)
        val parsedAmountCurrency = extractAmountAndCurrency(fullText, defaultCurrency) ?: return null
        val amount = parsedAmountCurrency.first
        val currencyCode = parsedAmountCurrency.second
        if (amount <= 0.0) return null

        val sourceAppLabel = resolveSourceAppLabel(packageName, title, body)

        // Extract reference ID if present
        val referenceId = upiRefRegex.find(fullText)?.groups?.get(1)?.value?.trim()

        // Extract account hint (last digits and/or bank name)
        val maskDigits = accountMaskRegex.findAll(fullText)
            .mapNotNull { m -> (m.groups[1]?.value ?: m.groups[2]?.value)?.trim() }
            .firstOrNull()
        val detectedBank = knownBankKeywords.firstOrNull { bank ->
            fullText.contains(bank, ignoreCase = true)
        }
        val accountHint = listOfNotNull(detectedBank, maskDigits)
            .joinToString(" ")
            .takeIf { it.isNotBlank() }

        // Extract merchant or payee
        val extractedMerchant = merchantRegex.find(fullText)?.groups?.get(1)?.value
            ?.trim()
            ?.trim('.', ',', '-', ':')
            ?.takeIf { it.length >= 2 && !it.equals("your", ignoreCase = true) && !it.equals("a/c", ignoreCase = true) }

        val merchantOrPayee = extractedMerchant
            ?: title.takeIf { it.isNotBlank() && !it.equals(sourceAppLabel, ignoreCase = true) }
            ?: if (txType == TransactionType.EXPENSE) "$sourceAppLabel Payment" else "$sourceAppLabel Credit"

        val suggestedCategory = inferCategoryName(merchantOrPayee, fullText, txType)

        return ParsedFinancialAlert(
            id = UUID.randomUUID().toString(),
            sourceAppLabel = sourceAppLabel,
            packageName = packageName,
            type = txType,
            amount = amount,
            currencyCode = currencyCode,
            merchantOrPayee = merchantOrPayee,
            accountHint = accountHint,
            referenceId = referenceId,
            suggestedCategoryName = suggestedCategory,
            rawNotificationText = body.ifBlank { fullText },
            timestamp = timestamp
        )
    }

    /**
     * Parses a notification AND checks the 2-minute smart duplicate filter so that when both
     * a UPI app (e.g. Super Money, GPay, PhonePe) and a Bank SMS fire for the same payment
     * within 2 minutes, only the first alert is accepted.
     */
    @Synchronized
    fun parseAndDeduplicate(
        packageName: String,
        title: String,
        body: String,
        defaultCurrency: String = "INR",
        timestamp: Long = System.currentTimeMillis()
    ): ParsedFinancialAlert? {
        val parsed = parseNotification(
            packageName = packageName,
            title = title,
            body = body,
            defaultCurrency = defaultCurrency,
            timestamp = timestamp
        ) ?: return null

        val currentDigits = extractAccountSuffixTokens(parsed.accountHint.orEmpty(), parsed.rawNotificationText)

        // Prune signatures older than 2 minutes
        recentSignatures.removeAll { abs(timestamp - it.timestamp) > DEDUP_WINDOW_MS }

        val matchingIndex = recentSignatures.indexOfFirst { existing ->
            val withinWindow = abs(timestamp - existing.timestamp) <= DEDUP_WINDOW_MS
            val sameAmount = abs(existing.amount - parsed.amount) < 0.01
            val sameType = existing.type == parsed.type
            val refMatches = if (!existing.referenceId.isNullOrBlank() && !parsed.referenceId.isNullOrBlank()) {
                existing.referenceId.equals(parsed.referenceId, ignoreCase = true)
            } else {
                true
            }
            val accountCompatible = if (existing.accountDigits.isNotEmpty() && currentDigits.isNotEmpty()) {
                existing.accountDigits.any { d1 ->
                    currentDigits.any { d2 ->
                        d1.endsWith(d2.takeLast(4)) || d2.endsWith(d1.takeLast(4))
                    }
                }
            } else {
                true
            }
            withinWindow && sameAmount && sameType && refMatches && accountCompatible
        }

        if (matchingIndex >= 0) {
            val existing = recentSignatures[matchingIndex]
            if (existing.accountDigits.isEmpty() && currentDigits.isNotEmpty()) {
                recentSignatures[matchingIndex] = existing.copy(accountDigits = currentDigits)
            }
            return null
        }

        recentSignatures.add(
            RecentAlertSignature(
                type = parsed.type,
                amount = parsed.amount,
                currencyCode = parsed.currencyCode,
                referenceId = parsed.referenceId,
                accountDigits = currentDigits,
                timestamp = timestamp
            )
        )
        return parsed
    }

    /**
     * Extracts 4-to-6 digit account/card suffix tokens specifically from account/card masks
     * while ignoring reference IDs, UTRs, dates, and currency amounts.
     */
    fun extractAccountSuffixTokens(hint: String, rawNotification: String): Set<String> {
        val combined = "$hint $rawNotification"
        val cleaned = upiRefRegex.replace(combined, " ")
        val tokens = mutableSetOf<String>()

        accountMaskRegex.findAll(cleaned).forEach { match ->
            val raw = match.groups[1]?.value ?: match.groups[2]?.value ?: ""
            val digits = raw.filter { it.isDigit() }
            if (digits.length in 4..6) {
                tokens.add(digits)
            }
        }
        return tokens
    }

    /**
     * Strictly matches a parsed alert to a user account by its last 4 to 6 digits.
     * Returns null if no account/card digits are found or if none of the user's configured
     * accounts match those digits (so the alert can be held in Pending Review).
     */
    fun matchAccountStrict(
        alert: ParsedFinancialAlert,
        accounts: List<AccountModel>
    ): AccountModel? {
        if (accounts.isEmpty()) return null

        val numericTokens = extractAccountSuffixTokens(
            hint = alert.accountHint.orEmpty(),
            rawNotification = alert.rawNotificationText
        )

        if (numericTokens.isEmpty()) {
            return null
        }

        for (token in numericTokens) {
            val matched = accounts.firstOrNull { acc ->
                val cleanAcct = acc.accountNumber.filter { it.isDigit() }
                val cleanMask = acc.maskedAccountNumber.filter { it.isDigit() }
                (cleanAcct.length >= 4 && (cleanAcct.endsWith(token) || token.endsWith(cleanAcct.takeLast(4)))) ||
                    (cleanMask.length >= 4 && (cleanMask.endsWith(token) || token.endsWith(cleanMask.takeLast(4))))
            }
            if (matched != null) return matched
        }

        return null
    }

    /**
     * Matches a parsed alert to the best-fitting user account in My Money My Plan (MMMP),
     * prioritizing strict 4–6 digit matching and falling back to name/currency hints for manual edit pre-selection.
     */
    fun matchAccount(
        alert: ParsedFinancialAlert,
        accounts: List<AccountModel>
    ): AccountModel? {
        if (accounts.isEmpty()) return null

        // 1. Strict matching by Account / Card number suffix (last 4 to 6 digits)
        matchAccountStrict(alert, accounts)?.let { return it }

        val hint = alert.accountHint.orEmpty()

        // 2. Match by account name keywords in hint
        val hintLower = hint.lowercase(Locale.US)
        if (hintLower.isNotBlank()) {
            val tokens = hintLower.split(" ", "-", "*", "x", "X").filter { it.length >= 3 }
            val byHint = accounts.firstOrNull { acc ->
                val accLower = acc.name.lowercase(Locale.US)
                tokens.any { token -> accLower.contains(token) }
            }
            if (byHint != null) return byHint
        }

        // 3. Fallback: match by currency code
        val byCurrency = accounts.firstOrNull {
            it.currencyCode.equals(alert.currencyCode, ignoreCase = true)
        }
        if (byCurrency != null) return byCurrency

        // 4. Default to first account
        return accounts.first()
    }

    /**
     * Matches the inferred category name to the user's configured CategoryModel list.
     */
    fun matchCategory(
        alert: ParsedFinancialAlert,
        categories: List<CategoryModel>
    ): CategoryModel? {
        val sameTypeCategories = categories.filter { it.type == alert.type }
        if (sameTypeCategories.isEmpty()) return categories.firstOrNull()

        return sameTypeCategories.firstOrNull {
            it.name.equals(alert.suggestedCategoryName, ignoreCase = true) ||
                it.name.contains(alert.suggestedCategoryName.take(5), ignoreCase = true)
        } ?: sameTypeCategories.first()
    }

    private fun extractAmountAndCurrency(fullText: String, defaultCurrency: String): Pair<Double, String>? {
        // Mask out balance clauses, reference IDs, and account suffixes first
        var sanitized = balanceClauseRegex.replace(fullText, " ")
        sanitized = upiRefRegex.replace(sanitized, " ")
        sanitized = accountMaskRegex.replace(sanitized, " ")

        // 1. Priority 1: Currency + amount directly adjacent to the debit/credit verb
        val verbMatch = verbAdjacentAmountRegex.find(sanitized)
        if (verbMatch != null) {
            val rawCurrencyToken = (verbMatch.groups[1]?.value ?: verbMatch.groups[3]?.value ?: defaultCurrency).trim()
            val rawAmountToken = (verbMatch.groups[2]?.value ?: verbMatch.groups[4]?.value ?: "").replace(",", "").trim()
            val amount = rawAmountToken.toDoubleOrNull()
            if (amount != null && amount > 0.0) {
                return Pair(amount, normalizeCurrencyToken(rawCurrencyToken, defaultCurrency))
            }
        }

        // 2. Priority 2: Standard currency + amount regex on sanitized text
        val match = currencyAmountRegex.find(sanitized)
        if (match != null) {
            val rawCurrencyToken = (match.groups[1]?.value ?: match.groups[4]?.value ?: defaultCurrency).trim()
            val rawAmountToken = (match.groups[2]?.value ?: match.groups[3]?.value ?: "").replace(",", "").trim()
            val amount = rawAmountToken.toDoubleOrNull()
            if (amount != null && amount > 0.0) {
                return Pair(amount, normalizeCurrencyToken(rawCurrencyToken, defaultCurrency))
            }
        }

        // 3. Priority 3: Explicit financial amount keyword ("Amt 1,250.00", "Amount: 500", "Sum: 250")
        val keywordMatch = Regex(
            """(?i)\b(?:amt|amount|sum|total|sum\s+of|value)\b\s*[:\-]?\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)"""
        ).find(sanitized)
        if (keywordMatch != null) {
            val rawAmountToken = (keywordMatch.groups[1]?.value ?: "").replace(",", "").trim()
            val amount = rawAmountToken.toDoubleOrNull()
            if (amount != null && amount > 0.0) {
                return Pair(amount, defaultCurrency)
            }
        }

        return null
    }

    private fun normalizeCurrencyToken(rawToken: String, fallback: String): String {
        val clean = rawToken.uppercase(Locale.US).trim()
        return when {
            clean == "₹" || clean.startsWith("RS") || clean == "INR" -> "INR"
            clean == "$" || clean == "USD" -> "USD"
            clean == "€" || clean == "EUR" -> "EUR"
            clean == "£" || clean == "GBP" -> "GBP"
            clean in setOf("AED", "SGD", "CAD", "AUD", "JPY") -> clean
            else -> fallback
        }
    }

    private fun inferCategoryName(
        merchant: String,
        fullText: String,
        type: TransactionType
    ): String {
        val combined = "$merchant $fullText".lowercase(Locale.US)
        if (type == TransactionType.INCOME) {
            return when {
                combined.contains("salary") || combined.contains("payroll") -> "Salary"
                combined.contains("dividend") || combined.contains("interest") || combined.contains("mutual fund") -> "Investments"
                else -> "Freelance"
            }
        }
        return when {
            listOf("swiggy", "zomato", "restaurant", "cafe", "starbucks", "mcdonald", "dominos", "kfc", "food", "dining", "bakery").any { combined.contains(it) } -> "Food & Dining"
            listOf("blinkit", "zepto", "instamart", "bigbasket", "dmart", "grocery", "supermarket", "reliance fresh", "mart").any { combined.contains(it) } -> "Groceries"
            listOf("uber", "ola", "rapido", "metro", "irctc", "fuel", "petrol", "shell", "hpcl", "bpcl", "iocl", "parking", "toll", "fastag", "flight", "indigo").any { combined.contains(it) } -> "Transport"
            listOf("electricity", "water", "broadband", "jio", "airtel", "vi ", "recharge", "bescom", "bill", "gas", "utility", "wifi", "rent").any { combined.contains(it) } -> "Bills & Utilities"
            listOf("netflix", "spotify", "prime", "bookmyshow", "pvr", "inox", "movie", "youtube", "hotstar", "gaming").any { combined.contains(it) } -> "Entertainment"
            listOf("apollo", "pharmeasy", "1mg", "medplus", "hospital", "clinic", "pharmacy", "gym", "cult.fit", "health", "doctor").any { combined.contains(it) } -> "Health & Fitness"
            else -> "Shopping"
        }
    }
}
