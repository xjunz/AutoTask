/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.event

import android.app.Notification
import android.os.Looper
import android.util.ArraySet
import android.view.accessibility.AccessibilityEvent
import androidx.core.os.HandlerCompat
import androidx.test.uiautomator.bridge.UiAutomatorBridge
import kotlinx.coroutines.*
import kotlinx.coroutines.android.asCoroutineDispatcher
import top.xjunz.tasker.BuildConfig
import top.xjunz.tasker.bridge.PackageManagerBridge
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.task.EventDispatcher
import top.xjunz.tasker.task.applet.flow.ComponentInfoWrapper
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext

/**
 * @author xjunz 2022/10/29
 */
class A11yEventDispatcher(looper: Looper, private val bridge: UiAutomatorBridge) :
    EventDispatcher() {

    override val coroutineContext: CoroutineContext =
        HandlerCompat.createAsync(looper)
            .asCoroutineDispatcher("A11yEventCoroutineDispatcher") + SupervisorJob()

    private val activityHashCache = ArraySet<Int>()

    private var latestEventTime: Long = -1
    private var latestPackageName: String? = null
    private var latestActivityName: String? = null
    private var latestPaneTitle: String? = null

    var contentChangeRateLimitMills = 100L

    private var eventDispatchScope: WeakReference<CoroutineScope>? = null

    fun startProcessing() {
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
            val packageName = event.packageName?.toString() ?: return
            // Do not send events from the host application!
            if (packageName == BuildConfig.APPLICATION_ID) return
            if (packageName == "com.android.systemui") return
            if (event.eventTime < latestEventTime && !event.isFullScreen) return
            val className = event.className?.toString()
            if (className == "android.inputmethodservice.SoftInputWindow") return
            dispatchEventsFromAccessibilityEvent(event, packageName, className)
        } finally {
            @Suppress("DEPRECATION")
            event.recycle()
        }
    }

    private fun dispatchEventsFromAccessibilityEvent(
        event: AccessibilityEvent,
        packageName: String,
        className: String?
    ) {
        latestEventTime = event.eventTime
        val firstText = event.text.firstOrNull()?.toString()
        val prevPanelTitle = latestPaneTitle
        if (firstText != null
            && event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            && event.contentChangeTypes != AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_DISAPPEARED
            && packageName == latestPackageName
        )
            latestPaneTitle = firstText

        when (event.eventType) {
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                if (className == Notification::class.java.name) {
                    dispatchEvents(
                        Event.obtain(
                            Event.EVENT_ON_NOTIFICATION_RECEIVED,
                            packageName, latestActivityName, firstText,
                        )
                    )
                }
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {

                val prevActivityName = latestActivityName
                val isNewActivity = className != null && className != prevActivityName
                        && isActivityExistent(packageName, className)

                if (isNewActivity) {
                    latestActivityName = className
                }

                if (
                    event.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED
                    // Only full screen windows, because there may be overlay windows
                    // todo: Need this?
                    && event.isFullScreen
                ) {
                    val prevPkgName = latestPackageName
                    latestPaneTitle = firstText
                    // New package detected
                    if (prevPkgName != packageName) {
                        latestPackageName = packageName

                        val enterEvent = Event.obtain(
                            Event.EVENT_ON_PACKAGE_ENTERED,
                            packageName,
                            latestActivityName,
                            latestPaneTitle
                        )
                        if (prevPkgName != null) {
                            val exitEvent = Event.obtain(
                                Event.EVENT_ON_PACKAGE_EXITED,
                                prevPkgName,
                                prevActivityName,
                                prevPanelTitle
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
                eventDispatchScope?.get()?.cancel()
                launch {
                    eventDispatchScope = WeakReference(this)
                    delay(contentChangeRateLimitMills)
                    dispatchContentChanged(packageName)
                }
            }
            else -> dispatchContentChanged(packageName)
        }
    }

    private fun dispatchContentChanged(packageName: String) {
        if (packageName != latestPackageName) return
        dispatchEvents(
            Event.obtain(
                Event.EVENT_ON_CONTENT_CHANGED,
                packageName,
                latestActivityName,
                latestPaneTitle,
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