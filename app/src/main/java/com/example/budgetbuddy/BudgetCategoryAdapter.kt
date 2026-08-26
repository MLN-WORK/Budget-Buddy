package com.example.budgetbuddy

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

//to allow user to create budget and add budget category to screen and set amount
class BudgetCategoryAdapter(private val budgetCategories: List<BudgetCategory>, private val categoryIconMap: Map<String, String?>, private val onAllocationChanged:() -> Unit): RecyclerView.Adapter<BudgetCategoryAdapter.BudgetViewHolder>(){
    inner class BudgetViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val imgBudgetCategoryIcon: ImageView = itemView.findViewById(R.id.imgBudgetCategoryIcon)
        val tvBudgetCategoryName: TextView = itemView.findViewById(R.id.tvBudgetCategoryName)
        val edtCategoryAllocation: EditText = itemView.findViewById(R.id.edtCategoryAllocation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.budget_category_item, parent,false)
        return BudgetViewHolder(view)
    }

    override fun getItemCount(): Int {
        return budgetCategories.size
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        val category = budgetCategories[position]
        holder.tvBudgetCategoryName.text = category.name
        //set icon, get from drawable name
        val context = holder.itemView.context
        val iconName = categoryIconMap[category.name]?: "ic_currency"
        val iconDrawableId = context.resources.getIdentifier(category.icon, "drawable", context.packageName)
        holder.imgBudgetCategoryIcon.setImageResource(iconDrawableId)

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

        //update allocated amount when text input - avoid rep
        holder.edtCategoryAllocation.setText(
            if(category.allocation> 0.0){
                category.allocation.toString()
            }
            else{""}//endif
        )

        holder.edtCategoryAllocation.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val amount = s.toString().toDoubleOrNull()?: 0
                category.allocation = amount.toDouble()
                //update the total in budgetactivity
                onAllocationChanged() //callback
            }
        })
    } //end onBindViewHolder

    //to get budget category when user is done setting their budget up
    fun getCategoryMap(): Map<String, BudgetCategory>{
        val categoryMap = mutableMapOf<String, BudgetCategory>()
        for(bc in budgetCategories){
            categoryMap[bc.name] = BudgetCategory(
                allocation = bc.allocation,
                amountSpent = 0.0
            )
        }//endfor
        return categoryMap
    }
}
