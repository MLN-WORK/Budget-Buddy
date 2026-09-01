package com.budgetbuddy

//for custom user categories
/*
 * Start of class
 * Name of class and related classes (parent/child classes): Category
 * Parent class: Any; child classes: none; related classes: LocalDataStore, CategoryAdapter, and TransactionCategoryPolicy.
 * What the class does: Stores a stable category identity, label, icon, and ownership flag.
 * What's important to other classes, if applicable: Consumers rely on its property meanings remaining stable across persistence, calculation, and display code.
 * Code with comments begins below.
 */
data class Category(
    /** User-facing label. This can change without changing the stored identity. */
    val name: String = "",
    val icon: String = "",
    val createdByUser: Boolean = false,
    /** Stable value stored on transactions and budgets. */
    val id: String = name
)
// End of class: Category
