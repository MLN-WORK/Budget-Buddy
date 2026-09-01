package com.budgetbuddy

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/*
 * Start of class
 * Name of class and related classes (parent/child classes): BaseActivity
 * Parent class: AppCompatActivity; child classes: AchievementActivity, AddCategoryActivity, AddImageActivity, AnalyticsActivity, BudgetActivity, CategorySummaryActivity, MainActivity, ProfileActivity, ThemeColorsActivity, TransactionActivity, TransactionDetailActivity, TransactionHistoryActivity, TutorialActivity, and WelcomeActivity; related classes: BudgetBuddyApplication, RuntimePaletteApplier, TutorialFlow, and all screen activities.
 * What the class does: Applies theme, custom palette, AMOLED, immersive, and tutorial behavior to every screen.
 * What's important to other classes, if applicable: It must preserve BaseActivity appearance behavior and use LocalDataStore as the offline source of truth.
 * Code with comments begins below.
 */
abstract class BaseActivity : AppCompatActivity() {
    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        applyRuntimeAppearanceIfNeeded()
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        applyRuntimeAppearanceIfNeeded()
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        super.setContentView(view, params)
        applyRuntimeAppearanceIfNeeded()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val appearance = LocalDataStore(this)
        val preview = appearancePreview()
        delegate.localNightMode = BudgetBuddyApplication.nightModeFor(
            preview?.themeMode ?: appearance.appThemeMode,
            preview?.customMain ?: appearance.customMainColor
        )
        if ((preview?.themeMode ?: appearance.appThemeMode) == AppThemeMode.AMOLED) {
            // Keep OLED's colour attributes available during view inflation. Its
            // style intentionally has no window/system-bar overrides, so geometry
            // follows the exact same lifecycle as Dark mode.
            setTheme(R.style.Theme_BudgetBuddy_Amoled)
        }
        super.onCreate(savedInstanceState)
        enterImmersiveMode()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveMode()
    }

    private fun enterImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    fun applyCustomPaletteIfNeeded(root: View = findViewById(android.R.id.content)) {
        val localData = LocalDataStore(this)
        val preview = appearancePreview()
        val mode = preview?.themeMode ?: localData.appThemeMode
        if (mode != AppThemeMode.CUSTOM) return
        RuntimePaletteApplier.apply(
            root,
            AppearanceDefaults.customAppPalette(
                main = preview?.customMain ?: localData.customMainColor,
                accent = preview?.customAccent ?: localData.customAccentColor
            )
        )
    }

    private fun applyRuntimeAppearanceIfNeeded(root: View = findViewById(android.R.id.content)) {
        val mode = appearancePreview()?.themeMode ?: LocalDataStore(this).appThemeMode
        when (mode) {
            AppThemeMode.CUSTOM -> applyCustomPaletteIfNeeded(root)
            AppThemeMode.AMOLED -> Unit
            else -> Unit
        }
        TutorialFlow.attachIfNeeded(this)
    }

    private fun appearancePreview(): AppearanceSelection? =
        AppearancePreviewStore.current.takeIf { this is ThemeColorsActivity }
}
// End of class: BaseActivity
