package com.example.budgetbuddy

import android.content.Intent
import android.os.Bundle
import com.example.budgetbuddy.databinding.ActivityWelcomeBinding

class WelcomeActivity : BaseActivity() {
    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (LocalDataStore(this).isProfileConfigured) {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnContinueOffline.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }
}
