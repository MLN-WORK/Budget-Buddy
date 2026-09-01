package com.budgetbuddy

/*
 * Start of class
 * Name of class and related classes (parent/child classes): Donut
 * Parent class: Any; child classes: none; related classes: DonutAdapter and MainActivity.
 * What the class does: Stores one category slice for the home spending summary.
 * What's important to other classes, if applicable: Consumers rely on its property meanings remaining stable across persistence, calculation, and display code.
 * Code with comments begins below.
 */
data class Donut(
    val categoryName: String = "",
    val allocation: Double = 0.0,
    val amountSpent: Double = 0.0
)
// End of class: Donut
