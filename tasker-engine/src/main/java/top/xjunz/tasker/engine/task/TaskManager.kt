/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.task

import java.util.*

/**
 * @author xjunz 2022/12/25
 */
abstract class TaskManager<TaskIdentifier, TaskCarrier> {

    protected val tasks: MutableList<XTask> = mutableListOf()

    protected abstract fun asTask(carrier: TaskCarrier): XTask

    protected abstract val TaskCarrier.identifier: TaskIdentifier

    protected abstract fun List<XTask>.indexOfTask(identifier: TaskIdentifier): Int

    fun getEnabledResidentTasks(): List<XTask> {
        return tasks.filter {
            it.isResident
        }
    }

    open fun disableResidentTask(identifier: TaskIdentifier) {
        val index = tasks.indexOfTask(identifier)
        if (index >= 0) {
            tasks[index].halt()
            tasks.removeAt(index)
        }
    }

    open fun enableResidentTask(carrier: TaskCarrier) {
        val task = asTask(carrier)
        check(!tasks.contains(task)) {
            "Task [${task.title}] already enabled!"
        }
        tasks.add(task)
    }

    open fun addOneshotTaskIfAbsent(carrier: TaskCarrier) {
        if (tasks.indexOfTask(carrier.identifier) < 0) {
            tasks.add(asTask(carrier))
        }
    }

    open fun updateTask(previousChecksum: Long, updated: TaskCarrier) {
        val task = tasks.find {
            it.checksum == previousChecksum
        }
        if (task != null) {
            task.halt()
            Collections.replaceAll(tasks, task, asTask(updated))
            task.snapshots.clear()
        }
    }

    open fun clearSnapshots(id: TaskIdentifier) {
        requireTask(id).snapshots.clear()
    }

    private fun findTask(id: TaskIdentifier): XTask? {
        return tasks.getOrNull(tasks.indexOfTask(id))
    }

    fun requireTask(id: TaskIdentifier): XTask {
        return tasks[tasks.indexOfTask(id)]
    }

    open fun getSnapshotCount(id: TaskIdentifier): Int {
        return findTask(id)?.snapshots?.size ?: 0
    }

    open fun getAllSnapshots(id: TaskIdentifier): Array<TaskSnapshot> {
        return requireTask(id).snapshots.toTypedArray()
    }

    fun clearAllSnapshots() {
        tasks.forEach {
            it.snapshots.clear()
        }
    }
}