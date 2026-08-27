package com.example.budgetbuddy

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class BudgetBuddyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val localData = LocalDataStore(this)
        applySavedTheme(localData.appThemeMode, localData.customMainColor)
    }

    companion object {
        fun applySavedTheme(mode: AppThemeMode, customMainColor: Int = AppearanceDefaults.CUSTOM_MAIN) {
            val nightMode = when (mode) {
                AppThemeMode.DARK, AppThemeMode.AMOLED -> AppCompatDelegate.MODE_NIGHT_YES
                AppThemeMode.MATERIAL_YOU -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                AppThemeMode.CUSTOM -> if (AppearanceDefaults.perceivedLuminance(customMainColor) < 0.45) {
                    AppCompatDelegate.MODE_NIGHT_YES
                } else {
                    AppCompatDelegate.MODE_NIGHT_NO
                }
                AppThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            }
            AppCompatDelegate.setDefaultNightMode(
                nightMode
            )
        }
    }
}
