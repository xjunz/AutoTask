/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import top.xjunz.shared.trace.logcat
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.engine.runtime.ValueRegistry
import top.xjunz.tasker.engine.task.EventDispatcher
import top.xjunz.tasker.engine.task.TaskManager
import top.xjunz.tasker.engine.task.TaskScheduler
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.isAppProcess

/**
 * @author xjunz 2022/08/05
 */
class ResidentTaskScheduler(private val taskManager: TaskManager<*, *>) : EventDispatcher.Callback,
    TaskScheduler {

    override var isSuppressed = false
        set(value) {
            if (value) {
                // Halt all running tasks once suppressed
                if (isAppProcess) {
                    LocalTaskManager.getEnabledTasks()
                } else {
                    RemoteTaskManager.getEnabledTasks()
                }.forEach {
                    it.halt()
                }
            }
            field = value
        }

    /**
     * Destroy the scheduler. After destroyed, you should not use it any more.
     */
    override fun shutdown() {
        TaskRuntime.drainPool()
    }

    private val listener = object : XTask.TaskStateListener {
        override fun onStarted(runtime: TaskRuntime) {
            super.onStarted(runtime)
            logcat("\n\n")
            logcat("******** $runtime Started[${Thread.currentThread()}] ********")
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
        if (isSuppressed) return
        val valueRegistry = ValueRegistry()
        for (task in taskManager.getEnabledTasks()) {
            if (!task.isExecuting || task.isSuspending) {
                // Create a new coroutine scope, do not suspend current coroutine
                supervisorScope {
                    launch(Dispatchers.Default) {
                        task.taskStateListener = listener
                        task.launch(valueRegistry, this, events)
                    }
                }
            } else {
                task.requireRuntime().onNewEvents(events)
            }
        }
    }

}