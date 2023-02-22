/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.runtime

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.engine.runtime.TaskRuntime.Companion.obtainTaskRuntime
import top.xjunz.tasker.engine.task.TaskScheduler
import top.xjunz.tasker.engine.task.XTask
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * @author xjunz 2023/02/22
 */
class OneshotTaskScheduler : TaskScheduler<Unit>() {

    override val taskType: Int = XTask.TYPE_ONESHOT

    override fun haltAll() {
        /* no-op */
    }

    override val coroutineContext: CoroutineContext =
        Dispatchers.Default + CoroutineName("OneshotTaskScope") + SupervisorJob()

    override fun scheduleTasks(
        tasks: Iterator<XTask>,
        arg: Unit,
        listener: XTask.TaskStateListener?
    ) {
        if (isSuppressed) return
        for (task in tasks) {
            check(task.metadata.taskType == taskType) {
                "Unsupported task type!"
            }
            launch {
                task.setListener(listener)
                task.launch(obtainTaskRuntime(task))
            }
        }
    }

    fun scheduleTask(task: XTask, onCompletion: ITaskCompletionCallback) {
        scheduleTasks(
            Collections.singleton(task).iterator(), Unit,
            object : XTask.TaskStateListener {
                override fun onTaskFinished(runtime: TaskRuntime) {
                    super.onTaskFinished(runtime)
                    onCompletion.onTaskCompleted(runtime.isSuccessful)
                }
            }
        )
    }

}