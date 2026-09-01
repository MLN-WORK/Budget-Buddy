package com.budgetbuddy

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.budgetbuddy.databinding.ActivityTransactionBinding
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale
import java.util.UUID

/*
 * Start of class
 * Name of class and related classes (parent/child classes): TransactionActivity
 * Parent class: BaseActivity; child classes: none; related classes: Transaction, TransactionDraft, LocalDataStore, and AddImageActivity.
 * What the class does: Creates and edits validated income and expense transactions.
 * What's important to other classes, if applicable: It must preserve BaseActivity appearance behavior and use LocalDataStore as the offline source of truth.
 * Code with comments begins below.
 */
class TransactionActivity : BaseActivity() {
    private lateinit var binding: ActivityTransactionBinding
    private lateinit var localData: LocalDataStore
    private var selectedDate = ""
    private var selectedPhotoPath: String? = null
    private var draftPhotoPath: String? = null
    private var isIncome = false
    private var isOcrTransaction = false
    private var editingTransaction: Transaction? = null
    private var preferredCategoryId: String? = null
    private var userCurrencySymbol = CurrencyCatalog.DEFAULT_SYMBOL
    private val isTutorialMode: Boolean
        get() = intent.getIntExtra(TutorialFlow.EXTRA_STEP, -1) >= 0

