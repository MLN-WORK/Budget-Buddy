package com.budgetbuddy

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.Locale

@RunWith(AndroidJUnit4::class)
/*
 * Start of class
 * Name of class and related classes (parent/child classes): OfflineOnboardingTest
 * Parent class: Any; child classes: none; related classes: ActivityScenario, LocalDataStore, and the app activities under test.
 * What the class does: Verifies offline onboarding, persistence, navigation, themes, permissions, and finance flows on Android.
 * What's important to other classes, if applicable: These device tests protect the user-visible contracts shared by both application editions.
 * Code with comments begins below.
 */
class OfflineOnboardingTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun clearLocalProfile() {
        context.getSharedPreferences("budget_buddy_offline_data", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After
    fun cleanUp() {
        context.getSharedPreferences("budget_buddy_offline_data", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun continueOfflineOpensLocalProfileInsteadOfOnlineSignIn() {
        ActivityScenario.launch(WelcomeActivity::class.java).use {
            onView(withId(R.id.btnContinueOffline)).check(matches(withText(R.string.continue_offline))).perform(click())
            onView(withId(R.id.tvProfileTitle)).check(matches(withText(R.string.local_profile_title)))
            onView(withId(R.id.edtDisplayName)).check(matches(isDisplayed()))
            onView(withId(R.id.spCurrency)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun profileNameAndCurrencyAreSavedOnlyOnDevice() {
        LocalDataStore(context).markInitialPermissionsRequested()
        ActivityScenario.launch(ProfileActivity::class.java).use {
            onView(withId(R.id.edtDisplayName)).perform(replaceText("Local Buddy"), closeSoftKeyboard())
            onView(withId(R.id.btnSaveProfile)).perform(click())

            val localData = LocalDataStore(context)
            assertTrue(localData.isProfileConfigured)
            assertEquals("Local Buddy", localData.displayName)
            assertEquals(LocalDataStore.DEFAULT_BUDDY_NAME, localData.buddyName)
            assertEquals("€", localData.currencySymbol)
            assertEquals("EUR", localData.currencyCode)
        }
    }

    @Test
    fun tutorialHomeDoesNotLaunchAPermissionRequestFromTheTemporaryActivity() {
        val localData = LocalDataStore(context)
        localData.saveProfile("Tutorial Tester", "R", "Budster")
        localData.requireTutorial()

        ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java).putExtra(TutorialFlow.EXTRA_STEP, 1)
        ).use {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            onView(withId(R.id.btnSettings)).check(matches(isDisplayed()))
        }

        assertTrue(localData.shouldRequestInitialPermissions)
    }

    @Test
    fun buddyNameAcceptsNumbersAndSpecialCharacters() {
        val customBuddyName = "Budster_#2026-€!@Budget-Buddy"
        LocalDataStore(context).markInitialPermissionsRequested()
        ActivityScenario.launch(ProfileActivity::class.java).use {
            onView(withId(R.id.edtDisplayName)).perform(replaceText("Local User"), closeSoftKeyboard())
            onView(withId(R.id.edtBuddyName)).perform(replaceText(customBuddyName), closeSoftKeyboard())
            onView(withId(R.id.btnSaveProfile)).perform(click())

            assertEquals(customBuddyName, LocalDataStore(context).buddyName)
            assertTrue(customBuddyName.length <= LocalDataStore.MAX_BUDDY_NAME_LENGTH)
        }
    }

    @Test
    fun editingAndDeletingExpenseRebuildsBudgetSpending() {
        val localData = LocalDataStore(context)
        val month = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(
            requireNotNull(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("2026-08-10"))
        )
        localData.saveBudget(
            month,
            Budget(
                budgetAmount = 500.0,
                maximumSpendingBudget = 500.0,
                categories = mapOf("Groceries" to BudgetCategory("Groceries", allocation = 500.0))
            )
        )

        val original = Transaction("expense-1", categoryId = "Groceries", amount = 120.0, date = "2026-08-10")
        localData.saveTransaction(original)
        assertEquals(120.0, localData.getBudget(month)?.categories?.get("Groceries")?.amountSpent ?: -1.0, 0.001)

        localData.saveTransaction(original.copy(amount = 45.0))
        assertEquals(45.0, localData.getBudget(month)?.categories?.get("Groceries")?.amountSpent ?: -1.0, 0.001)

        assertTrue(localData.deleteTransaction(original.transactionId))
        assertEquals(0.0, localData.getBudget(month)?.categories?.get("Groceries")?.amountSpent ?: -1.0, 0.001)
    }

    @Test
    fun bottomNavigationConsistentlyOpensEveryPrimaryPage() {
        LocalDataStore(context).apply {
            saveProfile("Local Buddy", "R")
            markInitialPermissionsRequested()
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.nav_budget)).perform(click())
            onView(withId(R.id.tvHeader)).check(matches(withText(R.string.budget)))

            onView(withId(R.id.nav_analytics)).perform(click())
            onView(withId(R.id.tvHeader)).check(matches(withText(R.string.analytics)))

            onView(withId(R.id.nav_add_transaction)).perform(click())
            onView(withId(R.id.tvHeader)).check(matches(withText(R.string.transaction)))

            onView(withId(R.id.nav_achievement)).perform(click())
            onView(withId(R.id.achievementRecyclerView)).check(matches(isDisplayed()))

            onView(withId(R.id.nav_home)).perform(click())
            onView(withId(R.id.tvHeader)).check(matches(withText(R.string.home)))
        }
    }

    @Test
    fun configuredProfileSkipsWelcomeOnLaterLaunches() {
        LocalDataStore(context).apply {
            saveProfile("Local Buddy", "R", "Budster #1!")
            markInitialPermissionsRequested()
        }
        ActivityScenario.launch(WelcomeActivity::class.java).use {
            onView(withId(R.id.tvHeader)).check(matches(withText(R.string.home)))
            onView(withId(R.id.tvBuddyName)).check(matches(withText("Budster #1!")))
        }
    }

    @Test
    fun analyticsStaysOpenForBoundaryBudgetAndOverspending() {
        val localData = LocalDataStore(context)
        val month = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(java.util.Date())
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
        localData.apply {
            saveProfile("Local Buddy", "R")
            markInitialPermissionsRequested()
            saveBudget(
                month,
                Budget(
                    budgetAmount = 100.0,
                    maximumSpendingBudget = 100.0,
                    categories = mapOf("Groceries" to BudgetCategory("Groceries", allocation = 100.0))
                )
            )
            saveTransaction(Transaction("overspend", categoryId = "Groceries", amount = 250.0, date = date))
        }

        ActivityScenario.launch(AnalyticsActivity::class.java).use {
            onView(withId(R.id.tvHeader)).check(matches(withText(R.string.analytics)))
            onView(withId(R.id.categoryBarGraph)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun analyticsIncludesExpensesOutsideBudgetCategories() {
        val localData = LocalDataStore(context)
        val month = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(java.util.Date())
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
        localData.apply {
            saveProfile("Local Buddy", "R")
            markInitialPermissionsRequested()
            saveBudget(
                month,
                Budget(
                    budgetAmount = 100.0,
                    maximumSpendingBudget = 200.0,
                    categories = mapOf("Groceries" to BudgetCategory("Groceries", allocation = 100.0))
                )
            )
            saveTransaction(
                Transaction("unexpected", categoryId = "Unexpected", amount = 50.0, date = date)
            )
        }

        ActivityScenario.launch(AnalyticsActivity::class.java).use {
            onView(withId(R.id.btnMinMaxHeader)).check(
                matches(withText(context.getString(R.string.spending_limit_used_detail, 25.0, "R", 200.0)))
            )
        }
    }

    @Test
    fun onboardingThemeSelectionDoesNotAdvanceUntilContinueIsPressed() {
        LocalDataStore(context).apply {
            saveProfile("Theme Tester", "R", "Budster")
            requireTutorial()
        }

        ActivityScenario.launch<ThemeColorsActivity>(
            Intent(context, ThemeColorsActivity::class.java)
                .putExtra(ThemeColorsActivity.EXTRA_ONBOARDING, true)
        ).use { scenario ->
            var activityIdentity = 0
            scenario.onActivity {
                activityIdentity = System.identityHashCode(it)
            }
            onView(withId(R.id.rbThemeAmoled)).perform(click())
            Thread.sleep(700)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            onView(withText(R.string.themes_and_colors)).check(matches(isDisplayed()))
            onView(withId(R.id.rbThemeAmoled)).check(matches(isChecked()))
            onView(withId(R.id.btnSaveAppearance)).check(matches(withText(R.string.continue_to_tutorial)))
            var amoledIdentity = 0
            scenario.onActivity {
                amoledIdentity = System.identityHashCode(it)
                assertEquals(activityIdentity, amoledIdentity)
                val page = it.findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)
                assertEquals(
                    android.graphics.Color.BLACK,
                    (page.background as android.graphics.drawable.ColorDrawable).color
                )
            }

            onView(withId(R.id.rbThemeLight)).perform(click())
            Thread.sleep(700)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            onView(withId(R.id.rbThemeLight)).check(matches(isChecked()))
            onView(withId(R.id.btnSaveAppearance)).check(matches(isDisplayed()))
            scenario.onActivity {
                assertEquals(amoledIdentity, System.identityHashCode(it))
                val page = it.findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)
                assertEquals(
                    AppearanceDefaults.LIGHT_APP.main,
                    (page.background as android.graphics.drawable.ColorDrawable).color
                )
            }
        }

        assertFalse(LocalDataStore(context).isTutorialComplete)
        assertEquals(AppThemeMode.LIGHT, LocalDataStore(context).appThemeMode)
    }

    @Test
    fun unsavedThemePreviewDoesNotReplaceTheStoredTheme() {
        val localData = LocalDataStore(context)
        localData.saveProfile("Theme Tester", "R", "Budster")

        ActivityScenario.launch(ThemeColorsActivity::class.java).use { scenario ->
            scenario.onActivity { assertFalse(it.findViewById<android.view.View>(R.id.btnSaveAppearance).isEnabled) }
            onView(withId(R.id.rbThemeAmoled)).perform(click())
            scenario.onActivity { assertTrue(it.findViewById<android.view.View>(R.id.btnSaveAppearance).isEnabled) }
        }

        assertEquals(AppThemeMode.LIGHT, localData.appThemeMode)
    }

    @Test
    fun savedThemeIsPersistedBeforeTheNewScreenStarts() {
        val localData = LocalDataStore(context)
        localData.saveProfile("Theme Tester", "R", "Budster")
        localData.completeTutorial()

        ActivityScenario.launch(ThemeColorsActivity::class.java).use {
            onView(withId(R.id.rbThemeDark)).perform(click())
            onView(withId(R.id.btnSaveAppearance)).perform(click())
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        }

        assertEquals(AppThemeMode.DARK, localData.appThemeMode)
    }

    @Test
    fun everySavedThemeCanLaunchTheHomeScreen() {
        val localData = LocalDataStore(context)
        localData.saveProfile("Theme Tester", "R", "Budster")
        localData.completeTutorial()
        localData.markInitialPermissionsRequested()

        AppThemeMode.entries.forEach { mode ->
            localData.saveAppearance(
                themeMode = mode,
                customAccent = AppearanceDefaults.CUSTOM_ACCENT,
                customMain = AppearanceDefaults.CUSTOM_MAIN,
                gaugeMode = GaugePaletteMode.DEFAULT,
                customGauge = AppearanceDefaults.DEFAULT_GAUGE
            )
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                onView(withId(R.id.btnSettings)).check(matches(isDisplayed()))
                scenario.onActivity {
                    assertEquals(
                        mode.name,
                        BudgetBuddyApplication.nightModeFor(mode, AppearanceDefaults.CUSTOM_MAIN),
                        it.delegate.localNightMode
                    )
                }
            }
        }
    }

    @Test
    fun ocrReviewConfirmationIsOptIn() {
        val localData = LocalDataStore(context)
        assertFalse(localData.reviewOcrBeforeApplying)

        localData.setReviewOcrBeforeApplying(true)

        assertTrue(localData.reviewOcrBeforeApplying)
    }

    @Test
    fun incomeAffectsCashFlowButDoesNotExpandSpendingLimit() {
        val localData = LocalDataStore(context)
        val month = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(java.util.Date())
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
        localData.saveBudget(month, Budget(0.0, 55.0, emptyMap()))
        localData.saveTransaction(Transaction("income", categoryId = "Income", amount = 500.0, date = date, isIncome = true))
        localData.saveTransaction(Transaction("expense", categoryId = "Other", amount = 55.0, date = date))

        val monthKey = date.take(7)
        val balance = localData.getBalance(monthKey)
        assertEquals(500.0, balance.totalIncome, 0.001)
        assertEquals(55.0, balance.totalExpenses, 0.001)
        assertEquals(445.0, balance.closingBalance, 0.001)
        assertEquals(55.0, localData.getBudget(month)?.maximumSpendingBudget ?: -1.0, 0.001)
        assertEquals(100, AnalyticsCalculator.spentPercentage(balance.totalExpenses, 55.0))
    }

    @Test
    fun optedInIncomeExpandsOnlyItsMonthlySpendingLimit() {
        val localData = LocalDataStore(context)
        val month = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(java.util.Date())
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
        localData.saveBudget(month, Budget(0.0, 55.0, emptyMap()))
        localData.saveTransaction(Transaction(
            "included-income",
            categoryId = "Income",
            amount = 500.0,
            date = date,
            isIncome = true,
            addsToSpendingLimit = true
        ))

        assertEquals(500.0, localData.getIncomeAddedToSpendingLimit(month), 0.001)
        assertEquals(555.0, localData.getEffectiveSpendingLimit(month), 0.001)
    }

    @Test
    fun ocrTagPersistsIndependentlyFromChosenCategory() {
        val localData = LocalDataStore(context)
        localData.saveTransaction(
            Transaction(
                transactionId = "ocr-grocery",
                categoryId = "Groceries",
                amount = 42.0,
                date = "2026-08-28",
                isOcr = true
            )
        )

        val stored = requireNotNull(localData.getTransaction("ocr-grocery"))
        assertEquals("Groceries", stored.categoryId)
        assertTrue(stored.isOcr)
    }

    @Test
    fun renamedOcrCategoryKeepsStableIdentityAndExistingRecords() {
        val localData = LocalDataStore(context)
        assertTrue(localData.setOcrCategoryName("Scanned receipts"))
        localData.saveTransaction(
            Transaction(
                transactionId = "stable-ocr",
                categoryId = LocalDataStore.OCR_CATEGORY,
                amount = 18.0,
                date = "2026-08-28",
                isOcr = true
            )
        )
        assertTrue(localData.setOcrCategoryName("Receipt imports"))

        assertEquals(LocalDataStore.OCR_CATEGORY, localData.getTransaction("stable-ocr")?.categoryId)
        assertEquals("Receipt imports", localData.categoryDisplayName(LocalDataStore.OCR_CATEGORY))
        assertEquals("ic_eye", localData.categoryIcon(LocalDataStore.OCR_CATEGORY))
    }

    @Test
    fun changingOcrNameFromSettingsReturnsHomeWithoutLosingTheRecord() {
        val localData = LocalDataStore(context)
        localData.saveProfile("Local User", "\$", "Budster")
        localData.saveTransaction(
            Transaction(
                transactionId = "settings-rename-ocr",
                categoryId = LocalDataStore.OCR_CATEGORY,
                amount = 18.0,
                date = "2026-08-28",
                isOcr = true
            )
        )

        ActivityScenario.launch<ProfileActivity>(
            Intent(context, ProfileActivity::class.java)
                .putExtra(ProfileActivity.EXTRA_SETTINGS_MODE, true)
        ).use {
            onView(withId(R.id.edtOcrCategoryName))
                .perform(replaceText("My receipt scans"), closeSoftKeyboard())
            onView(withId(R.id.btnSaveProfile)).perform(click())
            onView(withId(R.id.tvHeader)).check(matches(withText(R.string.home)))
        }

        assertEquals(
            LocalDataStore.OCR_CATEGORY,
            localData.getTransaction("settings-rename-ocr")?.categoryId
        )
        assertEquals("My receipt scans", localData.categoryDisplayName(LocalDataStore.OCR_CATEGORY))
    }

    @Test
    fun tutorialRunsOnceUnlessExplicitlyRequestedAgain() {
        val localData = LocalDataStore(context)
        localData.requireTutorial()
        assertFalse(localData.isTutorialComplete)
        assertTrue(localData.hasTutorialState)

        localData.completeTutorial()
        assertTrue(localData.isTutorialComplete)
    }

    @Test
    fun customCurrencyNameAndSymbolPersistTogether() {
        val localData = LocalDataStore(context)
        localData.saveProfile(
            displayName = "Local Buddy",
            currencySymbol = "¤¤",
            currencyCode = LocalDataStore.CUSTOM_CURRENCY_CODE,
            currencyName = "Buddy Credits"
        )

        assertEquals("Buddy Credits", localData.currencyName)
        assertEquals("¤¤", localData.currencySymbol)
        assertEquals(LocalDataStore.CUSTOM_CURRENCY_CODE, localData.currencyCode)
    }

    @Test
    fun homeBudgetUsesTheExactOkayGaugeColourInLightMode() {
        val localData = LocalDataStore(context)
        localData.saveProfile("Gauge Tester", "€", "Budster")
        localData.markInitialPermissionsRequested()
        val month = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(java.util.Date())
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
        localData.saveBudget(month, Budget(maximumSpendingBudget = 100.0))
        localData.saveTransaction(
            Transaction("okay-band", categoryId = "Other", amount = 60.0, date = date)
        )

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val expected = AppearanceDefaults.DEFAULT_GAUGE.okay
                val bar = activity.findViewById<android.widget.ProgressBar>(R.id.pgBudgetBar)
                val remaining = activity.findViewById<android.widget.TextView>(R.id.tvBudgetRemaining)
                assertEquals(expected, bar.progressTintList?.defaultColor)
                assertEquals(expected, remaining.currentTextColor)
            }
        }
    }

