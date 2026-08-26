# Budget Buddy — Offline Version

This repository contains the **offline version** of Budget Buddy, a native Android personal-finance tracker. The application does not request internet access, does not include Firebase or Google Services, does not require an account, and keeps its finance data in app-private storage on the device.

## Overview

Budget Buddy helps one local user record income and expenses, organize entries by category, create monthly category budgets, review current-month balances, inspect transaction history, visualize spending, and unlock budgeting achievements. The dashboard is the launcher screen; cloud registration, sign-in, synchronization, and remote storage from the earlier version have been removed.

## Implemented behavior

- Saves income and expense transactions locally with an amount, ISO date, category, and optional note.
- Provides eight built-in categories and supports additional user-created categories with bundled icons.
- Calculates monthly income, expenses, and net balance from locally stored transactions.
- Creates one budget per displayed month with category allocations and a minimum spending goal.
- Updates a budget category's amount spent when a new local expense is recorded.
- Shows current-month records, budget progress, per-category donut indicators, transaction filters, and category summaries.
- Displays bar-chart and gauge analytics for a month or selected ISO-date range.
- Persists achievement completion and progress locally.
- Can capture or select an image and save it through Android's local media APIs.

## Offline and privacy model

- No `INTERNET` permission is declared.
- Firebase Authentication, Realtime Database, Firestore, Storage, the Google Services Gradle plugin, and `google-services.json` are absent.
- Android cloud backup is disabled with `android:allowBackup="false"`.
- Transactions, budgets, custom categories, currency, and achievement state are serialized as JSON inside app-private `SharedPreferences`.
- The default display name is `Budget Buddy`, the local profile ID is an internal constant, and the default currency symbol is `R`.
- Data remains on the device unless the user separately exports or backs up device storage outside this application.

The local preferences are private to the Android app sandbox but are not additionally encrypted. Do not use this prototype for highly sensitive financial records on a rooted or otherwise compromised device.

## Technology stack

- Kotlin 2.1
- Android SDK (compile/target SDK 35; minimum SDK 27 / Android 8.1)
- AndroidX, Material Components, View Binding, and Data Binding
- Gradle 8.10.2 with Android Gradle Plugin 8.8.2 and the Kotlin DSL
- MPAndroidChart and SpeedView for local visualizations
- Glide for displaying local images
- JUnit 4 for host-side unit tests

## Architecture and important structure

The project is a single Android application module using Activity-based screens and XML layouts.

```text
app/src/main/java/com/example/budgetbuddy/
  LocalDataStore.kt          App-private JSON persistence
  FinanceCalculator.kt       Pure balance, range, and category calculations
  MainActivity.kt            Dashboard and launcher
  TransactionActivity.kt     Local transaction entry
  BudgetActivity.kt          Monthly category budgets
  AnalyticsActivity.kt       Charts and spending gauge
  TransactionHistoryActivity.kt
  CategorySummaryActivity.kt
  AchievementManager.kt
app/src/main/res/             XML layouts, icons, fonts, and bundled images
app/src/test/                 Host-side finance calculation tests
gradle/libs.versions.toml     Dependency versions
```

Application entry point: `com.example.budgetbuddy.MainActivity`.

## Prerequisites

- Android Studio with an Android SDK 35 installation, or the equivalent command-line Android SDK
- JDK 11 or newer (JDK 21 is supported by the included Gradle version)
- An Android 8.1+ emulator or physical device for running the app

Network access may be needed once during development to download Gradle and Maven dependencies. The built application itself is offline-only.

## Setup

1. Clone the repository.
2. Open the repository root in Android Studio and allow Gradle sync to finish.
3. Ensure `local.properties` points to your Android SDK. This file is intentionally ignored because it contains a machine-specific path.

Example `local.properties`:

```properties
sdk.dir=C:/path/to/Android/Sdk
```

No environment variables, API keys, service credentials, databases, cloud accounts, external services, migrations, or downloaded datasets are required.

## Dependency installation

The Gradle wrapper resolves dependencies declared in `app/build.gradle.kts` and `gradle/libs.versions.toml`:

```powershell
.\gradlew.bat dependencies
```

On macOS or Linux, use `./gradlew` instead of `.\gradlew.bat`.

## Build

```powershell
.\gradlew.bat assembleDebug
```

The generated APK is written under `app/build/outputs/apk/debug/` and is intentionally excluded from Git.

## Test and static validation

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
```

The unit suite verifies monthly balance calculation, inclusive ISO-date filtering, and expense grouping by category. Instrumented UI test scaffolding exists, but no substantive device-driven UI test suite is implemented.

## Run

From Android Studio, select the `app` configuration and run it on an Android 8.1+ device or emulator. From the command line, build the debug APK and install it with Android Debug Bridge:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Local data initialization and schema

No database migration is required. The store is created automatically on first launch with empty transaction, budget, custom-category, and achievement collections. Records use these logical fields:

- Transaction: ID, local user ID, category name, amount, `yyyy-MM-dd` date, optional note, income/expense flag, optional local photo path.
- Budget: display month (`MMMM yyyy`), total allocation, minimum goal, and category allocation/spend values.
- Category: name, drawable icon name, and custom-category flag.
- Achievement: identifier, completion flag, and progress count.

Clearing application data or uninstalling the app removes these records. There is no import, export, synchronization, or multi-device recovery feature.

## Current limitations

- This is a single-profile offline prototype; account registration, sign-in, cloud sync, and collaboration are intentionally unavailable.
- Currency defaults to `R`; there is currently no settings screen for changing the locally stored currency or display name.
- The image picker/camera screen can save a local image, but the transaction screen does not yet attach the returned image URI to a transaction. This is partially implemented UI, not a completed receipt-linking feature.
- Budget spending is updated when new expenses are recorded; editing/deleting transactions and retroactively rebuilding budget totals are not implemented.
- Achievement streak logic increments when a budget is saved and does not independently validate distinct consecutive months.
- UI test files are scaffolding only; the meaningful automated coverage is the host-side finance calculation suite.
- Several legacy layouts contain localization and accessibility lint warnings, but the verified lint run reports no errors.

## Security notes

The original download contained a real Firebase client configuration and machine-specific Android SDK settings. Neither file is included here. `google-services.json`, environment files, signing stores, private keys, local SDK paths, build output, IDE state, logs, and generated artifacts are protected by `.gitignore` or excluded from the sanitized copy. No secret configuration is required for this offline version.
