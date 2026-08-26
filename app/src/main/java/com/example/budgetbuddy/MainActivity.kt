package com.example.budgetbuddy

import android.content.Intent
import android.icu.util.Calendar
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgetbuddy.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var localData: LocalDataStore
    private var isExpanded = false
    private val formattedMonth: String
        get() = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Calendar.getInstance().time)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        localData = LocalDataStore(this)

        setupExpandableBudget()
        setupNavigation()
        refreshDashboard()
        binding.btnRecordsHeader.setOnClickListener {
            startActivity(Intent(this, TransactionHistoryActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        if (::localData.isInitialized) refreshDashboard()
    }

    private fun setupNavigation() {
        binding.bottomNavView.selectedItemId = R.id.nav_home
        binding.bottomNavView.setOnItemSelectedListener { item ->
            val destination = when (item.itemId) {
                R.id.nav_analytics -> AnalyticsActivity::class.java
                R.id.nav_add_transaction -> TransactionActivity::class.java
                R.id.nav_budget -> BudgetActivity::class.java
                R.id.nav_achievement -> AchievementActivity::class.java
                R.id.nav_home -> null
                else -> return@setOnItemSelectedListener false
            }
            destination?.let { startActivity(Intent(this, it)); finish() }
            true
        }
    }

    private fun setupExpandableBudget() {
        binding.btnBudgetSpentExpand.setOnClickListener {
            isExpanded = !isExpanded
            binding.hiddenBudgetContent.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.btnBudgetSpentExpand.setCompoundDrawablesWithIntrinsicBounds(
                0, 0,
                if (isExpanded) R.drawable.ic_upward_arrow else R.drawable.ic_downward_arrow,
                0
            )
        }
    }

    private fun refreshDashboard() {
        displayBalance()
        displayBudget()
        displayRecords()
        displayDonuts()
    }

    private fun displayBalance() {
        val monthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time)
        val balance = localData.getBalance(monthKey)
        val symbol = localData.currencySymbol
        binding.tvBalanceAmount.text = "%s%.2f".format(symbol, balance.closingBalance)
        binding.tvIncomeAmount.text = "%s%.2f".format(symbol, balance.totalIncome)
        binding.tvExpensesAmount.text = "%s%.2f".format(symbol, balance.totalExpenses)
    }

    private fun displayBudget() {
        val budget = localData.getBudget(formattedMonth)
        val spent = budget?.categories?.values?.sumOf { it.amountSpent ?: 0.0 } ?: 0.0
        val total = budget?.budgetAmount ?: 0.0
        val remaining = total - spent
        val symbol = localData.currencySymbol
        binding.tvBudgetSpent.text = "$symbol%.2f spent".format(spent)
        binding.tvBudgetRemaining.text = "$symbol%.2f remaining".format(remaining)
        binding.pgBudgetBar.progress = if (total > 0) ((spent / total) * 100).toInt().coerceIn(0, 100) else 0
    }

    private fun displayRecords() {
        val current = LocalDate.now()
        val transactions = localData.getTransactions()
            .filter { runCatching { LocalDate.parse(it.date) }.getOrNull()?.let { date ->
                date.year == current.year && date.monthValue == current.monthValue
            } == true }
            .sortedByDescending(Transaction::date)
        val icons = localData.getCategories().associate { it.name to it.icon }
        binding.rvRecords.layoutManager = LinearLayoutManager(this)
        binding.rvRecords.setHasFixedSize(true)
        binding.rvRecords.adapter = TransactionAdapterForHome(transactions, localData.currencySymbol, icons)
    }

    private fun displayDonuts() {
        val budget = localData.getBudget(formattedMonth)
        val donuts = budget?.categories?.map { (name, category) ->
            Donut(name, category.allocation, category.amountSpent ?: 0.0)
        }.orEmpty()
        binding.rvDonuts.layoutManager = GridLayoutManager(this, 2)
        binding.rvDonuts.setHasFixedSize(true)
        binding.rvDonuts.adapter = DonutAdapter(donuts, localData.currencySymbol)
    }
}
