/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.runtime

import top.xjunz.tasker.annotation.Privileged
import top.xjunz.tasker.engine.dto.XTaskDTO
import top.xjunz.tasker.engine.task.TaskManager
import top.xjunz.tasker.engine.task.TaskSnapshot
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.task.applet.option.AppletOptionFactory

/**
 * @author xjunz 2022/12/25
 */
@Privileged
object PrivilegedTaskManager : TaskManager<Long, XTaskDTO>() {

    object Delegate : IRemoteTaskManager.Stub() {

        private var initialized = false

        override fun initialize(carriers: MutableList<XTaskDTO>) {
            carriers.forEach {
                enableResidentTask(it)
            }
            initialized = true
        }

        override fun isInitialized(): Boolean {
            return initialized
        }

        override fun updateTask(previous: Long, updated: XTaskDTO) {
            PrivilegedTaskManager.updateTask(previous, updated)
        }

        override fun removeTask(identifier: Long) {
            PrivilegedTaskManager.removeTask(identifier)
        }

        override fun enableResidentTask(carrier: XTaskDTO) {
            PrivilegedTaskManager.enableResidentTask(carrier)
        }

        override fun addOneshotTaskIfAbsent(carrier: XTaskDTO) {
            PrivilegedTaskManager.addOneshotTaskIfAbsent(carrier)
        }

        override fun getSnapshotCount(identifier: Long): Int {
            return PrivilegedTaskManager.getSnapshotCount(identifier)
        }

        override fun clearSnapshots(identifier: Long) {
            PrivilegedTaskManager.clearSnapshots(identifier)
        }

        override fun clearLog(checksum: Long, snapshotId: String) {
            PrivilegedTaskManager.clearLog(checksum, snapshotId)
        }

        override fun getAllSnapshots(identifier: Long): Array<TaskSnapshot> {
            return PrivilegedTaskManager.getAllSnapshots(identifier)
        }

    }

    override fun asTask(carrier: XTaskDTO): XTask {
        return carrier.toXTask(AppletOptionFactory)
    }

    override fun List<XTask>.indexOfTask(identifier: Long): Int {
        return indexOfFirst {
            it.checksum == identifier
        }
    }

    override val XTaskDTO.identifier: Long get() = metadata.checksum

}