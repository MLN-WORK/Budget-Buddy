package com.example.budgetbuddy

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter

class CurrencySearchAdapter(context: Context) :
    ArrayAdapter<CurrencyOption>(context, R.layout.spinner_item, CurrencyCatalog.options.toMutableList()) {

    private val currencyFilter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults = FilterResults().apply {
            val matches = CurrencyCatalog.search(constraint?.toString())
            values = matches
            count = matches.size
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            clear()
            @Suppress("UNCHECKED_CAST")
            addAll((results.values as? List<CurrencyOption>).orEmpty())
            notifyDataSetChanged()
        }

        override fun convertResultToString(resultValue: Any?): CharSequence =
            (resultValue as? CurrencyOption)?.displayLabel.orEmpty()
    }

    override fun getFilter(): Filter = currencyFilter

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
        super.getView(position, convertView, parent).also(RuntimePaletteApplier::applyIfCustom)

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
        super.getDropDownView(position, convertView, parent).also(RuntimePaletteApplier::applyIfCustom)
}
