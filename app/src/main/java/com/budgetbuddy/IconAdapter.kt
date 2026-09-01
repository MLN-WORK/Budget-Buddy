package com.budgetbuddy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

/*
 * Start of class
 * Name of class and related classes (parent/child classes): IconAdapter
 * Parent class: RecyclerView.Adapter; child classes: IconViewHolder; related classes: AddCategoryActivity, CategoryIconCatalog, and IconViewHolder.
 * What the class does: Displays selectable packaged icons when a category is created.
 * What's important to other classes, if applicable: Callers supply its model data and depend on stable row binding and click-callback behavior.
 * Code with comments begins below.
 */
class IconAdapter(private val icons:List<String>, private val onItemClick:(String)->Unit) : RecyclerView.Adapter<IconAdapter.IconViewHolder>(){

    /*
     * Start of class
     * Name of class and related classes (parent/child classes): IconViewHolder
     * Parent class: RecyclerView.ViewHolder; child classes: none; related classes: IconAdapter and AddCategoryActivity.
     * What the class does: Caches the image control for one selectable category icon.
     * What's important to other classes, if applicable: Its enclosing adapter owns it; it must not retain activity state beyond the bound row.
     * Code with comments begins below.
     */
    inner class IconViewHolder(itemView:View): RecyclerView.ViewHolder(itemView){
        val icon: ImageView = itemView.findViewById(R.id.imgIcon)

        fun drawableName(name:String){
            CategoryIconCatalog.bind(icon, name)
            itemView.setOnClickListener{onItemClick(name)}
        }
    }
    // End of class: IconViewHolder

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
// End of class: IconAdapter
