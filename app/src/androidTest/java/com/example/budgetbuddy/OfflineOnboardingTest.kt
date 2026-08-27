package com.example.budgetbuddy

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.Locale

@RunWith(AndroidJUnit4::class)
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
                minimumGoal = 50.0,
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
                    minimumGoal = 150.0,
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
}
