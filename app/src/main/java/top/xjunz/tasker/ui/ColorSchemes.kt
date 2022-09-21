package top.xjunz.tasker.ui

import androidx.annotation.ColorInt
import top.xjunz.tasker.ktx.color
import top.xjunz.tasker.ktx.resolvedId

/**
 * @author xjunz 2022/09/10
 */
object ColorSchemes {

    @get:ColorInt
    val colorPrimary: Int by lazy {
        com.google.android.material.R.attr.colorPrimary.resolvedId.color
    }

    @get:ColorInt
    val colorOnSurface by lazy {
        com.google.android.material.R.attr.colorOnSurface.resolvedId.color
    }
}