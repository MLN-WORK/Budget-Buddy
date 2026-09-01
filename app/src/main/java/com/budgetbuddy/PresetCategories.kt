package com.budgetbuddy

/*
 * Start of class
 * Name of class and related classes (parent/child classes): PresetCategories
 * Parent class: Any; child classes: none; related classes: Category and LocalDataStore.
 * What the class does: Stores a legacy preset-category label and icon pair.
 * What's important to other classes, if applicable: Consumers rely on its property meanings remaining stable across persistence, calculation, and display code.
 * Code with comments begins below.
 */
data class PresetCategories(
    val name:String = "",
    val icon:String = ""
)
// End of class: PresetCategories
