package com.budgetbuddy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.budgetbuddy.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Locale

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
        binding.tvWelcomeName.text = getString(R.string.welcome_name, localData.displayName)
        binding.tvBuddyName.text = localData.buddyName
        binding.imgBuddy.contentDescription = localData.buddyName
        displayBalance()
        displayBudget()
        displayRecords()
        displayDonuts()
    }

    private fun requestInitialPermissionsOnce() {
        if (!localData.shouldRequestInitialPermissions) return
        localData.markInitialPermissionsRequested()
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) return
        binding.root.post {
            ToastUtil.showCustomToast(this, getString(R.string.camera_permission_intro))
            requestInitialCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun displayBalance() {
        val monthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time)
        val balance = localData.getBalance(monthKey)
        val symbol = localData.currencySymbol
        binding.tvBalanceAmount.text = getString(R.string.money_amount, symbol, balance.closingBalance)
        binding.tvIncomeAmount.text = getString(R.string.money_amount, symbol, balance.totalIncome)
        binding.tvExpensesAmount.text = getString(R.string.money_amount, symbol, balance.totalExpenses)
    }

    private fun displayBudget() {
        val budget = localData.getBudget(formattedMonth)
        val monthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time)
        val spent = localData.getBalance(monthKey).totalExpenses
        val total = budget?.maximumSpendingBudget ?: 0.0
        val remaining = total - spent
        val symbol = localData.currencySymbol
        binding.tvBudgetSpent.text = getString(R.string.spent_amount, symbol, spent)
        binding.tvBudgetRemaining.text = getString(R.string.remaining_amount, symbol, remaining)
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
