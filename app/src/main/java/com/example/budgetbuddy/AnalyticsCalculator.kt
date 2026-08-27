package com.example.budgetbuddy

import kotlin.math.roundToInt

object AnalyticsCalculator {
    enum class BuddyMood { HAPPY, NEUTRAL, ANGRY }

    fun spentPercentage(totalSpent: Double, maximumBudget: Double): Int {
        if (!totalSpent.isFinite() || !maximumBudget.isFinite() || maximumBudget <= 0.0) return 0
        return ((totalSpent.coerceAtLeast(0.0) / maximumBudget) * 100.0)
            .roundToInt()
            .coerceAtLeast(0)
    }

    fun minimumGoalRatio(minimumGoal: Double, maximumBudget: Double): Float {
        if (!minimumGoal.isFinite() || !maximumBudget.isFinite() || maximumBudget <= 0.0) return 0f
        return (minimumGoal / maximumBudget).toFloat().coerceIn(0f, 1f)
    }

    fun gaugeSpeed(percentage: Int): Float = percentage.coerceIn(0, 100).toFloat()

    fun remainingPercentage(totalSpent: Double, maximumBudget: Double): Int {
        if (!totalSpent.isFinite() || !maximumBudget.isFinite() || maximumBudget <= 0.0) return 0
        return (100 - spentPercentage(totalSpent, maximumBudget)).coerceAtMost(100)
    }

    /** Happy at 50%+ remaining, neutral at 15–49%, and angry below 15%. */
    fun buddyMood(totalSpent: Double, maximumBudget: Double): BuddyMood =
        when (remainingPercentage(totalSpent, maximumBudget)) {
            in 50..Int.MAX_VALUE -> BuddyMood.HAPPY
            in 15..49 -> BuddyMood.NEUTRAL
            else -> BuddyMood.ANGRY
        }
}
