package top.xjunz.tasker.service

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.UiAutomation
import android.app.UiAutomationConnection
import android.app.UiAutomationHidden
import android.os.Binder
import android.os.Handler
import android.os.HandlerThread
import android.system.Os
import android.view.accessibility.AccessibilityEvent
import androidx.test.uiautomator.bridge.UiAutomatorBridge
import top.xjunz.shared.ktx.unsafeCast
import top.xjunz.tasker.IAutomatorConnection
import top.xjunz.tasker.annotation.Local
import top.xjunz.tasker.annotation.LocalAndRemote
import top.xjunz.tasker.annotation.Remote
import top.xjunz.tasker.impl.AvailabilityChecker
import top.xjunz.tasker.impl.IAvailabilityChecker
import top.xjunz.tasker.impl.ShizukuUiAutomatorBridge
import top.xjunz.tasker.isInHostProcess
import top.xjunz.tasker.isInRemoteProcess
import top.xjunz.tasker.trace.logcat
import java.lang.ref.WeakReference
import kotlin.system.exitProcess

class ShizukuAutomatorService : IAutomatorConnection.Stub, AutomatorService {

    companion object {

        private var instanceRef: WeakReference<ShizukuAutomatorService>? = null

        fun require(): ShizukuAutomatorService {
            return requireNotNull(instanceRef?.get()) {
                "The ShizukuAutomatorService is not yet started or has dead!"
            }
        }
    }

    private lateinit var delegate: IAutomatorConnection

    private lateinit var uiAutomationHidden: UiAutomationHidden

    private val handlerThread = HandlerThread("ShizukuAutomatorThread")

    private val handler by lazy { Handler(looper) }

    private val looper get() = handlerThread.looper

    private val uiAutomation: UiAutomation by lazy {
        uiAutomationHidden.unsafeCast()
    }

    private var startTimestamp: Long = -1

    @Remote
    constructor() {
        logcat("Hello from the remote service! My uid is ${Os.getuid()} and my pid is ${Os.getpid()}")
        handlerThread.isDaemon = false
        handlerThread.start()
        instanceRef = WeakReference(this)
    }

    @Local
    constructor(connection: IAutomatorConnection) {
        delegate = connection
    }

    @Local
    override val isRunning: Boolean
        get() = delegate.asBinder().pingBinder() && delegate.asBinder().isBinderAlive

    override val uiAutomatorBridge: UiAutomatorBridge by lazy {
        ShizukuUiAutomatorBridge(uiAutomation)
    }

    @LocalAndRemote
    override fun getStartTimestamp(): Long {
        return if (isInRemoteProcess) startTimestamp else delegate.startTimestamp
    }

    @LocalAndRemote
    override fun createAvailabilityChecker(): IAvailabilityChecker {
        return if (isInRemoteProcess) {
            AvailabilityChecker(this, looper)
        } else {
            delegate.createAvailabilityChecker()
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
                        AccessibilityEvent.TYPE_WINDOWS_CHANGED
                notificationTimeout = 100
                flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                        AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            }
            startTimestamp = System.currentTimeMillis()
        } catch (t: Throwable) {
            error(t)
        }
    }

    @LocalAndRemote
    override fun destroy() {
        if (isInHostProcess) {
            delegate.destroy()
        } else try {
            handler.removeCallbacksAndMessages(null)
            if (::uiAutomationHidden.isInitialized) {
                uiAutomationHidden.disconnect()
            }
            if (handlerThread.isAlive) {
                handlerThread.quitSafely()
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        } finally {
            exitProcess(0)
        }
    }
}