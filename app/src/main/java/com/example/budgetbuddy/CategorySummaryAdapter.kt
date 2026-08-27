package com.example.budgetbuddy

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

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

    class CategorySummaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategoryName)
        val tvTotal: TextView = itemView.findViewById(R.id.tvCategoryTotal)
        val imgCatIcon: ImageView = itemView.findViewById(R.id.imgCatIcon)
    }

    private fun getIconResourceId(iconName: String, view: View): Int {
        return view.context.resources.getIdentifier(iconName, "drawable", view.context.packageName)
    }
}
