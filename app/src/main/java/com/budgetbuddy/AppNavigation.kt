package com.budgetbuddy

import android.app.Activity
import android.content.Intent
import com.google.android.material.bottomnavigation.BottomNavigationView

/*
 * Start of class
 * Name of class and related classes (parent/child classes): AppNavigation
 * Parent class: Any; child classes: none; related classes: BaseActivity and the primary navigation activities.
 * What the class does: Connects the shared bottom navigation to application screens.
 * What's important to other classes, if applicable: Related classes depend on this class keeping its inputs validated and its output contract deterministic.
 * Code with comments begins below.
 */
object AppNavigation {
    fun bind(
        activity: Activity,
        navigation: BottomNavigationView,
        selectedItemId: Int,
        selectedItemRepresentsCurrentScreen: Boolean = true
    ) {
        navigation.menu.findItem(selectedItemId)?.isChecked = true

        fun navigate(itemId: Int): Boolean {
            if (selectedItemRepresentsCurrentScreen && itemId == selectedItemId) return true
            val destination = when (itemId) {
                R.id.nav_home -> MainActivity::class.java
                R.id.nav_analytics -> AnalyticsActivity::class.java
                R.id.nav_add_transaction -> TransactionActivity::class.java
                R.id.nav_budget -> BudgetActivity::class.java
                R.id.nav_achievement -> AchievementActivity::class.java
                else -> return false
            }
            activity.startActivity(Intent(activity, destination).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            activity.finish()
            return true
        }

        navigation.setOnItemSelectedListener { navigate(it.itemId) }
        navigation.setOnItemReselectedListener {
            if (!selectedItemRepresentsCurrentScreen) navigate(it.itemId)
        }
    }
}
// End of class: AppNavigation
