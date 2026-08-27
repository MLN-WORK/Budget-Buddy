package com.example.budgetbuddy

import android.content.Intent
import android.os.Bundle
import com.example.budgetbuddy.databinding.ActivityProfileBinding

class ProfileActivity : BaseActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var localData: LocalDataStore
    private var settingsMode = false
    private var selectedCurrency: CurrencyOption? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        localData = LocalDataStore(this)
        settingsMode = intent.getBooleanExtra(EXTRA_SETTINGS_MODE, false)

        binding.spCurrency.setAdapter(CurrencySearchAdapter(this))
        binding.spCurrency.threshold = 0
        binding.spCurrency.setOnItemClickListener { parent, _, position, _ ->
            selectedCurrency = parent.getItemAtPosition(position) as? CurrencyOption
        }
        binding.spCurrency.setOnClickListener { binding.spCurrency.showDropDown() }
        binding.spCurrency.setOnFocusChangeListener { _, focused ->
            if (focused) binding.spCurrency.showDropDown()
        }
        val savedCurrency = CurrencyCatalog.findByCode(localData.currencyCode)
            ?: CurrencyCatalog.findBySymbol(localData.currencySymbol)
            ?: requireNotNull(CurrencyCatalog.findByCode(CurrencyCatalog.DEFAULT_CODE))
        selectedCurrency = savedCurrency
        binding.spCurrency.setText(savedCurrency.displayLabel, false)
        binding.edtBuddyName.setText(localData.buddyName)
        if (settingsMode) {
            binding.tvProfileTitle.setText(R.string.settings_title)
            binding.btnSaveProfile.setText(R.string.save_settings)
            binding.edtDisplayName.setText(localData.displayName)
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnThemesColors.visibility = if (settingsMode) android.view.View.VISIBLE else android.view.View.GONE
        binding.tvThemeSummary.visibility = if (settingsMode) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnThemesColors.setOnClickListener {
            startActivity(Intent(this, ThemeColorsActivity::class.java))
        }
        binding.btnSaveProfile.setOnClickListener { saveProfile() }
    }

    override fun onResume() {
        super.onResume()
        if (::localData.isInitialized) {
            binding.tvThemeSummary.text = getString(
                R.string.current_theme,
                when (localData.appThemeMode) {
                    AppThemeMode.LIGHT -> getString(R.string.theme_light)
                    AppThemeMode.DARK -> getString(R.string.theme_dark)
                    AppThemeMode.MATERIAL_YOU -> getString(R.string.theme_material_you)
                    AppThemeMode.AMOLED -> getString(R.string.theme_amoled)
                    AppThemeMode.CUSTOM -> getString(R.string.theme_custom)
                }
            )
        }
    }

    private fun saveProfile() {
        val name = binding.edtDisplayName.text.toString().trim()
        if (name.isBlank()) {
            binding.edtDisplayName.error = getString(R.string.name_required)
            return
        }
        val buddyName = binding.edtBuddyName.text.toString().trim()
        if (buddyName.isBlank()) {
            binding.edtBuddyName.error = getString(R.string.buddy_name_required)
            return
        }
        if (buddyName.length > LocalDataStore.MAX_BUDDY_NAME_LENGTH) {
            binding.edtBuddyName.error = getString(R.string.buddy_name_too_long)
            return
        }
        val currency = selectedCurrency
            ?.takeIf { it.displayLabel.equals(binding.spCurrency.text.toString().trim(), ignoreCase = true) }
            ?: CurrencyCatalog.findExact(binding.spCurrency.text.toString())
        if (currency == null) {
            binding.spCurrency.error = getString(R.string.select_valid_currency)
            return
        }
        localData.saveProfile(
            displayName = name,
            currencySymbol = currency.symbol,
            buddyName = buddyName,
            currencyCode = currency.code
        )
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    companion object {
        const val EXTRA_SETTINGS_MODE = "settingsMode"
    }
}
