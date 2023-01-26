/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.event

import android.app.Notification
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import androidx.test.uiautomator.bridge.UiAutomatorBridge
import kotlinx.coroutines.*
import kotlinx.coroutines.android.asCoroutineDispatcher
import top.xjunz.tasker.BuildConfig
import top.xjunz.tasker.bridge.PackageManagerBridge
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.task.EventDispatcher
import top.xjunz.tasker.service.uiAutomatorBridge
import top.xjunz.tasker.task.applet.flow.ComponentInfoWrapper
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext

/**
 * @author xjunz 2022/10/29
 */
class A11yEventDispatcher(looper: Looper) : EventDispatcher() {

    override val coroutineContext: CoroutineContext =
        Handler(looper).asCoroutineDispatcher() + SupervisorJob()

    private val activityHashCache = mutableSetOf<Int>()

    private var curEventTime: Long = -1
    private var curPackageName: String? = null
    private var curActivityName: String? = null
    private var curPanelTitle: String? = null

    var contentChangedTimeout = 250L

    private var eventDispatchScope: WeakReference<CoroutineScope>? = null

    fun startProcessing(bridge: UiAutomatorBridge) {
        bridge.addOnAccessibilityEventListener {
            launch {
                processAccessibilityEvent(it)
            }
        }
        bridge.startReceivingEvents()
    }

    override fun destroy() {
        cancel()
        uiAutomatorBridge.stopReceivingEvents()
    }

    private suspend fun processAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        // Do not send events from the host application!
        if (packageName == BuildConfig.APPLICATION_ID) return
        if (packageName == "com.android.systemui") return
        if (event.eventTime < curEventTime && !event.isFullScreen) return
        val className = event.className?.toString()
        if (className == "android.inputmethodservice.SoftInputWindow") return
        curEventTime = event.eventTime
        val firstText = event.text.firstOrNull()?.toString()
        val prevPanelTitle = curPanelTitle
        if (firstText != null
            && event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            && event.contentChangeTypes != AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_DISAPPEARED
            && packageName == curPackageName
        )
            curPanelTitle = firstText

        when (event.eventType) {
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                if (className == Notification::class.java.name) {
                    dispatchEvents(
                        Event.obtain(
                            Event.EVENT_ON_NOTIFICATION_RECEIVED,
                            packageName, curActivityName, firstText
                        )
                    )
                }
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {

                val prevActivityName = curActivityName
                val isNewActivity = className != null && className != prevActivityName
                        && isActivityExistent(packageName, className)

                if (isNewActivity) {
                    curActivityName = className
                }

                if (
                    event.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED
                    // Only full screen windows, because there may be overlay windows
                    && event.isFullScreen
                ) {
                    val prevPkgName = curPackageName
                    curPanelTitle = firstText
                    // New package detected
                    if (prevPkgName != packageName) {
                        curPackageName = packageName

                        val pkgEnterEvent = Event.obtain(
                            Event.EVENT_ON_PACKAGE_ENTERED,
                            packageName,
                            curActivityName,
                            curPanelTitle
                        )
                        if (prevPkgName != null) {
                            val pkgExitEvent = Event.obtain(
                                Event.EVENT_ON_PACKAGE_EXITED,
                                prevPkgName,
                                prevActivityName,
                                prevPanelTitle
                            )
                            dispatchEvents(pkgEnterEvent, pkgExitEvent)
                        } else {
                            dispatchEvents(pkgEnterEvent)
                        }
                        return
                    }
                }
                dispatchContentChanged(packageName)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                eventDispatchScope?.get()?.cancel()
                coroutineScope {
                    eventDispatchScope = WeakReference(this)
                    delay(contentChangedTimeout)
                    dispatchContentChanged(packageName)
                }
            }
            else -> dispatchContentChanged(packageName)
        }
    }

    private suspend fun dispatchContentChanged(packageName: String) {
        if (packageName != curPackageName) return
        dispatchEvents(
            Event.obtain(
                Event.EVENT_ON_CONTENT_CHANGED,
                packageName, curActivityName, curPanelTitle
            )
        )
    }

    fun getCurrentComponentInfo(): ComponentInfoWrapper {
        return ComponentInfoWrapper(curPackageName!!, curActivityName, curPanelTitle)
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