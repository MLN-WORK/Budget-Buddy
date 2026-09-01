package com.budgetbuddy

/*
 * Start of class
 * Name of class and related classes (parent/child classes): Transaction
 * Parent class: Any; child classes: none; related classes: LocalDataStore, TransactionRepo, finance calculators, and transaction screens.
 * What the class does: Stores one persisted income or expense record.
 * What's important to other classes, if applicable: Consumers rely on its property meanings remaining stable across persistence, calculation, and display code.
 * Code with comments begins below.
 */
data class Transaction(
    val transactionId: String = "",
    val userId: String = "",
    val categoryId: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val note: String? = null,
    val photoUrl: String? = null,
    val isIncome: Boolean = false,
    val photoPath: String? = null,
    val addsToSpendingLimit: Boolean = false,
    val isOcr: Boolean = false
)
// End of class: Transaction

/*
 * Start of class
 * Name of class and related classes (parent/child classes): TransactionDraft
 * Parent class: Any; child classes: none; related classes: TransactionActivity and LocalDataStore.
 * What the class does: Stores the optional unfinished transaction form state.
 * What's important to other classes, if applicable: Consumers rely on its property meanings remaining stable across persistence, calculation, and display code.
 * Code with comments begins below.
 */
data class TransactionDraft(
    val amount: String = "",
    val description: String = "",
    val date: String = "",
    val isIncome: Boolean = false,
    val categoryName: String? = null,
    val photoPath: String? = null,
    val addsToSpendingLimit: Boolean = false,
    val isOcr: Boolean = false
)
// End of class: TransactionDraft
