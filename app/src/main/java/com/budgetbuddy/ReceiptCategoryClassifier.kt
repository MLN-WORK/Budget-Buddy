package com.budgetbuddy

import java.util.Locale

/** Conservative on-device classification over recognized receipt text. */
/*
 * Start of class
 * Name of class and related classes (parent/child classes): ReceiptCategoryClassifier
 * Parent class: Any; child classes: none; related classes: ReceiptOcrResult, ReceiptParser, and TransactionActivity.
 * What the class does: Suggests a transaction category from parsed receipt text.
 * What's important to other classes, if applicable: OCR callers treat its output as a suggestion and must keep user review and input validation in place.
 * Code with comments begins below.
 */
object ReceiptCategoryClassifier {
    private val categoryKeywords = linkedMapOf(
        "Groceries" to listOf("supermarket", "grocery", "market", "foods", "woolworths", "checkers", "shoprite"),
        "Dining" to listOf("restaurant", "cafe", "coffee", "pizza", "burger", "grill", "kitchen"),
        "Transport" to listOf("fuel", "petrol", "diesel", "uber", "taxi", "train", "parking"),
        "Housing" to listOf("electricity", "water bill", "municipal", "rent", "hardware"),
        "Health" to listOf("pharmacy", "chemist", "clinic", "medical", "health"),
        "Entertainment" to listOf("cinema", "theatre", "game", "streaming", "tickets")
    )
    private val incomeKeywords = listOf("salary", "deposit", "payment received", "credit received", "refund")

    fun suggest(
        rawText: String,
        merchant: String?,
        availableCategories: List<Category>,
        fallback: String
    ): String {
        val searchable = "$merchant\n$rawText".lowercase(Locale.ROOT)
        val byName = availableCategories.associateBy { it.name.lowercase(Locale.ROOT) }
        val byId = availableCategories.associateBy(Category::id)
        val matched = categoryKeywords.entries.firstNotNullOfOrNull { (category, words) ->
            byName[category.lowercase(Locale.ROOT)]?.takeIf { words.any(searchable::contains) }
        }
        return matched?.id
            ?: byId[fallback]?.id
            ?: availableCategories.firstOrNull { it.name == fallback }?.id
            ?: LocalDataStore.OCR_CATEGORY
    }

    fun isLikelyIncome(rawText: String): Boolean {
        val searchable = rawText.lowercase(Locale.ROOT)
        return incomeKeywords.any(searchable::contains)
    }
}
// End of class: ReceiptCategoryClassifier
