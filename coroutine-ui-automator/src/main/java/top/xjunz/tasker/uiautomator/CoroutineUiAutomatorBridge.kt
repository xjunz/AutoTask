/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */
package top.xjunz.tasker.uiautomator

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.UiAutomation
import android.app.UiAutomation.OnAccessibilityEventListener
import android.os.Build
import android.os.SystemClock
import android.util.ArraySet
import android.util.DisplayMetrics
import android.view.Display
import android.view.InputEvent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.TimeoutException

abstract class CoroutineUiAutomatorBridge(val uiAutomation: UiAutomation) {


    companion object {
        const val PACKAGE_SYSTEM_UI = "com.android.systemui"
        const val CLASS_SOFT_INPUT_WINDOW = "android.inputmethodservice.SoftInputWindow"
        const val APPLICATION_ID = "top.xjunz.tasker"
    }

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

    private val mutex = Mutex()

    private val eventQueue = ArrayList<AccessibilityEvent>()

    private var waitingForEventDelivery = false

    private val waitForIdleJobs = ConcurrentLinkedDeque<Job>()

    private val eventListeners = ArraySet<OnAccessibilityEventListener>(2)

    @Suppress("DEPRECATION")
    private val eventListener = OnAccessibilityEventListener listener@{ event: AccessibilityEvent ->
        try {
            val packageName = event.packageName?.toString() ?: return@listener
            // Do not send events from the host application!
            if (packageName == APPLICATION_ID) return@listener
            if (packageName == PACKAGE_SYSTEM_UI) return@listener
            val className = event.className?.toString()
            if (className == CLASS_SOFT_INPUT_WINDOW) return@listener
            cancelAllWaitForIdleJobs()
            if (waitingForEventDelivery) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    eventQueue.add(AccessibilityEvent(event))
                } else {
                    eventQueue.add(AccessibilityEvent.obtain(event))
                }
            }
            for (listener in eventListeners) {
                listener.onAccessibilityEvent(event)
            }
        } finally {
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

    /**
     * Executes a command and waits for a specific accessibility event up to a
     * given wait timeout. To detect a sequence of events one can implement a
     * filter that keeps track of seen events of the expected sequence and
     * returns true after the last event of that sequence is received.
     *
     *
     * **Note:** It is caller's responsibility to recycle the returned event.
     *
     *
     * @param command The command to execute.
     * @param filter Filter that recognizes the expected event.
     * @param timeoutMillis The wait timeout in milliseconds.
     *
     */
    suspend fun executeAndWaitForEvent(
        command: suspend () -> Unit,
        filter: UiAutomation.AccessibilityEventFilter, timeoutMillis: Long
    ): AccessibilityEvent {
        // Acquire the lock and prepare for receiving events.
        mutex.withLock {
            eventQueue.clear()
            // Prepare to wait for an event.
            waitingForEventDelivery = true
        }

        // Note: We have to release the lock since calling out with this lock held
        // can bite. We will correctly filter out events from other interactions,
        // so starting to collect events before running the action is just fine.

        // We will ignore events from previous interactions.
        val executionStartTimeMillis = SystemClock.uptimeMillis()
        // Execute the command *without* the lock being held.
        command.invoke()
        val receivedEvents: MutableList<AccessibilityEvent> = ArrayList()

        // Acquire the lock and wait for the event.
        try {
            // Wait for the event.
            val startTimeMillis = SystemClock.uptimeMillis()
            while (true) {
                val localEvents: MutableList<AccessibilityEvent> = ArrayList()
                mutex.withLock {
                    localEvents.addAll(eventQueue)
                    eventQueue.clear()
                }
                // Drain the event queue
                while (localEvents.isNotEmpty()) {
                    val event = localEvents.removeAt(0)
                    // Ignore events from previous interactions.
                    if (event.eventTime < executionStartTimeMillis) {
                        continue
                    }
                    if (filter.accept(event)) {
                        return event
                    }
                    receivedEvents.add(event)
                }
                // Check if timed out and if not wait.
                val elapsedTimeMillis = SystemClock.uptimeMillis() - startTimeMillis
                val remainingTimeMillis = timeoutMillis - elapsedTimeMillis
                if (remainingTimeMillis <= 0) {
                    throw TimeoutException(
                        "Expected event not received within: "
                                + timeoutMillis + " ms among: " + receivedEvents
                    )
                }
                mutex.withLock {
                    if (eventQueue.isEmpty()) {
                        delay(remainingTimeMillis)
                    }
                }
            }
        } finally {
            val size = receivedEvents.size
            for (i in 0 until size) {
                @Suppress("DEPRECATION")
                receivedEvents[i].recycle()
            }
            mutex.withLock {
                waitingForEventDelivery = false
                eventQueue.clear()
            }
        }
    }

    private fun cancelAllWaitForIdleJobs() {
        for (it in waitForIdleJobs) {
            it?.cancel()
        }
        waitForIdleJobs.clear()
    }

    fun startReceivingEvents() {
        uiAutomation.setOnAccessibilityEventListener(eventListener)
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

    suspend fun waitForIdle(idleAckMills: Long, maxWaitMills: Long): Boolean {
        check(maxWaitMills >= idleAckMills)
        var elapsedMills = 0L
        while (elapsedMills < maxWaitMills) {
            val start = SystemClock.uptimeMillis()
            if (maxWaitMills - elapsedMills < idleAckMills) {
                return false
            }
            coroutineScope {
                val job = async(start = CoroutineStart.LAZY) {
                    delay(idleAckMills)
                }
                waitForIdleJobs.offer(job)
                job.join()
            }
            val actualIdleMills = SystemClock.uptimeMillis() - start
            if (actualIdleMills >= idleAckMills) {
                return true
            }
            elapsedMills += actualIdleMills
        }
        return false
    }

    fun addOnAccessibilityEventListener(listener: OnAccessibilityEventListener) {
        synchronized(eventListeners) {
            eventListeners.add(listener)
        }
    }


}