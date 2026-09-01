package com.budgetbuddy

import org.junit.Assert.assertEquals
import org.junit.Test

/*
 * Start of class
 * Name of class and related classes (parent/child classes): AchievementProgressCalculatorTest
 * Parent class: Any; child classes: none; related classes: AchievementProgressCalculator, JUnit, and the application code under test.
 * What the class does: Verifies the AchievementProgressCalculator behavior and its regression cases.
 * What's important to other classes, if applicable: Its assertions document the behavior production classes must preserve.
 * Code with comments begins below.
 */
class AchievementProgressCalculatorTest {
    @Test
    fun duplicateBudgetMonthsDoNotIncreaseStreak() {
        val months = listOf("2026-01", "2026-01", "2026-02").toSet()
        assertEquals(2, AchievementProgressCalculator.longestConsecutiveMonthStreak(months))
    }

    @Test
    fun gapsBreakAConsecutiveMonthStreak() {
        val months = setOf("2026-01", "2026-02", "2026-04", "2026-05", "2026-06")
        assertEquals(3, AchievementProgressCalculator.longestConsecutiveMonthStreak(months))
    }

    @Test
    fun streakWorksAcrossYearBoundary() {
        val months = setOf("2025-11", "2025-12", "2026-01")
        assertEquals(3, AchievementProgressCalculator.longestConsecutiveMonthStreak(months))
    }

    @Test
    fun malformedMonthsAreIgnored() {
        val months = setOf("not-a-month", "2026-08")
        assertEquals(1, AchievementProgressCalculator.longestConsecutiveMonthStreak(months))
    }
}
// End of class: AchievementProgressCalculatorTest
