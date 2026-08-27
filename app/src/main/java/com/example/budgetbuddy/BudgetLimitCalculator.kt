package com.example.budgetbuddy

object BudgetLimitCalculator {
    fun excess(categoryTotal: Double, maximumBudget: Double): Double {
        if (!categoryTotal.isFinite() || !maximumBudget.isFinite() || maximumBudget < 0.0) return 0.0
        return (categoryTotal - maximumBudget).coerceAtLeast(0.0)
    }

    fun allocationPercentage(categoryTotal: Double, maximumBudget: Double): Int =
        AnalyticsCalculator.spentPercentage(categoryTotal, maximumBudget)
}
