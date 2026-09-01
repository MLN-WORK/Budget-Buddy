package com.budgetbuddy

/*
 * Start of class
 * Name of class and related classes (parent/child classes): BudgetCategory
 * Parent class: Any; child classes: none; related classes: Budget, BudgetCategoryAdapter, and BudgetLimitCalculator.
 * What the class does: Stores one category allocation and its calculated spending.
 * What's important to other classes, if applicable: Consumers rely on its property meanings remaining stable across persistence, calculation, and display code.
 * Code with comments begins below.
 */
data class BudgetCategory(
    var name: String = "",
    var icon: String? = "",
    var allocation: Double = 0.0,
    val amountSpent: Double? = 0.0,
    val id: String = name
)
// End of class: BudgetCategory
