/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.service

import android.annotation.SuppressLint
import android.os.Looper
import android.os.PowerManager.WakeLock
import top.xjunz.tasker.bridge.OverlayToastBridge
import top.xjunz.tasker.bridge.PowerManagerBridge
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.task.applet.flow.ref.ComponentInfoWrapper
import top.xjunz.tasker.task.event.A11yEventDispatcher
import top.xjunz.tasker.task.event.MetaEventDispatcher
import top.xjunz.tasker.task.event.NetworkEventDispatcher
import top.xjunz.tasker.task.event.PollEventDispatcher
import top.xjunz.tasker.task.runtime.ITaskCompletionCallback
import top.xjunz.tasker.task.runtime.OneshotTaskScheduler
import top.xjunz.tasker.task.runtime.ResidentTaskScheduler
import top.xjunz.tasker.uiautomator.CoroutineUiAutomatorBridge

/**
 * A service defines the common abstractions of [A11yAutomatorService] and [ShizukuAutomatorService].
 *
 * @author xjunz 2022/07/21
 */
interface AutomatorService {

    val isRunning: Boolean

    val uiAutomatorBridge: CoroutineUiAutomatorBridge

    val looper: Looper

    val eventDispatcher: MetaEventDispatcher

    val overlayToastBridge: OverlayToastBridge

    val residentTaskScheduler: ResidentTaskScheduler

    val oneshotTaskScheduler: OneshotTaskScheduler

    val a11yEventDispatcher: A11yEventDispatcher

    var wakeLock: WakeLock?

    fun getCurrentComponentInfo(): ComponentInfoWrapper {
        return a11yEventDispatcher.getCurrentComponentInfo()
    }

    fun scheduleOneshotTask(task: XTask, onCompletion: ITaskCompletionCallback)

    fun stopOneshotTask(task: XTask)

    fun suppressResidentTaskScheduler(suppress: Boolean)

    fun initEventDispatcher() {
        eventDispatcher.registerEventDispatcher(a11yEventDispatcher)
        eventDispatcher.registerEventDispatcher(PollEventDispatcher(looper))
        eventDispatcher.registerEventDispatcher(NetworkEventDispatcher())
        //eventDispatcher.registerEventDispatcher(ClipboardEventDispatcher())
        eventDispatcher.addCallback(residentTaskScheduler)
        eventDispatcher.addCallback(oneshotTaskScheduler)
    }

    fun prepareWorkerMode(acquireWakeLock: Boolean) {
        initEventDispatcher()
        if (acquireWakeLock) {
            acquireWakeLock()
        }
    }

    @SuppressLint("WakelockTimeout")
    fun acquireWakeLock() {
        wakeLock = PowerManagerBridge.obtainWakeLock()
        wakeLock?.acquire()
    }

    fun releaseWakeLock() {
        wakeLock?.release()
        wakeLock = null
    }

    fun destroy() {
        AppletResult.drainPool()
        TaskRuntime.drainPool()
        releaseWakeLock()
    }

    fun getStartTimestamp(): Long

}