package top.xjunz.tasker.task.runtime

import android.util.ArraySet
import top.xjunz.tasker.engine.task.XTask

/**
 * @author xjunz 2022/12/25
 */
interface TaskManager<TaskIdentifier, TaskCarrier> {

    val enabledTasks: ArraySet<XTask>

    fun ArraySet<XTask>.findTask(identifier: TaskIdentifier): XTask?

    fun TaskCarrier.asTask(): XTask

    fun isInitialized(): Boolean

    fun removeResidentTask(identifier: TaskIdentifier) {
        enabledTasks.remove(checkNotNull(enabledTasks.findTask(identifier)))
    }

    fun initialize(carriers: Collection<TaskCarrier>) {
        check(!isInitialized())
        carriers.mapTo(enabledTasks) {
            it.asTask()
        }
    }

}