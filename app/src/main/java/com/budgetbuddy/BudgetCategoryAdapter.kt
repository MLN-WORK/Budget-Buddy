package com.budgetbuddy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView

/*
 * Start of class
 * Name of class and related classes (parent/child classes): BudgetCategoryAdapter
 * Parent class: RecyclerView.Adapter; child classes: BudgetViewHolder; related classes: BudgetCategory, BudgetActivity, and BudgetViewHolder.
 * What the class does: Binds editable budget-category allocations to a list.
 * What's important to other classes, if applicable: Callers supply its model data and depend on stable row binding and click-callback behavior.
 * Code with comments begins below.
 */
class BudgetCategoryAdapter(
    private val budgetCategories: List<BudgetCategory>,
    private val currencySymbol: String,
    private val onAllocationChanged: () -> Unit
) : RecyclerView.Adapter<BudgetCategoryAdapter.BudgetViewHolder>() {
    /*
     * Start of class
     * Name of class and related classes (parent/child classes): BudgetViewHolder
     * Parent class: RecyclerView.ViewHolder; child classes: none; related classes: BudgetCategoryAdapter and BudgetCategory.
     * What the class does: Caches and updates the controls for one budget-category row.
     * What's important to other classes, if applicable: Its enclosing adapter owns it; it must not retain activity state beyond the bound row.
     * Code with comments begins below.
     */
    inner class BudgetViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val imgBudgetCategoryIcon: ImageView = itemView.findViewById(R.id.imgBudgetCategoryIcon)
        val tvBudgetCategoryName: TextView = itemView.findViewById(R.id.tvBudgetCategoryName)
        val tvCategoryCurrency: TextView = itemView.findViewById(R.id.tvCategoryCurrency)
        val edtCategoryAllocation: EditText = itemView.findViewById(R.id.edtCategoryAllocation)
    }
    // End of class: BudgetViewHolder

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
        CategoryIconCatalog.bind(holder.imgBudgetCategoryIcon, category.icon)
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
            categoryMap[bc.id] = BudgetCategory(
                name = bc.name,
                icon = bc.icon,
                allocation = bc.allocation,
                amountSpent = 0.0,
                id = bc.id
            )
        }
        return categoryMap
    }
}
// End of class: BudgetCategoryAdapter
