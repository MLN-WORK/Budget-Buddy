package com.budgetbuddy

import java.time.YearMonth

/*
 * Start of class
 * Name of class and related classes (parent/child classes): AchievementProgressCalculator
 * Parent class: Any; child classes: none; related classes: Transaction, Budget, and AchievementManager.
 * What the class does: Calculates achievement progress from local finance records.
 * What's important to other classes, if applicable: Related classes depend on this class keeping its inputs validated and its output contract deterministic.
 * Code with comments begins below.
 */
object AchievementProgressCalculator {
    fun longestConsecutiveMonthStreak(monthKeys: Set<String>): Int {
        val months = monthKeys.mapNotNull { runCatching { YearMonth.parse(it) }.getOrNull() }
            .distinct()
            .sorted()
        var longest = 0
        var current = 0
        var previous: YearMonth? = null
        months.forEach { month ->
            current = if (previous?.plusMonths(1) == month) current + 1 else 1
            longest = maxOf(longest, current)
            previous = month
        }
        return longest
    }
}
// End of class: AchievementProgressCalculator
