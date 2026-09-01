package com.budgetbuddy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/*
 * Start of class
 * Name of class and related classes (parent/child classes): ReceiptCategoryClassifierTest
 * Parent class: Any; child classes: none; related classes: ReceiptCategoryClassifier, JUnit, and the application code under test.
 * What the class does: Verifies the ReceiptCategoryClassifier behavior and its regression cases.
 * What's important to other classes, if applicable: Its assertions document the behavior production classes must preserve.
 * Code with comments begins below.
 */
class ReceiptCategoryClassifierTest {
    private val categories = LocalDataStore.PRESET_CATEGORIES

    @Test
    fun `classifies common grocery and dining receipts`() {
        assertEquals(
            "Groceries",
            ReceiptCategoryClassifier.suggest("SUPERMARKET TOTAL 20.00", null, categories, "OCR")
        )
        assertEquals(
            "Dining",
            ReceiptCategoryClassifier.suggest("LATTE", "Corner Coffee", categories, "OCR")
        )
    }

    @Test
    fun `falls back to configured category`() {
        assertEquals("OCR", ReceiptCategoryClassifier.suggest("UNKNOWN SHOP", null, categories, "OCR"))
    }

    @Test
    fun `income detection is conservative`() {
        assertTrue(ReceiptCategoryClassifier.isLikelyIncome("PAYMENT RECEIVED"))
        assertFalse(ReceiptCategoryClassifier.isLikelyIncome("CARD PAYMENT TOTAL"))
    }
}
// End of class: ReceiptCategoryClassifierTest
