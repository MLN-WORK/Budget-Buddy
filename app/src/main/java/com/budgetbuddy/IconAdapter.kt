package com.budgetbuddy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class IconAdapter(private val icons:List<String>, private val onItemClick:(String)->Unit) : RecyclerView.Adapter<IconAdapter.IconViewHolder>(){

    inner class IconViewHolder(itemView:View): RecyclerView.ViewHolder(itemView){
        val icon: ImageView = itemView.findViewById(R.id.imgIcon)

        fun drawableName(name:String){
            CategoryIconCatalog.bind(icon, name)
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
