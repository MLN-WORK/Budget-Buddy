package com.budgetbuddy

import java.time.YearMonth

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
