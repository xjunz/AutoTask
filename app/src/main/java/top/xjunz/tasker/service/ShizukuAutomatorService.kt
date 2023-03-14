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
import androidx.core.os.bundleOf
import top.xjunz.shared.ktx.casted
import top.xjunz.shared.trace.logcat
import top.xjunz.shared.trace.logcatStackTrace
import top.xjunz.tasker.annotation.Anywhere
import top.xjunz.tasker.annotation.Local
import top.xjunz.tasker.annotation.Privileged
import top.xjunz.tasker.bridge.OverlayToastBridge
import top.xjunz.tasker.bridge.PrivilegedUiAutomatorBridge
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.isAppProcess
import top.xjunz.tasker.isPrivilegedProcess
import top.xjunz.tasker.ktx.isAlive
import top.xjunz.tasker.premium.PremiumMixin
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.task.event.A11yEventDispatcher
import top.xjunz.tasker.task.event.ClipboardEventDispatcher
import top.xjunz.tasker.task.event.MetaEventDispatcher
import top.xjunz.tasker.task.runtime.*
import java.lang.ref.WeakReference
import kotlin.system.exitProcess

/**
 * @see A11yAutomatorService
 */
class ShizukuAutomatorService : IRemoteAutomatorService.Stub, AutomatorService {

    companion object {

        private var instance: WeakReference<ShizukuAutomatorService>? = null

        const val KEY_CONNECTION_ERROR = "bundle.key.CONNECTION_ERROR"

        @Privileged
        fun require(): ShizukuAutomatorService {
            return requireNotNull(instance?.get()) {
                "The ShizukuAutomatorService is not yet started or has dead!"
            }
        }

        @Privileged
        fun get(): ShizukuAutomatorService? {
            return instance?.get()
        }
    }

    private lateinit var uiAutomationHidden: UiAutomationHidden

    private val handlerThread = HandlerThread("ShizukuAutomatorThread")

    private val looper get() = handlerThread.looper

    private val uiAutomation: UiAutomation by lazy {
        uiAutomationHidden.casted()
    }

    private val residentTaskScheduler = ResidentTaskScheduler(PrivilegedTaskManager)

    private val oneshotTaskScheduler by lazy {
        OneshotTaskScheduler()
    }

    private var startTimestamp: Long = -1

    @Local
    private lateinit var delegate: IRemoteAutomatorService

    private val a11yEventDispatcher by lazy {
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
    override val uiAutomatorBridge by lazy {
        ensurePrivilegedProcess()
        PrivilegedUiAutomatorBridge(uiAutomation)
    }

    override val eventDispatcher = MetaEventDispatcher()

    @Anywhere
    override fun getStartTimestamp(): Long {
        return if (isPrivilegedProcess) startTimestamp else delegate.startTimestamp
    }

    @Privileged
    override fun getTaskManager(): IRemoteTaskManager {
        return PrivilegedTaskManager.Delegate
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
            residentTaskScheduler.isSuppressed = suppress
        }
    }


    override fun setPremiumContextStoragePath(path: String) {
        PremiumMixin.premiumContextStoragePath = path
    }

    override fun loadPremiumContext() {
        PremiumMixin.loadPremiumFromFileSafely()
    }

    @Privileged
    override fun scheduleOneshotTask(id: Long, callback: ITaskCompletionCallback) {
        oneshotTaskScheduler.scheduleTask(PrivilegedTaskManager.requireTask(id), callback)
    }

    @Local
    override fun scheduleOneshotTask(task: XTask, onCompletion: ITaskCompletionCallback) {
        delegate.scheduleOneshotTask(task.checksum, onCompletion)
    }

    @Privileged
    override fun stopOneshotTask(id: Long) {
        PrivilegedTaskManager.requireTask(id).halt()
    }

    @Local
    override fun stopOneshotTask(task: XTask) {
        delegate.stopOneshotTask(task.checksum)
    }

    override fun isConnected(): Boolean {
        return startTimestamp != -1L
    }

    override fun connect(callback: ResultReceiver) {
        Binder.clearCallingIdentity()
        try {
            uiAutomationHidden = UiAutomationHidden(looper, UiAutomationConnection())
            uiAutomationHidden.connect(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
            uiAutomation.serviceInfo = uiAutomation.serviceInfo.apply {
                eventTypes = AccessibilityEvent.TYPES_ALL_MASK
                flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                        AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS and
                        AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS.inv()
            }
            Handler(looper).post {
                try {
                    AppletOptionFactory.preloadIfNeeded()
                    initEventDispatcher()
                    startTimestamp = System.currentTimeMillis()
                    callback.send(0, null)
                } catch (t: Throwable) {
                    callback.send(-1, bundleOf(KEY_CONNECTION_ERROR to t.stackTraceToString()))
                }
            }
        } catch (t: Throwable) {
            t.logcatStackTrace()
            callback.send(-1, bundleOf(KEY_CONNECTION_ERROR to t.stackTraceToString()))
        }
    }

    private fun initEventDispatcher() {
        eventDispatcher.registerEventDispatcher(a11yEventDispatcher)
        eventDispatcher.registerEventDispatcher(ClipboardEventDispatcher())
        eventDispatcher.addCallback(residentTaskScheduler)
        eventDispatcher.addCallback(oneshotTaskScheduler)
        a11yEventDispatcher.activate(false)
    }

    @Anywhere
    override fun destroy() {
        if (isAppProcess) {
            delegate.destroy()
        } else try {
            eventDispatcher.destroy()
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