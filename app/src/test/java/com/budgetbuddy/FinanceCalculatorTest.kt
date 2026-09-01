package com.budgetbuddy

import org.junit.Assert.assertEquals
import org.junit.Test

/*
 * Start of class
 * Name of class and related classes (parent/child classes): FinanceCalculatorTest
 * Parent class: Any; child classes: none; related classes: FinanceCalculator, JUnit, and the application code under test.
 * What the class does: Verifies the FinanceCalculator behavior and its regression cases.
 * What's important to other classes, if applicable: Its assertions document the behavior production classes must preserve.
 * Code with comments begins below.
 */
class FinanceCalculatorTest {
    private val transactions = listOf(
        Transaction("1", categoryId = "Salary", amount = 1_000.0, date = "2026-08-01", isIncome = true),
        Transaction("2", categoryId = "Groceries", amount = 125.5, date = "2026-08-05"),
        Transaction("3", categoryId = "Groceries", amount = 74.5, date = "2026-08-15"),
        Transaction("5", categoryId = "Unbudgeted custom category", amount = 25.0, date = "2026-08-20"),
        Transaction("4", categoryId = "Transport", amount = 50.0, date = "2026-07-31")
    )

    @Test
    fun balanceForMonthSeparatesIncomeAndExpenses() {
        val balance = FinanceCalculator.balanceForMonth(transactions, "2026-08")

        assertEquals(1_000.0, balance.totalIncome, 0.001)
        assertEquals(225.0, balance.totalExpenses, 0.001)
        assertEquals(775.0, balance.closingBalance, 0.001)
    }

    @Test
    fun inDateRangeUsesInclusiveIsoDateBoundaries() {
        val result = FinanceCalculator.inDateRange(transactions, "2026-08-05", "2026-08-15")

        assertEquals(listOf("2", "3"), result.map(Transaction::transactionId))
    }

    @Test
    fun expenseTotalsExcludeIncomeAndGroupCategories() {
        val totals = FinanceCalculator.expenseTotalsByCategory(transactions)

        assertEquals(200.0, totals["Groceries"] ?: 0.0, 0.001)
        assertEquals(25.0, totals["Unbudgeted custom category"] ?: 0.0, 0.001)
        assertEquals(50.0, totals["Transport"] ?: 0.0, 0.001)
        assertEquals(false, totals.containsKey("Salary"))
    }

    @Test
    fun phoneRegressionDatasetReconcilesAcrossHomeAndAnalytics() {
        val phoneTransactions = listOf(
            Transaction("dining", categoryId = "Dining", amount = 555.0, date = "2026-08-29"),
            Transaction("groceries", categoryId = "Groceries", amount = 204.0, date = "2026-08-29"),
            Transaction("salary", categoryId = "Salary", amount = 348.0, date = "2026-08-29", isIncome = true),
            Transaction("bonus", categoryId = "Salary", amount = 200.0, date = "2026-08-29", isIncome = true, addsToSpendingLimit = true),
            Transaction("small-expense", categoryId = "Other", amount = 2.0, date = "2026-08-29")
        )

        val balance = FinanceCalculator.balanceForMonth(phoneTransactions, "2026-08")
        val effectiveLimit = FinanceCalculator.effectiveSpendingLimit(2000.0, phoneTransactions, "2026-08")

        assertEquals(548.0, balance.totalIncome, 0.001)
        assertEquals(761.0, balance.totalExpenses, 0.001)
        assertEquals(-213.0, balance.closingBalance, 0.001)
        assertEquals(200.0, FinanceCalculator.incomeAddedToSpendingLimit(phoneTransactions, "2026-08"), 0.001)
        assertEquals(2200.0, effectiveLimit, 0.001)
        assertEquals(1439.0, effectiveLimit - balance.totalExpenses, 0.001)
        assertEquals(34.59, AnalyticsCalculator.spentPercentageExact(balance.totalExpenses, effectiveLimit), 0.005)
    }
}
// End of class: FinanceCalculatorTest
