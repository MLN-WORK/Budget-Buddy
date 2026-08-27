package com.example.budgetbuddy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class TransactionAdapterForHome(
    private val transactionList: List<Transaction>,
    private val currencySymbol: String,
    private val categoryIconMap: Map<String, String>
) : RecyclerView.Adapter<TransactionAdapterForHome.TransactionViewHolder>() {

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val amountText: TextView = itemView.findViewById(R.id.tvRecordAmount)
        val categoryText: TextView = itemView.findViewById(R.id.tvRecordCategory)
        val dateText: TextView = itemView.findViewById(R.id.tvRecordDate)
        val recordIcon: ImageView = itemView.findViewById(R.id.imgRecordIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.records_layout, parent, false)
        RuntimePaletteApplier.applyIfCustom(view)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactionList[position]
        val context = holder.itemView.context
        holder.categoryText.text = transaction.categoryId
        holder.dateText.text = transaction.date

        val isExpense = !transaction.isIncome

        if (isExpense) {
            holder.amountText.text = context.getString(R.string.expense_amount, currencySymbol, transaction.amount)
            holder.amountText.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.cherry)
            )
        } else {
            holder.amountText.text = context.getString(R.string.income_amount, currencySymbol, transaction.amount)
            holder.amountText.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.moss)
            )
        }

        val iconName = categoryIconMap[transaction.categoryId] ?: "ic_currency"
        val iconDrawableId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
        holder.recordIcon.setImageResource(iconDrawableId)

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
        holder.recordIcon.background = backgroundDrawable
    }

    override fun getItemCount(): Int = transactionList.size
}
