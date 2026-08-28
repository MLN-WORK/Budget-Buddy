package com.budgetbuddy

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

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
        if (LocalDataStore(this).appThemeMode == AppThemeMode.AMOLED) {
            setTheme(R.style.Theme_BudgetBuddy_Amoled)
        }
        super.onCreate(savedInstanceState)
        enforceAmoledWindowIfNeeded()
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

    private fun applyRuntimeAppearanceIfNeeded(root: View = findViewById(android.R.id.content)) {
        when (LocalDataStore(this).appThemeMode) {
            AppThemeMode.CUSTOM -> applyCustomPaletteIfNeeded(root)
            AppThemeMode.AMOLED -> root.setBackgroundColor(Color.BLACK)
            else -> Unit
        }
    }

    private fun enforceAmoledWindowIfNeeded() {
        if (LocalDataStore(this).appThemeMode != AppThemeMode.AMOLED) return
        window.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        window.decorView.setBackgroundColor(Color.BLACK)
    }
}
