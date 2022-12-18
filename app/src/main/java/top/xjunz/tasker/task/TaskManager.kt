package top.xjunz.tasker.task

import top.xjunz.tasker.annotation.Crossed
import top.xjunz.tasker.annotation.LocalOnly
import top.xjunz.tasker.annotation.RemoteOnly
import top.xjunz.tasker.engine.runtime.IRemoteTaskManager
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.engine.task.dto.XTaskDTO
import top.xjunz.tasker.engine.task.dto.XTaskDTO.Serializer.toDTO
import top.xjunz.tasker.isInRemoteProcess
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import java.util.*

/**
 * @author xjunz 2022/12/17
 */
object TaskManager : IRemoteTaskManager.Stub() {

    var delegate: IRemoteTaskManager? = null

    private val allTasks = mutableSetOf<XTask>()

    private var enabledTasks: MutableList<XTask> = Collections.emptyList()

    @Crossed
    override fun enableResidentTask(id: Long) {
        delegate?.enableResidentTask(id)
        enabledTasks.add(allTasks.first { it.id == id })
    }

    @Crossed
    override fun disableResidentTask(id: Long) {
        delegate?.disableResidentTask(id)
        enabledTasks.removeIf { it.id == id }
    }

    @RemoteOnly
    override fun addNewEnabledResidentTask(dto: XTaskDTO) {
        check(isInRemoteProcess) {
            "The operation is not supported in the host process!"
        }
        val task = dto.toAutomatorTask(AppletOptionFactory())
        allTasks.add(task)
        enabledTasks.add(task)
    }

    @LocalOnly
    fun addNewEnabledResidentTask(task: XTask) {
        delegate?.addNewEnabledResidentTask(task.toDTO())
        allTasks.add(task)
        enabledTasks.add(task)
    }

    @Crossed
    fun getEnabledResidentTasks(): List<XTask> {
        if (enabledTasks == Collections.EMPTY_LIST) {
            enabledTasks = allTasks.filter {
                it.isEnabled
            }.toMutableList()
        }
        return enabledTasks
    }

}