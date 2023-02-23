/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.service

import top.xjunz.tasker.bridge.OverlayToastBridge
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.task.event.A11yEventDispatcher
import top.xjunz.tasker.task.runtime.ITaskCompletionCallback
import top.xjunz.tasker.uiautomator.CoroutineUiAutomatorBridge

/**
 * A service defines the common abstractions of [A11yAutomatorService] and [ShizukuAutomatorService].
 *
 * @author xjunz 2022/07/21
 */
interface AutomatorService {

    val isRunning: Boolean

    val uiAutomatorBridge: CoroutineUiAutomatorBridge

    val a11yEventDispatcher: A11yEventDispatcher

    val overlayToastBridge: OverlayToastBridge

    fun scheduleOneshotTask(task: XTask, onCompletion: ITaskCompletionCallback)

    fun suppressResidentTaskScheduler(suppress: Boolean)

    fun destroy()

    fun getStartTimestamp(): Long

    fun createAvailabilityChecker(): IAvailabilityChecker
}