/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.event

import android.os.Looper
import android.os.SystemClock
import android.util.ArraySet
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.core.os.HandlerCompat
import androidx.test.uiautomator.bridge.UiAutomatorBridge
import kotlinx.coroutines.*
import kotlinx.coroutines.android.asCoroutineDispatcher
import top.xjunz.tasker.BuildConfig
import top.xjunz.tasker.bridge.PackageManagerBridge
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.task.EventDispatcher
import top.xjunz.tasker.task.applet.flow.ref.ComponentInfoWrapper
import top.xjunz.tasker.task.applet.flow.ref.NotificationReferent
import kotlin.coroutines.CoroutineContext

/**
 * @author xjunz 2022/10/29
 */
class A11yEventDispatcher(looper: Looper, private val bridge: UiAutomatorBridge) :
    EventDispatcher() {

    companion object {
        const val PACKAGE_SYSTEM_UI = "com.android.systemui"
        const val CLASS_SOFT_INPUT_WINDOW = "android.inputmethodservice.SoftInputWindow"
    }

    override val coroutineContext: CoroutineContext = HandlerCompat.createAsync(looper)
        .asCoroutineDispatcher("A11yEventCoroutineDispatcher") + SupervisorJob()

    private val activityHashCache = ArraySet<Int>()
    private var waitingForIdleJobs = ArraySet<Job>()
    private var previousEventTimestamp = -1L

    private var latestEventTime: Long = -1
    private var latestPackageName: String? = null
    private var latestActivityName: String? = null
    private var latestPaneTitle: String? = null

    fun start() {
        bridge.addOnAccessibilityEventListener {
            processAccessibilityEvent(it)
        }
        bridge.startReceivingEvents()
    }

    override fun destroy() {
        cancel()
        bridge.stopReceivingEvents()
    }

    private fun processAccessibilityEvent(event: AccessibilityEvent) {
        try {
            previousEventTimestamp = SystemClock.uptimeMillis()
            waitingForIdleJobs.forEach {
                it.cancel()
            }
            waitingForIdleJobs.clear()
            val packageName = event.packageName?.toString() ?: return
            // Do not send events from the host application!
            if (packageName == BuildConfig.APPLICATION_ID) return
            if (packageName == PACKAGE_SYSTEM_UI) return
            if (event.eventTime < latestEventTime && !event.isFullScreen) return
            val className = event.className?.toString()
            if (className == CLASS_SOFT_INPUT_WINDOW) return
            dispatchEventsFromAccessibilityEvent(event, packageName, className)
        } finally {
            @Suppress("DEPRECATION")
            event.recycle()
        }
    }

    suspend fun waitForIdle(idleMills: Long, maxWaitMills: Long): Boolean {
        check(maxWaitMills > idleMills)
        var elpased = SystemClock.uptimeMillis() - previousEventTimestamp
        if (elpased >= idleMills) {
            return true
        }
        while (elpased < maxWaitMills) {
            val start = SystemClock.uptimeMillis()
            val job = async(start = CoroutineStart.LAZY) {
                delay(idleMills)
            }
            waitingForIdleJobs.add(job)
            job.join()
            val idle = SystemClock.uptimeMillis() - start
            if (idle >= idleMills) return true
            elpased += idle
        }
        return false
    }

    private fun dispatchEventsFromAccessibilityEvent(
        a11yEvent: AccessibilityEvent,
        packageName: String,
        className: String?
    ) {
        latestEventTime = a11yEvent.eventTime
        val firstText = a11yEvent.text.firstOrNull()?.toString()
        val prevPanelTitle = latestPaneTitle
        if (firstText != null
            && a11yEvent.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            && a11yEvent.contentChangeTypes != AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_DISAPPEARED
            && packageName == latestPackageName
        ) {
            latestPaneTitle = firstText
        }

        when (a11yEvent.eventType) {
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                val event = Event(
                    Event.EVENT_ON_NOTIFICATION_RECEIVED, packageName,
                    latestActivityName, firstText,
                )
                event.putExtra(
                    NotificationReferent.EXTRA_IS_TOAST,
                    className == Toast::class.java.name
                )
                dispatchEvents(event)
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {

                val prevActivityName = latestActivityName
                val isNewActivity = className != null && className != prevActivityName
                        && isActivityExistent(packageName, className)

                if (isNewActivity) {
                    latestActivityName = className
                }

                if (
                    a11yEvent.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED
                    // Only full screen windows, because there may be overlay windows
                    // todo: Need this?
                    && a11yEvent.isFullScreen
                ) {
                    val prevPkgName = latestPackageName
                    latestPaneTitle = firstText
                    // New package detected
                    if (prevPkgName != packageName) {
                        latestPackageName = packageName

                        val enterEvent = Event(
                            Event.EVENT_ON_PACKAGE_ENTERED, packageName,
                            latestActivityName, latestPaneTitle
                        )
                        if (prevPkgName != null) {
                            val exitEvent = Event(
                                Event.EVENT_ON_PACKAGE_EXITED, prevPkgName,
                                prevActivityName, prevPanelTitle
                            )
                            dispatchEvents(enterEvent, exitEvent)
                        } else {
                            dispatchEvents(enterEvent)
                        }
                        return
                    }
                }
                dispatchContentChanged(packageName)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                dispatchContentChanged(packageName)
            }
            else -> dispatchContentChanged(packageName)
        }
    }

    private fun dispatchContentChanged(packageName: String) {
        if (packageName != latestPackageName) return
        //if (SystemClock.uptimeMillis() - latestContentChange < contentChangeRateLimitMills) return
        dispatchEvents(
            Event(
                Event.EVENT_ON_CONTENT_CHANGED, packageName,
                latestActivityName, latestPaneTitle,
            )
        )
    }

    fun getCurrentComponentInfo(): ComponentInfoWrapper {
        return ComponentInfoWrapper(latestPackageName!!, latestActivityName, latestPaneTitle)
    }

    private fun isActivityExistent(pkgName: String, actName: String): Boolean {
        val hashCode = 31 * pkgName.hashCode() + actName.hashCode()
        if (activityHashCache.contains(hashCode)) return true
        if (activityHashCache.contains(-hashCode)) return false
        return if (PackageManagerBridge.isActivityExistent(pkgName, actName)) {
            activityHashCache.add(hashCode)
            true
        } else {
            activityHashCache.add(-hashCode)
            false
        }
    }
}