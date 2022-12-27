/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.service

import android.app.UiAutomation
import androidx.test.uiautomator.UiDevice
import top.xjunz.tasker.annotation.LocalAndRemote
import top.xjunz.tasker.isInHostProcess
import top.xjunz.tasker.task.inspector.FloatingInspector

/**
 * @author xjunz 2022/10/10
 */
inline val serviceController get() = OperatingMode.CURRENT.serviceController

@LocalAndRemote
inline val currentService: AutomatorService
    get() = if (isInHostProcess) serviceController.requireService() else ShizukuAutomatorService.require()

inline val isFloatingInspectorShown get() = A11yAutomatorService.get()?.isInspectorShown() == true

inline val a11yAutomatorService get() = A11yAutomatorService.require()

inline val floatingInspector get() = FloatingInspector.require()

inline val uiAutomatorBridge get() = currentService.uiAutomatorBridge

inline val uiAutomation: UiAutomation get() = uiAutomatorBridge.uiAutomation

inline val uiDevice: UiDevice get() = uiAutomatorBridge.uiDevice