/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

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
        tasks: List<XTask>, arg: Arg, listener: XTask.TaskStateListener? = null
    )

    abstract fun haltAll()

    /**
     * After shutdown, do not use this object anymore.
     */
    open fun shutdown() {
        cancel()
    }
}