package com.example.budgetbuddy

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions

class BudgetBuddyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val localData = LocalDataStore(this)
        applySavedTheme(localData.appThemeMode, localData.customMainColor)
        registerMaterialYou()
    }

    private fun registerMaterialYou() {
        val options = DynamicColorsOptions.Builder()
            .setThemeOverlay(
                com.google.android.material.R.style.ThemeOverlay_Material3_DynamicColors_DayNight
            )
            .setPrecondition { activity, _ ->
                LocalDataStore(activity).appThemeMode == AppThemeMode.MATERIAL_YOU
            }
            .setOnAppliedCallback { activity ->
                activity.theme.applyStyle(R.style.ThemeOverlay_BudgetBuddy_MaterialMappings, true)
            }
            .build()
        DynamicColors.applyToActivitiesIfAvailable(this, options)
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
