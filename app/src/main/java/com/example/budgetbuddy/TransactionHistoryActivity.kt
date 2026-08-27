package com.example.budgetbuddy

import android.app.DatePickerDialog
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class TransactionHistoryActivity : BaseActivity() {

    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView
    private lateinit var rvTransactions: RecyclerView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var adapter: TransactionAdapter
    private lateinit var btnBackTHA: ImageButton
    private lateinit var btnCategoryTog: MaterialButton
    private lateinit var rgTransactionFilter: RadioGroup
    private lateinit var rbAll: RadioButton
    private lateinit var rbExpenses: RadioButton
    private lateinit var rbIncomes: RadioButton
    private var startDate: String = ""
    private var endDate: String = ""

    private lateinit var repo: TransactionRepo
    private lateinit var localData: LocalDataStore
    private var allCategories: List<Category> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_list)
        repo = TransactionRepo(this)
        localData = LocalDataStore(this)

        initViews()
        setupBottomNav()
        setupDatePickers()

        // Displaying all transactions by default
        rbAll.isChecked = true

        // Navigating to category summary page
        btnCategoryTog.setOnClickListener {
            startActivity(Intent(this, CategorySummaryActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
            overridePendingTransition(0, 0)
        }

        btnBackTHA.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                startActivity(Intent(this@TransactionHistoryActivity, MainActivity::class.java))
                finish()
            }
        })

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = createAdapter(emptyList())
        rvTransactions.layoutManager = LinearLayoutManager(this)
        rvTransactions.adapter = adapter

        fetchAllCategories { categories ->
            allCategories = categories
            adapter = createAdapter(allCategories)
            rvTransactions.layoutManager = LinearLayoutManager(this)
            rvTransactions.adapter = adapter
            applyFilters()
        }

        // Displaying transactions based on type and date filters
        rgTransactionFilter.setOnCheckedChangeListener { _, _ ->
            applyFilters()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        applyFilters()
    }

    private fun initViews() {
        tvStartDate = findViewById(R.id.tvStartDate)
        tvEndDate = findViewById(R.id.tvEndDate)
        rvTransactions = findViewById(R.id.rvTransactions)
        bottomNav = findViewById(R.id.bottomNavView)
        btnBackTHA = findViewById(R.id.btnBack)
        rgTransactionFilter = findViewById(R.id.rgTransactionFilter)
        rbAll = findViewById(R.id.rbAll)
        rbExpenses = findViewById(R.id.rbExpenses)
        rbIncomes = findViewById(R.id.rbIncomes)
        btnCategoryTog = findViewById(R.id.btnCategoryTog)
        btnBackTHA = findViewById(R.id.btnBackTHA)
    }

    private fun createAdapter(categories: List<Category>) = TransactionAdapter(
        mutableListOf(),
        categories,
        localData.currencySymbol,
        onEdit = { transaction ->
            startActivity(Intent(this, TransactionActivity::class.java).putExtra(
                TransactionActivity.EXTRA_TRANSACTION_ID,
                transaction.transactionId
            ))
        },
        onDelete = ::confirmDelete
    )

    private fun confirmDelete(transaction: Transaction) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_transaction)
            .setMessage(R.string.delete_transaction_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                if (localData.deleteTransaction(transaction.transactionId)) {
                    toast(getString(R.string.transaction_deleted))
                    applyFilters()
                }
            }
            .show()
    }

    //Applying radio and date filters
    private fun applyFilters() {
        val selectedType = rgTransactionFilter.checkedRadioButtonId

        if (startDate.isBlank() || endDate.isBlank()) {
            // No date filter, apply type only
            when (selectedType) {
                R.id.rbAll -> loadAllTransactions()
                R.id.rbExpenses -> loadAllExpenses()
                R.id.rbIncomes -> loadAllIncomes()
            }
        } else {
            // Filter by date and type
            repo.fetchInRange(startDate, endDate, { txns ->
                val filtered = when (selectedType) {
                    R.id.rbAll -> txns.filterALl()
                    R.id.rbExpenses -> txns.filterExpensesOnly()
                    R.id.rbIncomes -> txns.filterIncomesOnly()
                    else -> txns.filterALl()
                }
                adapter.updateData(filtered)
            }, {
                toast("Filter error: ${it.message}")
            })
        }
    }

    private fun setupDatePickers() {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = Calendar.getInstance()

        tvStartDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    startDate = fmt.format(Calendar.getInstance().apply {
                        set(year, month, day)
                    }.time)
                    tvStartDate.text = startDate
                    applyFilters()
                },
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        tvEndDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    endDate = fmt.format(Calendar.getInstance().apply {
                        set(year, month, day)
                    }.time)
                    tvEndDate.text = endDate
                    applyFilters()
                },
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    //Fetching all category IDs and icons
    private fun fetchAllCategories(onComplete: (List<Category>) -> Unit) {
        onComplete(localData.getCategories())
    }
    //loading ALL transactions
    private fun loadAllTransactions() {
        repo.fetchAll(
            onComplete = { txns ->
                adapter.updateData(txns.filterALl())
            },
            onError = { e ->
                toast("Load error: ${e.message}")
            }
        )
    }

    //loading EXPENSES
    private fun loadAllExpenses() {
        repo.fetchAll(
            onComplete = { txns ->
                adapter.updateData(txns.filterExpensesOnly())
            },
            onError = { e ->
                toast("Load error: ${e.message}")
            }
        )
    }

    //loading INCOMES
    private fun loadAllIncomes() {
        repo.fetchAll(
            onComplete = { txns ->
                adapter.updateData(txns.filterIncomesOnly())
            },
            onError = { e ->
                toast("Load error: ${e.message}")
            }
        )
    }

    private fun toast(msg: String) {
        ToastUtil.showCustomToast(this, msg)
    }

    private fun setupBottomNav() {
        AppNavigation.bind(
            this,
            bottomNav,
            R.id.nav_home,
            selectedItemRepresentsCurrentScreen = false
        )
    }
}
// ----------------------------------------------------------------
// Extension Functions
// ----------------------------------------------------------------

    //Fetching ALL transactions
    fun List<Transaction>.filterALl(): List<Transaction> {
        return this
    }

    //Fetching EXPENSES
    fun List<Transaction>.filterExpensesOnly(): List<Transaction> {
        return filterNot(Transaction::isIncome)
    }

    //Fetching INCOMES
    fun List<Transaction>.filterIncomesOnly(): List<Transaction> {
        return filter(Transaction::isIncome)
    }

