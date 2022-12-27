/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.bridge

import android.graphics.Point
import android.hardware.display.DisplayManagerGlobal
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import top.xjunz.tasker.app
import top.xjunz.tasker.isInHostProcess

/**
 * @author xjunz 2022/10/25
 */
object DisplayManagerBridge {

    private val windowManager by lazy {
        app.getSystemService(WindowManager::class.java)
    }

    val defaultDisplay: Display
        get() {
            return if (isInHostProcess) {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            } else {
                DisplayManagerGlobal.getInstance().getRealDisplay(Display.DEFAULT_DISPLAY);
            }
        }

    private val point = Point()

    val size: Point
        get() {
            @Suppress("DEPRECATION")
            defaultDisplay.getRealSize(point)
            return point
        }

    val density: Float by lazy {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        defaultDisplay.getRealMetrics(metrics)
        metrics.density
    }
}