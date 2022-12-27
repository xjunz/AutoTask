/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.runtime

import android.os.IBinder.DeathRecipient
import android.util.ArraySet
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.engine.task.dto.XTaskDTO.Serializer.toDTO
import top.xjunz.tasker.ktx.whenAlive

/**
 * @author xjunz 2022/12/17
 */
object LocalTaskManager : TaskManager<XTask, XTask> {

    private var initialized = false

    private var remotePeer: IRemoteTaskManager? = null

    private val peerDeathRecipient: DeathRecipient by lazy {
        DeathRecipient {
            remotePeer?.asBinder()?.unlinkToDeath(peerDeathRecipient, 0)
            remotePeer = null
        }
    }

    fun setRemotePeer(peer: IRemoteTaskManager) {
        remotePeer?.asBinder()?.unlinkToDeath(peerDeathRecipient, 0)
        remotePeer = peer
        peer.asBinder().linkToDeath(peerDeathRecipient, 0)
    }

    override val enabledTasks = ArraySet<XTask>()

    override fun ArraySet<XTask>.findTask(identifier: XTask): XTask? {
        return if (contains(identifier)) identifier else null
    }

    override fun XTask.asTask(): XTask {
        return this
    }

    fun removeRemoteCache(checksum: Long) {
        remotePeer?.whenAlive {
            it.removeCachedTask(checksum)
        }
    }

    override fun removeResidentTask(identifier: XTask) {
        super.removeResidentTask(identifier)
        remotePeer?.whenAlive {
            it.removeResidentTask(identifier.checksum)
        }
    }

    override fun initialize(carriers: Collection<XTask>) {
        super.initialize(carriers)
        initialized = true
    }

    fun addResidentTask(task: XTask) {
        check(enabledTasks.add(task))
        remotePeer?.whenAlive {
            if (it.isTaskCached(task.checksum)) {
                it.enableCachedResidentTask(task.checksum)
            } else {
                it.enableNewResidentTask(task.toDTO())
            }
        }
    }

    override fun isInitialized(): Boolean {
        return initialized
    }

}