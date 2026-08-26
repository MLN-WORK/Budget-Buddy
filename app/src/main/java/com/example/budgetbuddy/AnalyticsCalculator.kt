package com.example.budgetbuddy

import kotlin.math.roundToInt

object AnalyticsCalculator {
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
}
