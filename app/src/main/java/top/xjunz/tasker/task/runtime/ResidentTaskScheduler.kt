/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.runtime

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import top.xjunz.shared.trace.logcat
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.ControlFlow
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.runtime.Snapshot
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.engine.task.EventDispatcher
import top.xjunz.tasker.engine.task.TaskScheduler
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.service.uiAutomatorBridge
import top.xjunz.tasker.task.event.A11yEventDispatcher
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * @author xjunz 2022/08/05
 */
class ResidentTaskScheduler(
    private val looper: Looper,
    private val taskManager: TaskManager<*, *>
) : EventDispatcher.Callback, TaskScheduler {

    private val eventDispatcher = A11yEventDispatcher(this)

    private val handlerDispatcher = Handler(looper).asCoroutineDispatcher()

    /**
     * Schedule all active tasks, all of which are running in the thread that owns the [looper].
     */
    override fun scheduleTasks() {
        uiAutomatorBridge.addOnAccessibilityEventListener {
            launch {
                eventDispatcher.processAccessibilityEvent(it)
            }
        }
        uiAutomatorBridge.startReceivingEvents()
    }

    /**
     * Destroy the scheduler. After destroyed, you should not use it any more.
     */
    override fun destroy() {
        uiAutomatorBridge.stopReceivingEvents()
        cancel()
        TaskRuntime.drainPool()
    }

    private fun indent(count: Int): String {
        return Collections.nCopies(count, '-').joinToString("")
    }

    private fun Applet.isAndToString(): String {
        if (this is ControlFlow) return ""
        if (index == 0) return ""
        return if (isAnd) "And " else "Or "
    }

    private val observer = object : TaskRuntime.Observer {
        override fun onStarted(victim: Applet, runtime: TaskRuntime) {
            if (victim is Flow)
                logcat(indent(runtime.tracker.depth) + victim.isAndToString() + victim.javaClass.simpleName)
        }

        override fun onTerminated(victim: Applet, runtime: TaskRuntime) {
            logcat(indent(runtime.tracker.depth) + victim.isAndToString() + "${victim.javaClass.simpleName} -> ${runtime.isSuccessful}")
        }

        override fun onSkipped(victim: Applet, runtime: TaskRuntime) {
            logcat(indent(runtime.tracker.depth) + victim.isAndToString() + "${victim.javaClass.simpleName} -> skipped")
        }
    }

    private val listener = object : XTask.OnStateChangedListener {
        override fun onStarted(runtime: TaskRuntime) {
            super.onStarted(runtime)
            logcat("\n******** $runtime Started ********")
        }

        override fun onError(runtime: TaskRuntime, t: Throwable) {
            super.onError(runtime, t)
            logcat(t.message)
            logcat("-------- $runtime Error --------")
        }

        override fun onFailure(runtime: TaskRuntime) {
            super.onFailure(runtime)
            logcat("-------- $runtime Failure --------")
        }

        override fun onSuccess(runtime: TaskRuntime) {
            super.onSuccess(runtime)
            logcat("-------- $runtime Success --------")
        }

        override fun onCancelled(runtime: TaskRuntime) {
            super.onCancelled(runtime)
            logcat("-------- $runtime Cancelled --------")
        }
    }

    override fun onEvents(events: Array<out Event>) {
        launch {
            val snapshot = Snapshot()
            try {
                for (task in taskManager.enabledTasks) {
                    task.onStateChangedListener = listener
                    task.launch(snapshot, this, events, observer)
                }
            } finally {
                events.forEach {
                    it.recycle()
                }
            }
        }
    }

    override val coroutineContext: CoroutineContext = handlerDispatcher + SupervisorJob()
}