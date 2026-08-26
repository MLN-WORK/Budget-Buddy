package com.example.budgetbuddy

import AchievementAdapter
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgetbuddy.AchievementManager.achievements
import com.example.budgetbuddy.databinding.ActivityAchievementBinding

class AchievementActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAchievementBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
        binding.tvAchievements.text = "You've got this, \n${LocalDataStore(this).displayName}"
    }

    private fun appNavigationSetup(){
        binding.bottomNavView.selectedItemId = R.id.nav_achievement
        binding.bottomNavView.setOnItemSelectedListener { item ->
            when(item.itemId)
            {
                R.id.nav_analytics -> {
                    startActivity(Intent(applicationContext, AnalyticsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_home -> {
                    startActivity(Intent(applicationContext, MainActivity::class.java))
                    finish()
                    true
                }

                R.id.nav_add_transaction ->{
                    startActivity(Intent(applicationContext, TransactionActivity::class.java))
                    finish()
                    true
                }

                R.id.nav_budget ->{
                    startActivity(Intent(applicationContext, BudgetActivity::class.java))
                    finish()
                    true
                }

                R.id.nav_achievement ->{
                    true
                }
                else -> false
            }//end when
        }//end selected listener
    }
}
