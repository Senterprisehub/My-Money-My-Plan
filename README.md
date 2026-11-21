# My Money My Plan (MMMP)

<p align="center">
  <img src="icon.png" alt="My Money My Plan app icon" width="160">
</p>

**My Money My Plan** is an offline-first Android personal finance app for tracking accounts, transactions, budgets, exchange rates, and financial performance. It is built with Kotlin and Jetpack Compose and keeps financial data on the device in an encrypted Room/SQLite database.

> **Privacy model:** MMMP is designed to operate without external REST or GraphQL APIs. Financial records are stored locally, and sensitive fields are encrypted before persistence.

## Features

- **Multi-account ledger** for cash, bank, credit card, savings, and investment accounts
- **Income, expense, transfer, and brought-forward records** with monthly ledger views
- **Balance Brought Forward (B/F) and Carry Forward (C/F) accounting**
- **Multi-currency support** with offline exchange-rate normalization and custom FX overrides
- **Monthly budgets** with category limits and configurable 80%/100% notifications
- **Interactive analytics**, including custom Compose charts, savings velocity, and top merchant analysis
- **Records filtering** by date range, account, transaction type, category, and sort order
- **Bank/UPI alert parsing** for optional automatic transaction detection from Android notifications
- **Selectable auto-track sources** such as bank apps, Google Pay, PhonePe, Paytm, BHIM, CRED, Amazon Pay, and WhatsApp Pay
- **PIN and biometric vault protection** using Android biometric APIs and an app-level PIN fallback
- **AES-256-GCM field encryption** with a key managed by Android Keystore
- **Privacy masking** for hiding balances on screen
- **CSV and PDF statements**, custom export filters, downloads, and sharing
- **Encrypted vault backup and restore**, including JSON import/export workflows
- **Customizable themes**, dynamic colors, font styles, base currency, and dashboard widgets
- **Adaptive phone/tablet navigation** with bottom navigation on compact screens and a navigation rail on wide screens

## Screens

| Screen | Purpose |
|---|---|
| Dashboard | Monthly overview, quick records, pending alerts, widgets, and statement exports |
| Records | Searchable monthly ledger with date/account/type/category filters and sorting |
| Accounts | Create and reconcile accounts, track balances, and open account ledgers |
| Budgets | Set monthly category budgets, copy prior-month budgets, and manage categories |
| Analytics | Review savings, spending, merchant, FX, and period trends |
| Settings | Security, theme, currency, notifications, auto-track, backup, restore, and preferences |

## Technology Stack

- **Language:** Kotlin 2.2.10
- **UI:** Jetpack Compose + Material 3
- **Platform:** Android SDK 36.1; minimum SDK 24; Java 11 bytecode target
- **Build:** Android Gradle Plugin 9.1.1, Gradle Version Catalog
- **Architecture:** Activity → Compose UI → ViewModel → Repository → Room/DAOs
- **Local persistence:** Room 2.7.0 with SQLite
- **Preferences:** Jetpack DataStore Preferences
- **Security:** Android Keystore and AES-256-GCM field encryption
- **Concurrency:** Kotlin Coroutines
- **Serialization:** Moshi
- **Charts:** Custom Jetpack Compose canvas components
- **Testing:** JUnit, AndroidX Test, Espresso, and Robolectric
- **Firebase:** Gradle integration is configured, but authentication/firestore/AI integrations are not enabled in the current dependency set

## Project Structure

```text
.
├── app/
│   ├── build.gradle.kts              # Android module, dependencies, signing, and APK sync tasks
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   # App entry point, notification listener, and FileProvider
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt   # App shell, navigation, dialogs, and Compose root
│       │   │   ├── data/
│       │   │   │   ├── local/        # Room entities, database, and FinanceDao
│       │   │   │   ├── repository/   # Finance and DataStore preference repositories
│       │   │   │   └── security/     # CryptoManager and biometric authentication
│       │   │   ├── domain/           # Shared models and domain enums
│       │   │   ├── service/          # Notification listener service
│       │   │   ├── ui/
│       │   │   │   ├── components/   # Dialogs and reusable Compose components
│       │   │   │   ├── screens/      # Dashboard, Records, Accounts, Budgets, Analytics, Settings
│       │   │   │   ├── theme/        # Compose Material theme
│       │   │   │   └── viewmodel/    # FinanceViewModel and UI state
│       │   │   └── util/             # FX/B/F calculations, parsing, exports, notifications, backups
│       │   └── res/                  # Icons, resources, fonts, styles, and XML configuration
│       ├── test/                     # JVM/JUnit/Robolectric tests
│       └── androidTest/              # Android instrumentation tests
├── docs/
│   ├── 00_Baseline_State.md          # Architecture and baseline audit
│   └── changes/delta_spec.md         # Round 2 implementation specification
├── gradle/libs.versions.toml         # Locked dependency/plugin version catalog
├── build.gradle.kts                  # Root plugin configuration
├── settings.gradle.kts               # Project modules and repositories
├── gradle.properties                 # JVM, Kotlin, Gradle, and build settings
└── metadata.json                     # App metadata and capability declaration
```

