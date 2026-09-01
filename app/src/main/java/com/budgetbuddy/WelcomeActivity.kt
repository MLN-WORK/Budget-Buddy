package com.budgetbuddy

import android.content.Intent
import android.os.Bundle
import com.budgetbuddy.databinding.ActivityWelcomeBinding

/*
 * Start of class
 * Name of class and related classes (parent/child classes): WelcomeActivity
 * Parent class: BaseActivity; child classes: none; related classes: ProfileActivity, MainActivity, and LocalDataStore.
 * What the class does: Routes a new or returning user to onboarding or the home screen.
 * What's important to other classes, if applicable: It must preserve BaseActivity appearance behavior and use LocalDataStore as the offline source of truth.
 * Code with comments begins below.
 */
class WelcomeActivity : BaseActivity() {
    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val localData = LocalDataStore(this)
        if (localData.isProfileConfigured) {
            // Existing installations did not have tutorial state. Do not interrupt them after an update.
            if (!localData.hasTutorialState) localData.completeTutorial()
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                if (!localData.isTutorialComplete) putExtra(TutorialFlow.EXTRA_STEP, 1)
            })
            finish()
            return
        }
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnContinueOffline.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }
}
// End of class: WelcomeActivity
