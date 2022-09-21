/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.base

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import top.xjunz.shared.ktx.unsafeCast

/**
 * @author xjunz 2022/04/23
 */
class GenericViewHolder<T : ViewBinding>(
    val binding: T, initializer: (GenericViewHolder<T>) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    init {
        initializer(this)
    }
}

inline fun <Data, Binding : ViewDataBinding> inlineAdapter(
    data: List<Data>, itemViewBinding: Class<Binding>,
    noinline initializer: GenericViewHolder<Binding>.() -> Unit,
    crossinline onBindViewHolder: (binding: Binding, index: Int, data: Data) -> Unit
): RecyclerView.Adapter<*> {

    return object : RecyclerView.Adapter<GenericViewHolder<Binding>>() {

        private lateinit var layoutInflater: LayoutInflater

        override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
            super.onAttachedToRecyclerView(recyclerView)
            layoutInflater = LayoutInflater.from(recyclerView.context)
        }

        override fun onCreateViewHolder(
            parent: ViewGroup, viewType: Int
        ): GenericViewHolder<Binding> {
            val binding: Binding = itemViewBinding.getDeclaredMethod(
                "inflate", LayoutInflater::class.java, ViewGroup::class.java, Boolean::class.java
            ).invoke(null, layoutInflater, parent, false)!!.unsafeCast()
            return GenericViewHolder(binding, initializer)
        }

        override fun onBindViewHolder(holder: GenericViewHolder<Binding>, position: Int) {
            onBindViewHolder(holder.binding, position, data[position])
        }

        override fun getItemCount() = data.size

    }
}