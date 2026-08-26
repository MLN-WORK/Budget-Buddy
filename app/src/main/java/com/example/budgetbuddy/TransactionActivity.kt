package com.example.budgetbuddy

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.budgetbuddy.databinding.ActivityTransactionBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class TransactionActivity : BaseActivity() {
    private lateinit var binding: ActivityTransactionBinding
    private lateinit var localData: LocalDataStore
    private var selectedDate = ""
    private var selectedPhotoPath: String? = null
    private var isIncome = false
    private var editingTransaction: Transaction? = null
    private var preferredCategoryName: String? = null
    private var userCurrencySymbol = "R"

    private val imagePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedPhotoPath = result.data?.getStringExtra(AddImageActivity.EXTRA_IMAGE_PATH)
            showReceipt()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        localData = LocalDataStore(this)
        userCurrencySymbol = localData.currencySymbol
        binding.tvCurrencySymbol.text = userCurrencySymbol
        initDate()
        selectExpense()
        setupNavigation()
        setupCategorySpinner()
        loadTransactionForEditing()

        binding.etAmount.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && binding.etAmount.text.toString() == getString(R.string.zero_amount)) binding.etAmount.text.clear()
        }
        binding.tvDate.setOnClickListener { showDatePicker() }
        binding.ivCalendar.setOnClickListener { showDatePicker() }
        binding.btnQuickAddCategory.setOnClickListener { startActivity(Intent(this, AddCategoryActivity::class.java)) }
        binding.btnSave.setOnClickListener { saveTransaction() }
        binding.btnAddImage.setOnClickListener { imagePicker.launch(Intent(this, AddImageActivity::class.java)) }
        binding.ivAttachedReceipt.setOnClickListener { imagePicker.launch(Intent(this, AddImageActivity::class.java)) }

        savedInstanceState?.let {
            binding.etAmount.setText(it.getString(STATE_AMOUNT, ""))
            binding.etDescription.setText(it.getString(STATE_DESCRIPTION, ""))
            selectedDate = it.getString(STATE_DATE, selectedDate)
            binding.tvDate.text = selectedDate
            isIncome = it.getBoolean(STATE_IS_INCOME, false)
            selectedPhotoPath = it.getString(STATE_PHOTO_PATH)
            preferredCategoryName = it.getString(STATE_CATEGORY)
            if (isIncome) selectIncome() else selectExpense()
            setupCategorySpinner()
            showReceipt()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::localData.isInitialized) setupCategorySpinner()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_AMOUNT, binding.etAmount.text.toString())
        outState.putString(STATE_DESCRIPTION, binding.etDescription.text.toString())
        outState.putString(STATE_DATE, selectedDate)
        outState.putBoolean(STATE_IS_INCOME, isIncome)
        outState.putString(STATE_PHOTO_PATH, selectedPhotoPath)
        outState.putString(STATE_CATEGORY, (binding.spinnerCategory.selectedItem as? Category)?.name)
    }

    private fun loadTransactionForEditing() {
        val transactionId = intent.getStringExtra(EXTRA_TRANSACTION_ID) ?: return
        val transaction = localData.getTransaction(transactionId) ?: return
        editingTransaction = transaction
        binding.tvHeader.setText(R.string.edit_transaction)
        binding.btnSave.setText(R.string.update_transaction)
        binding.etAmount.setText(getString(R.string.plain_decimal_amount, transaction.amount))
        binding.etDescription.setText(transaction.note.orEmpty())
        selectedDate = transaction.date
        binding.tvDate.text = selectedDate
        selectedPhotoPath = transaction.photoPath
        preferredCategoryName = transaction.categoryId
        if (transaction.isIncome) selectIncome() else selectExpense()
        setupCategorySpinner()
        showReceipt()
    }

    private fun setupNavigation() {
        AppNavigation.bind(this, binding.bottomNavView, R.id.nav_add_transaction)
        binding.btnExpense.setOnClickListener { selectExpense() }
        binding.btnIncome.setOnClickListener { selectIncome() }
    }

    private fun setupCategorySpinner() {
        val currentSelection = preferredCategoryName ?: (binding.spinnerCategory.selectedItem as? Category)?.name
        val categories = buildList {
            add(Category(getString(R.string.select_a_category), ""))
            addAll(localData.getCategories())
            add(Category(getString(R.string.add_category_spinner), ""))
        }
        val adapter = object : ArrayAdapter<Category>(this, R.layout.spinner_item_category, categories) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = bind(position, convertView, parent)
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View = bind(position, convertView, parent)

            private fun bind(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: layoutInflater.inflate(R.layout.spinner_item_category, parent, false)
                val item = getItem(position) ?: return view
                view.findViewById<TextView>(R.id.text).text = item.name
                val iconView = view.findViewById<android.widget.ImageView>(R.id.imgIcon)
                val iconId = resources.getIdentifier(item.icon, "drawable", packageName)
                iconView.visibility = if (iconId == 0) View.GONE else View.VISIBLE
                if (iconId != 0) iconView.setImageResource(iconId)
                return view
            }
        }
        binding.spinnerCategory.adapter = adapter
        currentSelection?.let { selected ->
            categories.indexOfFirst { it.name == selected }.takeIf { it >= 0 }?.let(binding.spinnerCategory::setSelection)
        }
        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                preferredCategoryName = categories[position].name
                if (categories[position].name == getString(R.string.add_category_spinner)) {
                    startActivity(Intent(this@TransactionActivity, AddCategoryActivity::class.java))
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        }
    }

    private fun selectExpense() {
        isIncome = false
        binding.btnExpense.setBackgroundResource(R.drawable.bg_toggle_left_selected)
        binding.btnIncome.setBackgroundResource(R.drawable.bg_toggle_right_unselected)
        binding.btnExpense.setTextColor(ContextCompat.getColor(this, R.color.white))
        binding.btnIncome.setTextColor(ContextCompat.getColor(this, R.color.black))
    }

    private fun selectIncome() {
        isIncome = true
        binding.btnIncome.setBackgroundResource(R.drawable.bg_toggle_right_selected)
        binding.btnExpense.setBackgroundResource(R.drawable.bg_toggle_left_unselected)
        binding.btnIncome.setTextColor(ContextCompat.getColor(this, R.color.white))
        binding.btnExpense.setTextColor(ContextCompat.getColor(this, R.color.black))
    }

    private fun initDate() {
        selectedDate = SimpleDateFormat(DATE_PATTERN, Locale.US).format(Calendar.getInstance().time)
        binding.tvDate.text = selectedDate
        if (binding.etAmount.text.isNullOrBlank()) binding.etAmount.setText(R.string.zero_amount)
    }

    private fun showDatePicker() {
        val initial = runCatching {
            Calendar.getInstance().apply { time = requireNotNull(SimpleDateFormat(DATE_PATTERN, Locale.US).parse(selectedDate)) }
        }.getOrDefault(Calendar.getInstance())
        DatePickerDialog(this, { _, year, month, day ->
            val selected = Calendar.getInstance().apply { set(year, month, day) }
            selectedDate = SimpleDateFormat(DATE_PATTERN, Locale.US).format(selected.time)
            binding.tvDate.text = selectedDate
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showReceipt() {
        val file = selectedPhotoPath?.let(::File)?.takeIf(File::exists)
        binding.ivAttachedReceipt.visibility = if (file == null) View.GONE else View.VISIBLE
        binding.tvReceiptStatus.visibility = if (file == null) View.GONE else View.VISIBLE
        file?.let { Glide.with(this).load(it).placeholder(R.drawable.placeholder_image).into(binding.ivAttachedReceipt) }
    }

    private fun saveTransaction() {
        val amount = binding.etAmount.text.toString().removePrefix(userCurrencySymbol).trim().toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            toast(getString(R.string.enter_valid_amount))
            return
        }
        val selected = binding.spinnerCategory.selectedItem as? Category
        if (selected == null || binding.spinnerCategory.selectedItemPosition == 0 || selected.name == getString(R.string.add_category_spinner)) {
            toast(getString(R.string.please_select_category))
            return
        }
        val original = editingTransaction
        val transaction = Transaction(
            transactionId = original?.transactionId ?: UUID.randomUUID().toString(),
            userId = LocalDataStore.LOCAL_USER_ID,
            categoryId = selected.name,
            amount = amount,
            date = selectedDate,
            note = binding.etDescription.text.toString().takeIf(String::isNotBlank),
            isIncome = isIncome,
            photoPath = selectedPhotoPath
        )
        runCatching { localData.saveTransaction(transaction) }
            .onSuccess {
                toast(getString(if (original == null) R.string.transaction_saved else R.string.transaction_updated))
                AchievementManager.unlockAchievement("first_transaction", this)
                AchievementManager.checkStayWithinBudgetForMonth(LocalDataStore.LOCAL_USER_ID, selectedDate.take(7), this)
                if (original != null) finish() else resetForm()
            }
            .onFailure { toast(getString(R.string.transaction_save_failed)) }
    }

    private fun resetForm() {
        binding.etAmount.setText(R.string.zero_amount)
        binding.etDescription.text.clear()
        binding.spinnerCategory.setSelection(0)
        preferredCategoryName = null
        selectedPhotoPath = null
        showReceipt()
        selectExpense()
        initDate()
    }

    private fun toast(message: String) = ToastUtil.showCustomToast(this, message)

    companion object {
        const val EXTRA_TRANSACTION_ID = "transactionId"
        private const val DATE_PATTERN = "yyyy-MM-dd"
        private const val STATE_AMOUNT = "amount"
        private const val STATE_DESCRIPTION = "description"
        private const val STATE_DATE = "selectedDate"
        private const val STATE_IS_INCOME = "isIncome"
        private const val STATE_PHOTO_PATH = "photoPath"
        private const val STATE_CATEGORY = "category"
    }
}
