/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui

import android.content.res.ColorStateList
import android.util.SparseArray
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.ktx.attrColor
import top.xjunz.tasker.ktx.attrColorStateList

/**
 * @author xjunz 2022/09/10
 */
object ColorScheme {

    private val colors = SparseArray<Any>()

    fun release() {
        colors.clear()
    }

    private inline fun <V> getOrPut(key: Int, block: () -> V): V {
        var cache = colors[key]
        if (cache == null) {
            cache = block()
            colors.put(key, cache)
        }
        return cache.casted()
    }

    private fun getAttrColor(@AttrRes attr: Int): Int {
        return getOrPut(attr) {
            attr.attrColor
        }
    }

    private fun getAttrColorStateList(@AttrRes attr: Int): ColorStateList {
        return getOrPut(attr) {
            attr.attrColorStateList
        }
    }

    @get:ColorInt
    val colorError: Int
        get() = getAttrColor(com.google.android.material.R.attr.colorError)

    @get:ColorInt
    val textColorDisabled: Int
        get() = getOrPut("textColorDisabled".hashCode()) {
            android.R.attr.textColorTertiary.attrColorStateList.getColorForState(
                intArrayOf(-android.R.attr.state_enabled), -1
            )
        }

    val textColorPrimary: ColorStateList
        get() = getAttrColorStateList(android.R.attr.textColorPrimary)

    @get:ColorInt
    val colorTertiaryContainer: Int
        get() = getAttrColor(com.google.android.material.R.attr.colorTertiaryContainer)

    @get:ColorInt
    val colorPrimary: Int
        get() = getAttrColor(com.google.android.material.R.attr.colorPrimary)

    @get:ColorInt
    val colorOnSurface: Int
        get() = getAttrColor(com.google.android.material.R.attr.colorOnSurface)

    @get:ColorInt
    val colorSurface: Int
        get() = getAttrColor(com.google.android.material.R.attr.colorSurface)

    @get:ColorInt
    val colorPrimaryContainer
        get() = getAttrColor(com.google.android.material.R.attr.colorPrimaryContainer)
}