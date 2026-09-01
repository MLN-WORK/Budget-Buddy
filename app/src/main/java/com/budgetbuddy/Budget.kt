package com.budgetbuddy

/*
 * Start of class
 * Name of class and related classes (parent/child classes): Budget
 * Parent class: Any; child classes: none; related classes: BudgetCategory, LocalDataStore, BudgetActivity, and AnalyticsActivity.
 * What the class does: Stores one month's spending limit and category allocations.
 * What's important to other classes, if applicable: Consumers rely on its property meanings remaining stable across persistence, calculation, and display code.
 * Code with comments begins below.
 */
data class Budget(
    var budgetAmount: Double = 0.0,
    val maximumSpendingBudget: Double = budgetAmount,
    val categories: Map<String, BudgetCategory> = emptyMap()
)
// End of class: Budget
