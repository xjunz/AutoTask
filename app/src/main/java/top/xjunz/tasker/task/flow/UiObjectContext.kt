package top.xjunz.tasker.task.flow

import android.view.Surface
import android.view.accessibility.AccessibilityNodeInfo
import top.xjunz.tasker.service.controller.currentService

/**
 * @author xjunz 2022/10/01
 */
class UiObjectContext {

    lateinit var source: AccessibilityNodeInfo

    val density: Float = currentService.uiAutomatorBridge.displayMetrics.density

    private val rotation = currentService.uiAutomatorBridge.rotation

    private val isNaturalOrientation =
        rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180

    val screenWidthPixels by lazy {
        val displayMetrics = currentService.uiAutomatorBridge.displayMetrics
        if (isNaturalOrientation) displayMetrics.widthPixels else displayMetrics.heightPixels
    }

    val screenHeightPixels by lazy {
        val displayMetrics = currentService.uiAutomatorBridge.displayMetrics
        if (isNaturalOrientation) displayMetrics.heightPixels else displayMetrics.widthPixels
    }
}