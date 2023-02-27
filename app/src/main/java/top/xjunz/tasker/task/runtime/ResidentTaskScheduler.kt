/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.runtime

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import top.xjunz.shared.trace.logcat
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.engine.runtime.TaskRuntime.Companion.obtainTaskRuntime
import top.xjunz.tasker.engine.runtime.ValueRegistry
import top.xjunz.tasker.engine.task.EventDispatcher
import top.xjunz.tasker.engine.task.TaskManager
import top.xjunz.tasker.engine.task.TaskScheduler
import top.xjunz.tasker.engine.task.XTask
import kotlin.coroutines.CoroutineContext

/**
 * @author xjunz 2022/08/05
 */
class ResidentTaskScheduler(private val taskManager: TaskManager<*, *>) : EventDispatcher.Callback,
    TaskScheduler<Array<Event>>() {

    override val taskType: Int = XTask.TYPE_RESIDENT

    override val coroutineContext: CoroutineContext =
        Dispatchers.Default + CoroutineName("ResidentTaskScope") + SupervisorJob()

    override fun haltAll() {
        taskManager.getEnabledResidentTasks().asSequence().filter {
            it.metadata.taskType == taskType
        }.forEach {
            it.halt()
        }
    }

    override fun scheduleTasks(
        tasks: Iterator<XTask>,
        arg: Array<Event>,
        listener: XTask.TaskStateListener?
    ) {
        if (isSuppressed) return
        val registry = ValueRegistry()
        for (task in tasks) {
            check(task.metadata.taskType == taskType) {
                "Unsupported task type!"
            }
            if (!task.isExecuting || task.isSuspending) {
                val eventsHash = arg.contentHashCode()
                // Too hot, do not touch it now!
                if (task.isOverheat(eventsHash)) continue
                launch {
                    task.previousArgumentHash = eventsHash
                    task.setListener(listener)
                    task.launch(obtainTaskRuntime(task, registry, arg))
                }
            } else {
                task.getRuntime()?.onNewEvents(arg)
            }
        }
    }

    private fun getResidentTaskSequence(): Sequence<XTask> {
        return taskManager.getEnabledResidentTasks().asSequence().filter {
            it.metadata.taskType == XTask.TYPE_RESIDENT
        }
    }

    override fun onEvents(events: Array<Event>) {
        scheduleTasks(getResidentTaskSequence().iterator(), events, listener)
    }


    private val listener = object : XTask.TaskStateListener {
        override fun onTaskStarted(runtime: TaskRuntime) {
            super.onTaskStarted(runtime)
            logcat("\n\n")
            logcat("******** $runtime Started[${Thread.currentThread()}] ********")
        }

        override fun onTaskError(runtime: TaskRuntime, t: Throwable) {
            super.onTaskError(runtime, t)
            logcat(t.stackTraceToString())
            logcat("-------- $runtime Error --------")
        }

        override fun onTaskFailure(runtime: TaskRuntime) {
            super.onTaskFailure(runtime)
            logcat("-------- $runtime Failure --------")
        }

        override fun onTaskSuccess(runtime: TaskRuntime) {
            super.onTaskSuccess(runtime)
            logcat("-------- $runtime Success --------")
        }

        override fun onTaskCancelled(runtime: TaskRuntime) {
            super.onTaskCancelled(runtime)
            logcat("-------- $runtime Cancelled --------")
        }
    }

}