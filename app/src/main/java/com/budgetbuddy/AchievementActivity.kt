package com.budgetbuddy

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.budgetbuddy.AchievementManager.achievements
import com.budgetbuddy.databinding.ActivityAchievementBinding

/*
 * Start of class
 * Name of class and related classes (parent/child classes): AchievementActivity
 * Parent class: BaseActivity; child classes: none; related classes: AchievementManager and AchievementAdapter.
 * What the class does: Displays the user's achievements and their progress.
 * What's important to other classes, if applicable: It must preserve BaseActivity appearance behavior and use LocalDataStore as the offline source of truth.
 * Code with comments begins below.
 */
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
// End of class: AchievementActivity
