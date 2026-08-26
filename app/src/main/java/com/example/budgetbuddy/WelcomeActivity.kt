package com.example.budgetbuddy

import android.content.Intent
import android.os.Bundle
import com.example.budgetbuddy.databinding.ActivityWelcomeBinding

class WelcomeActivity : BaseActivity() {
    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val profileConfigured = LocalDataStore(this).isProfileConfigured
        if (profileConfigured) binding.btnContinueOffline.setText(R.string.continue_to_budget)
        binding.btnContinueOffline.setOnClickListener {
            val destination = if (profileConfigured) MainActivity::class.java else ProfileActivity::class.java
            startActivity(Intent(this, destination))
        }
    }
}