## Architecture

`MainActivity` creates the Room database and repositories, then owns a `FinanceViewModel`. Compose screens collect `FinanceUiState`, render adaptive navigation, and send user actions back to the ViewModel.

```text
AndroidManifest/MainActivity
        │
        ▼
Compose screens ──► FinanceViewModel ──► FinanceRepository ──► Room FinanceDao
        │                    │                                      │
        │                    ├──► DataStore preferences             ▼
        │                    └──► FX/B/F/currency utilities       SQLite entities
        │
        ├──► CryptoManager ──► Android Keystore / AES-256-GCM
        ├──► BankAndUpiAlertParser / NotificationListener
        └──► StatementExporter / VaultBackupManager
```

### Local data model

The Room database is composed of these core entities:

- `AccountEntity` — account type, currency, encrypted B/F balance, and reconciliation settings
- `CategoryEntity` — category type, icon, color, ordering, and custom-category state
- `TransactionEntity` — transaction type, account, category, encrypted amount, FX values, title, and timestamp
- `BudgetEntity` — category/month limit, alert thresholds, and encrypted amount
- `ExchangeRateEntity` — symbol, display name, USD conversion rate, custom override, and update time

## Security and Privacy

- Sensitive financial fields are encrypted with **AES-256-GCM** before storage.
- The AES master key is generated or retrieved through **Android Keystore**.
- Optional app locking uses a **4-digit PIN** or Android `BiometricPrompt`.
- Android backup and cloud data extraction are disabled in the app manifest.
- Vault backup exports are encrypted rather than plain-ledger exports.
- The app does not require normal network permissions in its manifest.

## Building and Running

### Requirements

- Android Studio with Android SDK 36
- JDK 11 or newer supported by your Android Studio/Gradle setup
- An Android device or emulator running **API 24+**
- A configured release keystore only when building a signed release APK

### Clone and open

```bash
git clone https://github.com/Senterprisehub/My-Money-My-Plan.git
cd My-Money-My-Plan
```

Open the repository root in Android Studio and allow Gradle synchronization to finish.

### Debug build

```bash
./gradlew assembleDebug
```

The build copies the resulting APK to:

```text
apk/My_Money_My_Plan_MMMP-debug.apk
```

Install it through Android Studio or use:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Release build

The release configuration expects a keystore at `my-upload-key.jks` by default, or it uses these environment variables:

```bash
export KEYSTORE_PATH=/absolute/path/to/release.jks
export STORE_PASSWORD='your-store-password'
export KEY_PASSWORD='your-key-password'
./gradlew assembleRelease
```

The build copies the resulting APK to:

```text
apk/My_Money_My_Plan_MMMP-release.apk
```

## Testing

Run JVM and Robolectric unit tests:

```bash
./gradlew test
```

Run a connected-device instrumentation test suite:

```bash
./gradlew connectedAndroidTest
```

Android Studio test entry points are also available through:

```text
app/src/test/
app/src/androidTest/
```

## Configuration

The app reads optional secrets through the Secrets Gradle Plugin convention:

```text
.env           # Optional local secrets
.env.example   # Optional default/property template
```

No API key or secret is needed for the core offline ledger. Firebase services are not enabled by default.

> Signing keys, `.env`, `google-services.json`, debug keystores, and other local secrets are intentionally excluded by `.gitignore`.

## Documentation

- [`docs/00_Baseline_State.md`](docs/00_Baseline_State.md) — baseline architecture, schema, security flow, and protected files
- [`docs/changes/delta_spec.md`](docs/changes/delta_spec.md) — Round 2 feature and QA specification

## License

No license file is currently included in this repository. All rights are reserved by the repository owner unless a license is explicitly added.
