package top.xjunz.tasker.engine.task

import kotlinx.coroutines.CoroutineScope

/**
 * @author xjunz 2022/12/04
 */
interface TaskScheduler : CoroutineScope {

    fun scheduleTasks()

    fun destroy()
}