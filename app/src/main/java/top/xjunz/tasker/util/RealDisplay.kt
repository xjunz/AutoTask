package top.xjunz.tasker.util

import android.graphics.Point
import android.view.WindowManager
import top.xjunz.tasker.app

/**
 * @author xjunz 2022/10/25
 */
object RealDisplay {

    private val windowManager = app.getSystemService(WindowManager::class.java)

    private val point = Point()

    @Suppress("DEPRECATION")
    val size: Point
        get() {
            windowManager.defaultDisplay.getRealSize(point)
            return point
        }

    val density get() = app.resources.displayMetrics.density
}