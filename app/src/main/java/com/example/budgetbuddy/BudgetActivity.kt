package com.example.budgetbuddy

import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetbuddy.databinding.ActivityBudgetBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.Locale

class BudgetActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBudgetBinding
    private lateinit var localData: LocalDataStore
    private lateinit var budgetCategoryAdapter: BudgetCategoryAdapter
    private val currentMonth: Calendar = Calendar.getInstance()
    private val selectedBudgetCategories = mutableListOf<BudgetCategory>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        localData = LocalDataStore(this)

        binding.tvCurrency.text = localData.currencySymbol
        binding.tvCurrency1.text = localData.currencySymbol
        setBudgetCategoryList()
        updateMonthDisplay()
        loadBudgetForMonthInView()

        binding.btnBack.setOnClickListener {
            currentMonth.add(Calendar.MONTH, -1)
            updateMonthDisplay()
            loadBudgetForMonthInView()
        }
        binding.btnForward.setOnClickListener {
            currentMonth.add(Calendar.MONTH, 1)
            updateMonthDisplay()
            loadBudgetForMonthInView()
        }
        binding.btnAddCategory.setOnClickListener { showCategoryBottomSheet(this) }
        binding.fabSaveBudget.setOnClickListener { saveBudget() }
        appNavigationSetup()
    }

    private fun updateMonthDisplay() {
        binding.tvCurrentMonth.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            .format(currentMonth.time)
    }

    private fun appNavigationSetup() {
        binding.bottomNavView.selectedItemId = R.id.nav_budget
        binding.bottomNavView.setOnItemSelectedListener { item ->
            val destination = when (item.itemId) {
                R.id.nav_home -> MainActivity::class.java
                R.id.nav_analytics -> AnalyticsActivity::class.java
                R.id.nav_add_transaction -> TransactionActivity::class.java
                R.id.nav_achievement -> AchievementActivity::class.java
                R.id.nav_budget -> null
                else -> return@setOnItemSelectedListener false
            }
            destination?.let { startActivity(Intent(this, it)); finish() }
            true
        }
    }

    private fun showCategoryBottomSheet(context: Context) {
        val dialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_select_category, null)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvAllCategories)
        val categories = localData.getCategories().toMutableList()
        val adapter = CategoryAdapter(categories) { selected ->
            if (selectedBudgetCategories.none { it.name == selected.name }) {
                selectedBudgetCategories += BudgetCategory(name = selected.name, icon = selected.icon)
                budgetCategoryAdapter.notifyItemInserted(selectedBudgetCategories.lastIndex)
                binding.fabSaveBudget.visibility = View.VISIBLE
            }
            dialog.dismiss()
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
        dialog.setContentView(view)
        dialog.show()
    }

    private fun setBudgetCategoryList() {
        budgetCategoryAdapter = BudgetCategoryAdapter(
            selectedBudgetCategories,
            selectedBudgetCategories.associate { it.name to it.icon }
        ) { updateBudgetAmount() }
        binding.rvBudgetCategories.layoutManager = LinearLayoutManager(this)
        binding.rvBudgetCategories.adapter = budgetCategoryAdapter
    }

    private fun updateBudgetAmount() {
        binding.tvTotalBudgeted.text = selectedBudgetCategories.sumOf(BudgetCategory::allocation).toString()
    }

    private fun saveBudget() {
        val minimumGoal = binding.edtMinimumGoal.text.toString().toDoubleOrNull()
        if (minimumGoal == null || minimumGoal < 0 || selectedBudgetCategories.isEmpty()) {
            ToastUtil.showCustomToast(this, "Add categories and enter a valid minimum goal")
            return
        }
        val categories = budgetCategoryAdapter.getCategoryMap()
        val budgetAmount = categories.values.sumOf(BudgetCategory::allocation)
        val budget = Budget(budgetAmount, minimumGoal, categories)
        localData.saveBudget(binding.tvCurrentMonth.text.toString(), budget)
        ToastUtil.showCustomToast(this, "Budget saved locally")
        AchievementManager.unlockAchievement("first_budget", this)
        AchievementManager.unlockOrProgress("monthly_budget_once", this)
        AchievementManager.unlockOrProgress("monthly_budget_streak", this)
        binding.fabSaveBudget.visibility = View.GONE
        binding.btnAddCategory.visibility = View.GONE
    }

    private fun loadBudgetForMonthInView() {
        val budget = localData.getBudget(binding.tvCurrentMonth.text.toString())
        selectedBudgetCategories.clear()
        binding.edtMinimumGoal.text.clear()
        if (budget == null) {
            binding.btnAddCategory.visibility = View.VISIBLE
            binding.fabSaveBudget.visibility = View.GONE
        } else {
            val iconByName = localData.getCategories().associate { it.name to it.icon }
            selectedBudgetCategories += budget.categories.map { (name, category) ->
                category.copy(name = name, icon = iconByName[name] ?: category.icon ?: "ic_currency")
            }
            binding.edtMinimumGoal.setText(budget.minimumGoal.toString())
            binding.btnAddCategory.visibility = View.GONE
            binding.fabSaveBudget.visibility = View.GONE
        }
        budgetCategoryAdapter.notifyDataSetChanged()
        updateBudgetAmount()
    }
}
