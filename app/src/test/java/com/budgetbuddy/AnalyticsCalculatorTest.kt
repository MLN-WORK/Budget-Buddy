package com.budgetbuddy

import org.junit.Assert.assertEquals
import org.junit.Test

/*
 * Start of class
 * Name of class and related classes (parent/child classes): AnalyticsCalculatorTest
 * Parent class: Any; child classes: none; related classes: AnalyticsCalculator, JUnit, and the application code under test.
 * What the class does: Verifies the AnalyticsCalculator behavior and its regression cases.
 * What's important to other classes, if applicable: Its assertions document the behavior production classes must preserve.
 * Code with comments begins below.
 */
class AnalyticsCalculatorTest {
    @Test
    fun percentageCanReportOverspendingWhileGaugeSpeedStaysInBounds() {
        val percentage = AnalyticsCalculator.spentPercentage(250.0, 100.0)
        assertEquals(250, percentage)
        assertEquals(100f, AnalyticsCalculator.gaugeSpeed(percentage))
    }

    @Test
    fun exactPercentageRetainsTwoDecimalPrecision() {
        assertEquals(34.59, AnalyticsCalculator.spentPercentageExact(761.0, 2200.0), 0.005)
        assertEquals(65.41, AnalyticsCalculator.remainingPercentageExact(761.0, 2200.0), 0.005)
        assertEquals(34.5909f, AnalyticsCalculator.gaugeSpeed(34.5909), 0.001f)
    }

    @Test
    fun zeroOrInvalidBudgetProducesSafeZeroValues() {
        assertEquals(0, AnalyticsCalculator.spentPercentage(100.0, 0.0))
        assertEquals(0, AnalyticsCalculator.spentPercentage(Double.POSITIVE_INFINITY, 100.0))
        assertEquals(0f, AnalyticsCalculator.gaugeSpeed(-10))
    }

    @Test
    fun gaugeColourUsesTheExactSelectedPaletteAcrossAllBands() {
        val palette = GaugePalette(good = 0xFF123456.toInt(), okay = 0xFFFFD600.toInt(), bad = 0xFF654321.toInt())
        assertEquals(palette.good, AnalyticsCalculator.gaugeColor(49.99, palette))
        assertEquals(palette.okay, AnalyticsCalculator.gaugeColor(50.0, palette))
        assertEquals(palette.okay, AnalyticsCalculator.gaugeColor(84.99, palette))
        assertEquals(palette.bad, AnalyticsCalculator.gaugeColor(85.0, palette))
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
// End of class: AnalyticsCalculatorTest
