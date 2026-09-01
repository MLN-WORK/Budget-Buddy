package com.budgetbuddy

/*
 * Start of class
 * Name of class and related classes (parent/child classes): BudgetLimitCalculator
 * Parent class: Any; child classes: none; related classes: BudgetActivity, AnalyticsActivity, and Budget.
 * What the class does: Provides pure spending-limit and allocation calculations.
 * What's important to other classes, if applicable: Related classes depend on this class keeping its inputs validated and its output contract deterministic.
 * Code with comments begins below.
 */
object BudgetLimitCalculator {
    fun excess(categoryTotal: Double, maximumBudget: Double): Double {
        if (!categoryTotal.isFinite() || !maximumBudget.isFinite() || maximumBudget < 0.0) return 0.0
        return (categoryTotal - maximumBudget).coerceAtLeast(0.0)
    }

    fun allocationPercentage(categoryTotal: Double, maximumBudget: Double): Int =
        AnalyticsCalculator.spentPercentage(categoryTotal, maximumBudget)

    fun allocationPercentageExact(categoryTotal: Double, maximumBudget: Double): Double =
        AnalyticsCalculator.spentPercentageExact(categoryTotal, maximumBudget)

    fun unallocatedPercentage(categoryTotal: Double, maximumBudget: Double): Double {
        if (!categoryTotal.isFinite() || !maximumBudget.isFinite() || maximumBudget <= 0.0) return 0.0
        return (((maximumBudget - categoryTotal).coerceAtLeast(0.0) / maximumBudget) * 100.0)
            .coerceIn(0.0, 100.0)
    }
}
// End of class: BudgetLimitCalculator
