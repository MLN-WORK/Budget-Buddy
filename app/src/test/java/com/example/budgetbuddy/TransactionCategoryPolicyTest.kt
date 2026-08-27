package com.example.budgetbuddy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionCategoryPolicyTest {
    @Test
    fun `income without a category uses a stable default`() {
        assertEquals(
            TransactionCategoryPolicy.DEFAULT_INCOME_CATEGORY,
            TransactionCategoryPolicy.persistedCategory(isIncome = true, selectedCategory = null)
        )
    }

    @Test
    fun `expense without a category remains invalid`() {
        assertNull(
            TransactionCategoryPolicy.persistedCategory(isIncome = false, selectedCategory = null)
        )
    }

    @Test
    fun `selected category is retained for either transaction type`() {
        assertEquals(
            "Side hustle",
            TransactionCategoryPolicy.persistedCategory(isIncome = true, selectedCategory = " Side hustle ")
        )
        assertEquals(
            "Groceries",
            TransactionCategoryPolicy.persistedCategory(isIncome = false, selectedCategory = "Groceries")
        )
    }
}
