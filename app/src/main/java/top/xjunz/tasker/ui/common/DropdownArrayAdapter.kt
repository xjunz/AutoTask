/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.common

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import top.xjunz.shared.ktx.casted

class DropdownArrayAdapter(
    context: Context, val data: MutableList<CharSequence>
) : ArrayAdapter<CharSequence>(context, android.R.layout.simple_list_item_1, data) {

    private val candidates by lazy {
        ArrayList(data)
    }

    private val containmentFilter by lazy {
        object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                return if (constraint.isNullOrEmpty()) {
                    results.also {
                        it.values = candidates
                        it.count = candidates.size
                    }
                } else {
                    val res = candidates.filter { it.contains(constraint, true) }
                    results.also {
                        it.values = res
                        it.count = res.size
                    }
                }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults) {
                data.clear()
                data.addAll(results.values?.casted() ?: emptyList())
                if (results.count == 0) {
                    notifyDataSetInvalidated()
                } else {
                    notifyDataSetChanged()
                }
            }
        }
    }

    override fun getFilter(): Filter {
        return containmentFilter
    }
}