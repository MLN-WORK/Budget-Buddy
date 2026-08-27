package com.example.budgetbuddy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView

class BudgetCategoryAdapter(
    private val budgetCategories: List<BudgetCategory>,
    private val currencySymbol: String,
    private val onAllocationChanged: () -> Unit
) : RecyclerView.Adapter<BudgetCategoryAdapter.BudgetViewHolder>() {
    inner class BudgetViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val imgBudgetCategoryIcon: ImageView = itemView.findViewById(R.id.imgBudgetCategoryIcon)
        val tvBudgetCategoryName: TextView = itemView.findViewById(R.id.tvBudgetCategoryName)
        val tvCategoryCurrency: TextView = itemView.findViewById(R.id.tvCategoryCurrency)
        val edtCategoryAllocation: EditText = itemView.findViewById(R.id.edtCategoryAllocation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.budget_category_item, parent,false)
        RuntimePaletteApplier.applyIfCustom(view)
        return BudgetViewHolder(view)
    }

    override fun getItemCount(): Int {
        return budgetCategories.size
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        val category = budgetCategories[position]
        holder.tvBudgetCategoryName.text = category.name
        val context = holder.itemView.context
        val iconName = category.icon?.takeIf(String::isNotBlank) ?: "ic_currency"
        val iconDrawableId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
            .takeIf { it != 0 }
            ?: R.drawable.ic_currency
        holder.imgBudgetCategoryIcon.setImageResource(iconDrawableId)
        holder.tvCategoryCurrency.text = currencySymbol

        // set background color (your color changing logic here)
        val bgColors = listOf(
            R.color.pastelPink,
            R.color.pastelOrange,
            R.color.pastelYellow,
            R.color.pastelGreen,
            R.color.pastelBlue,
            R.color.pastelPurple
        )

        val bgColorRes = bgColors[position % bgColors.size]
        val bgColor = ContextCompat.getColor(context, bgColorRes)

        val backgroundDrawable = ContextCompat.getDrawable(context, R.drawable.circle_background)?.mutate()
        backgroundDrawable?.setTint(bgColor)
        holder.imgBudgetCategoryIcon.background = backgroundDrawable

        (holder.edtCategoryAllocation.tag as? android.text.TextWatcher)?.let {
            holder.edtCategoryAllocation.removeTextChangedListener(it)
        }
        holder.edtCategoryAllocation.setText(
            category.allocation.takeIf { it > 0.0 }?.let { amount ->
                if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()
            }.orEmpty()
        )
        val watcher = holder.edtCategoryAllocation.doAfterTextChanged { value ->
            category.allocation = value.toString().toDoubleOrNull() ?: 0.0
            onAllocationChanged()
        }
        holder.edtCategoryAllocation.tag = watcher
    } //end onBindViewHolder

    fun getCategoryMap(): Map<String, BudgetCategory>{
        val categoryMap = mutableMapOf<String, BudgetCategory>()
        for(bc in budgetCategories){
            categoryMap[bc.name] = BudgetCategory(
                name = bc.name,
                icon = bc.icon,
                allocation = bc.allocation,
                amountSpent = 0.0
            )
        }
        return categoryMap
    }
}
