<p align="center">
  <img src="app/src/main/res/drawable/logo.png" alt="Budget Buddy logo" width="180" />
  <br />
  <img src="app/src/main/res/drawable/happy_buddy.png" alt="A happy Budget Buddy" width="220" />
</p>

# Budget Buddy — Offline Version

Budget Buddy is a heartfelt Android project trying to bridge budgeting with a playful and quirky companion. This offline edition is an evolution and improvement over the original Budget Buddy: it removes abandoned online account features, makes setup simple, and keeps personal finance information on the Android device.

## Install the app

You do not need Android Studio, programming tools, an online account, or a technical setup.

1. Open the [Budget Buddy Releases page](https://github.com/MLN-WORK/Budget-Buddy/releases) on your Android phone.
2. Open the latest release and tap **Budget-Buddy.apk** under **Assets**.
3. If Android asks for permission, allow your browser or file manager to install apps from this source.
4. Open the downloaded file and tap **Install**.
5. Open **Budget Buddy**, tap **Continue offline**, enter the name your buddy should use, and choose a currency.

Android may warn that the app came from outside the Play Store. This is expected for a directly downloaded APK. Only install the APK from this repository's official release page.

## What the app does

- Works completely offline with no sign-in, email address, password, online verification, or cloud account.
- Uses an immersive full-screen interface so Android system bars do not cover the app controls; swipe from an edge to reveal them temporarily.
- Saves a local display name and currency that can be changed from the profile button on the Home screen.
- Records income and expenses with a date, category, amount, and optional note.
- Creates custom spending categories alongside the built-in categories.
- Builds monthly category budgets and shows spending, remaining funds, income, expenses, and balance.
- Recalculates budget totals whenever a transaction is added, edited, or deleted.
- Attaches a receipt from the camera or gallery and keeps the copied image inside the app.
- Filters transaction history by type and date and summarizes spending by category.
- Shows local charts, a spending gauge, and encouraging buddy moods.
- Unlocks achievements, including a streak that requires budgets in three distinct consecutive months.

In transaction history, tap a transaction to edit it. Press and hold a transaction to delete it and rebuild the related budget totals.

## Privacy

Budget Buddy has no internet permission and contains no Firebase, advertising, analytics service, remote database, or cloud synchronization code. The local profile, transactions, receipt copies, budgets, categories, settings, and achievements remain in the app's private storage on the device. Android cloud backup is disabled.

Uninstalling the app or clearing its app data permanently removes its records. There is currently no export, backup, import, or multi-device recovery feature. The local store is protected by Android's app sandbox but is not separately encrypted, so the app should not be treated as a vault on a rooted or compromised device.

## Requirements

- Android 8.1 or newer
- Camera permission only when taking a receipt photo
- No internet connection, account, API, database, dataset, or external service

## Current limitations

- Budget Buddy is intentionally a single-device, local-profile app.
- Receipt images stay inside the app and cannot currently be exported.
- There is no automatic backup or restore after uninstalling or replacing the device.
- Currency formatting uses the chosen symbol; it does not apply country-specific decimal or grouping rules.
- The APK is distributed directly through repository releases rather than an app store.

## For developers

Budget Buddy is a single-module native Android application written in Kotlin with Activity-based screens and XML layouts.

### Technology

- Kotlin 2.1 and Android SDK 36 (minimum SDK 27)
- AndroidX, Material Components, View Binding, and Data Binding
- Gradle 8.14.5, Android Gradle Plugin 8.13.2, and Kotlin DSL
- MPAndroidChart and SpeedView for offline visualizations
- Glide for local receipt images
- JUnit 4 and AndroidX Espresso tests

### Important structure

```text
app/src/main/java/com/example/budgetbuddy/
  WelcomeActivity.kt                   Offline welcome screen
  ProfileActivity.kt                   Local profile and settings
  LocalDataStore.kt                    App-private persistence and budget rebuilds
  FinanceCalculator.kt                 Pure finance calculations
  AchievementProgressCalculator.kt     Distinct-month streak calculation
  MainActivity.kt                      Dashboard
  TransactionActivity.kt               Add and edit transactions and receipts
  TransactionHistoryActivity.kt        Filter, edit, and delete history
  BudgetActivity.kt                    Monthly category budgets
  AnalyticsActivity.kt                 Charts and spending gauge
app/src/main/res/                       Layouts, fonts, icons, and buddy artwork
app/src/test/                           Host-side finance and streak tests
app/src/androidTest/                    Offline onboarding UI tests
```

The launcher entry point is `com.example.budgetbuddy.WelcomeActivity`.

### Build and test

Prerequisites are JDK 17 or newer, Android SDK 36, and Android Studio or the Android command-line SDK. Dependency downloads require internet access during development; the built app does not.

On Windows:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
.\gradlew.bat assembleDebugAndroidTest
```

On macOS or Linux, use `./gradlew` instead. The installable debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

Device UI tests require a connected Android device or emulator:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

### Local configuration

No environment variables or service credentials are required. A developer's `local.properties` may point Gradle to the Android SDK and is intentionally ignored because it contains a machine-specific path:

```properties
sdk.dir=C:/path/to/Android/Sdk
```

There are no database migrations or datasets. App-private storage initializes automatically on first launch.

## Security notes

Machine-specific SDK paths, online-service configuration from the original project, signing stores, keys, environment files, IDE state, logs, generated output, and APK files are excluded from source control. No secret configuration is required or supported by this offline version.
