/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */
package top.xjunz.tasker.uiautomator

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.UiAutomation
import android.app.UiAutomation.OnAccessibilityEventListener
import android.os.SystemClock
import android.util.ArraySet
import android.util.DisplayMetrics
import android.view.Display
import android.view.InputEvent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentLinkedDeque

abstract class CoroutineUiAutomatorBridge(val uiAutomation: UiAutomation) {

    private var latestEventTimestamp = -1L

    /**
     * Only for density purpose, may not accurate for screen width and screen height.
     */
    @Suppress("DEPRECATION")
    val displayMetrics: DisplayMetrics by lazy {
        DisplayMetrics().also {
            defaultDisplay.getMetrics(it)
        }
    }

    val uiDevice: CoroutineUiDevice by lazy {
        CoroutineUiDevice(this)
    }

    private val waitForIdleJobs = ConcurrentLinkedDeque<WeakReference<CoroutineScope>>()

    private val eventListeners = ArraySet<OnAccessibilityEventListener>(2)

    private val eventListener = OnAccessibilityEventListener { event: AccessibilityEvent ->
        try {
            for (listener in eventListeners) {
                listener.onAccessibilityEvent(event)
            }
        } finally {
            @Suppress("DEPRECATION")
            event.recycle()
        }
    }

    abstract val rotation: Int

    abstract val isScreenOn: Boolean

    abstract val defaultDisplay: Display

    abstract val launcherPackageName: String?

    abstract val scaledMinimumFlingVelocity: Int

    abstract val interactionController: CoroutineInteractionController

    abstract val gestureController: CoroutineGestureController

    private fun cancelAllWaitForIdleJobs() {
        for (it in waitForIdleJobs) {
            it.get()?.cancel()
        }
        waitForIdleJobs.clear()
    }

    fun startReceivingEvents() {
        uiAutomation.setOnAccessibilityEventListener(eventListener)
        addOnAccessibilityEventListener {
            latestEventTimestamp = SystemClock.uptimeMillis()
            cancelAllWaitForIdleJobs()
        }
    }

    fun stopReceivingEvents() {
        cancelAllWaitForIdleJobs()
        eventListeners.clear()
        uiAutomation.setOnAccessibilityEventListener(null)
    }

    fun injectInputEvent(event: InputEvent, sync: Boolean): Boolean {
        return uiAutomation.injectInputEvent(event, sync)
    }

    fun setRotation(rotation: Int): Boolean {
        return uiAutomation.setRotation(rotation)
    }

    fun setCompressedLayoutHierarchy(compressed: Boolean) {
        val info = uiAutomation.serviceInfo
        if (compressed) info.flags =
            info.flags and AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS.inv() else info.flags =
            info.flags or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        uiAutomation.serviceInfo = info
    }

    suspend fun waitForIdle(idleMills: Long, maxWaitMills: Long): Boolean {
        check(maxWaitMills >= idleMills)
        var elapsed = if (latestEventTimestamp == -1L) 0
        else SystemClock.uptimeMillis() - latestEventTimestamp

        if (elapsed >= idleMills) {
            return true
        }
        while (elapsed < maxWaitMills) {
            val start = SystemClock.uptimeMillis()
            coroutineScope {
                delay(idleMills)
                waitForIdleJobs.addLast(WeakReference(this))
            }
            val idle = SystemClock.uptimeMillis() - start
            if (idle >= idleMills) {
                return true
            }
            elapsed += idle
        }
        return false
    }

    fun addOnAccessibilityEventListener(listener: OnAccessibilityEventListener) {
        synchronized(eventListeners) {
            eventListeners.add(listener)
        }
    }


}