package com.budgetbuddy

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import com.budgetbuddy.databinding.ActivityProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/*
 * Start of class
 * Name of class and related classes (parent/child classes): ProfileActivity
 * Parent class: BaseActivity; child classes: none; related classes: LocalDataStore, CurrencyCatalog, CurrencySearchAdapter, and ThemeColorsActivity.
 * What the class does: Creates or edits the local profile and application preferences.
 * What's important to other classes, if applicable: It must preserve BaseActivity appearance behavior and use LocalDataStore as the offline source of truth.
 * Code with comments begins below.
 */
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

        binding.spCurrency.setOnClickListener { showCurrencyPicker() }
        val savedCurrency = if (localData.currencyCode == LocalDataStore.CUSTOM_CURRENCY_CODE) {
            CurrencyOption(localData.currencyName, LocalDataStore.CUSTOM_CURRENCY_CODE, localData.currencySymbol)
        } else CurrencyCatalog.findByCode(localData.currencyCode)
            ?: CurrencyCatalog.findBySymbol(localData.currencySymbol)
            ?: requireNotNull(CurrencyCatalog.findByCode(CurrencyCatalog.DEFAULT_CODE))
        selectCurrency(savedCurrency)
        binding.edtBuddyName.setText(localData.buddyName)
        if (settingsMode) {
            binding.tvProfileTitle.setText(R.string.settings_title)
            binding.btnSaveProfile.setText(R.string.save_settings)
            binding.edtDisplayName.setText(localData.displayName)
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnThemesColors.visibility = if (settingsMode) android.view.View.VISIBLE else android.view.View.GONE
        binding.tvThemeSummary.visibility = if (settingsMode) android.view.View.VISIBLE else android.view.View.GONE
        binding.switchPreserveTransactionDrafts.visibility =
            if (settingsMode) android.view.View.VISIBLE else android.view.View.GONE
        binding.tvPreserveTransactionDraftsDescription.visibility =
            if (settingsMode) android.view.View.VISIBLE else android.view.View.GONE
        binding.switchStatusMoneyColors.visibility = if (settingsMode) View.VISIBLE else View.GONE
        binding.tvStatusMoneyColorsDescription.visibility = if (settingsMode) View.VISIBLE else View.GONE
        binding.btnReplayTutorial.visibility = if (settingsMode) View.VISIBLE else View.GONE
        binding.switchPreserveTransactionDrafts.isChecked = localData.preserveTransactionDrafts
        binding.switchStatusMoneyColors.isChecked = localData.useStatusMoneyColors
        binding.btnThemesColors.setOnClickListener {
            startActivity(Intent(this, ThemeColorsActivity::class.java))
        }
        binding.btnReplayTutorial.setOnClickListener {
            TutorialFlow.start(this)
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
        if (currency == null) {
            binding.spCurrency.error = getString(R.string.select_valid_currency)
            return
        }
        localData.saveProfile(
            displayName = name,
            currencySymbol = currency.symbol,
            buddyName = buddyName,
            currencyCode = currency.code,
            currencyName = currency.name
        )
        if (settingsMode) {
            val settingsSaved = runCatching {
                localData.setPreserveTransactionDrafts(binding.switchPreserveTransactionDrafts.isChecked)
                localData.setUseStatusMoneyColors(binding.switchStatusMoneyColors.isChecked)
                true
            }.getOrDefault(false)
            if (!settingsSaved) {
                ToastUtil.showCustomToast(this, getString(R.string.settings_save_failed))
                return
            }
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        } else {
            localData.requireTutorial()
            startActivity(Intent(this, ThemeColorsActivity::class.java).putExtra(
                ThemeColorsActivity.EXTRA_ONBOARDING,
                true
            ))
            finish()
        }
    }

    private fun selectCurrency(currency: CurrencyOption) {
        selectedCurrency = currency
        binding.spCurrency.setText(currency.displayLabel, false)
        binding.spCurrency.error = null
    }

    private fun showCurrencyPicker() {
        val view = layoutInflater.inflate(R.layout.dialog_currency_picker, null)
        val search = view.findViewById<EditText>(R.id.edtCurrencyFilter)
        val list = view.findViewById<ListView>(R.id.listCurrencies)
        val customButton = view.findViewById<Button>(R.id.btnCustomCurrency)
        val customFields = view.findViewById<LinearLayout>(R.id.customCurrencyFields)
        val customName = view.findViewById<EditText>(R.id.edtCustomCurrencyName)
        val customSymbol = view.findViewById<EditText>(R.id.edtCustomCurrencySymbol)
        val useCustom = view.findViewById<Button>(R.id.btnUseCustomCurrency)
        val adapter = CurrencySearchAdapter(this)
        list.adapter = adapter

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.choose_currency)
            .setView(view)
            .setNegativeButton(R.string.cancel, null)
            .create()
        list.setOnItemClickListener { parent, _, position, _ ->
            (parent.getItemAtPosition(position) as? CurrencyOption)?.let(::selectCurrency)
            dialog.dismiss()
        }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(value: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(value: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter.filter(value)
            }
            override fun afterTextChanged(value: Editable?) = Unit
        })
        customButton.setOnClickListener {
            customFields.visibility = if (customFields.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        useCustom.setOnClickListener {
            val name = customName.text.toString().trim()
            val symbol = customSymbol.text.toString().trim()
            if (name.isBlank()) {
                customName.error = getString(R.string.custom_currency_name_required)
            } else if (symbol.isBlank()) {
                customSymbol.error = getString(R.string.custom_currency_symbol_required)
            } else {
                selectCurrency(CurrencyOption(name, LocalDataStore.CUSTOM_CURRENCY_CODE, symbol))
                dialog.dismiss()
            }
        }
        dialog.setOnShowListener { search.requestFocus() }
        dialog.show()
    }

    companion object {
        const val EXTRA_SETTINGS_MODE = "settingsMode"
    }
}
// End of class: ProfileActivity
