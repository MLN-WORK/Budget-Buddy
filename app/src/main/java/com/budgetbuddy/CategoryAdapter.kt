package com.budgetbuddy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/*
 * Start of class
 * Name of class and related classes (parent/child classes): CategoryAdapter
 * Parent class: RecyclerView.Adapter; child classes: CategoryViewHolder; related classes: Category, TransactionActivity, and CategoryViewHolder.
 * What the class does: Binds selectable categories to transaction and filter lists.
 * What's important to other classes, if applicable: Callers supply its model data and depend on stable row binding and click-callback behavior.
 * Code with comments begins below.
 */
class CategoryAdapter(
    private val categories: List<Category>,
    private val onItemClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    /*
     * Start of class
     * Name of class and related classes (parent/child classes): CategoryViewHolder
     * Parent class: RecyclerView.ViewHolder; child classes: none; related classes: CategoryAdapter and Category.
     * What the class does: Caches the views and click behavior for one category row.
     * What's important to other classes, if applicable: Its enclosing adapter owns it; it must not retain activity state beyond the bound row.
     * Code with comments begins below.
     */
    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        val categoryIcon: ImageView = itemView.findViewById((R.id.imgCategoryIcon))
    }
    // End of class: CategoryViewHolder

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.category_item, parent, false)
        RuntimePaletteApplier.applyIfCustom(view)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.categoryName.text = category.name
        //get icon
        CategoryIconCatalog.bind(holder.categoryIcon, category.icon)

        holder.itemView.setOnClickListener {
            onItemClick(category)
        }
    }

    override fun getItemCount(): Int {
        return categories.size
    }
}
// End of class: CategoryAdapter
