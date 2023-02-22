/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/12/04
 */
abstract class TaskScheduler<Arg> : CoroutineScope {

    protected abstract val taskType: Int

    var isSuppressed: Boolean = false
        set(value) {
            field = value
            if (value) {
                haltAll()
            }
        }

    protected abstract fun scheduleTasks(
        tasks: Iterator<XTask>, arg: Arg, listener: XTask.TaskStateListener? = null
    )

    abstract fun haltAll()

    /**
     * After shutdown, do not use this object anymore.
     */
    fun shutdown() {
        cancel()
        TaskRuntime.drainPool()
    }
}