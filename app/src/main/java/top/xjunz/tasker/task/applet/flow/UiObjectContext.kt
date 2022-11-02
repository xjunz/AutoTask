package top.xjunz.tasker.task.applet.flow

import android.graphics.Point
import android.view.Surface
import android.view.accessibility.AccessibilityNodeInfo
import top.xjunz.tasker.service.currentService

/**
 * @author xjunz 2022/10/01
 */
class UiObjectContext {

    var depth: Int = 0

    var index: Int = 0

    lateinit var source: AccessibilityNodeInfo

    val density: Float = currentService.uiAutomatorBridge.displayMetrics.density

    private val rotation = currentService.uiAutomatorBridge.rotation

    private val isNaturalOrientation =
        rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180

    private val realSize by lazy {
        val point = Point()
        @Suppress("DEPRECATION")
        currentService.uiAutomatorBridge.defaultDisplay.getRealSize(point)
        point
    }

    val isPortrait get() = realSize.x <= realSize.y

    val screenWidthPixels get() = realSize.x

    val screenHeightPixels get() = realSize.y
}