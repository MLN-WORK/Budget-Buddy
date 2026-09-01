package com.budgetbuddy

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** App-private, device-local persistence. No data is sent off the device. */
/*
 * Start of class
 * Name of class and related classes (parent/child classes): LocalDataStore
 * Parent class: Any; child classes: none; related classes: all activities, finance models, ReceiptStorage, and AppearanceSelection.
 * What the class does: Owns validated, private, offline persistence for profiles, transactions, budgets, and settings.
 * What's important to other classes, if applicable: Other classes depend on its validation and ownership boundaries to keep financial and receipt data private and safe.
 * Code with comments begins below.
 */
class LocalDataStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val currencySymbol: String
        get() = preferences.getString(KEY_CURRENCY, DEFAULT_CURRENCY) ?: DEFAULT_CURRENCY

    val currencyCode: String
        get() = preferences.getString(KEY_CURRENCY_CODE, null)
            ?: CurrencyCatalog.findBySymbol(currencySymbol)?.code
            ?: CurrencyCatalog.DEFAULT_CODE

    val currencyName: String
        get() = preferences.getString(KEY_CURRENCY_NAME, null)
            ?: CurrencyCatalog.findByCode(currencyCode)?.name
            ?: getStringFallbackCurrencyName()

    val displayName: String
        get() = preferences.getString(KEY_DISPLAY_NAME, DEFAULT_DISPLAY_NAME) ?: DEFAULT_DISPLAY_NAME

    val buddyName: String
        get() = preferences.getString(KEY_BUDDY_NAME, DEFAULT_BUDDY_NAME) ?: DEFAULT_BUDDY_NAME

    val isDarkThemeEnabled: Boolean
        get() = when (appThemeMode) {
            AppThemeMode.DARK, AppThemeMode.AMOLED -> true
            AppThemeMode.CUSTOM -> AppearanceDefaults.perceivedLuminance(customMainColor) < 0.45
            else -> false
        }

    val isMaterialYouEnabled: Boolean
        get() = appThemeMode == AppThemeMode.MATERIAL_YOU

    val appThemeMode: AppThemeMode
        get() = AppThemeMode.fromStored(
            preferences.getString(KEY_APP_THEME_MODE, null),
            preferences.getBoolean(KEY_DARK_THEME, false),
            preferences.getBoolean(KEY_MATERIAL_YOU, false)
        )

    val customAccentColor: Int
        get() = preferences.getInt(KEY_CUSTOM_ACCENT, AppearanceDefaults.CUSTOM_ACCENT)

    val customMainColor: Int
        get() = preferences.getInt(KEY_CUSTOM_MAIN, AppearanceDefaults.CUSTOM_MAIN)

    val gaugePaletteMode: GaugePaletteMode
        get() = GaugePaletteMode.fromStored(preferences.getString(KEY_GAUGE_PALETTE_MODE, null))

    val customGaugePalette: GaugePalette
        get() = GaugePalette(
            good = preferences.getInt(KEY_GAUGE_GOOD, AppearanceDefaults.DEFAULT_GAUGE.good),
            okay = preferences.getInt(KEY_GAUGE_OKAY, AppearanceDefaults.DEFAULT_GAUGE.okay),
            bad = preferences.getInt(KEY_GAUGE_BAD, AppearanceDefaults.DEFAULT_GAUGE.bad)
        )

    val gaugePalette: GaugePalette
        get() = when (gaugePaletteMode) {
            GaugePaletteMode.DEFAULT -> AppearanceDefaults.DEFAULT_GAUGE
            GaugePaletteMode.COLOR_BLIND -> AppearanceDefaults.COLOR_BLIND_GAUGE
            GaugePaletteMode.CUSTOM -> customGaugePalette
        }

    val isProfileConfigured: Boolean
        get() = preferences.getBoolean(KEY_PROFILE_CONFIGURED, false)

    val preserveTransactionDrafts: Boolean
        get() = preferences.getBoolean(KEY_PRESERVE_TRANSACTION_DRAFTS, false)

    val useStatusMoneyColors: Boolean
        get() = preferences.getBoolean(KEY_USE_STATUS_MONEY_COLORS, true)

    val reviewOcrBeforeApplying: Boolean
        get() = preferences.getBoolean(KEY_REVIEW_OCR_BEFORE_APPLYING, false)

    val ocrCategoryName: String
        get() = runCatching { preferences.getString(KEY_OCR_CATEGORY_NAME, OCR_CATEGORY) }
            .getOrNull()
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: OCR_CATEGORY

    /** Stable category id used when OCR cannot make a more specific suggestion. */
    val ocrDefaultCategory: String
        get() {
            val stored = runCatching {
                preferences.getString(KEY_OCR_DEFAULT_CATEGORY, OCR_CATEGORY)
            }.getOrNull() ?: OCR_CATEGORY
            return getCategories().firstOrNull { it.id == stored || it.name == stored }?.id ?: OCR_CATEGORY
        }

    val isTutorialComplete: Boolean
        get() = preferences.getBoolean(KEY_TUTORIAL_COMPLETE, false)

    val hasTutorialState: Boolean
        get() = preferences.contains(KEY_TUTORIAL_INITIALIZED)

    fun setUseStatusMoneyColors(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_USE_STATUS_MONEY_COLORS, enabled).apply()
    }

    fun setReviewOcrBeforeApplying(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_REVIEW_OCR_BEFORE_APPLYING, enabled).apply()
    }

    fun setOcrDefaultCategory(categoryId: String) {
        val safe = getCategories().firstOrNull { it.id == categoryId || it.name == categoryId }?.id ?: OCR_CATEGORY
        preferences.edit().putString(KEY_OCR_DEFAULT_CATEGORY, safe).apply()
    }

    fun setOcrCategoryName(value: String): Boolean {
        val name = cleanSingleLine(value, MAX_CATEGORY_NAME_LENGTH)
        if (name.isBlank()) return false
        return runCatching {
            // Do not build the live category list through ocrCategoryName while that same
            // preference is being replaced. OCR keeps the stable id "OCR" forever; only
            // its user-facing label changes, so existing records and budgets remain linked.
            val duplicate = (PRESET_CATEGORIES.filterNot { it.id == OCR_CATEGORY } + getCustomCategories())
                .any { it.name.equals(name, ignoreCase = true) }
            if (duplicate) return@runCatching false
            preferences.edit()
                .remove(KEY_OCR_CATEGORY_NAME)
                .putString(KEY_OCR_CATEGORY_NAME, name)
                .commit()
        }.getOrDefault(false)
    }

    fun requireTutorial() {
        preferences.edit()
            .putBoolean(KEY_TUTORIAL_INITIALIZED, true)
            .putBoolean(KEY_TUTORIAL_COMPLETE, false)
            .apply()
    }

    fun completeTutorial() {
        preferences.edit()
            .putBoolean(KEY_TUTORIAL_INITIALIZED, true)
            .putBoolean(KEY_TUTORIAL_COMPLETE, true)
            .apply()
    }

    fun setPreserveTransactionDrafts(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_PRESERVE_TRANSACTION_DRAFTS, enabled).apply()
        if (!enabled) {
            ReceiptStorage.deleteIfOwned(appContext, getTransactionDraft()?.photoPath)
            clearTransactionDraft()
        }
    }

    fun saveTransactionDraft(draft: TransactionDraft) {
        preferences.edit().putString(
            KEY_TRANSACTION_DRAFT,
            JSONObject()
                .put("amount", draft.amount)
                .put("description", draft.description)
                .put("date", draft.date)
                .put("isIncome", draft.isIncome)
                .put("categoryName", draft.categoryName)
                .put("photoPath", draft.photoPath)
                .put("addsToSpendingLimit", draft.addsToSpendingLimit)
                .put("isOcr", draft.isOcr)
                .toString()
        ).apply()
    }

    fun getTransactionDraft(): TransactionDraft? = runCatching {
        val raw = preferences.getString(KEY_TRANSACTION_DRAFT, null) ?: return null
        val value = JSONObject(raw)
        TransactionDraft(
            amount = value.optString("amount"),
            description = value.optString("description"),
            date = value.optString("date"),
            isIncome = value.optBoolean("isIncome"),
            categoryName = value.optNullableString("categoryName"),
            photoPath = value.optNullableString("photoPath"),
            addsToSpendingLimit = value.optBoolean("addsToSpendingLimit"),
            isOcr = value.optBoolean("isOcr")
        )
    }.getOrNull()

    fun clearTransactionDraft() {
        preferences.edit().remove(KEY_TRANSACTION_DRAFT).apply()
    }

    fun saveProfile(
        displayName: String,
        currencySymbol: String,
        buddyName: String = DEFAULT_BUDDY_NAME,
        darkThemeEnabled: Boolean? = null,
        materialYouEnabled: Boolean? = null,
        currencyCode: String = CurrencyCatalog.findBySymbol(currencySymbol)?.code
            ?: CurrencyCatalog.DEFAULT_CODE,
        currencyName: String = CurrencyCatalog.findByCode(currencyCode)?.name ?: currencyCode
    ) {
        val safeDisplayName = cleanSingleLine(displayName, MAX_DISPLAY_NAME_LENGTH)
            .ifBlank { DEFAULT_DISPLAY_NAME }
        val safeBuddyName = cleanSingleLine(buddyName, MAX_BUDDY_NAME_LENGTH)
            .ifBlank { DEFAULT_BUDDY_NAME }
        val safeCurrencySymbol = cleanSingleLine(currencySymbol, MAX_CURRENCY_SYMBOL_LENGTH)
            .ifBlank { DEFAULT_CURRENCY }
        val safeCurrencyCode = cleanSingleLine(currencyCode, MAX_CURRENCY_CODE_LENGTH)
            .ifBlank { CurrencyCatalog.DEFAULT_CODE }
        val safeCurrencyName = cleanSingleLine(currencyName, MAX_CURRENCY_NAME_LENGTH)
            .ifBlank { safeCurrencyCode }
        val editor = preferences.edit()
            .putString(KEY_DISPLAY_NAME, safeDisplayName)
            .putString(KEY_BUDDY_NAME, safeBuddyName)
            .putString(KEY_CURRENCY, safeCurrencySymbol)
            .putString(KEY_CURRENCY_CODE, safeCurrencyCode)
            .putString(KEY_CURRENCY_NAME, safeCurrencyName)
            .putBoolean(KEY_PROFILE_CONFIGURED, true)
        if (darkThemeEnabled != null || materialYouEnabled != null) {
            val material = materialYouEnabled == true
            val dark = darkThemeEnabled == true && !material
            editor
                .putBoolean(KEY_DARK_THEME, dark)
                .putBoolean(KEY_MATERIAL_YOU, material)
                .putString(
                    KEY_APP_THEME_MODE,
                    if (material) AppThemeMode.MATERIAL_YOU.name
                    else if (dark) AppThemeMode.DARK.name
                    else AppThemeMode.LIGHT.name
                )
        }
        editor.apply()
    }

    fun saveAppearance(
        themeMode: AppThemeMode,
        customAccent: Int,
        customMain: Int,
        gaugeMode: GaugePaletteMode,
        customGauge: GaugePalette
    ) {
        preferences.edit()
            .putString(KEY_APP_THEME_MODE, themeMode.name)
            .putBoolean(
                KEY_DARK_THEME,
                themeMode == AppThemeMode.DARK || themeMode == AppThemeMode.AMOLED ||
                    (themeMode == AppThemeMode.CUSTOM &&
                        AppearanceDefaults.perceivedLuminance(customMain) < 0.45)
            )
            .putBoolean(KEY_MATERIAL_YOU, themeMode == AppThemeMode.MATERIAL_YOU)
            .putInt(KEY_CUSTOM_ACCENT, customAccent)
            .putInt(KEY_CUSTOM_MAIN, customMain)
            .putString(KEY_GAUGE_PALETTE_MODE, gaugeMode.name)
            .putInt(KEY_GAUGE_GOOD, customGauge.good)
            .putInt(KEY_GAUGE_OKAY, customGauge.okay)
            .putInt(KEY_GAUGE_BAD, customGauge.bad)
            .apply()
    }

    val shouldRequestInitialPermissions: Boolean
        get() = !preferences.getBoolean(KEY_INITIAL_PERMISSION_REQUESTED, false)

    fun markInitialPermissionsRequested() {
        preferences.edit().putBoolean(KEY_INITIAL_PERMISSION_REQUESTED, true).apply()
    }

    fun getCategories(): List<Category> = PRESET_CATEGORIES.map { category ->
        if (category.id == OCR_CATEGORY) {
            category.copy(name = ocrCategoryName, createdByUser = true)
        } else {
            category
        }
    } + getCustomCategories()

    fun categoryDisplayName(categoryId: String?): String = when {
        categoryId.isNullOrBlank() -> "Uncategorised"
        else -> getCategories().firstOrNull { it.id == categoryId || it.name == categoryId }?.name ?: categoryId
    }

    fun categoryIcon(categoryId: String?): String =
        getCategories().firstOrNull { it.id == categoryId || it.name == categoryId }?.icon ?: "ic_currency"

    fun addCategory(category: Category): Boolean {
        val safeName = cleanSingleLine(category.name, MAX_CATEGORY_NAME_LENGTH)
        if (safeName.isBlank() || safeName != category.name.trim()) return false
        val safeIcon = category.icon.takeIf(CategoryIconCatalog.selectableIcons::contains) ?: return false
        val categories = getCustomCategories().toMutableList()
        if (getCategories().any { it.name.equals(safeName, ignoreCase = true) }) return false
        categories += category.copy(name = safeName, icon = safeIcon, createdByUser = true, id = safeName)
        preferences.edit().putString(KEY_CATEGORIES, categories.toJson().toString()).apply()
        return true
    }

    fun getTransactions(): List<Transaction> = runCatching {
        val array = JSONArray(preferences.getString(KEY_TRANSACTIONS, "[]"))
        buildList {
            for (index in 0 until array.length()) add(array.getJSONObject(index).toTransaction())
        }.sortedBy { it.date }
    }.getOrDefault(emptyList())

    fun saveTransaction(transaction: Transaction) {
        require(transaction.transactionId.isNotBlank() && transaction.transactionId.length <= MAX_TRANSACTION_ID_LENGTH)
        require(transaction.amount.isFinite() && transaction.amount in 0.01..MAX_TRANSACTION_AMOUNT)
        require(isValidStoredDate(transaction.date))
        require(transaction.categoryId.length <= MAX_CATEGORY_ID_LENGTH)
        require(
            transaction.photoPath == null ||
                ReceiptStorage.isUsableOwnedReceipt(appContext, java.io.File(transaction.photoPath))
        ) { "The receipt image is not app-owned" }
        val safeTransaction = transaction.copy(
            userId = LOCAL_USER_ID,
            categoryId = transaction.categoryId.trim().take(MAX_CATEGORY_ID_LENGTH),
            note = transaction.note?.trim()?.take(MAX_TRANSACTION_NOTE_LENGTH)?.takeIf(String::isNotBlank)
        )
        val existing = getTransaction(safeTransaction.transactionId)
        val transactions = getTransactions()
            .filterNot { it.transactionId == safeTransaction.transactionId } + safeTransaction
        val array = JSONArray()
        transactions.forEach { array.put(it.toJson()) }
        preferences.edit().putString(KEY_TRANSACTIONS, array.toString()).apply()
        if (existing?.photoPath != safeTransaction.photoPath) deleteLocalReceipt(existing?.photoPath)
        rebuildBudgetSpending()
    }

    fun getTransaction(transactionId: String): Transaction? =
        getTransactions().firstOrNull { it.transactionId == transactionId }

    fun deleteTransaction(transactionId: String): Boolean {
        val existing = getTransactions()
        val remaining = existing.filterNot { it.transactionId == transactionId }
        if (remaining.size == existing.size) return false
        deleteLocalReceipt(existing.firstOrNull { it.transactionId == transactionId }?.photoPath)
        val array = JSONArray()
        remaining.forEach { array.put(it.toJson()) }
        preferences.edit().putString(KEY_TRANSACTIONS, array.toString()).apply()
        rebuildBudgetSpending()
        return true
    }

    fun getTransactions(start: String, end: String): List<Transaction> =
        getTransactions().filter { it.date in start..end }

    fun getBalance(month: String): Balance {
        return FinanceCalculator.balanceForMonth(getTransactions(), month)
    }

    fun getMonthlyExpenseTotal(displayMonth: String): Double {
        val monthKey = displayMonth.toMonthKey() ?: return 0.0
        return FinanceCalculator.balanceForMonth(getTransactions(), monthKey).totalExpenses
    }

    fun getIncomeAddedToSpendingLimit(displayMonth: String): Double {
        val monthKey = displayMonth.toMonthKey() ?: return 0.0
        return FinanceCalculator.incomeAddedToSpendingLimit(getTransactions(), monthKey)
    }

    fun getEffectiveSpendingLimit(displayMonth: String): Double {
        val monthKey = displayMonth.toMonthKey() ?: return 0.0
        return FinanceCalculator.effectiveSpendingLimit(
            baseLimit = getBudget(displayMonth)?.maximumSpendingBudget ?: 0.0,
            transactions = getTransactions(),
            monthKey = monthKey
        )
    }

    fun saveBudget(month: String, budget: Budget) {
        val budgets = readBudgets()
        budgets.put(month, budget.toJson())
        preferences.edit().putString(KEY_BUDGETS, budgets.toString()).apply()
        rebuildBudgetSpending()
    }

    fun getBudget(month: String): Budget? = runCatching {
        readBudgets().optJSONObject(month)?.toBudget()
    }.getOrNull()

    fun isAchievementCompleted(id: String): Boolean =
        readAchievements().optJSONObject(id)?.optBoolean("completed", false) ?: false

    fun achievementProgress(id: String): Int =
        readAchievements().optJSONObject(id)?.optInt("progress", 0) ?: 0

    fun saveAchievement(id: String, completed: Boolean, progress: Int) {
        val values = readAchievements()
        values.put(id, JSONObject().put("completed", completed).put("progress", progress))
        preferences.edit().putString(KEY_ACHIEVEMENTS, values.toString()).apply()
    }

    fun recordBudgetMonth(displayMonth: String): Set<String> {
        val normalizedMonth = displayMonth.toMonthKey() ?: return getBudgetMonths()
        val months = getBudgetMonths() + normalizedMonth
        preferences.edit().putStringSet(KEY_BUDGET_MONTHS, months).apply()
        return months
    }

    fun getBudgetMonths(): Set<String> =
        preferences.getStringSet(KEY_BUDGET_MONTHS, emptySet())?.toSet().orEmpty()

    /** Recalculates every stored budget from the transaction source of truth. */
    fun rebuildBudgetSpending() {
        val budgets = readBudgets()
        val rebuilt = JSONObject()
        val transactionsByMonth = getTransactions()
            .filterNot(Transaction::isIncome)
            .mapNotNull { transaction -> transaction.date.toDisplayMonth()?.let { it to transaction } }
            .groupBy({ it.first }, { it.second })

        val months = budgets.keys()
        while (months.hasNext()) {
            val month = months.next()
            val budget = budgets.getJSONObject(month).toBudget()
            val spentByCategory = transactionsByMonth[month].orEmpty()
                .groupBy(Transaction::categoryId)
                .mapValues { (_, transactions) -> transactions.sumOf(Transaction::amount) }
            val categories = budget.categories.mapValues { (name, category) ->
                category.copy(amountSpent = spentByCategory[name] ?: 0.0)
            }
            rebuilt.put(month, budget.copy(categories = categories).toJson())
        }
        preferences.edit().putString(KEY_BUDGETS, rebuilt.toString()).apply()
    }

    private fun getCustomCategories(): List<Category> = runCatching {
        val array = JSONArray(preferences.getString(KEY_CATEGORIES, "[]"))
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val name = item.optString("name")
                add(Category(name, item.optString("icon"), true, item.optString("id", name)))
            }
        }
    }.getOrDefault(emptyList())

    private fun List<Category>.toJson(): JSONArray = JSONArray().also { array ->
        forEach {
            array.put(JSONObject().put("name", it.name).put("icon", it.icon).put("id", it.id))
        }
    }

    private fun Transaction.toJson() = JSONObject()
        .put("transactionId", transactionId)
        .put("categoryId", categoryId)
        .put("amount", amount)
        .put("date", date)
        .put("note", note)
        .put("photoPath", photoPath)
        .put("isIncome", isIncome)
        .put("addsToSpendingLimit", addsToSpendingLimit)
        .put("isOcr", isOcr)

    private fun JSONObject.toTransaction(): Transaction {
        val isOcr = optBoolean("isOcr")
        val storedCategory = optString("categoryId").takeIf(String::isNotBlank)
        return Transaction(
            transactionId = optString("transactionId"),
            userId = LOCAL_USER_ID,
            categoryId = storedCategory ?: OCR_CATEGORY.takeIf { isOcr }.orEmpty(),
            amount = optDouble("amount"),
            date = optString("date"),
            note = optNullableString("note"),
            isIncome = optBoolean("isIncome"),
            photoPath = optNullableString("photoPath"),
            addsToSpendingLimit = optBoolean("addsToSpendingLimit"),
            isOcr = isOcr
        )
    }

    private fun Budget.toJson() = JSONObject()
        .put("budgetAmount", budgetAmount)
        .put("maximumSpendingBudget", maximumSpendingBudget)
        .put("categories", JSONObject().also { values ->
            categories.forEach { (name, category) ->
                values.put(name, JSONObject()
                    .put("name", category.name)
                    .put("icon", category.icon)
                    .put("allocation", category.allocation)
                    .put("amountSpent", category.amountSpent ?: 0.0))
            }
        })

    private fun JSONObject.toBudget(): Budget {
        val categoryValues = optJSONObject("categories") ?: JSONObject()
        val categories = mutableMapOf<String, BudgetCategory>()
        val keys = categoryValues.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = categoryValues.getJSONObject(key)
            categories[key] = BudgetCategory(
                name = value.optString("name", key),
                icon = value.optNullableString("icon"),
                allocation = value.optDouble("allocation"),
                amountSpent = value.optDouble("amountSpent"),
                id = key
            )
        }
        val categoryTotal = optDouble("budgetAmount").takeIf { it.isFinite() && it >= 0.0 }
            ?: categories.values.sumOf(BudgetCategory::allocation)
        val maximumBudget = optDouble("maximumSpendingBudget", Double.NaN)
            .takeIf { it.isFinite() && it > 0.0 }
            // Legacy budgets stored a minimum goal with different semantics. Defaulting to the
            // category total gives existing users a safe monthly maximum without changing data.
            ?: categoryTotal
        return Budget(categoryTotal, maximumBudget, categories)
    }

    private fun readBudgets() = runCatching {
        JSONObject(preferences.getString(KEY_BUDGETS, "{}") ?: "{}")
    }.getOrDefault(JSONObject())

    private fun readAchievements() = runCatching {
        JSONObject(preferences.getString(KEY_ACHIEVEMENTS, "{}") ?: "{}")
    }.getOrDefault(JSONObject())

    private fun JSONObject.optNullableString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)

    /** Removes control characters and bounds user-facing values before private persistence. */
    private fun cleanSingleLine(value: String, maxLength: Int): String =
        value.filterNot(Char::isISOControl).trim().take(maxLength)

    private fun isValidStoredDate(value: String): Boolean =
        value.length == 10 && runCatching { java.time.LocalDate.parse(value) }.isSuccess

    private fun deleteLocalReceipt(path: String?) {
        ReceiptStorage.deleteIfOwned(appContext, path)
    }

    private fun getStringFallbackCurrencyName(): String =
        if (currencyCode == CUSTOM_CURRENCY_CODE) "Custom currency" else currencyCode

    private fun String.toDisplayMonth(): String? = runCatching {
        val input = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val output = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
        output.format(requireNotNull(input.parse(this)))
    }.getOrNull()

    private fun String.toMonthKey(): String? = runCatching {
        val input = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).apply {
            isLenient = false
        }
        val output = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US)
        output.format(requireNotNull(input.parse(this)))
    }.getOrNull()

    companion object {
        const val LOCAL_USER_ID = "local-user"
        private const val PREFERENCES_NAME = "budget_buddy_offline_data"
        private const val KEY_TRANSACTIONS = "transactions"
        private const val KEY_CATEGORIES = "categories"
        private const val KEY_BUDGETS = "budgets"
        private const val KEY_ACHIEVEMENTS = "achievements"
        private const val KEY_CURRENCY = "currency"
        private const val KEY_CURRENCY_CODE = "currency_code"
        private const val KEY_CURRENCY_NAME = "currency_name"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_BUDDY_NAME = "buddy_name"
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_MATERIAL_YOU = "material_you"
        private const val KEY_APP_THEME_MODE = "app_theme_mode"
        private const val KEY_CUSTOM_ACCENT = "custom_accent"
        private const val KEY_CUSTOM_MAIN = "custom_main"
        private const val KEY_GAUGE_PALETTE_MODE = "gauge_palette_mode"
        private const val KEY_GAUGE_GOOD = "gauge_good"
        private const val KEY_GAUGE_OKAY = "gauge_okay"
        private const val KEY_GAUGE_BAD = "gauge_bad"
        private const val KEY_PROFILE_CONFIGURED = "profile_configured"
        private const val KEY_INITIAL_PERMISSION_REQUESTED = "initial_permission_requested"
        private const val KEY_BUDGET_MONTHS = "budget_months"
        private const val KEY_PRESERVE_TRANSACTION_DRAFTS = "preserve_transaction_drafts"
        private const val KEY_TRANSACTION_DRAFT = "transaction_draft"
        private const val KEY_USE_STATUS_MONEY_COLORS = "use_status_money_colors"
        private const val KEY_REVIEW_OCR_BEFORE_APPLYING = "review_ocr_before_applying"
        private const val KEY_OCR_DEFAULT_CATEGORY = "ocr_default_category"
        private const val KEY_OCR_CATEGORY_NAME = "ocr_category_name"
        private const val KEY_TUTORIAL_COMPLETE = "tutorial_complete"
        private const val KEY_TUTORIAL_INITIALIZED = "tutorial_initialized"
        private const val DEFAULT_CURRENCY = CurrencyCatalog.DEFAULT_SYMBOL
        private const val DEFAULT_DISPLAY_NAME = "Budget Buddy"
        const val DEFAULT_BUDDY_NAME = "Budster the Budgeter"
        const val MAX_BUDDY_NAME_LENGTH = 32
        const val MAX_CATEGORY_NAME_LENGTH = 40
        const val MAX_DISPLAY_NAME_LENGTH = 80
        const val MAX_CURRENCY_SYMBOL_LENGTH = 8
        const val MAX_CURRENCY_NAME_LENGTH = 80
        private const val MAX_CURRENCY_CODE_LENGTH = 12
        const val MAX_TRANSACTION_NOTE_LENGTH = 500
        private const val MAX_TRANSACTION_ID_LENGTH = 80
        private const val MAX_CATEGORY_ID_LENGTH = 80
        private const val MAX_TRANSACTION_AMOUNT = 1.0E15
        const val OCR_CATEGORY = "OCR"
        const val CUSTOM_CURRENCY_CODE = "CUSTOM"

        val PRESET_CATEGORIES = listOf(
            Category("Groceries", "ic_shopping_basket"),
            Category("Transport", "ic_car"),
            Category("Housing", "ic_house"),
            Category("Health", "ic_health_heart"),
            Category("Dining", "ic_forkin_knife"),
            Category("Entertainment", "ic_play_button"),
            Category("Salary", "ic_cash_paper"),
            Category(OCR_CATEGORY, "ic_eye", createdByUser = true, id = OCR_CATEGORY),
            Category("Other", "ic_currency")
        )
    }
}
// End of class: LocalDataStore
