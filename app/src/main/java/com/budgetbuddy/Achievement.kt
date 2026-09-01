package com.budgetbuddy

/*
 * Start of class
 * Name of class and related classes (parent/child classes): Achievement
 * Parent class: Any; child classes: none; related classes: AchievementManager, AchievementAdapter, and AchievementProgressCalculator.
 * What the class does: Stores one achievement definition and its completion state.
 * What's important to other classes, if applicable: Consumers rely on its property meanings remaining stable across persistence, calculation, and display code.
 * Code with comments begins below.
 */
data class Achievement(
    val achievementId: String,
    val title: String,
    val description: String,
    val badgeResId: Int,
    var isCompleted: Boolean = false,
    val isRecurring: Boolean = false,
    var progress: Int = 0, //for monthly achievements
    var target: Int = 1, //for monthly achievements
    var isExpanded: Boolean = false
)
// End of class: Achievement
