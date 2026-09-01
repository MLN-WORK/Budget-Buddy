package com.budgetbuddy

/*
 * Start of class
 * Name of class and related classes (parent/child classes): TransactionCategoryPolicy
 * Parent class: Any; child classes: none; related classes: TransactionActivity, Category, and LocalDataStore.
 * What the class does: Applies category rules for income, expenses, and spending-limit treatment.
 * What's important to other classes, if applicable: Related classes depend on this class keeping its inputs validated and its output contract deterministic.
 * Code with comments begins below.
 */
object TransactionCategoryPolicy {
    const val DEFAULT_INCOME_CATEGORY = "Income"

    fun persistedCategory(isIncome: Boolean, selectedCategory: String?): String? {
        val selected = selectedCategory?.trim()?.takeIf(String::isNotEmpty)
        return selected ?: DEFAULT_INCOME_CATEGORY.takeIf { isIncome }
    }
}
// End of class: TransactionCategoryPolicy
