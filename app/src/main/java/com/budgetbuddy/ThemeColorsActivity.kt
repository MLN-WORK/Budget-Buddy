package com.budgetbuddy

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import com.budgetbuddy.databinding.ActivityThemeColorsBinding
import com.google.android.material.color.DynamicColors

/*
 * Start of class
 * Name of class and related classes (parent/child classes): ThemeColorsActivity
 * Parent class: BaseActivity; child classes: none; related classes: AppearanceSelection, AppearancePreviewStore, RuntimePaletteApplier, and LocalDataStore.
 * What the class does: Provides live, reversible theme and gauge previews and commits the chosen appearance.
 * What's important to other classes, if applicable: It must preserve BaseActivity appearance behavior and use LocalDataStore as the offline source of truth.
 * Code with comments begins below.
 */
class ThemeColorsActivity : BaseActivity() {
    private lateinit var binding: ActivityThemeColorsBinding
    private lateinit var localData: LocalDataStore
    private lateinit var savedSelection: AppearanceSelection
    private lateinit var renderedPalette: AppPalette
    private var onboarding = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThemeColorsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        localData = LocalDataStore(this)
        savedSelection = AppearanceSelection.from(localData)
        val previewSelection = AppearancePreviewStore.begin(savedSelection)
        renderedPalette = previewSelection.appPalette
        onboarding = intent.getBooleanExtra(EXTRA_ONBOARDING, false)
        if (onboarding) binding.btnSaveAppearance.setText(R.string.continue_to_tutorial)

        binding.btnBack.setOnClickListener { finish() }
        val materialYouAvailable = DynamicColors.isDynamicColorAvailable()
        binding.rbThemeMaterialYou.isEnabled = materialYouAvailable
        binding.tvMaterialAvailability.text = getString(
            if (materialYouAvailable) {
                R.string.material_you_available
            } else {
                R.string.material_you_unavailable
            }
        )

        loadChoices(previewSelection)
        bindColorPicker(binding.pickerAccent, binding.swatchAccent, binding.tvAccentHex, true)
        bindColorPicker(binding.pickerMain, binding.swatchMain, binding.tvMainHex, true)
        bindColorPicker(binding.pickerGaugeGood, binding.swatchGaugeGood, binding.tvGaugeGoodHex, false)
        bindColorPicker(binding.pickerGaugeOkay, binding.swatchGaugeOkay, binding.tvGaugeOkayHex, false)
        bindColorPicker(binding.pickerGaugeBad, binding.swatchGaugeBad, binding.tvGaugeBadHex, false)

