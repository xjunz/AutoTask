/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.service

import android.app.UiAutomation
import androidx.test.uiautomator.UiDevice
import top.xjunz.tasker.annotation.Anywhere
import top.xjunz.tasker.isAppProcess

/**
 * @author xjunz 2022/10/10
 */
inline val serviceController get() = OperatingMode.CURRENT.serviceController

@Anywhere
inline val currentService: AutomatorService
    get() = if (isAppProcess) serviceController.requireService() else ShizukuAutomatorService.require()

inline val isFloatingInspectorShown get() = A11yAutomatorService.get()?.isInspectorShown() == true

inline val a11yAutomatorService get() = A11yAutomatorService.require()

inline val floatingInspector get() = a11yAutomatorService.inspector

inline val uiAutomatorBridge get() = currentService.uiAutomatorBridge

inline val uiAutomation: UiAutomation get() = uiAutomatorBridge.uiAutomation

inline val uiDevice: UiDevice get() = uiAutomatorBridge.uiDevice