    private val imagePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val returned = result.data?.getStringExtra(AddImageActivity.EXTRA_IMAGE_PATH)?.let(::File)
            if (ReceiptStorage.isUsableOwnedReceipt(this, returned)) {
                ReceiptStorage.deleteIfOwned(this, draftPhotoPath)
                selectedPhotoPath = returned?.absolutePath
                draftPhotoPath = returned?.absolutePath
                showReceipt()
                if (result.data?.getBooleanExtra(AddImageActivity.EXTRA_OCR_MODE, false) == true) {
                    isOcrTransaction = true
                    showReceiptSuggestions(result.data)
                }
            } else {
                toast(getString(R.string.image_load_failed))
            }
        }
    }

    private val categoryCreator = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            preferredCategoryId = result.data?.getStringExtra(AddCategoryActivity.EXTRA_CATEGORY_NAME)
        }
        setupCategorySpinner()
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
        binding.btnQuickAddCategory.setOnClickListener { launchCategoryCreator() }
        binding.btnSave.setOnClickListener { saveTransaction() }
        binding.btnAddImage.setOnClickListener { imagePicker.launch(Intent(this, AddImageActivity::class.java)) }
        binding.btnScanReceipt.setOnClickListener {
            imagePicker.launch(Intent(this, AddImageActivity::class.java).putExtra(AddImageActivity.EXTRA_OCR_MODE, true))
        }
        binding.ivAttachedReceipt.setOnClickListener { imagePicker.launch(Intent(this, AddImageActivity::class.java)) }

        savedInstanceState?.let {
            binding.etAmount.setText(it.getString(STATE_AMOUNT, ""))
            binding.etDescription.setText(it.getString(STATE_DESCRIPTION, ""))
            selectedDate = it.getString(STATE_DATE, selectedDate)
            binding.tvDate.text = selectedDate
            isIncome = it.getBoolean(STATE_IS_INCOME, false)
            isOcrTransaction = it.getBoolean(STATE_IS_OCR, false)
            binding.switchAddIncomeToLimit.isChecked = it.getBoolean(STATE_ADD_INCOME_TO_LIMIT, false)
            selectedPhotoPath = it.getString(STATE_PHOTO_PATH)
            draftPhotoPath = it.getString(STATE_DRAFT_PHOTO_PATH)
            preferredCategoryId = it.getString(STATE_CATEGORY)
            if (isIncome) selectIncome() else selectExpense()
            setupCategorySpinner()
            showReceipt()
        }
        if (savedInstanceState == null && editingTransaction == null && localData.preserveTransactionDrafts) {
            localData.getTransactionDraft()?.let(::restoreDraft)
        }
        if (savedInstanceState == null) consumeQuickOcrIntent()
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
        outState.putBoolean(STATE_IS_OCR, isOcrTransaction)
        outState.putBoolean(STATE_ADD_INCOME_TO_LIMIT, binding.switchAddIncomeToLimit.isChecked)
        outState.putString(STATE_PHOTO_PATH, selectedPhotoPath)
        outState.putString(STATE_DRAFT_PHOTO_PATH, draftPhotoPath)
        outState.putString(STATE_CATEGORY, (binding.spinnerCategory.selectedItem as? Category)?.id)
    }

    override fun onStop() {
        if (!isTutorialMode && ::localData.isInitialized && editingTransaction == null) {
            val draft = currentDraft()
            if (localData.preserveTransactionDrafts && draft.isMeaningful()) {
                localData.saveTransactionDraft(draft)
            } else {
                localData.clearTransactionDraft()
            }
        }
        super.onStop()
    }

    override fun onDestroy() {
        val shouldKeepPersistentReceipt = isTutorialMode || (::localData.isInitialized &&
            localData.preserveTransactionDrafts &&
            editingTransaction == null &&
            currentDraft().isMeaningful())
        if (isFinishing && !shouldKeepPersistentReceipt) {
            ReceiptStorage.deleteIfOwned(this, draftPhotoPath)
        }
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
        isOcrTransaction = transaction.isOcr
        binding.switchAddIncomeToLimit.isChecked = transaction.addsToSpendingLimit
        preferredCategoryId = transaction.categoryId
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
        val currentSelection = (preferredCategoryId ?: (binding.spinnerCategory.selectedItem as? Category)?.id)
            ?.takeUnless { it in prompts || it == TransactionCategoryPolicy.DEFAULT_INCOME_CATEGORY }
        val categories = buildList {
            add(Category(getString(
                if (isIncome) R.string.select_optional_income_category else R.string.select_a_category
            ), "", id = ""))
            addAll(localData.getCategories())
            add(Category(getString(R.string.add_category_spinner), "", id = ""))
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
                iconView.visibility = if (item.icon.isBlank()) View.GONE else View.VISIBLE
                if (item.icon.isNotBlank()) CategoryIconCatalog.bind(iconView, item.icon)
                return view
            }
        }
        binding.spinnerCategory.adapter = adapter
        currentSelection?.let { selected ->
            categories.indexOfFirst { it.id == selected || it.name == selected }
                .takeIf { it >= 0 }
                ?.let(binding.spinnerCategory::setSelection)
        }
        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (categories[position].name == getString(R.string.add_category_spinner)) {
                    launchCategoryCreator()
                } else {
                    preferredCategoryId = categories[position].id.takeIf(String::isNotBlank)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        }
    }

    private fun selectExpense() {
        isIncome = false
        binding.switchAddIncomeToLimit.visibility = View.GONE
        binding.btnExpense.setBackgroundResource(R.drawable.bg_toggle_left_selected)
        binding.btnIncome.setBackgroundResource(R.drawable.bg_toggle_right_unselected)
        binding.btnExpense.setTextColor(MaterialColors.getColor(binding.root, R.attr.budgetOnPrimaryColor))
        binding.btnIncome.setTextColor(MaterialColors.getColor(binding.root, R.attr.budgetTextColor))
        if (::localData.isInitialized) setupCategorySpinner()
    }

    private fun selectIncome() {
        isIncome = true
        binding.switchAddIncomeToLimit.visibility = View.VISIBLE
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

    private fun showReceiptSuggestions(data: Intent?) {
        val merchant = data?.getStringExtra(AddImageActivity.EXTRA_OCR_MERCHANT)
        val items = data?.getStringArrayListExtra(AddImageActivity.EXTRA_OCR_ITEMS).orEmpty()
        val date = data?.getStringExtra(AddImageActivity.EXTRA_OCR_DATE) ?: today()
        val total = data?.takeIf { it.hasExtra(AddImageActivity.EXTRA_OCR_TOTAL) }
            ?.getDoubleExtra(AddImageActivity.EXTRA_OCR_TOTAL, 0.0)
            ?.takeIf { it.isFinite() && it > 0.0 }
        val categoryId = data?.getStringExtra(AddImageActivity.EXTRA_OCR_CATEGORY) ?: localData.ocrDefaultCategory
        val categoryName = localData.categoryDisplayName(categoryId)
        val predictedIncome = data?.getBooleanExtra(AddImageActivity.EXTRA_OCR_IS_INCOME, false) == true
        if (!localData.reviewOcrBeforeApplying) {
            applyReceiptSuggestions(merchant, items, date, total, categoryId, predictedIncome)
            return
        }
        val missing = getString(R.string.receipt_ocr_not_found)
        val review = layoutInflater.inflate(R.layout.dialog_ocr_review, null)
        review.findViewById<TextView>(R.id.tvOcrReviewTitle).setText(
            if (predictedIncome) R.string.confirm_income else R.string.confirm_expense
        )
        review.findViewById<TextView>(R.id.tvOcrRecommendation).text = getString(
            R.string.buddy_ocr_recommendation,
            localData.buddyName
        )
        review.findViewById<TextView>(R.id.tvOcrMerchant).text = getString(R.string.ocr_merchant_value, merchant ?: missing)
        review.findViewById<TextView>(R.id.tvOcrDate).text = getString(R.string.ocr_date_value, date)
        review.findViewById<TextView>(R.id.tvOcrMonthWarning).apply {
            val scannedMonth = date.take(7)
            val currentMonth = today().take(7)
            visibility = if (scannedMonth != currentMonth) View.VISIBLE else View.GONE
            if (visibility == View.VISIBLE) {
                text = getString(
                    R.string.ocr_other_month_warning,
                    date,
                    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(java.util.Date())
                )
            }
        }
        review.findViewById<TextView>(R.id.tvOcrTotal).text = getString(
            R.string.ocr_total_value,
            total?.let { getString(R.string.money_amount, userCurrencySymbol, it) } ?: missing
        )
        review.findViewById<TextView>(R.id.tvOcrCategory).text = getString(R.string.ocr_category_value, categoryName)
        review.findViewById<TextView>(R.id.tvOcrItems).text = getString(
            R.string.ocr_items_value,
            items.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: missing
        )
        bindOcrBuddy(review, date, categoryId, total ?: 0.0, predictedIncome)
        MaterialAlertDialogBuilder(this)
            .setView(review)
            .setNegativeButton(R.string.keep_current_details, null)
            .setPositiveButton(R.string.apply_details) { _, _ ->
                applyReceiptSuggestions(merchant, items, date, total, categoryId, predictedIncome)
            }
            .show()
    }

    private fun applyReceiptSuggestions(
        merchant: String?,
        items: List<String>,
        date: String,
        total: Double?,
        categoryId: String,
        predictedIncome: Boolean
    ) {
        if (predictedIncome) selectIncome() else selectExpense()
        total?.let { binding.etAmount.setText(getString(R.string.plain_decimal_amount, it)) }
        date.takeIf { runCatching { LocalDate.parse(it) }.isSuccess }?.let {
            selectedDate = it
            binding.tvDate.text = it
        }
        if (binding.etDescription.text.isNullOrBlank()) {
            val suggestedDescription = listOfNotNull(
                merchant?.takeIf(String::isNotBlank),
                items.takeIf { it.isNotEmpty() }?.joinToString(", ")
            ).joinToString(" — ")
            if (suggestedDescription.isNotBlank()) binding.etDescription.setText(suggestedDescription)
        }
        preferredCategoryId = categoryId
        setupCategorySpinner()
    }

    private fun bindOcrBuddy(view: View, date: String?, categoryId: String, amount: Double, income: Boolean) {
        val displayMonth = runCatching {
            val parsed = requireNotNull(SimpleDateFormat(DATE_PATTERN, Locale.US).parse(date ?: today()))
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(parsed)
        }.getOrElse { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(java.util.Date()) }
        val budget = localData.getBudget(displayMonth)
        val categoryBudget = budget?.categories?.get(categoryId)
        val image = view.findViewById<ImageView>(R.id.imgOcrBuddy)
        val status = view.findViewById<TextView>(R.id.tvOcrBudgetImpact)
        if (income || amount <= 0.0) {
            image.setImageResource(R.drawable.neutral_buddy_1)
            status.setText(R.string.ocr_income_buddy_message)
            return
        }
        val monthKey = runCatching {
            SimpleDateFormat("yyyy-MM", Locale.US).format(
                requireNotNull(SimpleDateFormat("MMMM yyyy", Locale.getDefault()).parse(displayMonth))
            )
        }.getOrDefault("")
        val (spent, limit, categorySpecific) = if (categoryBudget != null && categoryBudget.allocation > 0.0) {
            val categorySpent = localData.getTransactions()
                .filter { !it.isIncome && it.categoryId == categoryId && it.date.startsWith(monthKey) }
                .sumOf(Transaction::amount)
            Triple(categorySpent + amount, categoryBudget.allocation, true)
        } else {
            Triple(localData.getMonthlyExpenseTotal(displayMonth) + amount, localData.getEffectiveSpendingLimit(displayMonth), false)
        }
        if (limit <= 0.0) {
            image.setImageResource(R.drawable.happy_buddy)
            status.setText(R.string.ocr_no_budget_message)
            return
        }
        when (AnalyticsCalculator.buddyMood(spent, limit)) {
            AnalyticsCalculator.BuddyMood.HAPPY -> image.setImageResource(R.drawable.neutral_buddy_1)
            AnalyticsCalculator.BuddyMood.NEUTRAL -> image.setImageResource(R.drawable.happy_buddy)
            AnalyticsCalculator.BuddyMood.ANGRY -> image.setImageResource(R.drawable.angry_buddy)
        }
        status.text = getString(
            if (categorySpecific) R.string.ocr_category_budget_impact else R.string.ocr_month_budget_impact,
            AnalyticsCalculator.spentPercentage(spent, limit)
        )
    }

    private fun consumeQuickOcrIntent() {
        if (!intent.getBooleanExtra(EXTRA_QUICK_OCR, false)) return
        val file = intent.getStringExtra(AddImageActivity.EXTRA_IMAGE_PATH)
            ?.let(::File)
            ?.takeIf { ReceiptStorage.isUsableOwnedReceipt(this, it) }
            ?: return
        selectedPhotoPath = file.absolutePath
        draftPhotoPath = file.absolutePath
        isOcrTransaction = true
        showReceipt()
        binding.root.post { showReceiptSuggestions(intent) }
        intent.removeExtra(EXTRA_QUICK_OCR)
    }

    private fun launchCategoryCreator() {
        val selected = (binding.spinnerCategory.selectedItem as? Category)
        if (selected != null && selected.id.isNotBlank()) {
            preferredCategoryId = selected.id
        }
        categoryCreator.launch(Intent(this, AddCategoryActivity::class.java))
    }

    private fun restoreDraft(draft: TransactionDraft) {
        binding.etAmount.setText(draft.amount)
        binding.etDescription.setText(draft.description)
        selectedDate = draft.date.takeIf(String::isNotBlank) ?: today()
        binding.tvDate.text = selectedDate
        selectedPhotoPath = draft.photoPath
        draftPhotoPath = draft.photoPath
        preferredCategoryId = draft.categoryName
        isOcrTransaction = draft.isOcr
        binding.switchAddIncomeToLimit.isChecked = draft.addsToSpendingLimit
        if (draft.isIncome) selectIncome() else selectExpense()
        setupCategorySpinner()
        showReceipt()
    }

    private fun currentDraft(): TransactionDraft {
        fun String?.validCategory(): String? = this?.takeIf(String::isNotBlank)
        val category = (binding.spinnerCategory.selectedItem as? Category)?.id.validCategory()
        return TransactionDraft(
            amount = binding.etAmount.text.toString(),
            description = binding.etDescription.text.toString(),
            date = selectedDate,
            isIncome = isIncome,
            categoryName = category ?: preferredCategoryId.validCategory()
                ?: localData.ocrDefaultCategory.takeIf { isOcrTransaction },
            photoPath = selectedPhotoPath,
            addsToSpendingLimit = isIncome && binding.switchAddIncomeToLimit.isChecked,
            isOcr = isOcrTransaction
        )
    }

    private fun TransactionDraft.isMeaningful(): Boolean =
        amount.toDoubleOrNull()?.let { it > 0.0 } == true ||
            description.isNotBlank() ||
            !categoryName.isNullOrBlank() ||
            !photoPath.isNullOrBlank() ||
            isIncome ||
            addsToSpendingLimit ||
            isOcr ||
            (date.isNotBlank() && date != today())

    private fun today(): String =
        SimpleDateFormat(DATE_PATTERN, Locale.US).format(Calendar.getInstance().time)

    private fun saveTransaction() {
        val amount = binding.etAmount.text.toString().removePrefix(userCurrencySymbol).trim().toDoubleOrNull()
        if (amount == null || !amount.isFinite() || amount <= 0.0) {
            toast(getString(R.string.enter_valid_amount))
            return
        }
        val selected = (binding.spinnerCategory.selectedItem as? Category)?.takeIf {
            binding.spinnerCategory.selectedItemPosition > 0 &&
                it.name != getString(R.string.add_category_spinner)
        }
        if (!isIncome && selected == null && !isOcrTransaction) {
            toast(getString(R.string.please_select_category))
            return
        }
        val original = editingTransaction
        val transaction = Transaction(
            transactionId = original?.transactionId ?: UUID.randomUUID().toString(),
            userId = LocalDataStore.LOCAL_USER_ID,
            categoryId = requireNotNull(TransactionCategoryPolicy.persistedCategory(
                isIncome,
                selected?.id ?: localData.ocrDefaultCategory.takeIf { isOcrTransaction }
            )),
            amount = amount,
            date = selectedDate,
            note = binding.etDescription.text.toString().takeIf(String::isNotBlank),
            isIncome = isIncome,
            photoPath = selectedPhotoPath,
            addsToSpendingLimit = isIncome && binding.switchAddIncomeToLimit.isChecked,
            isOcr = isOcrTransaction
        )
        runCatching { localData.saveTransaction(transaction) }
            .onSuccess {
                draftPhotoPath = null
                localData.clearTransactionDraft()
                toast(getString(if (original == null) R.string.transaction_saved else R.string.transaction_updated))
                AchievementManager.unlockAchievement("first_transaction", this)
                AchievementManager.checkStayWithinBudgetForMonth(LocalDataStore.LOCAL_USER_ID, selectedDate.take(7), this)
                if (original != null) finish() else resetForm()
            }
            .onFailure { toast(getString(R.string.transaction_save_failed)) }
    }

    private fun resetForm() {
        localData.clearTransactionDraft()
        binding.etAmount.setText(R.string.zero_amount)
        binding.etDescription.text.clear()
        binding.spinnerCategory.setSelection(0)
        preferredCategoryId = null
        selectedPhotoPath = null
        isOcrTransaction = false
        binding.switchAddIncomeToLimit.isChecked = false
        showReceipt()
        selectExpense()
        initDate()
    }

    private fun toast(message: String) = ToastUtil.showCustomToast(this, message)

    companion object {
        const val EXTRA_TRANSACTION_ID = "transactionId"
        const val EXTRA_QUICK_OCR = "quickOcr"
        private const val DATE_PATTERN = "yyyy-MM-dd"
        private const val STATE_AMOUNT = "amount"
        private const val STATE_DESCRIPTION = "description"
        private const val STATE_DATE = "selectedDate"
        private const val STATE_IS_INCOME = "isIncome"
        private const val STATE_PHOTO_PATH = "photoPath"
        private const val STATE_DRAFT_PHOTO_PATH = "draftPhotoPath"
        private const val STATE_CATEGORY = "category"
        private const val STATE_ADD_INCOME_TO_LIMIT = "addIncomeToLimit"
        private const val STATE_IS_OCR = "isOcr"
    }
}
// End of class: TransactionActivity