// ----------------------------------------------------------------
// RecyclerView Adapter for ease of access
// ----------------------------------------------------------------

    class TransactionAdapter(
        private var items: MutableList<Transaction>,
        private val categories: List<Category>,
        private val currencySymbol: String,
        private val onEdit: (Transaction) -> Unit,
        private val onDelete: (Transaction) -> Unit
    ) : RecyclerView.Adapter<TransactionAdapter.TransactionVH>() {

        inner class TransactionVH(view: View) : RecyclerView.ViewHolder(view) {
            val tvDesc: TextView = view.findViewById(R.id.tvDescription)
            val tvAmount: TextView = view.findViewById(R.id.tvAmount)
            val tvDate: TextView = view.findViewById(R.id.tvDate)
            val tvCategory: TextView = view.findViewById(R.id.tvCategory)
            val imgCatIcon: ImageView = view.findViewById(R.id.imgCatIcon)
            val ivPhoto: ImageView = view.findViewById(R.id.ivPhoto)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_transaction, parent, false)
            RuntimePaletteApplier.applyIfCustom(view)
            return TransactionVH(view)
        }

        override fun onBindViewHolder(holder: TransactionVH, position: Int) {
            val txn = items[position]

            holder.itemView.setOnClickListener { onEdit(txn) }
            holder.itemView.setOnLongClickListener {
                onDelete(txn)
                true
            }

            val context = holder.itemView.context
            holder.tvDesc.text = txn.note ?: context.getString(R.string.no_description)
            holder.tvDate.text = txn.date
            holder.tvCategory.text = txn.categoryId

            val isExpense = !txn.isIncome

            //Setting text colour depending on transaction type
            if (isExpense) {
                holder.tvAmount.text = context.getString(R.string.expense_amount, currencySymbol, txn.amount)
                holder.tvAmount.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.cherry)
                )
            } else {
                holder.tvAmount.text = context.getString(R.string.income_amount, currencySymbol, txn.amount)
                holder.tvAmount.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.moss)
                )
            }

            if (!txn.photoPath.isNullOrBlank()) {
                val imgFile = File(txn.photoPath)
                if (ReceiptStorage.isUsableOwnedReceipt(context, imgFile)) {
                    holder.ivPhoto.visibility = View.VISIBLE
                    Glide.with(holder.itemView)
                        .load(imgFile)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .fitCenter()
                        .into(holder.ivPhoto)
                } else {
                    holder.ivPhoto.visibility = View.GONE
                }
            } else {
                holder.ivPhoto.visibility = View.GONE
            }

            val category = categories.find { it.name == txn.categoryId }
            val iconName = category?.icon ?: "ic_currency"
            val iconResId = context.resources.getIdentifier(iconName, "drawable", context.packageName)

            // 🐞 DEBUG LOGS
            Log.d("CategoryLoad", "Txn category: ${txn.categoryId}")
            Log.d("CategoryLoad", "Matched category: ${category?.name}, iconName: $iconName")


            if (iconResId != 0) {
                holder.imgCatIcon.setImageResource(iconResId)
            } else {
                Log.w("CategoryLoad", "Icon resource not found for iconName: $iconName. Using default.")
                holder.imgCatIcon.setImageResource(R.drawable.ic_currency)
            }

            val bgColors = listOf(
                R.color.pastelPink,
                R.color.pastelOrange,
                R.color.pastelYellow,
                R.color.pastelGreen,
                R.color.pastelBlue,
                R.color.pastelPurple
            )

            val bgColorRes = bgColors[position % bgColors.size]
            val bgColor = ContextCompat.getColor(context, bgColorRes)

            val backgroundDrawable = ContextCompat.getDrawable(context, R.drawable.circle_background)?.mutate()
            backgroundDrawable?.setTint(bgColor)
            holder.imgCatIcon.background = backgroundDrawable

        }

        override fun getItemCount(): Int = items.size

        fun updateData(newItems: List<Transaction>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }
    }
