/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.util

import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.BitmapDrawable
import me.zhanghai.android.appiconloader.AppIconLoader
import top.xjunz.tasker.app
import top.xjunz.tasker.ktx.dp

/**
 * @author xjunz 2021/9/15
 */
object Icons {

    fun loadIcon(requireAppInfo: ApplicationInfo): Bitmap {
        return iconLoader.loadIcon(requireAppInfo)
    }

    private val iconLoader by lazy {
        AppIconLoader(48.dp, true, app)
    }

    val myIcon by lazy {
        iconLoader.loadIcon(app.applicationInfo)
    }

    val desaturatedColorFilter = ColorMatrixColorFilter(ColorMatrix().also { it.setSaturation(0F) })

    val desaturatedMyIcon by lazy {
        BitmapDrawable(app.resources, myIcon).apply {
            colorFilter = desaturatedColorFilter
        }
    }
}