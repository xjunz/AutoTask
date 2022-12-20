package top.xjunz.tasker.task

import top.xjunz.tasker.annotation.LocalAndRemote
import top.xjunz.tasker.annotation.LocalOnly
import top.xjunz.tasker.annotation.RemoteOnly
import top.xjunz.tasker.engine.runtime.IRemoteTaskManager
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.engine.task.dto.XTaskDTO
import top.xjunz.tasker.engine.task.dto.XTaskDTO.Serializer.toDTO
import top.xjunz.tasker.isInRemoteProcess
import top.xjunz.tasker.task.applet.option.AppletOptionFactory

/**
 * @author xjunz 2022/12/17
 */
object TaskManager : IRemoteTaskManager.Stub() {

    var delegate: IRemoteTaskManager? = null

    private val allTasks = mutableSetOf<XTask>()

    private var enabledTasks = mutableListOf<XTask>()

    @RemoteOnly
    override fun enableResidentTask(id: Long) {
        enabledTasks.add(allTasks.first { it.checksum == id })
    }

    @LocalOnly
    fun enableResidentTask(task: XTask) {
        delegate?.enableResidentTask(task.checksum)
        enabledTasks.add(task)
    }

    @RemoteOnly
    override fun disableResidentTask(id: Long) {
        enabledTasks.removeIf { it.checksum == id }
    }

    @LocalOnly
    fun disableResidentTask(task: XTask) {
        delegate?.disableResidentTask(task.checksum)
        enabledTasks.remove(task)
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

    @LocalAndRemote
    fun getEnabledResidentTasks(): List<XTask> {
        return enabledTasks
    }

}