package com.example.budgetbuddy

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.budgetbuddy.databinding.ActivityTransactionBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class TransactionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTransactionBinding
    private lateinit var spinnerCategory: Spinner
    private lateinit var btnExpense: Button
    private lateinit var btnIncome: Button
    private lateinit var etAmount: EditText
    private lateinit var tvDate: TextView
    private lateinit var ivCalendar: ImageView
    private lateinit var etDescription: EditText
    private lateinit var btnAddImage: Button
    private lateinit var btnSave: Button
    private lateinit var btnQuickAddCategory: ImageButton
    private lateinit var tvCurrencySymbol: TextView
    private lateinit var localData: LocalDataStore

    private var selectedDate = ""
    private var isIncome = false
    private var savedCatPosition: Int? = null
    private var userCurrencySymbol = "R"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        localData = LocalDataStore(this)

        spinnerCategory = binding.spinnerCategory
        btnExpense = binding.btnExpense
        btnIncome = binding.btnIncome
        etAmount = binding.etAmount
        tvDate = binding.tvDate
        ivCalendar = binding.ivCalendar
        etDescription = binding.etDescription
        btnAddImage = binding.btnAddImage
        btnSave = binding.btnSave
        btnQuickAddCategory = binding.btnQuickAddCategory
        tvCurrencySymbol = binding.tvCurrencySymbol

        userCurrencySymbol = localData.currencySymbol
        tvCurrencySymbol.text = userCurrencySymbol
        etAmount.setText("0.00")
        etAmount.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && etAmount.text.toString() == "0.00") etAmount.text.clear()
        }

        selectExpense()
        setupNavigation()
        setupCategorySpinner()
        initDate()

        tvDate.setOnClickListener { showDatePicker() }
        ivCalendar.setOnClickListener { showDatePicker() }
        btnQuickAddCategory.setOnClickListener { startActivity(Intent(this, AddCategoryActivity::class.java)) }
        btnSave.setOnClickListener { saveTransaction() }
        btnAddImage.setOnClickListener { startActivity(Intent(this, AddImageActivity::class.java)) }

        savedInstanceState?.let {
            etAmount.setText(it.getString("amt", ""))
            etDescription.setText(it.getString("desc", ""))
            selectedDate = it.getString("selectedDate", selectedDate)
            tvDate.text = selectedDate
            isIncome = it.getBoolean("isIncome", false)
            if (isIncome) selectIncome() else selectExpense()
            savedCatPosition = it.getInt("spinnerCategoryPosition", 0)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::spinnerCategory.isInitialized) setupCategorySpinner()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("amt", etAmount.text.toString())
        outState.putString("desc", etDescription.text.toString())
        outState.putString("selectedDate", selectedDate)
        outState.putBoolean("isIncome", isIncome)
        outState.putInt("spinnerCategoryPosition", spinnerCategory.selectedItemPosition)
    }

    private fun setupNavigation() {
        binding.bottomNavView.selectedItemId = R.id.nav_add_transaction
        binding.bottomNavView.setOnItemSelectedListener { item ->
            val destination = when (item.itemId) {
                R.id.nav_home -> MainActivity::class.java
                R.id.nav_analytics -> AnalyticsActivity::class.java
                R.id.nav_budget -> BudgetActivity::class.java
                R.id.nav_achievement -> AchievementActivity::class.java
                R.id.nav_add_transaction -> null
                else -> return@setOnItemSelectedListener false
            }
            destination?.let { startActivity(Intent(this, it)); finish() }
            true
        }
        btnExpense.setOnClickListener { selectExpense() }
        btnIncome.setOnClickListener { selectIncome() }
    }

    private fun setupCategorySpinner() {
        val categories = buildList {
            add(Category("Select Category", ""))
            addAll(localData.getCategories())
            add(Category("+ Add Category", ""))
        }
        val adapter = object : ArrayAdapter<Category>(this, R.layout.spinner_item_category, categories) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                bind(position, convertView, parent)

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
                bind(position, convertView, parent)

            private fun bind(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: layoutInflater.inflate(R.layout.spinner_item_category, parent, false)
                val item = getItem(position) ?: return view
                view.findViewById<TextView>(R.id.text).text = item.name
                val iconView = view.findViewById<ImageView>(R.id.imgIcon)
                val iconId = resources.getIdentifier(item.icon, "drawable", packageName)
                iconView.visibility = if (iconId == 0) View.GONE else View.VISIBLE
                if (iconId != 0) iconView.setImageResource(iconId)
                return view
            }
        }
        spinnerCategory.adapter = adapter
        savedCatPosition?.let { spinnerCategory.setSelection(it.coerceIn(categories.indices)) }
        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (categories[position].name == "+ Add Category") {
                    startActivity(Intent(this@TransactionActivity, AddCategoryActivity::class.java))
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        }
    }

    private fun selectExpense() {
        isIncome = false
        btnExpense.setBackgroundResource(R.drawable.bg_toggle_left_selected)
        btnIncome.setBackgroundResource(R.drawable.bg_toggle_right_unselected)
        btnExpense.setTextColor(ContextCompat.getColor(this, R.color.white))
        btnIncome.setTextColor(ContextCompat.getColor(this, R.color.black))
    }

    private fun selectIncome() {
        isIncome = true
        btnIncome.setBackgroundResource(R.drawable.bg_toggle_right_selected)
        btnExpense.setBackgroundResource(R.drawable.bg_toggle_left_unselected)
        btnIncome.setTextColor(ContextCompat.getColor(this, R.color.white))
        btnExpense.setTextColor(ContextCompat.getColor(this, R.color.black))
    }

    private fun initDate() {
        selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
        tvDate.text = selectedDate
    }

    private fun showDatePicker() {
        val today = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            val selected = Calendar.getInstance().apply { set(year, month, day) }
            selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selected.time)
            tvDate.text = selectedDate
        }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun saveTransaction() {
        val amount = etAmount.text.toString().removePrefix(userCurrencySymbol).trim().toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            toast("Please enter a valid amount")
            return
        }
        val selected = spinnerCategory.selectedItem as? Category
        if (selected == null || spinnerCategory.selectedItemPosition == 0 || selected.name == "+ Add Category") {
            toast("Please select a category")
            return
        }

        val transaction = Transaction(
            transactionId = UUID.randomUUID().toString(),
            userId = LocalDataStore.LOCAL_USER_ID,
            categoryId = selected.name,
            amount = amount,
            date = selectedDate,
            note = etDescription.text.toString().takeIf(String::isNotBlank),
            isIncome = isIncome
        )
        runCatching { localData.saveTransaction(transaction) }
            .onSuccess {
                toast("Transaction saved locally")
                etAmount.setText("0.00")
                etDescription.text.clear()
                spinnerCategory.setSelection(0)
                selectExpense()
                initDate()
                AchievementManager.unlockAchievement("first_transaction", this)
                AchievementManager.checkStayWithinBudgetForMonth(
                    LocalDataStore.LOCAL_USER_ID,
                    selectedDate.take(7),
                    this
                )
            }
            .onFailure { toast("Could not save transaction") }
    }

    private fun toast(message: String) = ToastUtil.showCustomToast(this, message)
}
