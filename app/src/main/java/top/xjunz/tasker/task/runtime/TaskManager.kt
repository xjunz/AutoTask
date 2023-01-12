/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.runtime

import top.xjunz.tasker.engine.task.XTask
import java.util.*

/**
 * @author xjunz 2022/12/25
 */
abstract class TaskManager<TaskIdentifier, TaskCarrier> {

    protected val enabled: MutableList<XTask> = mutableListOf()

    protected abstract fun asTask(carrier: TaskCarrier): XTask

    protected abstract fun List<XTask>.indexOfTask(identifier: TaskIdentifier): Int

    fun getEnabledTasks(): List<XTask> {
        return enabled
    }

    open fun disableResidentTask(identifier: TaskIdentifier) {
        val index = enabled.indexOfTask(identifier)
        if (index >= 0) {
            enabled[index].halt()
            enabled.removeAt(index)
        }
    }

    open fun enableResidentTask(carrier: TaskCarrier) {
        val task = asTask(carrier)
        check(!enabled.contains(task)) {
            "Task [${task.title}] already enabled!"
        }
        enabled.add(task)
    }

    open fun updateResidentTask(previousChecksum: Long, updated: TaskCarrier) {
        val task = enabled.find {
            it.checksum == previousChecksum
        }
        if (task != null) {
            task.halt()
            Collections.replaceAll(enabled, task, asTask(updated))
        }
    }
}