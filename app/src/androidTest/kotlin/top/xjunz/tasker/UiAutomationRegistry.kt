/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.UiAutomation
import android.view.accessibility.AccessibilityEvent
import androidx.test.platform.app.InstrumentationRegistry

/**
 * @author xjunz 2022/11/01
 */
internal object UiAutomationRegistry {

    fun getUiAutomation(): UiAutomation {
        val uiAutomation = InstrumentationRegistry.getInstrumentation()
            .getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        uiAutomation.serviceInfo = uiAutomation.serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOWS_CHANGED
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS and
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS.inv()
        }
        return uiAutomation
    }
}