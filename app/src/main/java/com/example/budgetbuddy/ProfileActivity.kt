package com.example.budgetbuddy

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetbuddy.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var localData: LocalDataStore
    private var settingsMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        localData = LocalDataStore(this)
        settingsMode = intent.getBooleanExtra(EXTRA_SETTINGS_MODE, false)

        val options = resources.getStringArray(R.array.currency_options)
        binding.spCurrency.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            options
        )
        if (settingsMode) {
            binding.tvProfileTitle.setText(R.string.settings_title)
            binding.btnSaveProfile.setText(R.string.save_settings)
            binding.edtDisplayName.setText(localData.displayName)
            val symbols = resources.getStringArray(R.array.currency_symbols)
            binding.spCurrency.setSelection(symbols.indexOf(localData.currencySymbol).coerceAtLeast(0))
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnSaveProfile.setOnClickListener { saveProfile() }
    }

    private fun saveProfile() {
        val name = binding.edtDisplayName.text.toString().trim()
        if (name.isBlank()) {
            binding.edtDisplayName.error = getString(R.string.name_required)
            return
        }
        val symbols = resources.getStringArray(R.array.currency_symbols)
        localData.saveProfile(name, symbols[binding.spCurrency.selectedItemPosition])
        if (settingsMode) {
            finish()
        } else {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    companion object {
        const val EXTRA_SETTINGS_MODE = "settingsMode"
    }
}
