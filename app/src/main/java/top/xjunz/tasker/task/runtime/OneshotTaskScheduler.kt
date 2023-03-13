/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.runtime

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.engine.runtime.TaskRuntime.Companion.obtainTaskRuntime
import top.xjunz.tasker.engine.task.EventDispatcher
import top.xjunz.tasker.engine.task.TaskScheduler
import top.xjunz.tasker.engine.task.XTask
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * @author xjunz 2023/02/22
 */
class OneshotTaskScheduler : TaskScheduler<Unit>(), EventDispatcher.Callback {

    override val taskType: Int = XTask.TYPE_ONESHOT

    private var currentActiveTask: XTask? = null

    override fun haltAll() {
        currentActiveTask?.halt()
    }

    override val coroutineContext: CoroutineContext =
        Dispatchers.Default + CoroutineName("OneshotTaskScope") + SupervisorJob()

    override fun scheduleTasks(tasks: List<XTask>, arg: Unit, listener: XTask.TaskStateListener?) {
        if (isSuppressed) return
        for (task in tasks) {
            check(task.metadata.taskType == taskType) {
                "Unsupported task type!"
            }
            launch {
                task.setListener(listener)
                currentActiveTask = task
                task.launch(obtainTaskRuntime(task))
                currentActiveTask = null
            }
        }
    }

    fun scheduleTask(task: XTask, onCompletion: ITaskCompletionCallback) {
        if (currentActiveTask != null) return
        scheduleTasks(
            Collections.singletonList(task), Unit,
            object : XTask.TaskStateListener {
                override fun onTaskFinished(runtime: TaskRuntime) {
                    super.onTaskFinished(runtime)
                    onCompletion.onTaskCompleted(runtime.isSuccessful)
                }
            }
        )
    }

    override fun onEvents(events: Array<Event>) {
        currentActiveTask?.getRuntime()?.onNewEvents(events)
    }

}