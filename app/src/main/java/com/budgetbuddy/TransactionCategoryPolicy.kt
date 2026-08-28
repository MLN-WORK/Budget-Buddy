package com.budgetbuddy

object TransactionCategoryPolicy {
    const val DEFAULT_INCOME_CATEGORY = "Income"

    fun persistedCategory(isIncome: Boolean, selectedCategory: String?): String? {
        val selected = selectedCategory?.trim()?.takeIf(String::isNotEmpty)
        return selected ?: DEFAULT_INCOME_CATEGORY.takeIf { isIncome }
    }
}
