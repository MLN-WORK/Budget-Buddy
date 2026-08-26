package com.example.budgetbuddy

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
