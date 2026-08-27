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
import com.bumptech.glide.Glide
import com.example.budgetbuddy.databinding.ActivityTransactionBinding
import com.google.android.material.color.MaterialColors
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
    private var draftPhotoPath: String? = null
    private var isIncome = false
    private var editingTransaction: Transaction? = null
    private var preferredCategoryName: String? = null
    private var userCurrencySymbol = CurrencyCatalog.DEFAULT_SYMBOL

    private val imagePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val returned = result.data?.getStringExtra(AddImageActivity.EXTRA_IMAGE_PATH)?.let(::File)
            if (ReceiptStorage.isUsableOwnedReceipt(this, returned)) {
                ReceiptStorage.deleteIfOwned(this, draftPhotoPath)
                selectedPhotoPath = returned?.absolutePath
                draftPhotoPath = returned?.absolutePath
                showReceipt()
            } else {
                toast(getString(R.string.image_load_failed))
            }
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
            draftPhotoPath = it.getString(STATE_DRAFT_PHOTO_PATH)
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
        outState.putString(STATE_DRAFT_PHOTO_PATH, draftPhotoPath)
        outState.putString(STATE_CATEGORY, (binding.spinnerCategory.selectedItem as? Category)?.name)
    }

    override fun onDestroy() {
        if (isFinishing) ReceiptStorage.deleteIfOwned(this, draftPhotoPath)
        super.onDestroy()
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
        val prompts = setOf(
            getString(R.string.select_a_category),
            getString(R.string.select_optional_income_category)
        )
        val currentSelection = (preferredCategoryName ?: (binding.spinnerCategory.selectedItem as? Category)?.name)
            ?.takeUnless { it in prompts || it == TransactionCategoryPolicy.DEFAULT_INCOME_CATEGORY }
        val categories = buildList {
            add(Category(getString(
                if (isIncome) R.string.select_optional_income_category else R.string.select_a_category
            ), ""))
            addAll(localData.getCategories())
            add(Category(getString(R.string.add_category_spinner), ""))
        }
        val adapter = object : ArrayAdapter<Category>(this, R.layout.spinner_item_category, categories) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = bind(position, convertView, parent)
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View = bind(position, convertView, parent)

            private fun bind(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: layoutInflater.inflate(R.layout.spinner_item_category, parent, false)
                RuntimePaletteApplier.applyIfCustom(view)
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
        binding.btnExpense.setTextColor(MaterialColors.getColor(binding.root, R.attr.budgetOnPrimaryColor))
        binding.btnIncome.setTextColor(MaterialColors.getColor(binding.root, R.attr.budgetTextColor))
        if (::localData.isInitialized) setupCategorySpinner()
    }

    private fun selectIncome() {
        isIncome = true
        binding.btnIncome.setBackgroundResource(R.drawable.bg_toggle_right_selected)
        binding.btnExpense.setBackgroundResource(R.drawable.bg_toggle_left_unselected)
        binding.btnIncome.setTextColor(MaterialColors.getColor(binding.root, R.attr.budgetOnPrimaryColor))
        binding.btnExpense.setTextColor(MaterialColors.getColor(binding.root, R.attr.budgetTextColor))
        if (::localData.isInitialized) setupCategorySpinner()
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
        val file = selectedPhotoPath?.let(::File)?.takeIf { ReceiptStorage.isUsableOwnedReceipt(this, it) }
        binding.ivAttachedReceipt.visibility = if (file == null) View.GONE else View.VISIBLE
        binding.tvReceiptStatus.visibility = if (file == null) View.GONE else View.VISIBLE
        file?.let {
            runCatching {
                Glide.with(this)
                    .load(it)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .fitCenter()
                    .into(binding.ivAttachedReceipt)
            }.onFailure { toast(getString(R.string.image_load_failed)) }
        }
    }

    private fun saveTransaction() {
        val amount = binding.etAmount.text.toString().removePrefix(userCurrencySymbol).trim().toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            toast(getString(R.string.enter_valid_amount))
            return
        }
        val selected = (binding.spinnerCategory.selectedItem as? Category)?.takeIf {
            binding.spinnerCategory.selectedItemPosition > 0 &&
                it.name != getString(R.string.add_category_spinner)
        }
        if (!isIncome && selected == null) {
            toast(getString(R.string.please_select_category))
            return
        }
        val original = editingTransaction
        val transaction = Transaction(
            transactionId = original?.transactionId ?: UUID.randomUUID().toString(),
            userId = LocalDataStore.LOCAL_USER_ID,
            categoryId = requireNotNull(
                TransactionCategoryPolicy.persistedCategory(isIncome, selected?.name)
            ),
            amount = amount,
            date = selectedDate,
            note = binding.etDescription.text.toString().takeIf(String::isNotBlank),
            isIncome = isIncome,
            photoPath = selectedPhotoPath
        )
        runCatching { localData.saveTransaction(transaction) }
            .onSuccess {
                draftPhotoPath = null
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
        private const val STATE_DRAFT_PHOTO_PATH = "draftPhotoPath"
        private const val STATE_CATEGORY = "category"
    }
}
