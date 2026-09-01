package com.budgetbuddy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.icu.util.Calendar
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.budgetbuddy.databinding.ActivityMainBinding
import com.google.android.material.color.MaterialColors
import java.text.SimpleDateFormat
import java.util.Locale

/*
 * Start of class
 * Name of class and related classes (parent/child classes): MainActivity
 * Parent class: BaseActivity; child classes: none; related classes: LocalDataStore, AnalyticsCalculator, TransactionAdapterForHome, and AppNavigation.
 * What the class does: Displays the home dashboard, monthly totals, gauge, and recent transactions.
 * What's important to other classes, if applicable: It must preserve BaseActivity appearance behavior and use LocalDataStore as the offline source of truth.
 * Code with comments begins below.
 */
class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var localData: LocalDataStore
    private var isExpanded = false
    private val formattedMonth: String
        get() = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Calendar.getInstance().time)

    private val requestInitialCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) ToastUtil.showCustomToast(this, getString(R.string.camera_permission_needed))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        localData = LocalDataStore(this)

        setupExpandableBudget()
        setupNavigation()
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java).putExtra(ProfileActivity.EXTRA_SETTINGS_MODE, true))
        }
        refreshDashboard()
        requestInitialPermissionsOnce()
        binding.btnRecordsHeader.setOnClickListener {
            startActivity(Intent(this, TransactionHistoryActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        if (::localData.isInitialized) refreshDashboard()
    }

    private fun setupNavigation() {
        AppNavigation.bind(this, binding.bottomNavView, R.id.nav_home)
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
        binding.tvBuddyName.text = localData.buddyName
        binding.imgBuddy.contentDescription = localData.buddyName
        displayBalance()
        displayBudget()
        displayRecords()
        displayDonuts()
    }

    private fun requestInitialPermissionsOnce() {
        // Tutorial steps replace this activity immediately. A permission launch posted
        // from that short-lived screen becomes unregistered and crashes the process.
        if (intent.getIntExtra(TutorialFlow.EXTRA_STEP, -1) >= 0) return
        if (!localData.shouldRequestInitialPermissions) return
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            localData.markInitialPermissionsRequested()
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            localData.markInitialPermissionsRequested()
            return
        }
        binding.root.post {
            if (isFinishing || isDestroyed ||
                !lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)
            ) return@post
            localData.markInitialPermissionsRequested()
            ToastUtil.showCustomToast(this, getString(R.string.camera_permission_intro))
            requestInitialCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun displayBalance() {
        val monthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time)
        val balance = localData.getBalance(monthKey)
        val symbol = localData.currencySymbol
        binding.tvBalance.text = getString(R.string.net_cash_flow_for_month, formattedMonth)
        binding.btnRecordsHeader.text = getString(R.string.records_all_months)
        binding.tvBalanceAmount.text = getString(R.string.money_amount, symbol, balance.closingBalance)
        binding.tvIncomeAmount.text = getString(R.string.money_amount, symbol, balance.totalIncome)
        binding.tvExpensesAmount.text = getString(R.string.money_amount, symbol, balance.totalExpenses)
        applyMoneyColors(balance)
    }

    private fun displayBudget() {
        val monthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time)
        val spent = localData.getBalance(monthKey).totalExpenses
        val total = localData.getEffectiveSpendingLimit(formattedMonth)
        val includedIncome = localData.getIncomeAddedToSpendingLimit(formattedMonth)
        val balance = localData.getBalance(monthKey)
        val remaining = total - spent
        val symbol = localData.currencySymbol
        val percentage = AnalyticsCalculator.spentPercentage(spent, total)
        val remainingPercentage = AnalyticsCalculator.remainingPercentageExact(spent, total)
        binding.tvBudgetLimit.text = if (total > 0.0) {
            getString(R.string.spending_limit_amount, symbol, total)
        } else {
            getString(R.string.spending_limit_not_set)
        }
        binding.tvBudgetSpent.text = getString(R.string.spent_amount, symbol, spent)
        binding.tvBudgetIncome.text = getString(R.string.income_amount_label, symbol, balance.totalIncome)
        binding.tvBudgetIncludedIncome.visibility = if (includedIncome > 0.0) View.VISIBLE else View.GONE
        binding.tvBudgetIncludedIncome.text = getString(R.string.included_income_amount, symbol, includedIncome)
        binding.tvBudgetRemaining.text = if (remaining >= 0.0) {
            getString(R.string.remaining_amount, symbol, remaining)
        } else {
            getString(R.string.over_limit_amount, symbol, -remaining)
        }
        binding.tvBudgetUsage.text = if (total > 0.0) {
            getString(R.string.budget_remaining_percentage, remainingPercentage)
        } else {
            getString(R.string.set_limit_to_track_spending)
        }
        binding.pgBudgetBar.progress = AnalyticsCalculator.gaugeSpeed(percentage).toInt()
        applyBudgetColors(percentage, total > 0.0)
    }

    private fun applyMoneyColors(balance: Balance) {
        val themed = MaterialColors.getColor(binding.root, R.attr.budgetTextColor)
        if (!localData.useStatusMoneyColors) {
            binding.tvBalanceAmount.setTextColor(themed)
            binding.tvIncomeAmount.setTextColor(themed)
            binding.tvExpensesAmount.setTextColor(themed)
            return
        }
        val palette = localData.gaugePalette
        binding.tvBalanceAmount.setTextColor(if (balance.closingBalance >= 0.0) palette.good else palette.bad)
        binding.tvIncomeAmount.setTextColor(palette.good)
        // Expenses are always money going out. Their colour must never imply that
        // a larger expense is itself a positive result.
        binding.tvExpensesAmount.setTextColor(palette.bad)
    }

    private fun applyBudgetColors(percentage: Int, hasLimit: Boolean) {
        val themed = MaterialColors.getColor(binding.root, R.attr.budgetTextColor)
        val usePalette = localData.useStatusMoneyColors
        val gaugeStatus = if (hasLimit) statusColor(percentage) else themed
        val textStatus = if (usePalette && hasLimit) gaugeStatus else themed
        binding.tvBudgetIncome.setTextColor(if (usePalette) localData.gaugePalette.good else themed)
        binding.tvBudgetIncludedIncome.setTextColor(
            if (usePalette) localData.gaugePalette.good else themed
        )
        binding.tvBudgetSpent.setTextColor(if (usePalette) localData.gaugePalette.bad else themed)
        binding.tvBudgetRemaining.setTextColor(textStatus)
        binding.tvBudgetUsage.setTextColor(textStatus)
        // The bar is a gauge visualization, so it always uses the exact selected
        // analytics gauge colour instead of a contrast-adjusted brown derivative.
        binding.pgBudgetBar.progressTintList = ColorStateList.valueOf(gaugeStatus)
    }

    private fun statusColor(percentage: Int): Int =
        AnalyticsCalculator.gaugeColor(percentage.toDouble(), localData.gaugePalette)

    private fun displayRecords() {
        val transactions = localData.getTransactions()
            .sortedByDescending(Transaction::date)
        val categoriesById = localData.getCategories().associateBy(Category::id)
        binding.rvRecords.layoutManager = LinearLayoutManager(this)
        binding.rvRecords.setHasFixedSize(true)
        binding.rvRecords.adapter = TransactionAdapterForHome(
            transactions,
            localData.currencySymbol,
            categoriesById
        ) { transaction ->
            startActivity(Intent(this, TransactionDetailActivity::class.java).putExtra(
                TransactionDetailActivity.EXTRA_TRANSACTION_ID,
                transaction.transactionId
            ))
        }
    }

    private fun displayDonuts() {
        val budget = localData.getBudget(formattedMonth)
        val donuts = budget?.categories?.map { (id, category) ->
            Donut(localData.categoryDisplayName(id), category.allocation, category.amountSpent ?: 0.0)
        }.orEmpty()
        binding.rvDonuts.layoutManager = GridLayoutManager(this, 2)
        binding.rvDonuts.setHasFixedSize(true)
        binding.rvDonuts.adapter = DonutAdapter(donuts, localData.currencySymbol)
    }
}
// End of class: MainActivity
