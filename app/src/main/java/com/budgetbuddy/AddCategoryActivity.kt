package com.budgetbuddy

import android.content.ContentValues.TAG
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.budgetbuddy.databinding.ActivityAddCategoryBinding

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

            Log.d(TAG, "Selected $iconName")

            CategoryIconCatalog.bind(binding.ivSelectedIcon, iconName)
        }
        binding.rvAllCategoryIcons.adapter = adapter
    }

    private fun saveCategory() {
        val categoryName = binding.edtCategoryName.text.toString().trim()

        if (categoryName.isEmpty()) {
            Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "User hasn't entered a category name")
            return
        }

        if (selectedIconId.isEmpty()) {
            Toast.makeText(this, "Please select an icon", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "User hasn't selected an icon")
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
