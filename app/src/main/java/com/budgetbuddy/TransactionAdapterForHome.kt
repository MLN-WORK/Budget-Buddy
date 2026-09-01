package com.budgetbuddy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

/*
 * Start of class
 * Name of class and related classes (parent/child classes): TransactionAdapterForHome
 * Parent class: RecyclerView.Adapter; child classes: TransactionViewHolder; related classes: Transaction, MainActivity, and TransactionViewHolder.
 * What the class does: Binds recent transactions to the compact home list.
 * What's important to other classes, if applicable: Callers supply its model data and depend on stable row binding and click-callback behavior.
 * Code with comments begins below.
 */
class TransactionAdapterForHome(
    private val transactionList: List<Transaction>,
    private val currencySymbol: String,
    private val categoriesById: Map<String, Category>,
    private val onView: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapterForHome.TransactionViewHolder>() {

    /*
     * Start of class
     * Name of class and related classes (parent/child classes): TransactionViewHolder
     * Parent class: RecyclerView.ViewHolder; child classes: none; related classes: TransactionAdapterForHome and Transaction.
     * What the class does: Caches the views used for one compact transaction row.
     * What's important to other classes, if applicable: Its enclosing adapter owns it; it must not retain activity state beyond the bound row.
     * Code with comments begins below.
     */
    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val amountText: TextView = itemView.findViewById(R.id.tvRecordAmount)
        val categoryText: TextView = itemView.findViewById(R.id.tvRecordCategory)
        val dateText: TextView = itemView.findViewById(R.id.tvRecordDate)
        val recordIcon: ImageView = itemView.findViewById(R.id.imgRecordIcon)
        val ocrBadge: TextView = itemView.findViewById(R.id.tvRecordOcrBadge)
    }
    // End of class: TransactionViewHolder

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.records_layout, parent, false)
        RuntimePaletteApplier.applyIfCustom(view)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactionList[position]
        val context = holder.itemView.context
        holder.categoryText.text = categoriesById[transaction.categoryId]?.name
            ?: transaction.categoryId.takeIf(String::isNotBlank)
            ?: context.getString(R.string.uncategorised)
        holder.dateText.text = transaction.date
        holder.itemView.setOnClickListener { onView(transaction) }
        holder.ocrBadge.visibility = if (transaction.isOcr) View.VISIBLE else View.GONE

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

        val iconName = categoriesById[transaction.categoryId]?.icon ?: "ic_currency"
        CategoryIconCatalog.bind(holder.recordIcon, iconName)

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
// End of class: TransactionAdapterForHome
