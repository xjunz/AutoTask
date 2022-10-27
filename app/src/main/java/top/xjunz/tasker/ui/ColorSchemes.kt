package top.xjunz.tasker.ui

import androidx.annotation.ColorInt
import top.xjunz.tasker.ktx.attrColor
import top.xjunz.tasker.ktx.attrColorStateList

/**
 * @author xjunz 2022/09/10
 */
object ColorSchemes {

    @get:ColorInt
    val textColorDisabled: Int
        get() = android.R.attr.textColorTertiary.attrColorStateList.getColorForState(
            intArrayOf(-android.R.attr.state_enabled), -1
        )

    @get:ColorInt
    val textColorTertiary: Int
        get() = android.R.attr.textColorTertiary.attrColorStateList.defaultColor

    @get:ColorInt
    val colorTertiary: Int
        get() = com.google.android.material.R.attr.colorTertiary.attrColor

    @get:ColorInt
    val colorPrimary: Int
        get() = com.google.android.material.R.attr.colorPrimary.attrColor

    @get:ColorInt
    val colorOnSurface: Int
        get() = com.google.android.material.R.attr.colorOnSurface.attrColor

    @get:ColorInt
    val colorSurface: Int
        get() = com.google.android.material.R.attr.colorSurface.attrColor

    @get:ColorInt
    val colorPrimaryContainer
        get() = com.google.android.material.R.attr.colorPrimaryContainer.attrColor
}