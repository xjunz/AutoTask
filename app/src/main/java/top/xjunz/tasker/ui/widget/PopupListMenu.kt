/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.get
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import top.xjunz.tasker.databinding.ItemMenuBinding
import top.xjunz.tasker.databinding.LayoutMenuListBinding
import top.xjunz.tasker.ktx.resolvedId
import top.xjunz.tasker.ui.ColorScheme
import top.xjunz.tasker.util.AntiMonkeyUtil.setAntiMoneyClickListener
import top.xjunz.tasker.util.ReflectionUtil.invokeSuperMethod

/**
 * @author xjunz 2022/12/10
 */
class PopupListMenu @JvmOverloads constructor(
    context: Context,
    anchor: View,
    gravity: Int,
    @AttrRes popupStyleAttr: Int = com.google.android.material.R.attr.popupMenuStyle,
    @StyleRes popupStyleRes: Int = 0
) : PopupMenu(context, anchor, gravity, popupStyleAttr, popupStyleRes) {

    private var onMenuItemClickListener: OnMenuItemClickListener? = null

    private inner class MenuItemViewHolder(val binding: ItemMenuBinding) :
        ViewHolder(binding.root) {

        init {
            binding.content.setAntiMoneyClickListener {
                if (onMenuItemClickListener?.onMenuItemClick(menu[adapterPosition]) == true) {
                    menu.close()
                }
            }
        }
    }

    override fun setOnMenuItemClickListener(listener: OnMenuItemClickListener?) {
        onMenuItemClickListener = listener
    }

    private inner class MenuAdapter : RecyclerView.Adapter<MenuItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuItemViewHolder {
            return MenuItemViewHolder(
                ItemMenuBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        }

        override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) {
            val item = menu[position]
            if (item.itemId != 0) {
                holder.binding.content.isClickable = true
                holder.binding.title.setTextAppearance(
                    com.google.android.material.R.attr.textAppearanceLargePopupMenu.resolvedId
                )
                holder.binding.title.setTextColor(ColorScheme.textColorPrimary)
            } else {
                holder.binding.content.isClickable = false
                holder.binding.title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                holder.binding.title.setTextColor(ColorScheme.colorPrimary)
            }
            holder.binding.title.text = item.title
        }

        override fun getItemCount(): Int {
            return menu.size()
        }

    }

    override fun show() {
        super.show()
        val list = invokeSuperMethod<ListView>("getMenuListView")
        val parent = (list.parent as ViewGroup)
        parent.removeView(list)
        val recycledView =
            LayoutMenuListBinding.inflate(LayoutInflater.from(parent.context)).rvList
        parent.addView(
            recycledView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        recycledView.adapter = MenuAdapter()
    }
}