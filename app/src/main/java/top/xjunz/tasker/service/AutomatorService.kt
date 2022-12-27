/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.service

import androidx.test.uiautomator.bridge.UiAutomatorBridge

/**
 * A service defines the common abstractions of [A11yAutomatorService] and [ShizukuAutomatorService].
 *
 * @author xjunz 2022/07/21
 */
interface AutomatorService {

    val isRunning: Boolean

    val uiAutomatorBridge: UiAutomatorBridge

    fun destroy()

    fun getStartTimestamp(): Long

    fun createAvailabilityChecker(): IAvailabilityChecker
}