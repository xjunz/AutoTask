package top.xjunz.tasker.task

import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import top.xjunz.tasker.UiAutomationRegistry

/**
 * @author xjunz 2022/11/01
 */
class AccessibilityNodeInfo {

    @Test
    fun test() {
        val uiAutomation = UiAutomationRegistry.getUiAutomation()
        println(uiAutomation.serviceInfo)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            uiAutomation.waitForIdle(100, 500)
            val window = uiAutomation.rootInActiveWindow
        }
    }

    private fun AccessibilityNodeInfo.findFirst(condition: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        for (i in 0 until childCount) {
            val child = getChild(i) ?: continue
            // Need this?
            if (!child.isVisibleToUser) continue
            return try {
                if (condition(child)) child else child.findFirst(condition)
            } finally {
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }
        return null
    }
}