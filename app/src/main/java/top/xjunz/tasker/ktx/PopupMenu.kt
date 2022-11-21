package top.xjunz.tasker.ktx

import android.view.MenuItem
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.children
import androidx.core.view.doOnPreDraw
import androidx.core.view.get
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.ui.ColorSchemes
import top.xjunz.tasker.util.ReflectionUtil.invokeDeclaredMethod


/**
 * @author xjunz 2022/11/09
 */

/**
 * Config the first item as menu title. Call this after [PopupMenu.show].
 */
fun PopupMenu.configHeaderTitle() {
    val list = invokeDeclaredMethod<ListView>("getMenuListView")
    list.doOnPreDraw {
        val text = list[0].casted<ViewGroup>()
            .findViewById<TextView>(com.google.android.material.R.id.title)
        text.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
        text.setTextColor(ColorSchemes.colorPrimary)
    }
}

fun PopupMenu.indexOf(item: MenuItem): Int {
    return menu.children.indexOf(item)
}