package com.example.budgetbuddy

import org.junit.Assert.assertEquals
import org.junit.Test

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
}
