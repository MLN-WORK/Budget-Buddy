package com.example.budgetbuddy

import android.media.Image
import android.text.Layout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class IconAdapter(private val icons:List<String>, private val onItemClick:(String)->Unit) : RecyclerView.Adapter<IconAdapter.IconViewHolder>(){

    inner class IconViewHolder(itemView:View): RecyclerView.ViewHolder(itemView){
        val icon: ImageView = itemView.findViewById(R.id.imgIcon)

        fun drawableName(name:String){
            val context = itemView.context
            val iconId = context.resources.getIdentifier(name, "drawable", context.packageName)
            icon.setImageResource(iconId)
            itemView.setOnClickListener{onItemClick(name)}
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.icon_item, parent,
            false)
        RuntimePaletteApplier.applyIfCustom(view)
        return IconViewHolder(view)
    }

    override fun onBindViewHolder(holder: IconAdapter.IconViewHolder, position: Int) {
        holder.drawableName(icons[position])
    }

    override fun getItemCount(): Int {
        return icons.size
    }

}
