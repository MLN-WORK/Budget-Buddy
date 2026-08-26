package com.example.budgetbuddy

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgetbuddy.AchievementManager.achievements
import com.example.budgetbuddy.databinding.ActivityAchievementBinding

class AchievementActivity : BaseActivity() {
    private lateinit var binding: ActivityAchievementBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAchievementBinding.inflate(layoutInflater)
        setContentView(binding.root)
        appNavigationSetup()
        displayUserName()

        val adapter = AchievementAdapter(achievements)
        binding.achievementRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.achievementRecyclerView.adapter = adapter

        loadCompletedAchievements {
            adapter.notifyDataSetChanged()
        }
    }//end onCreate

    private fun loadCompletedAchievements(onComplete: () -> Unit) {
        AchievementManager.restore(this)
        onComplete()
    }

    private fun displayUserName() {
        binding.tvAchievements.text = getString(
            R.string.achievement_greeting,
            LocalDataStore(this).displayName
        )
    }

    private fun appNavigationSetup() {
        AppNavigation.bind(this, binding.bottomNavView, R.id.nav_achievement)
    }
}
