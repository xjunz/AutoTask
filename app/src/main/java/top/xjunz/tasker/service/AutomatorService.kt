/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.service

import androidx.test.uiautomator.bridge.UiAutomatorBridge
import top.xjunz.tasker.bridge.OverlayToastBridge
import top.xjunz.tasker.task.event.A11yEventDispatcher
import top.xjunz.tasker.task.runtime.ResidentTaskScheduler

/**
 * A service defines the common abstractions of [A11yAutomatorService] and [ShizukuAutomatorService].
 *
 * @author xjunz 2022/07/21
 */
interface AutomatorService {

    val isRunning: Boolean

    val uiAutomatorBridge: UiAutomatorBridge

    val residentTaskScheduler: ResidentTaskScheduler

    val a11yEventDispatcher: A11yEventDispatcher

    val overlayToastBridge: OverlayToastBridge

    fun suppressResidentTaskScheduler(suppress: Boolean)

    fun destroy()

    fun getStartTimestamp(): Long

    fun createAvailabilityChecker(): IAvailabilityChecker
}