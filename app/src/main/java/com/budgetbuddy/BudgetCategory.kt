package com.budgetbuddy

data class BudgetCategory(
    var name:String = "",
    var icon:String? = "",
    var allocation:Double = 0.0,
    val amountSpent:Double? = 0.0)

