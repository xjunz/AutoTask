package top.xjunz.tasker.task

import top.xjunz.tasker.engine.task.AutomatorTask

/**
 * @author xjunz 2022/08/05
 */
object TaskManager {

    fun loadAllTasks() {

    }

    fun getAllTasks(): List<AutomatorTask> {
        TODO()
    }

    fun getActiveTasks(): List<AutomatorTask> {
        TODO()
    }

    fun requireTaskById(id: Int): AutomatorTask {
        return requireNotNull(getAllTasks().find { it.id == id }) { "Cannot find task with id: $id!" }
    }
}