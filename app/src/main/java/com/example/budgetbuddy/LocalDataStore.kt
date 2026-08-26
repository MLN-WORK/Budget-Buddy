package com.example.budgetbuddy

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** App-private, device-local persistence. No data is sent off the device. */
class LocalDataStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val currencySymbol: String
        get() = preferences.getString(KEY_CURRENCY, DEFAULT_CURRENCY) ?: DEFAULT_CURRENCY

    val displayName: String
        get() = preferences.getString(KEY_DISPLAY_NAME, DEFAULT_DISPLAY_NAME) ?: DEFAULT_DISPLAY_NAME

    val buddyName: String
        get() = preferences.getString(KEY_BUDDY_NAME, DEFAULT_BUDDY_NAME) ?: DEFAULT_BUDDY_NAME

    val isDarkThemeEnabled: Boolean
        get() = preferences.getBoolean(KEY_DARK_THEME, false)

    val isProfileConfigured: Boolean
        get() = preferences.getBoolean(KEY_PROFILE_CONFIGURED, false)

    fun saveProfile(
        displayName: String,
        currencySymbol: String,
        buddyName: String = DEFAULT_BUDDY_NAME,
        darkThemeEnabled: Boolean = isDarkThemeEnabled
    ) {
        preferences.edit()
            .putString(KEY_DISPLAY_NAME, displayName.trim())
            .putString(KEY_BUDDY_NAME, buddyName.trim().take(MAX_BUDDY_NAME_LENGTH))
            .putString(KEY_CURRENCY, currencySymbol)
            .putBoolean(KEY_DARK_THEME, darkThemeEnabled)
            .putBoolean(KEY_PROFILE_CONFIGURED, true)
            .apply()
    }

    val shouldRequestInitialPermissions: Boolean
        get() = !preferences.getBoolean(KEY_INITIAL_PERMISSION_REQUESTED, false)

    fun markInitialPermissionsRequested() {
        preferences.edit().putBoolean(KEY_INITIAL_PERMISSION_REQUESTED, true).apply()
    }

    fun getCategories(): List<Category> = PRESET_CATEGORIES + getCustomCategories()

    fun addCategory(category: Category): Boolean {
        val categories = getCustomCategories().toMutableList()
        if (getCategories().any { it.name.equals(category.name, ignoreCase = true) }) return false
        categories += category.copy(createdByUser = true)
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
        val existing = getTransaction(transaction.transactionId)
        val transactions = getTransactions().filterNot { it.transactionId == transaction.transactionId } + transaction
        val array = JSONArray()
        transactions.forEach { array.put(it.toJson()) }
        preferences.edit().putString(KEY_TRANSACTIONS, array.toString()).apply()
        if (existing?.photoPath != transaction.photoPath) deleteLocalReceipt(existing?.photoPath)
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
                add(Category(item.optString("name"), item.optString("icon"), true))
            }
        }
    }.getOrDefault(emptyList())

    private fun List<Category>.toJson(): JSONArray = JSONArray().also { array ->
        forEach { array.put(JSONObject().put("name", it.name).put("icon", it.icon)) }
    }

    private fun Transaction.toJson() = JSONObject()
        .put("transactionId", transactionId)
        .put("categoryId", categoryId)
        .put("amount", amount)
        .put("date", date)
        .put("note", note)
        .put("photoPath", photoPath)
        .put("isIncome", isIncome)

    private fun JSONObject.toTransaction() = Transaction(
        transactionId = optString("transactionId"),
        userId = LOCAL_USER_ID,
        categoryId = optString("categoryId"),
        amount = optDouble("amount"),
        date = optString("date"),
        note = optNullableString("note"),
        isIncome = optBoolean("isIncome"),
        photoPath = optNullableString("photoPath")
    )

    private fun Budget.toJson() = JSONObject()
        .put("budgetAmount", budgetAmount)
        .put("minimumGoal", minimumGoal)
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
                amountSpent = value.optDouble("amountSpent")
            )
        }
        return Budget(optDouble("budgetAmount"), optDouble("minimumGoal"), categories)
    }

    private fun readBudgets() = runCatching {
        JSONObject(preferences.getString(KEY_BUDGETS, "{}") ?: "{}")
    }.getOrDefault(JSONObject())

    private fun readAchievements() = runCatching {
        JSONObject(preferences.getString(KEY_ACHIEVEMENTS, "{}") ?: "{}")
    }.getOrDefault(JSONObject())

    private fun JSONObject.optNullableString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)

    private fun deleteLocalReceipt(path: String?) {
        val receipt = path?.let(::File) ?: return
        val receiptDirectories = buildList {
            add(File(appContext.filesDir, "receipts").canonicalFile)
            appContext.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)?.let {
                add(File(it, "BudgetBuddy/Receipts").canonicalFile)
            }
        }
        val candidate = runCatching { receipt.canonicalFile }.getOrNull() ?: return
        if (candidate.parentFile?.let { it in receiptDirectories } == true && candidate.isFile) candidate.delete()
    }

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
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_BUDDY_NAME = "buddy_name"
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_PROFILE_CONFIGURED = "profile_configured"
        private const val KEY_INITIAL_PERMISSION_REQUESTED = "initial_permission_requested"
        private const val KEY_BUDGET_MONTHS = "budget_months"
        private const val DEFAULT_CURRENCY = "R"
        private const val DEFAULT_DISPLAY_NAME = "Budget Buddy"
        const val DEFAULT_BUDDY_NAME = "Budster the Budgeter"
        const val MAX_BUDDY_NAME_LENGTH = 32

        val PRESET_CATEGORIES = listOf(
            Category("Groceries", "ic_shopping_basket"),
            Category("Transport", "ic_car"),
            Category("Housing", "ic_house"),
            Category("Health", "ic_health_heart"),
            Category("Dining", "ic_forkin_knife"),
            Category("Entertainment", "ic_play_button"),
            Category("Salary", "ic_cash_paper"),
            Category("Other", "ic_currency")
        )
    }
}
