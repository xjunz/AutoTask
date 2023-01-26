/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.runtime

import kotlinx.coroutines.coroutineScope
import top.xjunz.shared.trace.logcat
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.runtime.EventScope
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.engine.task.EventDispatcher
import top.xjunz.tasker.engine.task.TaskManager
import top.xjunz.tasker.engine.task.TaskScheduler
import top.xjunz.tasker.engine.task.XTask

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

    private val listener = object : XTask.TaskStateListener {
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
                task.taskStateListener = listener
                coroutineScope {
                    task.launch(eventScope, this, events)
                }
            }
        } finally {
            events.forEach {
                it.recycle()
            }
        }
    }

}