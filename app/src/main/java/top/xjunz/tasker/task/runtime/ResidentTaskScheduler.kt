/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.runtime

import kotlinx.coroutines.coroutineScope
import top.xjunz.shared.trace.logcat
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.ControlFlow
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.runtime.EventScope
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.engine.task.EventDispatcher
import top.xjunz.tasker.engine.task.TaskScheduler
import top.xjunz.tasker.engine.task.XTask
import java.util.*

/**
 * @author xjunz 2022/08/05
 */
class ResidentTaskScheduler(private val taskManager: TaskManager<*, *>) : EventDispatcher.Callback,
    TaskScheduler {

    /**
     * Destroy the scheduler. After destroyed, you should not use it any more.
     */
    override fun release() {
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
                logcat(indent(runtime.tracker.depth) + victim.isAndToString() + victim)
        }

        override fun onTerminated(victim: Applet, runtime: TaskRuntime) {
            val indents = indent(runtime.tracker.depth)
            logcat(indents + victim.isAndToString() + "$victim -> ${runtime.isSuccessful}")
            if (!runtime.isSuccessful) {
                val failure = runtime.getFailure(victim)
                if (failure != null) {
                    logcat(indents + "expected: ${failure.first}, actual: ${failure.second}")
                }
            }
        }

        override fun onSkipped(victim: Applet, runtime: TaskRuntime) {
            logcat(indent(runtime.tracker.depth) + victim.isAndToString() + "$victim -> skipped")
        }
    }

    private val listener = object : XTask.OnStateChangedListener {
        override fun onStarted(runtime: TaskRuntime) {
            super.onStarted(runtime)
            logcat("\n\n")
            logcat("******** $runtime Started ********")
        }

        override fun onError(runtime: TaskRuntime, t: Throwable) {
            super.onError(runtime, t)
            logcat(t.stackTraceToString())
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

    override suspend fun onEvents(events: Array<out Event>) {
        val eventScope = EventScope()
        try {
            for (task in taskManager.getEnabledTasks()) {
                task.onStateChangedListener = listener
                coroutineScope {
                    task.launch(eventScope, this, events, observer)
                }
            }
        } finally {
            events.forEach {
                it.recycle()
            }
        }
    }

}