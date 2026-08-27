package com.example.budgetbuddy

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsCalculatorTest {
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

    @Test
    fun budsterMoodUsesTheRemainingMonthlyBudget() {
        assertEquals(AnalyticsCalculator.BuddyMood.HAPPY, AnalyticsCalculator.buddyMood(0.0, 100.0))
        assertEquals(AnalyticsCalculator.BuddyMood.HAPPY, AnalyticsCalculator.buddyMood(50.0, 100.0))
        assertEquals(AnalyticsCalculator.BuddyMood.NEUTRAL, AnalyticsCalculator.buddyMood(51.0, 100.0))
        assertEquals(AnalyticsCalculator.BuddyMood.NEUTRAL, AnalyticsCalculator.buddyMood(85.0, 100.0))
        assertEquals(AnalyticsCalculator.BuddyMood.ANGRY, AnalyticsCalculator.buddyMood(86.0, 100.0))
        assertEquals(AnalyticsCalculator.BuddyMood.ANGRY, AnalyticsCalculator.buddyMood(150.0, 100.0))
    }
}
