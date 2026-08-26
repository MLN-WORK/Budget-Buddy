package com.example.budgetbuddy

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** App-private, device-local persistence. No data is sent off the device. */
class LocalDataStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val currencySymbol: String
        get() = preferences.getString(KEY_CURRENCY, DEFAULT_CURRENCY) ?: DEFAULT_CURRENCY

    val displayName: String
        get() = preferences.getString(KEY_DISPLAY_NAME, DEFAULT_DISPLAY_NAME) ?: DEFAULT_DISPLAY_NAME

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
        val transactions = getTransactions().filterNot { it.transactionId == transaction.transactionId } + transaction
        val array = JSONArray()
        transactions.forEach { array.put(it.toJson()) }
        preferences.edit().putString(KEY_TRANSACTIONS, array.toString()).apply()
        if (!transaction.isIncome) updateBudgetSpending(transaction)
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

    private fun updateBudgetSpending(transaction: Transaction) {
        val month = transaction.date.toDisplayMonth() ?: return
        val budget = getBudget(month) ?: return
        val category = budget.categories[transaction.categoryId] ?: return
        val updated = budget.categories.toMutableMap()
        updated[transaction.categoryId] = category.copy(
            amountSpent = (category.amountSpent ?: 0.0) + transaction.amount
        )
        saveBudget(month, budget.copy(categories = updated))
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

    private fun String.toDisplayMonth(): String? = runCatching {
        val input = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val output = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
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
        private const val DEFAULT_CURRENCY = "R"
        private const val DEFAULT_DISPLAY_NAME = "Budget Buddy"

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