        binding.themeRadioGroup.setOnCheckedChangeListener { _, _ ->
            val previous = AppearancePreviewStore.current
            updateCustomVisibility()
            updateLivePreview(previous)
        }
        binding.gaugeRadioGroup.setOnCheckedChangeListener { _, _ ->
            updateCustomVisibility()
            updateLivePreview(AppearancePreviewStore.current)
        }
        updateCustomVisibility()
        updateAllSwatches()
        updateGaugePreview(previewSelection.gaugePalette)
        updateSaveState()
        binding.btnSaveAppearance.setOnClickListener { saveAppearance() }
    }

    override fun finish() {
        AppearancePreviewStore.clear()
        super.finish()
    }

    override fun onDestroy() {
        if (isFinishing && !isChangingConfigurations) AppearancePreviewStore.clear()
        super.onDestroy()
    }

    private fun loadChoices(selection: AppearanceSelection) {
        binding.themeRadioGroup.check(
            when (selection.themeMode) {
                AppThemeMode.LIGHT -> R.id.rbThemeLight
                AppThemeMode.DARK -> R.id.rbThemeDark
                AppThemeMode.MATERIAL_YOU -> R.id.rbThemeMaterialYou
                AppThemeMode.AMOLED -> R.id.rbThemeAmoled
                AppThemeMode.CUSTOM -> R.id.rbThemeCustom
            }
        )
        binding.gaugeRadioGroup.check(
            when (selection.gaugeMode) {
                GaugePaletteMode.DEFAULT -> R.id.rbGaugeDefault
                GaugePaletteMode.COLOR_BLIND -> R.id.rbGaugeColorBlind
                GaugePaletteMode.CUSTOM -> R.id.rbGaugeCustom
            }
        )
        binding.pickerAccent.selectedColor = selection.customAccent
        binding.pickerMain.selectedColor = selection.customMain
        binding.pickerGaugeGood.selectedColor = selection.customGauge.good
        binding.pickerGaugeOkay.selectedColor = selection.customGauge.okay
        binding.pickerGaugeBad.selectedColor = selection.customGauge.bad
    }

    private fun bindColorPicker(
        picker: ColorPickerView,
        swatch: View,
        label: android.widget.TextView,
        changesAppTheme: Boolean
    ) {
        picker.onColorChanged = { color ->
            val previous = AppearancePreviewStore.current
            updateSwatch(swatch, label, color)
            updateLivePreview(previous, transitionCustomTheme = changesAppTheme)
        }
    }

    private fun updateAllSwatches() {
        updateSwatch(binding.swatchAccent, binding.tvAccentHex, binding.pickerAccent.selectedColor)
        updateSwatch(binding.swatchMain, binding.tvMainHex, binding.pickerMain.selectedColor)
        updateSwatch(binding.swatchGaugeGood, binding.tvGaugeGoodHex, binding.pickerGaugeGood.selectedColor)
        updateSwatch(binding.swatchGaugeOkay, binding.tvGaugeOkayHex, binding.pickerGaugeOkay.selectedColor)
        updateSwatch(binding.swatchGaugeBad, binding.tvGaugeBadHex, binding.pickerGaugeBad.selectedColor)
    }

    private fun updateSwatch(swatch: View, label: android.widget.TextView, color: Int) {
        swatch.setBackgroundColor(color)
        label.text = String.format("#%06X", color and 0xFFFFFF)
    }

    private fun updateCustomVisibility() {
        binding.customThemeControls.visibility =
            if (binding.rbThemeCustom.isChecked) View.VISIBLE else View.GONE
        binding.customGaugeControls.visibility =
            if (binding.rbGaugeCustom.isChecked) View.VISIBLE else View.GONE
    }

    private fun updateLivePreview(
        previous: AppearanceSelection?,
        transitionCustomTheme: Boolean = false
    ) {
        val next = currentSelection()
        AppearancePreviewStore.update(next)
        updateGaugePreview(next.gaugePalette)

        val themeChanged = previous?.themeMode != next.themeMode
        // Material You must be applied before views are inflated, but the four
        // deterministic palettes can repaint this hierarchy immediately. Avoiding
        // recreation here also prevents slow devices from showing an old screen for
        // several seconds and accidentally receiving the user's next tap.
        if (
            themeChanged &&
            (previous?.themeMode == AppThemeMode.MATERIAL_YOU || next.themeMode == AppThemeMode.MATERIAL_YOU)
        ) {
            recreate()
            return
        }
        if (
            themeChanged ||
            (
                transitionCustomTheme &&
                previous?.themeMode == AppThemeMode.CUSTOM &&
                next.themeMode == AppThemeMode.CUSTOM &&
                previous.appPalette != next.appPalette
            )
        ) {
            val nextPalette = next.appPalette
            RuntimePaletteApplier.transition(binding.root, renderedPalette, nextPalette)
            renderedPalette = nextPalette
            window.setBackgroundDrawable(ColorDrawable(nextPalette.main))
            window.decorView.setBackgroundColor(nextPalette.main)
            applyThemeScreenPalette(nextPalette)
            updateAllSwatches()
            updateGaugePreview(next.gaugePalette)
        }
        updateSaveState()
    }

    /** Keeps controls backed by Material drawables identical to the selected palette. */
    private fun applyThemeScreenPalette(palette: AppPalette) {
        binding.themeHeader.setBackgroundColor(palette.accent)
        binding.btnBack.imageTintList = ColorStateList.valueOf(palette.onAccent)
        binding.btnSaveAppearance.backgroundTintList = ColorStateList.valueOf(palette.accent)
        binding.btnSaveAppearance.setTextColor(palette.onAccent)
        binding.customThemeControls.background = previewPanelBackground(palette)
        binding.customGaugeControls.background = previewPanelBackground(palette)
    }

    private fun previewPanelBackground(palette: AppPalette) = GradientDrawable().apply {
        val density = resources.displayMetrics.density
        setColor(palette.surface)
        setStroke((2f * density).toInt().coerceAtLeast(1), palette.onMain)
        cornerRadius = 16f * density
    }

    private fun updateGaugePreview(palette: GaugePalette) {
        binding.previewGaugeGood.setBackgroundColor(palette.good)
        binding.previewGaugeOkay.setBackgroundColor(palette.okay)
        binding.previewGaugeBad.setBackgroundColor(palette.bad)
    }

    private fun saveAppearance() {
        val selection = currentSelection()
        saveCurrentAppearance(selection)
        savedSelection = selection
        AppearancePreviewStore.clear()
        val destination = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (onboarding) putExtra(TutorialFlow.EXTRA_STEP, 1)
        }
        // Each new screen reads the saved choice before it is inflated. Avoid changing
        // the process-wide night mode while this activity is clearing its task: that
        // recreation race was the source of broken and partially themed screens.
        startActivity(destination)
    }

    private fun selectedTheme() = when (binding.themeRadioGroup.checkedRadioButtonId) {
            R.id.rbThemeDark -> AppThemeMode.DARK
            R.id.rbThemeMaterialYou -> AppThemeMode.MATERIAL_YOU
            R.id.rbThemeAmoled -> AppThemeMode.AMOLED
            R.id.rbThemeCustom -> AppThemeMode.CUSTOM
            else -> AppThemeMode.LIGHT
    }

    private fun selectedGaugeMode() = when (binding.gaugeRadioGroup.checkedRadioButtonId) {
        R.id.rbGaugeColorBlind -> GaugePaletteMode.COLOR_BLIND
        R.id.rbGaugeCustom -> GaugePaletteMode.CUSTOM
        else -> GaugePaletteMode.DEFAULT
    }

    private fun saveCurrentAppearance(selection: AppearanceSelection) {
        if (!::localData.isInitialized) return
        localData.saveAppearance(
            themeMode = selection.themeMode,
            customAccent = selection.customAccent,
            customMain = selection.customMain,
            gaugeMode = selection.gaugeMode,
            customGauge = selection.customGauge
        )
    }

    private fun currentSelection() = AppearanceSelection(
        themeMode = selectedTheme(),
        customAccent = binding.pickerAccent.selectedColor,
        customMain = binding.pickerMain.selectedColor,
        gaugeMode = selectedGaugeMode(),
        customGauge = GaugePalette(
            good = binding.pickerGaugeGood.selectedColor,
            okay = binding.pickerGaugeOkay.selectedColor,
            bad = binding.pickerGaugeBad.selectedColor
        )
    )

    private fun updateSaveState() {
        if (!::binding.isInitialized || !::savedSelection.isInitialized) return
        val enabled = onboarding || currentSelection() != savedSelection
        binding.btnSaveAppearance.isEnabled = enabled
        binding.btnSaveAppearance.alpha = if (enabled) 1f else 0.5f
    }

    companion object {
        const val EXTRA_ONBOARDING = "onboarding"
    }
}
// End of class: ThemeColorsActivity
