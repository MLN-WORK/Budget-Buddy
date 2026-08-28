package com.budgetbuddy

data class Budget(
    var budgetAmount: Double = 0.0,
    val maximumSpendingBudget: Double = budgetAmount,
    val categories: Map<String, BudgetCategory> = emptyMap()
)
