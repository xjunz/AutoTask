/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.service

import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.UiAutomation
import android.app.UiAutomationConnection
import android.app.UiAutomationHidden
import android.graphics.Typeface
import android.os.*
import android.system.Os
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.Keep
import androidx.test.uiautomator.bridge.UiAutomatorBridge
import top.xjunz.shared.ktx.casted
import top.xjunz.shared.trace.logcat
import top.xjunz.shared.trace.logcatStackTrace
import top.xjunz.shared.utils.rethrowInRemoteProcess
import top.xjunz.tasker.annotation.Anywhere
import top.xjunz.tasker.annotation.Local
import top.xjunz.tasker.annotation.Privileged
import top.xjunz.tasker.bridge.OverlayToastBridge
import top.xjunz.tasker.isAppProcess
import top.xjunz.tasker.isPrivilegedProcess
import top.xjunz.tasker.ktx.isAlive
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.task.event.A11yEventDispatcher
import top.xjunz.tasker.task.runtime.IRemoteTaskManager
import top.xjunz.tasker.task.runtime.RemoteTaskManager
import top.xjunz.tasker.task.runtime.ResidentTaskScheduler
import top.xjunz.tasker.uiautomator.ShizukuUiAutomatorBridge
import top.xjunz.tasker.util.ReflectionUtil.isLazilyInitialized
import java.lang.ref.WeakReference
import kotlin.system.exitProcess

/**
 * @see A11yAutomatorService
 */
class ShizukuAutomatorService : IRemoteAutomatorService.Stub, AutomatorService {

    companion object {

        private var instance: WeakReference<ShizukuAutomatorService>? = null

        @Privileged
        fun require(): ShizukuAutomatorService {
            return requireNotNull(instance?.get()) {
                "The ShizukuAutomatorService is not yet started or has dead!"
            }
        }
    }

    private lateinit var uiAutomationHidden: UiAutomationHidden

    private val handlerThread = HandlerThread("ShizukuAutomatorThread")

    private val looper get() = handlerThread.looper

    private val uiAutomation: UiAutomation by lazy {
        uiAutomationHidden.casted()
    }

    private var startTimestamp: Long = -1

    @Local
    private lateinit var delegate: IRemoteAutomatorService

    override val residentTaskScheduler by lazy {
        ResidentTaskScheduler(RemoteTaskManager)
    }

    override val a11yEventDispatcher by lazy {
        A11yEventDispatcher(looper, uiAutomatorBridge)
    }

    override val overlayToastBridge: OverlayToastBridge by lazy {
        OverlayToastBridge(looper)
    }

    /**
     * This constructor will be called by ShizukuServer, keep it!
     */
    @Keep
    @Privileged
    constructor() {
        ensurePrivilegedProcess()
        logcat("Hello from the remote service! My uid is ${Os.getuid()} and my pid is ${Os.getpid()}")
        handlerThread.isDaemon = false
        handlerThread.start()
        instance = WeakReference(this)
    }

    @Local
    constructor(remote: IRemoteAutomatorService) {
        delegate = remote
    }

    private fun ensurePrivilegedProcess() {
        check(isPrivilegedProcess) {
            "You cannot access Shizuku UiAutomatorBridge from the host process!"
        }
    }

    @Local
    override val isRunning: Boolean get() = delegate.asBinder().isAlive

    @Privileged
    override val uiAutomatorBridge: UiAutomatorBridge by lazy {
        ensurePrivilegedProcess()
        ShizukuUiAutomatorBridge(uiAutomation)
    }

    @Anywhere
    override fun getStartTimestamp(): Long {
        return if (isPrivilegedProcess) startTimestamp else delegate.startTimestamp
    }

    @Anywhere
    override fun createAvailabilityChecker(): IAvailabilityChecker {
        return if (isPrivilegedProcess) {
            AvailabilityChecker(this, looper)
        } else {
            delegate.createAvailabilityChecker()
        }
    }

    override fun executeShellCmd(cmd: String) {

    }

    override fun getTaskManager(): IRemoteTaskManager {
        return RemoteTaskManager.Delegate
    }

    /**
     * A decompiled code snippet from `TakoStats`. **Praise Rikka**!
     */
    @SuppressLint("BlockedPrivateApi")
    override fun setSystemTypefaceSharedMemory(mem: SharedMemory) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            Typeface::class.java.getDeclaredMethod("setSystemFontMap", SharedMemory::class.java)
                .invoke(null, mem)
        }
    }

    override fun suppressResidentTaskScheduler(suppress: Boolean) {
        if (isAppProcess) {
            delegate.suppressResidentTaskScheduler(suppress)
        } else {
            if (::residentTaskScheduler.isLazilyInitialized) {
                residentTaskScheduler.isSuppressed = suppress
            }
        }
    }

    override fun isConnected(): Boolean {
        return startTimestamp != -1L
    }

    override fun connect() {
        Binder.clearCallingIdentity()
        try {
            uiAutomationHidden = UiAutomationHidden(looper, UiAutomationConnection())
            uiAutomationHidden.connect(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
            uiAutomation.serviceInfo = uiAutomation.serviceInfo.apply {
                eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED or
                        AccessibilityEvent.TYPE_ANNOUNCEMENT
                flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                        AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS and
                        AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS.inv()
            }
            AppletOptionFactory.preloadIfNeeded()
            residentTaskScheduler.scheduleTasks(a11yEventDispatcher)
            a11yEventDispatcher.start()
            startTimestamp = System.currentTimeMillis()
        } catch (t: Throwable) {
            t.rethrowInRemoteProcess()
        }
    }

    @Anywhere
    override fun destroy() {
        if (isAppProcess) {
            delegate.destroy()
        } else try {
            a11yEventDispatcher.destroy()
            residentTaskScheduler.shutdown()
            if (::uiAutomationHidden.isInitialized) {
                Binder.clearCallingIdentity()
                uiAutomationHidden.disconnect()
            }
            if (handlerThread.isAlive) {
                handlerThread.quitSafely()
            }
        } catch (t: Throwable) {
            t.logcatStackTrace()
        } finally {
            exitProcess(0)
        }
    }
}