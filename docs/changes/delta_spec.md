# Delta Specification — Round 2 Client QA Plan of Improvement (My Money My Plan - MMMP)

## 1. ADDED
- `AutoTrackSource` enum (`SUPER_MONEY`, `GOOGLE_PAY`, `PHONEPE`, `PAYTM`, `BHIM`, `CRED`, `AMAZON_PAY`, `WHATSAPP_PAY`, `BANKING_APPS`, `BANK_SMS`) in `/app/src/main/java/com/example/domain/FinanceModels.kt`
- `RecordSortOrder` enum (`NEWEST_FIRST`, `OLDEST_FIRST`, `HIGHEST_AMOUNT`, `LOWEST_AMOUNT`) in `/app/src/main/java/com/example/domain/FinanceModels.kt`
- `MerchantSpendSlice` and `ExecutiveAnalyticsSummary` domain models in `/app/src/main/java/com/example/domain/FinanceModels.kt`
- `DeleteTransactionConfirmationDialog` in `/app/src/main/java/com/example/ui/components/CommonComponents.kt`
- `ExecutiveVelocityAndTopMerchantsCard` in `/app/src/main/java/com/example/ui/screens/AnalyticsScreen.kt`
- `ConfirmVaultRestoreDialog` in `/app/src/main/java/com/example/ui/screens/SettingsScreen.kt`

## 2. MODIFIED
- `/app/src/main/java/com/example/domain/FinanceModels.kt`: Add `AutoTrackSource`, `RecordSortOrder`, `MerchantSpendSlice`, `ExecutiveAnalyticsSummary`, and `enabledAutoTrackSources` in `UserPreferencesState`.
- `/app/src/main/java/com/example/data/repository/UserPreferencesRepository.kt`: Persist `ENABLED_AUTO_TRACK_SOURCES` and add `setAutoTrackSourceEnabled(source, enabled)`.
- `/app/src/main/java/com/example/util/BankAndUpiAlertParser.kt`: Add `classifyAlertSource(packageName, sourceAppLabel): AutoTrackSource`.
- `/app/src/main/java/com/example/util/AutoTrackedAlertManager.kt`: Filter incoming live notifications by `prefs.enabledAutoTrackSources`.
- `/app/src/main/java/com/example/ui/viewmodel/FinanceViewModel.kt`: Add `selectedCategoryId` and `sortOrder` to `FilterState`, compute `executiveSummary: ExecutiveAnalyticsSummary` in `FinanceUiState`, add `selectCategoryFilter`, `selectSortOrder`, `copyPreviousMonthBudgets`, and `setAutoTrackSourceEnabled`.
- `/app/src/main/java/com/example/ui/screens/RecordsScreen.kt`: Add horizontal Category filter chip bar and Sort Order chip bar.
- `/app/src/main/java/com/example/ui/screens/BudgetsScreen.kt`: Add 1-tap `Copy Previous Month Budgets` action button (`copy_prev_month_budgets_button`).
- `/app/src/main/java/com/example/ui/screens/AnalyticsScreen.kt`: Render `ExecutiveVelocityAndTopMerchantsCard` displaying Net Savings Rate %, Daily Avg Spend, Largest Expense, and Top 5 Merchants/Payees.
- `/app/src/main/java/com/example/ui/screens/SettingsScreen.kt`: Add Per-App Source Filter Chips (`auto_track_source_chip_<name>`) inside Auto-Track card and `ConfirmVaultRestoreDialog` before restoring any vault backup.
- `/app/src/main/java/com/example/MainActivity.kt`: Intercept transaction deletion with `DeleteTransactionConfirmationDialog` and wire all Round 2 callbacks.
- `/app/src/test/java/com/example/ExampleUnitTest.kt`: Add unit tests for `BankAndUpiAlertParser.classifyAlertSource`.

## 3. PROTECTED (DO NOT TOUCH)
- `/app/src/main/java/com/example/data/security/CryptoManager.kt`
- `/app/src/main/java/com/example/data/security/BiometricAuthHelper.kt`
- `/app/src/main/java/com/example/data/local/AppDatabase.kt`
- `/app/src/main/java/com/example/data/local/FinanceDao.kt`
- `/app/src/main/java/com/example/util/CurrencyAndBfCalculator.kt`
- `/app/src/main/java/com/example/util/StatementExporter.kt`
- `/app/src/main/java/com/example/ui/screens/LockScreen.kt`
