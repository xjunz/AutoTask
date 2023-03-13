/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.event

import android.os.Looper
import android.util.ArraySet
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.core.os.HandlerCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.android.asCoroutineDispatcher
import top.xjunz.tasker.BuildConfig
import top.xjunz.tasker.bridge.PackageManagerBridge
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.task.EventDispatcher
import top.xjunz.tasker.isAppProcess
import top.xjunz.tasker.premium.PremiumMixin
import top.xjunz.tasker.service.A11yAutomatorService
import top.xjunz.tasker.service.ShizukuAutomatorService
import top.xjunz.tasker.service.isPremium
import top.xjunz.tasker.task.applet.flow.ref.ComponentInfoWrapper
import top.xjunz.tasker.task.applet.flow.ref.NotificationReferent
import top.xjunz.tasker.uiautomator.CoroutineUiAutomatorBridge
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * @author xjunz 2022/10/29
 */
class A11yEventDispatcher(looper: Looper, private val bridge: CoroutineUiAutomatorBridge) :
    EventDispatcher(), CoroutineScope, PremiumMixin.Callback {

    companion object {
        const val PACKAGE_SYSTEM_UI = "com.android.systemui"
        const val CLASS_SOFT_INPUT_WINDOW = "android.inputmethodservice.SoftInputWindow"
    }

    override val coroutineContext: CoroutineContext = HandlerCompat.createAsync(looper)
        .asCoroutineDispatcher("A11yEventCoroutineDispatcher") + SupervisorJob()

    private val activityRecords = ArraySet<Int>()

    private var latestFilteredEventTimestamp = -1L
    private var latestPackageName: String? = null
    private var latestActivityName: String? = null
    private var latestPaneTitle: String? = null
    private var pendingStopJob: WeakReference<Job>? = null

    fun activate(isInInspectorMode: Boolean) {
        bridge.addOnAccessibilityEventListener {
            processAccessibilityEvent(it)
        }
        bridge.startReceivingEvents()
        if (!isPremium && !isInInspectorMode) {
            val job = launch {
                delay(6.toDuration(DurationUnit.HOURS))
                if (isAppProcess) {
                    A11yAutomatorService.get()?.destroy()
                } else {
                    ShizukuAutomatorService.get()?.destroy()
                }
            }
            pendingStopJob = WeakReference(job)
            PremiumMixin.addOnPremiumStateChangedCallback(this)
        }
    }

    override fun destroy() {
        cancel()
        bridge.stopReceivingEvents()
    }

    private fun processAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        // Do not send events from the host application!
        if (packageName == BuildConfig.APPLICATION_ID) return
        if (packageName == PACKAGE_SYSTEM_UI) return
        if (event.eventTime < latestFilteredEventTimestamp) return
        val className = event.className?.toString()
        if (className == CLASS_SOFT_INPUT_WINDOW) return
        dispatchEventsFromAccessibilityEvent(event, packageName, className)
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
                        latestActivityName = className
                    } else {
                        latestActivityName = className
                        dispatchEvents(newEvent(Event.EVENT_ON_NEW_WINDOW, packageName))
                    }
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

    override fun onPremiumStateChanged(isPremium: Boolean) {
        if (isPremium) {
            pendingStopJob?.get()?.cancel()
        }
    }
}