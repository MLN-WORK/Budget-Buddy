package com.example.budgetbuddy

import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetLimitCalculatorTest {
    @Test
    fun categoryTotalAtOrBelowMaximumHasNoExcess() {
        assertEquals(0.0, BudgetLimitCalculator.excess(620.0, 700.0), 0.001)
        assertEquals(0.0, BudgetLimitCalculator.excess(620.0, 620.0), 0.001)
    }

    @Test
    fun categoryTotalAboveMaximumReportsExactExcess() {
        assertEquals(120.0, BudgetLimitCalculator.excess(620.0, 500.0), 0.001)
        assertEquals(124, BudgetLimitCalculator.allocationPercentage(620.0, 500.0))
    }

    @Test
    fun invalidValuesNeverCreateAWarning() {
        assertEquals(0.0, BudgetLimitCalculator.excess(Double.NaN, 500.0), 0.001)
        assertEquals(0.0, BudgetLimitCalculator.excess(620.0, -1.0), 0.001)
    }
}
