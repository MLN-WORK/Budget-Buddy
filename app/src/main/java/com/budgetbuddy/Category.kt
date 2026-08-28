package com.budgetbuddy

//for custom user categories
data class Category(
    val name:String = "",
    val icon:String = "",
    val createdByUser: Boolean = false // new field
)
