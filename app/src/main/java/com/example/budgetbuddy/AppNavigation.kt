package com.example.budgetbuddy

import android.app.Activity
import android.content.Intent
import com.google.android.material.bottomnavigation.BottomNavigationView

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
