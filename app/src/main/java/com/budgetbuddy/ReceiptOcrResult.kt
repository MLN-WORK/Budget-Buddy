package com.budgetbuddy

/*
 * Start of class
 * Name of class and related classes (parent/child classes): ReceiptOcrResult
 * Parent class: Any; child classes: none; related classes: ReceiptOcrScanner, ReceiptParser, and TransactionActivity.
 * What the class does: Carries sanitized merchant, date, total, and source text returned by receipt recognition.
 * What's important to other classes, if applicable: Consumers rely on its property meanings remaining stable across persistence, calculation, and display code.
 * Code with comments begins below.
 */
data class ReceiptOcrResult(
    val merchant: String? = null,
    val date: String? = null,
    val total: Double? = null,
    val items: List<String> = emptyList(),
    val rawText: String = ""
) {
    val hasSuggestions: Boolean
        get() = !merchant.isNullOrBlank() || !date.isNullOrBlank() || total != null || items.isNotEmpty()
}
// End of class: ReceiptOcrResult
