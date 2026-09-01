package com.budgetbuddy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/*
 * Start of class
 * Name of class and related classes (parent/child classes): TransactionCategoryPolicyTest
 * Parent class: Any; child classes: none; related classes: TransactionCategoryPolicy, JUnit, and the application code under test.
 * What the class does: Verifies the TransactionCategoryPolicy behavior and its regression cases.
 * What's important to other classes, if applicable: Its assertions document the behavior production classes must preserve.
 * Code with comments begins below.
 */
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
// End of class: TransactionCategoryPolicyTest
