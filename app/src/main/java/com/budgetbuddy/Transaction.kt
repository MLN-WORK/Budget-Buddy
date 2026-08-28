package com.budgetbuddy

data class Transaction(
    val transactionId: String = "",
    val userId: String = "",
    val categoryId: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val note: String? = null,
    val photoUrl: String? = null,
    val isIncome: Boolean = false,
    val photoPath: String? = null
)

data class TransactionDraft(
    val amount: String = "",
    val description: String = "",
    val date: String = "",
    val isIncome: Boolean = false,
    val categoryName: String? = null,
    val photoPath: String? = null
)

