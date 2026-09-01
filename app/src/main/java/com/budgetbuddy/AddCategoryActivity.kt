package com.budgetbuddy

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.budgetbuddy.databinding.ActivityAddCategoryBinding

/*
 * Start of class
 * Name of class and related classes (parent/child classes): AddCategoryActivity
 * Parent class: BaseActivity; child classes: none; related classes: Category, IconAdapter, and LocalDataStore.
 * What the class does: Collects and validates a new custom transaction category.
 * What's important to other classes, if applicable: It must preserve BaseActivity appearance behavior and use LocalDataStore as the offline source of truth.
 * Code with comments begins below.
 */
class AddCategoryActivity : BaseActivity() {

    private lateinit var binding: ActivityAddCategoryBinding
    private lateinit var adapter: IconAdapter
    private var selectedIconId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupIconGrid()

        binding.btnSaveCategory.setOnClickListener {
            saveCategory()
        }

        binding.ivBackBtn.setOnClickListener {
            finish()
        }
    }

    private fun setupIconGrid() {
        binding.rvAllCategoryIcons.layoutManager = GridLayoutManager(this, 4)
        val iconList = getIconsFromDrawable()

        adapter = IconAdapter(iconList) { iconName ->
            selectedIconId = iconName

            CategoryIconCatalog.bind(binding.ivSelectedIcon, iconName)
        }
        binding.rvAllCategoryIcons.adapter = adapter
    }

    private fun saveCategory() {
        val categoryName = binding.edtCategoryName.text.toString().trim()

        if (categoryName.isEmpty()) {
            Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedIconId.isEmpty()) {
            Toast.makeText(this, "Please select an icon", Toast.LENGTH_SHORT).show()
            return
        }

        val localData = LocalDataStore(this)
        val firstCustomCategory = localData.getCategories().none(Category::createdByUser)
        val newCategory = Category(categoryName, selectedIconId, createdByUser = true)
        if (!localData.addCategory(newCategory)) {
            Toast.makeText(this, "A category with that name already exists", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "$categoryName has been added", Toast.LENGTH_SHORT).show()
        if (firstCustomCategory) AchievementManager.unlockAchievement("first_category", this)
        setResult(
            Activity.RESULT_OK,
            Intent().putExtra(EXTRA_CATEGORY_NAME, categoryName)
        )
        finish()
    }

    private fun getIconsFromDrawable(): List<String> {
        return CategoryIconCatalog.selectableIcons
    }

    companion object {
        const val EXTRA_CATEGORY_NAME = "createdCategoryName"
    }
}
// End of class: AddCategoryActivity
