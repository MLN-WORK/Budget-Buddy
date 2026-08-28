package com.budgetbuddy

import java.math.BigDecimal

data class Balance(
    val startingBalance: Double = 0.0,
    val totalIncome:Double = 0.0,
    val totalExpenses:Double = 0.0,
    val closingBalance:Double = 0.0
)
