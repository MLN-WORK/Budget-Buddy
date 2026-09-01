package com.budgetbuddy

import java.math.BigDecimal

/*
 * Start of class
 * Name of class and related classes (parent/child classes): Balance
 * Parent class: Any; child classes: none; related classes: FinanceCalculator, LocalDataStore, MainActivity, and AnalyticsActivity.
 * What the class does: Stores calculated income, expenses, and opening and closing balances.
 * What's important to other classes, if applicable: Consumers rely on its property meanings remaining stable across persistence, calculation, and display code.
 * Code with comments begins below.
 */
data class Balance(
    val startingBalance: Double = 0.0,
    val totalIncome:Double = 0.0,
    val totalExpenses:Double = 0.0,
    val closingBalance:Double = 0.0
)
// End of class: Balance
