package com.budgetbuddy

import org.junit.Assert.assertEquals
import org.junit.Test

/*
 * Start of class
 * Name of class and related classes (parent/child classes): BudgetLimitCalculatorTest
 * Parent class: Any; child classes: none; related classes: BudgetLimitCalculator, JUnit, and the application code under test.
 * What the class does: Verifies the BudgetLimitCalculator behavior and its regression cases.
 * What's important to other classes, if applicable: Its assertions document the behavior production classes must preserve.
 * Code with comments begins below.
 */
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
        assertEquals(124.0, BudgetLimitCalculator.allocationPercentageExact(620.0, 500.0), 0.001)
    }

    @Test
    fun invalidValuesNeverCreateAWarning() {
        assertEquals(0.0, BudgetLimitCalculator.excess(Double.NaN, 500.0), 0.001)
        assertEquals(0.0, BudgetLimitCalculator.excess(620.0, -1.0), 0.001)
    }

    @Test
    fun unallocatedPercentageShowsHowMuchOfLimitIsLeftToAssign() {
        assertEquals(100.0, BudgetLimitCalculator.unallocatedPercentage(0.0, 500.0), 0.001)
        assertEquals(40.0, BudgetLimitCalculator.unallocatedPercentage(300.0, 500.0), 0.001)
        assertEquals(99.99, BudgetLimitCalculator.unallocatedPercentage(0.1, 1000.0), 0.001)
        assertEquals(0.0, BudgetLimitCalculator.unallocatedPercentage(620.0, 500.0), 0.001)
        assertEquals(0.0, BudgetLimitCalculator.unallocatedPercentage(20.0, 0.0), 0.001)
    }
}
// End of class: BudgetLimitCalculatorTest
