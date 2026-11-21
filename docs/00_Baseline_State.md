# 00 — Baseline Architecture & Brownfield Audit State

## 1. Current Tech Stack & Locked Package Versions

| Layer / Component | Artifact / Plugin | Locked Version (`gradle/libs.versions.toml`) |
| :--- | :--- | :--- |
| **Android Gradle Plugin (AGP)** | `com.android.application` | `9.1.1` |
| **Kotlin / Compose Compiler** | `org.jetbrains.kotlin.plugin.compose` | `2.2.10` |
| **KSP (Symbol Processing)** | `com.google.devtools.ksp` | `2.3.5` |
| **Secrets Gradle Plugin** | `com.google.android.libraries.mapsplatform.secrets-gradle-plugin` | `2.0.1` |
| **Google Services Plugin** | `com.google.gms.google-services` | `4.5.0` |
| **Compile / Target / Min SDK** | Android SDK | `compileSdk = 36.1`, `targetSdk = 36`, `minSdk = 24` |
| **Java Source / Target** | JVM Bytecode Target | `JavaVersion.VERSION_11` |
| **Jetpack Compose BOM** | `androidx.compose:compose-bom` | `2024.09.00` |
| **Activity Compose** | `androidx.activity:activity-compose` | `1.10.1` |
| **Lifecycle & ViewModel** | `androidx.lifecycle:lifecycle-*-compose` | `2.8.7` |
| **Navigation Compose** | `androidx.navigation:navigation-compose` | `2.8.9` |
| **Room Persistence (SQLite)** | `androidx.room:room-{runtime,ktx,compiler}` | `2.7.0` |
| **DataStore Preferences** | `androidx.datastore:datastore-preferences` | `1.1.7` |
| **Coroutines** | `org.jetbrains.kotlinx:kotlinx-coroutines-android` | `1.10.2` |
| **Moshi Serialization** | `com.squareup.moshi:moshi-kotlin{-codegen}` | `1.15.2` |
| **Unit & JVM UI Testing** | `junit` / `org.robolectric:robolectric` | `4.13.2` / `4.16.1` |

---

## 2. Database Schema & Table Relationships (Offline Encrypted Room Ledger)

All local data is persisted on-device in SQLite via `AppDatabase` (`androidx.room.RoomDatabase`), paired with field-level **AES-256-GCM authenticated encryption** (`CryptoManager`) backed by the hardware `AndroidKeyStore`.

### Core Entities & Relationships

1. **`accounts` (`AccountEntity`)**
   - **Primary Key**: `id: Long` (auto-generated)
   - **Columns**: `name`, `accountType` (`CASH`, `BANK`, `CREDIT_CARD`, `SAVINGS`, `INVESTMENT`), `currencyCode` (ISO 4217), `encryptedBroughtForwardBalance` (AES-256-GCM ciphertext of initial Balance Brought Forward), `broughtForwardDate` (epoch millis), `carryOverEnabled` (`Boolean`), `colorHex`, `iconKey`, `isArchived`, `createdAt`.
   - **Relationships**: `1:N` parent to `transactions.accountId` and optional `transactions.targetAccountId` (for transfers).

2. **`categories` (`CategoryEntity`)**
   - **Primary Key**: `id: Long` (auto-generated)
   - **Columns**: `name`, `type` (`EXPENSE`, `INCOME`, `BROUGHT_FORWARD`), `iconKey`, `colorHex`, `isCustom`, `sortOrder`.
   - **Relationships**: `1:N` parent to `transactions.categoryId` and `budgets.categoryId`.

3. **`transactions` (`TransactionEntity`)**
   - **Primary Key**: `id: Long` (auto-generated)
   - **Foreign Keys (Indexed)**: `accountId -> accounts.id`, `targetAccountId -> accounts.id (nullable)`, `categoryId -> categories.id (nullable)`
   - **Columns**: `type` (`EXPENSE`, `INCOME`, `TRANSFER`, `BROUGHT_FORWARD`), `encryptedAmount` (AES-256-GCM ciphertext), `encryptedTargetAmount` (nullable, for cross-currency transfers), `currencyCode`, `encryptedTitle` (AES-256-GCM ciphertext), `encryptedNote` (AES-256-GCM ciphertext), `timestamp` (epoch millis), `createdAt`.

