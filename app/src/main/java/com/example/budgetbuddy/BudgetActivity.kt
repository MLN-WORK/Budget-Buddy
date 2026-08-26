package com.example.budgetbuddy

import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetbuddy.databinding.ActivityBudgetBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.Locale

class BudgetActivity : BaseActivity() {
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
        AppNavigation.bind(this, binding.bottomNavView, R.id.nav_budget)
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
        binding.tvTotalBudgeted.text = getString(
            R.string.plain_decimal_amount,
            selectedBudgetCategories.sumOf(BudgetCategory::allocation)
        )
    }

    private fun saveBudget() {
        val minimumGoal = binding.edtMinimumGoal.text.toString().toDoubleOrNull()
        if (minimumGoal == null || !minimumGoal.isFinite() || minimumGoal < 0 || selectedBudgetCategories.isEmpty()) {
            ToastUtil.showCustomToast(this, getString(R.string.invalid_budget_values))
            return
        }
        val categories = budgetCategoryAdapter.getCategoryMap()
        val budgetAmount = categories.values.sumOf(BudgetCategory::allocation)
        if (!budgetAmount.isFinite() || budgetAmount <= 0.0 ||
            categories.values.any { !it.allocation.isFinite() || it.allocation < 0.0 } ||
            minimumGoal > budgetAmount
        ) {
            ToastUtil.showCustomToast(this, getString(R.string.invalid_budget_range))
            return
        }
        val budget = Budget(budgetAmount, minimumGoal, categories)
        val saved = runCatching {
            localData.saveBudget(binding.tvCurrentMonth.text.toString(), budget)
        }.isSuccess
        if (!saved) {
            ToastUtil.showCustomToast(this, getString(R.string.budget_save_failed))
            return
        }
        ToastUtil.showCustomToast(this, "Budget saved locally")
        AchievementManager.unlockAchievement("first_budget", this)
        AchievementManager.recordBudgetForMonth(binding.tvCurrentMonth.text.toString(), this)
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
            binding.edtMinimumGoal.setText(getString(R.string.plain_decimal_amount, budget.minimumGoal))
            binding.btnAddCategory.visibility = View.GONE
            binding.fabSaveBudget.visibility = View.GONE
        }
        budgetCategoryAdapter.notifyDataSetChanged()
        updateBudgetAmount()
    }
}
