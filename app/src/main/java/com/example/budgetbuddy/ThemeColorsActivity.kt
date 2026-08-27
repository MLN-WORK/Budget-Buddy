package com.example.budgetbuddy

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.example.budgetbuddy.databinding.ActivityThemeColorsBinding
import com.google.android.material.color.DynamicColors

class ThemeColorsActivity : BaseActivity() {
    private lateinit var binding: ActivityThemeColorsBinding
    private lateinit var localData: LocalDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThemeColorsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        localData = LocalDataStore(this)

        binding.btnBack.setOnClickListener { finish() }
        binding.tvMaterialAvailability.text = getString(
            if (DynamicColors.isDynamicColorAvailable()) {
                R.string.material_you_available
            } else {
                R.string.material_you_unavailable
            }
        )

        if (savedInstanceState == null) loadSavedChoices()
        bindColorPicker(binding.pickerAccent, binding.swatchAccent, binding.tvAccentHex)
        bindColorPicker(binding.pickerMain, binding.swatchMain, binding.tvMainHex)
        bindColorPicker(binding.pickerGaugeGood, binding.swatchGaugeGood, binding.tvGaugeGoodHex)
        bindColorPicker(binding.pickerGaugeOkay, binding.swatchGaugeOkay, binding.tvGaugeOkayHex)
        bindColorPicker(binding.pickerGaugeBad, binding.swatchGaugeBad, binding.tvGaugeBadHex)

        binding.themeRadioGroup.setOnCheckedChangeListener { _, _ -> updateCustomVisibility() }
        binding.gaugeRadioGroup.setOnCheckedChangeListener { _, _ -> updateCustomVisibility() }
        updateCustomVisibility()
        updateAllSwatches()
        binding.btnSaveAppearance.setOnClickListener { saveAppearance() }
    }

    private fun loadSavedChoices() {
        binding.themeRadioGroup.check(
            when (localData.appThemeMode) {
                AppThemeMode.LIGHT -> R.id.rbThemeLight
                AppThemeMode.DARK -> R.id.rbThemeDark
                AppThemeMode.MATERIAL_YOU -> R.id.rbThemeMaterialYou
                AppThemeMode.AMOLED -> R.id.rbThemeAmoled
                AppThemeMode.CUSTOM -> R.id.rbThemeCustom
            }
        )
        binding.gaugeRadioGroup.check(
            when (localData.gaugePaletteMode) {
                GaugePaletteMode.DEFAULT -> R.id.rbGaugeDefault
                GaugePaletteMode.COLOR_BLIND -> R.id.rbGaugeColorBlind
                GaugePaletteMode.CUSTOM -> R.id.rbGaugeCustom
            }
        )
        binding.pickerAccent.selectedColor = localData.customAccentColor
        binding.pickerMain.selectedColor = localData.customMainColor
        binding.pickerGaugeGood.selectedColor = localData.customGaugePalette.good
        binding.pickerGaugeOkay.selectedColor = localData.customGaugePalette.okay
        binding.pickerGaugeBad.selectedColor = localData.customGaugePalette.bad
    }

    private fun bindColorPicker(picker: ColorPickerView, swatch: View, label: android.widget.TextView) {
        picker.onColorChanged = { color -> updateSwatch(swatch, label, color) }
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

    private fun saveAppearance() {
        val themeMode = when (binding.themeRadioGroup.checkedRadioButtonId) {
            R.id.rbThemeDark -> AppThemeMode.DARK
            R.id.rbThemeMaterialYou -> AppThemeMode.MATERIAL_YOU
            R.id.rbThemeAmoled -> AppThemeMode.AMOLED
            R.id.rbThemeCustom -> AppThemeMode.CUSTOM
            else -> AppThemeMode.LIGHT
        }
        val gaugeMode = when (binding.gaugeRadioGroup.checkedRadioButtonId) {
            R.id.rbGaugeColorBlind -> GaugePaletteMode.COLOR_BLIND
            R.id.rbGaugeCustom -> GaugePaletteMode.CUSTOM
            else -> GaugePaletteMode.DEFAULT
        }
        localData.saveAppearance(
            themeMode = themeMode,
            customAccent = binding.pickerAccent.selectedColor,
            customMain = binding.pickerMain.selectedColor,
            gaugeMode = gaugeMode,
            customGauge = GaugePalette(
                good = binding.pickerGaugeGood.selectedColor,
                okay = binding.pickerGaugeOkay.selectedColor,
                bad = binding.pickerGaugeBad.selectedColor
            )
        )
        BudgetBuddyApplication.applySavedTheme(themeMode, binding.pickerMain.selectedColor)
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }
}
