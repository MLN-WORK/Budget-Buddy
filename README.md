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
5. Open **Budget Buddy**, tap **Continue offline**, add your name, choose a currency, and keep the default buddy name **Budster the Budgeter** or replace it with your own.

The current `com.budgetbuddy` package installs independently from older `com.example.budgetbuddy` builds. Android does not automatically copy local data between those two app identities.

Android may warn that the app came from outside the Play Store. This is expected for a directly downloaded APK. Only install the APK from this repository's official release page.

## What the app does

- Works completely offline with no sign-in, email address, password, online verification, or cloud account.
- Shows setup only on the first launch; later launches open the Home screen directly.
- Uses an immersive full-screen interface so Android system bars do not cover the app controls; swipe from an edge to reveal them temporarily.
- Introduces the companion as **Budster the Budgeter** and lets the user choose a custom buddy name of up to 32 characters, including numbers and special characters.
- Saves the user name, buddy name, currency, and theme locally; all can be changed from the settings button on the Home screen. New profiles default to Euro. The A-Z currency picker can be filtered by name, ISO code, or symbol and supports a custom currency name and symbol.
- Provides an optional inverted dark theme from the settings screen.
- Keeps the same bottom navigation size, position, icons, and instant page transition across every primary screen, including Home.
- Records income and expenses with a date, category, amount, and optional note.
- Creates custom spending categories alongside the built-in categories.
- Keeps the monthly spending limit separate from income by default. Each income record can explicitly add its amount to that month's spending limit, and the dashboard shows the resulting limit, included income, remaining allowance, expenses, and net cash flow.
- Recalculates budget totals whenever a transaction is added, edited, or deleted.
- Requests the camera permission once on the first Home-screen visit, captures receipts through Android’s standard camera flow, and uses Android’s privacy-friendly photo picker for gallery images.
- Decodes and normalizes both camera and gallery receipts into rotated, size-bounded local JPEG files on a background worker, then validates each image before it can be attached. Failed, abandoned, replaced, and partial drafts are cleaned up.
- Uses a bundled ML Kit model to read Latin-script receipts on-device, suggests the merchant, date, and total, and requires the user to review the suggestions before applying them to a transaction.
- Opens the camera directly from the Home receipt shortcut, provides a separate receipt scan action below image attachment, and gives every scanned record an OCR tag independent of its category.
- Gives OCR records a stable built-in category identity with a renameable label, so renaming it never disconnects existing transactions, budgets, icons, or filters. Missing receipt dates default to the scan date, and prominent top-of-receipt text is prioritized for merchant suggestions.
- Shows all saved records on Home, filters transaction history by type, OCR source, and date, opens one record when tapped, and summarizes spending by category.
- Guides fresh installs from local profile creation through appearance customization and a six-part Buddy tutorial, including a dedicated offline OCR lesson. The tutorial can be skipped on its first page and replayed from Settings.
- Shows local charts and a spending gauge. Budster is happy while at least 50% of the monthly budget remains, neutral from 15% through 49%, and angry below 15% or when over budget.
- Shows a dedicated badge for every achievement, with a lock overlay until it is earned, and includes a streak that requires budgets in three distinct consecutive months.

In transaction history, tap a transaction to edit it. Press and hold a transaction to delete it and rebuild the related budget totals.

## Privacy

Budget Buddy has no internet permission and contains no Firebase, advertising, remote database, or cloud synchronization code. The local profile, transactions, receipt copies, recognized receipt text, budgets, categories, settings, and achievements remain in the app's private storage on the device. Android cloud backup is disabled.

Receipt recognition uses Google's bundled ML Kit Text Recognition SDK. Google states that receipt inputs and recognition outputs are processed on-device and are not sent to its servers. Its terms also state that ML Kit may collect technical performance and API-usage metrics; Settings discloses this to users. Use of ML Kit remains subject to the ML Kit and Google APIs terms.

Uninstalling the app or clearing its app data permanently removes its records. There is currently no export, backup, import, or multi-device recovery feature. The local store is protected by Android's app sandbox but is not separately encrypted, so the app should not be treated as a vault on a rooted or compromised device.

## Requirements

- Android 8.1 or newer
- Camera permission prompt on the first Home-screen visit; denying it does not block budgeting or gallery selection
- No internet connection, account, database, dataset, model download, or runtime external service

## Current limitations

- Budget Buddy is intentionally a single-device, local-profile app.
- Receipt images stay in app-owned storage and cannot currently be exported through the interface.
- Receipt OCR suggestions can be wrong on blurry, angled, handwritten, unusually formatted, or non-Latin-script receipts and must be reviewed before saving.
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
- Bundled Google ML Kit Text Recognition for offline receipt OCR
- JUnit 4 and AndroidX Espresso tests

### Important structure

```text
app/src/main/java/com/budgetbuddy/
  WelcomeActivity.kt                   Offline welcome screen
  ProfileActivity.kt                   Local profile and searchable currency settings
  CurrencyCatalog.kt                   Offline currency names, ISO codes, and symbols
  BudgetBuddyApplication.kt           Saved light/dark theme setup
  LocalDataStore.kt                    App-private persistence and budget rebuilds
  FinanceCalculator.kt                 Pure finance calculations
  AchievementProgressCalculator.kt     Distinct-month streak calculation
  MainActivity.kt                      Dashboard
  TransactionActivity.kt               Add and edit transactions and receipts
  AddImageActivity.kt                  Camera and Android photo-picker screen
  ReceiptStorage.kt                    App-owned receipt validation and cleanup
  ReceiptFileCopier.kt                 Atomic, size-limited gallery copying
  ReceiptImageNormalizer.kt            Safe JPEG decoding, orientation, and scaling
  ReceiptOcrScanner.kt                 Bundled on-device text recognition
  ReceiptParser.kt                     Merchant, date, and total suggestions
  TransactionHistoryActivity.kt        Filter, edit, and delete history
  BudgetActivity.kt                    Monthly category budgets
  AnalyticsActivity.kt                 Charts and spending gauge
  AnalyticsCalculator.kt               Bounded, crash-safe gauge calculations
app/src/main/res/                       Layouts, fonts, icons, and buddy artwork
app/src/test/                           Host-side finance and streak tests
app/src/androidTest/                    Offline onboarding UI tests
```

The launcher entry point is `com.budgetbuddy.WelcomeActivity`. It redirects configured users directly to the Home screen, so onboarding runs only once.

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
