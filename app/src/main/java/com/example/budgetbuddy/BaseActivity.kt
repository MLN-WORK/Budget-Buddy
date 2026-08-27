package com.example.budgetbuddy

import android.content.Context
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.color.DynamicColors

abstract class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        val themedContext = when (LocalDataStore(newBase).appThemeMode) {
            AppThemeMode.MATERIAL_YOU -> ContextThemeWrapper(
                DynamicColors.wrapContextIfAvailable(newBase),
                R.style.ThemeOverlay_BudgetBuddy_MaterialMappings
            )
            AppThemeMode.AMOLED -> ContextThemeWrapper(
                newBase,
                R.style.ThemeOverlay_BudgetBuddy_Amoled
            )
            else -> newBase
        }
        super.attachBaseContext(themedContext)
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        applyCustomPaletteIfNeeded()
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        applyCustomPaletteIfNeeded()
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        super.setContentView(view, params)
        applyCustomPaletteIfNeeded()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
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
        if (localData.appThemeMode != AppThemeMode.CUSTOM) return
        RuntimePaletteApplier.apply(
            root,
            AppearanceDefaults.customAppPalette(
                main = localData.customMainColor,
                accent = localData.customAccentColor
            )
        )
    }
}
