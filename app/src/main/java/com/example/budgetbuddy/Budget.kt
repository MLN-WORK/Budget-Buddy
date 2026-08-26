package com.example.budgetbuddy

import java.math.BigDecimal

data class Budget(
    var budgetAmount:Double = 0.0,
    val minimumGoal:Double = 0.0,
    val categories:Map<String, BudgetCategory> = emptyMap()
    //string is category name
)
