package com.example.budgetbuddy

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsCalculatorTest {
    @Test
    fun goalRatioIsAlwaysSafeForGaugeSections() {
        assertEquals(0f, AnalyticsCalculator.minimumGoalRatio(-50.0, 100.0))
        assertEquals(0.5f, AnalyticsCalculator.minimumGoalRatio(50.0, 100.0))
        assertEquals(1f, AnalyticsCalculator.minimumGoalRatio(150.0, 100.0))
        assertEquals(0f, AnalyticsCalculator.minimumGoalRatio(Double.NaN, 100.0))
    }

    @Test
    fun percentageCanReportOverspendingWhileGaugeSpeedStaysInBounds() {
        val percentage = AnalyticsCalculator.spentPercentage(250.0, 100.0)
        assertEquals(250, percentage)
        assertEquals(100f, AnalyticsCalculator.gaugeSpeed(percentage))
    }

    @Test
    fun zeroOrInvalidBudgetProducesSafeZeroValues() {
        assertEquals(0, AnalyticsCalculator.spentPercentage(100.0, 0.0))
        assertEquals(0, AnalyticsCalculator.spentPercentage(Double.POSITIVE_INFINITY, 100.0))
        assertEquals(0f, AnalyticsCalculator.gaugeSpeed(-10))
    }
}
