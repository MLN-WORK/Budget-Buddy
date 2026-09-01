package com.budgetbuddy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

/*
 * Start of class
 * Name of class and related classes (parent/child classes): CategorySummaryAdapter
 * Parent class: RecyclerView.Adapter; child classes: CategorySummaryViewHolder; related classes: Transaction, CategorySummaryActivity, and CategorySummaryViewHolder.
 * What the class does: Binds category summary transactions to their list rows.
 * What's important to other classes, if applicable: Callers supply its model data and depend on stable row binding and click-callback behavior.
 * Code with comments begins below.
 */
class CategorySummaryAdapter(private val currencySymbol: String) :
    RecyclerView.Adapter<CategorySummaryAdapter.CategorySummaryViewHolder>() {

    private val categoryData = mutableListOf<Triple<String, Double, String>>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategorySummaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_summary, parent, false)
        RuntimePaletteApplier.applyIfCustom(view)
        return CategorySummaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategorySummaryViewHolder, position: Int) {
        val (category, total, icon) = categoryData[position]
        holder.tvCategory.text = category
        holder.tvTotal.text = String.format(Locale.getDefault(), "%s%.2f", currencySymbol, total)
        holder.imgCatIcon.setImageResource(getIconResourceId(icon, holder.itemView))

        val bgColors = listOf(
            R.color.pastelPink,
            R.color.pastelOrange,
            R.color.pastelYellow,
            R.color.pastelGreen,
            R.color.pastelBlue,
            R.color.pastelPurple
        )
        val context = holder.itemView.context
        val bgColorRes = bgColors[position % bgColors.size]
        val bgColor = ContextCompat.getColor(context, bgColorRes)

        val backgroundDrawable = ContextCompat.getDrawable(context, R.drawable.circle_background)?.mutate()
        backgroundDrawable?.setTint(bgColor)
        holder.imgCatIcon.background = backgroundDrawable
    }

    override fun getItemCount(): Int = categoryData.size

    fun updateData(newData: List<Triple<String, Double, String>>) {
        categoryData.clear()
        categoryData.addAll(newData)
        notifyDataSetChanged()
    }

    /*
     * Start of class
     * Name of class and related classes (parent/child classes): CategorySummaryViewHolder
     * Parent class: RecyclerView.ViewHolder; child classes: none; related classes: CategorySummaryAdapter and Transaction.
     * What the class does: Caches the views used by one category summary row.
     * What's important to other classes, if applicable: Its enclosing adapter owns it; it must not retain activity state beyond the bound row.
     * Code with comments begins below.
     */
    class CategorySummaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategoryName)
        val tvTotal: TextView = itemView.findViewById(R.id.tvCategoryTotal)
        val imgCatIcon: ImageView = itemView.findViewById(R.id.imgCatIcon)
    }
    // End of class: CategorySummaryViewHolder

    private fun getIconResourceId(iconName: String, view: View): Int {
        return view.context.resources.getIdentifier(iconName, "drawable", view.context.packageName)
    }
}
// End of class: CategorySummaryAdapter