4. **`budgets` (`BudgetEntity`)**
   - **Primary Key**: `id: Long` (auto-generated)
   - **Unique Composite Index**: `(categoryId, yearMonth)`
   - **Columns**: `categoryId`, `yearMonth` (`YYYY-MM`), `encryptedLimitAmount` (AES-256-GCM ciphertext), `currencyCode`, `alertAt80Percent`, `alertAt100Percent`, `lastNotifiedLevel`.

5. **`exchange_rates` (`ExchangeRateEntity`)**
   - **Primary Key**: `currencyCode: String` (e.g., `USD`, `EUR`, `GBP`, `JPY`, `INR`, `CAD`, `AUD`, `CHF`, `CNY`, `SGD`)
   - **Columns**: `symbol`, `displayName`, `rateToUsd` (`Double`), `isCustomOverride` (`Boolean`), `updatedAt`.

---

## 3. Offline Service Layer, Security Flow & File Dependency Graph

Because MyMoney Pro operates **100% offline** for strict financial privacy, there are zero external REST/GraphQL network endpoints.

### Authentication & Cryptographic Flow
1. **Master Key Provisioning**: `CryptoManager` generates/retrieves a 256-bit AES master key (`mymoney_pro_master_key_v1`) inside `AndroidKeyStore` using `KeyGenParameterSpec` (`BLOCK_MODE_GCM`, `ENCRYPTION_PADDING_NONE`, 128-bit GCM authentication tag, random 12-byte IV prefixed to each payload).
2. **Biometric & PIN Gate**:
   - On launch, `MainActivity` checks `SecurityPreferences` (App Lock enabled state + salted SHA-256 PIN digest).
   - If locked, `BiometricLockScreen` invokes the Android system `BiometricPrompt` (fingerprint/face) and provides a 4-digit PIN pad fallback before revealing the financial ledger.

### Dependency Graph
```
MainActivity.kt
 └── MyMoneyProApp.kt (Adaptive Navigation & Biometric Gate)
      ├── ui/screens/
      │    ├── LockScreen.kt (BiometricPrompt + PIN Vault)
      │    ├── DashboardScreen.kt (Customizable Widgets + B/F Cash Flow Hero)
      │    ├── RecordsScreen.kt (Monthly Ledger + Brought Forward B/F & C/F Bar)
      │    ├── AccountsScreen.kt (Multi-Currency + Account B/F Reconciliation)
      │    ├── BudgetsScreen.kt (Category Caps + Threshold Progress)
      │    ├── AnalyticsScreen.kt (Interactive Compose Canvas Charts + Export)
      │    └── SettingsScreen.kt (Dark Mode, FX Rates, Biometric/PIN, Backups)
      ├── ui/viewmodel/FinanceViewModel.kt
      │    ├── util/BroughtForwardCalculator.kt (Opening B/F, Period Delta, Closing C/F)
      │    ├── util/CurrencyConverter.kt (Offline FX Normalization)
      │    ├── util/NotificationHelper.kt (Real-time Transaction & Budget Alerts)
      │    └── util/StatementExporter.kt (RFC-4180 CSV & Android PdfDocument + FileProvider)
      └── data/
           ├── repository/FinanceRepository.kt
           ├── security/CryptoManager.kt (AndroidKeyStore AES-256-GCM)
           └── local/AppDatabase.kt (Room DAOs & Entities)
```

---

## 4. Critical Core Files (Protected — Never Modify Without Explicit Approval)

- `/build.gradle.kts` & `/settings.gradle.kts` (Root Gradle toolchain configuration)
- `/gradle/libs.versions.toml` (Locked dependency and plugin version catalog)
- `/app/build.gradle.kts` — Specifically the `signingConfigs` (`release` and `debugConfig`), `buildTypes` signing assignments, `compileSdk`/`targetSdk`/`minSdk`, and `namespace = "com.example"`
- `/debug.keystore` & `/debug.keystore.base64` (Platform APK signing identity)
- `/metadata.json` — Specifically the `majorCapabilities` array (`MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API`)
