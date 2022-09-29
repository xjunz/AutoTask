package top.xjunz.tasker.ui

import androidx.annotation.ColorInt
import top.xjunz.tasker.ktx.attrColor

/**
 * @author xjunz 2022/09/10
 */
class ColorSchemes {

    @get:ColorInt
    val textColorSecondaryNoDisable: Int by lazy {
        android.R.attr.textColorSecondaryNoDisable.attrColor
    }

    @get:ColorInt
    val colorPrimary: Int by lazy {
        com.google.android.material.R.attr.colorPrimary.attrColor
    }

    @get:ColorInt
    val colorOnSurface by lazy {
        com.google.android.material.R.attr.colorOnSurface.attrColor
    }
}