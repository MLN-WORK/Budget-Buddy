package com.example.budgetbuddy

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