    @Test
    fun tutorialAnalyticsNeutralUsesTheExactGaugeColourInLightMode() {
        val localData = LocalDataStore(context)
        localData.saveProfile("Tutorial Gauge Tester", "€", "Budster")
        localData.markInitialPermissionsRequested()
        localData.saveAppearance(
            themeMode = AppThemeMode.LIGHT,
            customAccent = AppearanceDefaults.CUSTOM_ACCENT,
            customMain = AppearanceDefaults.CUSTOM_MAIN,
            gaugeMode = GaugePaletteMode.DEFAULT,
            customGauge = AppearanceDefaults.DEFAULT_GAUGE
        )

        val intent = Intent(context, AnalyticsActivity::class.java)
            .putExtra(TutorialFlow.EXTRA_STEP, 5)
        ActivityScenario.launch<AnalyticsActivity>(intent).use { scenario ->
            onView(withText(R.string.tutorial_state_neutral)).perform(click())
            scenario.onActivity { activity ->
                val remaining = activity.findViewById<android.widget.TextView>(R.id.tvAnalyticsBudgetRemaining)
                assertEquals(AppearanceDefaults.DEFAULT_GAUGE.okay, remaining.currentTextColor)
            }
        }
    }

    @Test
    fun packagedAppDoesNotRequestInternetPermission() {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        }
        assertFalse(packageInfo.requestedPermissions.orEmpty().contains(Manifest.permission.INTERNET))
    }

    @Test
    fun persistenceRejectsMalformedFinanceRecordsAndOversizedCategories() {
        val localData = LocalDataStore(context)
        val invalidSave = runCatching {
            localData.saveTransaction(
                Transaction("invalid", categoryId = "Other", amount = Double.POSITIVE_INFINITY, date = "bad-date")
            )
        }

        assertTrue(invalidSave.isFailure)
        assertNull(localData.getTransaction("invalid"))
        assertFalse(
            localData.addCategory(
                Category("x".repeat(LocalDataStore.MAX_CATEGORY_NAME_LENGTH + 1), "ic_currency", true)
            )
        )
    }

    @Test
    fun changingOrRemovingCategoryBudgetsKeepsExistingMonthlySpending() {
        val localData = LocalDataStore(context)
        val month = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(java.util.Date())
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
        localData.saveTransaction(
            Transaction("existing-expense", categoryId = "Groceries", amount = 75.0, date = date)
        )
        localData.saveBudget(
            month,
            Budget(
                budgetAmount = 100.0,
                maximumSpendingBudget = 200.0,
                categories = mapOf("Groceries" to BudgetCategory("Groceries", allocation = 100.0))
            )
        )

        localData.saveBudget(
            month,
            Budget(
                budgetAmount = 0.0,
                maximumSpendingBudget = 300.0,
                categories = emptyMap()
            )
        )

        assertEquals(75.0, localData.getMonthlyExpenseTotal(month), 0.001)
        assertEquals(25, AnalyticsCalculator.spentPercentage(75.0, 300.0))
    }

    @Test
    fun transactionDraftPreservationIsOptInAndCanBeCleared() {
        val localData = LocalDataStore(context)
        assertFalse(localData.preserveTransactionDrafts)
        localData.setPreserveTransactionDrafts(true)
        localData.saveTransactionDraft(
            TransactionDraft(amount = "42", description = "Keep me", categoryName = "Other")
        )

        assertEquals("42", localData.getTransactionDraft()?.amount)
        localData.setPreserveTransactionDrafts(false)

        assertFalse(localData.preserveTransactionDrafts)
        assertNull(localData.getTransactionDraft())
    }

    @Test
    fun incomeCanBeSavedWithoutSelectingCategory() {
        LocalDataStore(context).apply {
            saveProfile("Local Buddy", "R")
            markInitialPermissionsRequested()
        }

        ActivityScenario.launch(TransactionActivity::class.java).use {
            onView(withId(R.id.btnIncome)).perform(click())
            onView(withId(R.id.etAmount)).perform(replaceText("125.50"), closeSoftKeyboard())
            onView(withId(R.id.btnSave)).perform(click())

            val saved = LocalDataStore(context).getTransactions().single()
            assertTrue(saved.isIncome)
            assertEquals(TransactionCategoryPolicy.DEFAULT_INCOME_CATEGORY, saved.categoryId)
        }
    }
}
// End of class: OfflineOnboardingTest
