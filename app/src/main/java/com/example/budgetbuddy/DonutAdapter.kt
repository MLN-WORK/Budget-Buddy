package com.example.budgetbuddy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import android.content.Context
import androidx.core.content.ContextCompat
import com.example.budgetbuddy.R

class DonutAdapter(private val donutList: List<Donut>, private val currencySymbol: String) :
    RecyclerView.Adapter<DonutAdapter.DonutViewHolder>() {

    class DonutViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryName: TextView = itemView.findViewById(R.id.tvDonutCategory)
        val progressBar: CircularProgressIndicator = itemView.findViewById(R.id.pbDonut)
        val amountRemaining: TextView = itemView.findViewById(R.id.tvDonutAmountLeft)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DonutViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.donut_charts_layout, parent, false)
        RuntimePaletteApplier.applyIfCustom(view)
        return DonutViewHolder(view)
    }

    override fun onBindViewHolder(holder: DonutViewHolder, position: Int) {
        val donut = donutList[position]
        holder.categoryName.text = donut.categoryName

        val spentPercentage = if (donut.allocation > 0)
            ((donut.amountSpent / donut.allocation) * 100).toInt()
        else 0

        holder.progressBar.progress = spentPercentage

        val remaining = donut.allocation - donut.amountSpent
        holder.amountRemaining.text = holder.itemView.context.getString(
            R.string.amount_left,
            currencySymbol,
            remaining
        )

        //***Testing out alternating colours***
        val context = holder.itemView.context
        val colors = listOf(
            ContextCompat.getColor(context, R.color.lightPink),
            ContextCompat.getColor(context, R.color.lightBlue),
            ContextCompat.getColor(context, R.color.lightYellow)
        )

        //Repeating every 3 donuts
        val colorToUse = colors[position % colors.size]
        holder.progressBar.setIndicatorColor(colorToUse)
    }

    override fun getItemCount(): Int = donutList.size
}
