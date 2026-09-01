package com.budgetbuddy

import kotlin.math.roundToInt

/*
 * Start of class
 * Name of class and related classes (parent/child classes): AnalyticsCalculator
 * Parent class: Any; child classes: BuddyMood; related classes: AnalyticsActivity, MainActivity, and BuddyMood.
 * What the class does: Provides pure percentage, gauge, and buddy-mood calculations.
 * What's important to other classes, if applicable: Related classes depend on this class keeping its inputs validated and its output contract deterministic.
 * Code with comments begins below.
 */
object AnalyticsCalculator {
    /*
     * Start of class
     * Name of class and related classes (parent/child classes): BuddyMood
     * Parent class: Enum; child classes: none; related classes: AnalyticsCalculator, AnalyticsActivity, and MainActivity.
     * What the class does: Names the three budget-health states used by analytics and Budster.
     * What's important to other classes, if applicable: Related classes depend on this class keeping its inputs validated and its output contract deterministic.
     * Code with comments begins below.
     */
    enum class BuddyMood { HAPPY, NEUTRAL, ANGRY }
    // End of class: BuddyMood

    fun spentPercentageExact(totalSpent: Double, maximumBudget: Double): Double {
        if (!totalSpent.isFinite() || !maximumBudget.isFinite() || maximumBudget <= 0.0) return 0.0
        return ((totalSpent.coerceAtLeast(0.0) / maximumBudget) * 100.0).coerceAtLeast(0.0)
    }

    fun spentPercentage(totalSpent: Double, maximumBudget: Double): Int {
        return spentPercentageExact(totalSpent, maximumBudget).roundToInt().coerceAtLeast(0)
    }

    fun gaugeSpeed(percentage: Int): Float = percentage.coerceIn(0, 100).toFloat()

    fun gaugeSpeed(percentage: Double): Float = percentage.coerceIn(0.0, 100.0).toFloat()

    /** Returns an exact user-selected gauge colour without theme-derived blending. */
    fun gaugeColor(percentage: Double, palette: GaugePalette): Int = when {
        percentage < 50.0 -> palette.good
        percentage < 85.0 -> palette.okay
        else -> palette.bad
    }

    fun remainingPercentage(totalSpent: Double, maximumBudget: Double): Int {
        if (!totalSpent.isFinite() || !maximumBudget.isFinite() || maximumBudget <= 0.0) return 0
        return (100 - spentPercentage(totalSpent, maximumBudget)).coerceAtMost(100)
    }

    fun remainingPercentageExact(totalSpent: Double, maximumBudget: Double): Double {
        if (!totalSpent.isFinite() || !maximumBudget.isFinite() || maximumBudget <= 0.0) return 0.0
        return (100.0 - spentPercentageExact(totalSpent, maximumBudget)).coerceIn(0.0, 100.0)
    }

    /** Happy at 50%+ remaining, neutral at 15–49%, and angry below 15%. */
    fun buddyMood(totalSpent: Double, maximumBudget: Double): BuddyMood =
        when (remainingPercentage(totalSpent, maximumBudget)) {
            in 50..Int.MAX_VALUE -> BuddyMood.HAPPY
            in 15..49 -> BuddyMood.NEUTRAL
            else -> BuddyMood.ANGRY
        }
}
// End of class: AnalyticsCalculator
