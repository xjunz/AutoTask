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

    private val activityRecords = ArraySet<Int>()

    private val suspendingJobs = ArrayDeque<Job>()

    private var previousEventTimestamp = -1L

    private var latestFilteredEventTimestamp = -1L
    private var latestPackageName: String? = null
    private var latestActivityName: String? = null
    private var latestPaneTitle: String? = null

    fun activate() {
        bridge.addOnAccessibilityEventListener {
            processAccessibilityEvent(it)
        }
        bridge.startReceivingEvents()
    }

    override fun destroy() {
        cancel()
        bridge.stopReceivingEvents()
    }

    suspend fun waitForIdle(idleMills: Long, maxWaitMills: Long): Boolean {
        check(maxWaitMills >= idleMills)
        var elpased = SystemClock.uptimeMillis() - previousEventTimestamp
        if (elpased >= idleMills) {
            return true
        }
        while (elpased < maxWaitMills) {
            val start = SystemClock.uptimeMillis()
            val job = async(start = CoroutineStart.LAZY) {
                delay(idleMills)
            }
            suspendingJobs.addLast(job)
            job.join()
            val idle = SystemClock.uptimeMillis() - start
            if (idle >= idleMills) return true
            elpased += idle
        }
        return false
    }

    private fun processAccessibilityEvent(event: AccessibilityEvent) {
        try {
            previousEventTimestamp = SystemClock.uptimeMillis()
            for (job in suspendingJobs) {
                job.cancel()
            }
            suspendingJobs.clear()
            val packageName = event.packageName?.toString() ?: return
            // Do not send events from the host application!
            if (packageName == BuildConfig.APPLICATION_ID) return
            if (packageName == PACKAGE_SYSTEM_UI) return
            if (event.eventTime < latestFilteredEventTimestamp) return
            val className = event.className?.toString()
            if (className == CLASS_SOFT_INPUT_WINDOW) return
            dispatchEventsFromAccessibilityEvent(event, packageName, className)
        } finally {
            @Suppress("DEPRECATION")
            event.recycle()
        }
    }

    private fun dispatchEventsFromAccessibilityEvent(
        a11yEvent: AccessibilityEvent,
        packageName: String,
        className: String?
    ) {
        latestFilteredEventTimestamp = a11yEvent.eventTime
        val firstText = a11yEvent.text.firstOrNull()?.toString()
        val prevPaneTitle = latestPaneTitle
        if (!firstText.isNullOrEmpty()
            && a11yEvent.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            && a11yEvent.contentChangeTypes != AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_DISAPPEARED
        ) {
            latestPaneTitle = firstText
        }

        when (a11yEvent.eventType) {
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                val event = newEvent(Event.EVENT_ON_NOTIFICATION_RECEIVED, packageName)
                event.putExtra(
                    NotificationReferent.EXTRA_IS_TOAST,
                    className == Toast::class.java.name
                )
                dispatchEvents(event)
            }

            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val isNewActivity = className != null
                        && className != latestActivityName
                        && isActivity(packageName, className)

                if (isNewActivity) {
                    if (latestPackageName != packageName) {
                        val newWindowEvent =
                            newEvent(Event.EVENT_ON_NEW_WINDOW, packageName, className)
                        val enterEvent =
                            newEvent(Event.EVENT_ON_PACKAGE_ENTERED, packageName, className)
                        if (latestPackageName != null) {
                            val exitEvent = Event.obtain(
                                Event.EVENT_ON_PACKAGE_EXITED, latestPackageName!!,
                                latestActivityName, prevPaneTitle
                            )
                            dispatchEvents(enterEvent, exitEvent, newWindowEvent)
                        } else {
                            dispatchEvents(enterEvent, newWindowEvent)
                        }
                        latestPackageName = packageName
                    }
                    latestActivityName = className
                } else {
                    dispatchEvents(newEvent(Event.EVENT_ON_NEW_WINDOW, packageName))
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                dispatchEvents(newEvent(Event.EVENT_ON_CONTENT_CHANGED, packageName))
            }
            else -> {
                // logcat(a11yEvent)
            }
        }
    }

    private fun newEvent(
        event: Int,
        packageName: String,
        actName: String? = latestActivityName,
        paneTitle: String? = latestPaneTitle
    ): Event {
        return Event.obtain(event, packageName, actName, paneTitle)
    }

    fun getCurrentComponentInfo(): ComponentInfoWrapper {
        return ComponentInfoWrapper(latestPackageName!!, latestActivityName, latestPaneTitle)
    }

    private fun isActivity(pkgName: String, actName: String): Boolean {
        val hashCode = 31 * pkgName.hashCode() + actName.hashCode()
        if (activityRecords.contains(hashCode)) return true
        if (activityRecords.contains(-hashCode)) return false
        return if (PackageManagerBridge.isActivityExistent(pkgName, actName)) {
            activityRecords.add(hashCode)
            true
        } else {
            activityRecords.add(-hashCode)
            false
        }
    }
}