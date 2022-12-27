/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task

import android.app.Instrumentation
import android.app.Instrumentation.ActivityMonitor
import android.content.Intent
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
        uiAutomation.setOnAccessibilityEventListener {
            println(it)
        }
        InstrumentationRegistry.getInstrumentation().addMonitor(object : ActivityMonitor() {
            override fun onStartActivity(intent: Intent?): Instrumentation.ActivityResult {
                return super.onStartActivity(intent)
            }
        })
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