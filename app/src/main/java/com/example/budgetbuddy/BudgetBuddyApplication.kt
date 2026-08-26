package com.example.budgetbuddy

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class BudgetBuddyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        applySavedTheme(LocalDataStore(this).isDarkThemeEnabled)
    }

    companion object {
        fun applySavedTheme(enabled: Boolean) {
            AppCompatDelegate.setDefaultNightMode(
                if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }
}
