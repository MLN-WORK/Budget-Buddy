package com.example.budgetbuddy

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class CategorySummaryActivity : AppCompatActivity() {

    private lateinit var rvSummary: RecyclerView
    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView
    private lateinit var btnTransactionTog: MaterialButton
    private lateinit var btnBackCS: ImageButton
    private var startDate = ""
    private var endDate = ""

    private lateinit var repo: TransactionRepo
    private lateinit var localData: LocalDataStore
    private lateinit var summaryAdapter: CategorySummaryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_summary)
        repo = TransactionRepo(this)
        localData = LocalDataStore(this)
        summaryAdapter = CategorySummaryAdapter(localData.currencySymbol)

        //Navigating to transaction summary page
        btnTransactionTog = findViewById(R.id.btnTransactionTog)
        btnTransactionTog.setOnClickListener {
            startActivity(Intent(this, TransactionHistoryActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
            overridePendingTransition(0, 0)
        }

        btnBackCS = findViewById(R.id.btnBackCS)
        btnBackCS.setOnClickListener{
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        tvStartDate = findViewById(R.id.tvStartDateSummary)
        tvEndDate = findViewById(R.id.tvEndDateSummary)
        rvSummary = findViewById(R.id.rvCategorySummary)

        rvSummary.layoutManager = LinearLayoutManager(this)
        rvSummary.adapter = summaryAdapter
        fetchSummary()
        setupDatePickers()
    }

    private fun setupDatePickers() {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = Calendar.getInstance()

        tvStartDate.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                startDate = fmt.format(Calendar.getInstance().apply { set(y, m, d) }.time)
                tvStartDate.text = startDate
                fetchSummary()
            }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)).show()
        }

        tvEndDate.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                endDate = fmt.format(Calendar.getInstance().apply { set(y, m, d) }.time)
                tvEndDate.text = endDate
                fetchSummary()
            }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun fetchAllCategories(onComplete: (List<Category>) -> Unit) {
        onComplete(localData.getCategories())
    }

    private fun fetchSummary() {
        val onTransactionsFetched: (List<Transaction>) -> Unit = { txns ->
            val expenses = txns.filter { !it.isIncome }

            // Group and sum expenses by category ID
            val categoryTotals = expenses
                .groupBy { it.categoryId ?: "Uncategorised" }
                .mapValues { it.value.sumOf { txn -> txn.amount } }

            // Fetch categories to get icon names
            fetchAllCategories { categories ->
                val categoryIconMap = categories.associateBy({ it.name }, { it.icon })

                val summaryData = categoryTotals.map { (categoryName, total) ->
                    val icon = categoryIconMap[categoryName] ?: "ic_default"
                    Triple(categoryName, total, icon)
                }

                summaryAdapter.updateData(summaryData)
            }
        }

        val onError: (Exception) -> Unit = {
            Log.e("CategorySummary", "Error fetching transactions", it)
            ToastUtil.showCustomToast(this, "Error: ${it.message}")
        }

        if (startDate.isNotBlank() && endDate.isNotBlank()) {
            repo.fetchInRange(startDate, endDate, onComplete = onTransactionsFetched, onError = onError)
        } else {
            repo.fetchAll(onComplete = onTransactionsFetched, onError = onError)
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
