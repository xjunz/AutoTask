/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.runtime

import android.os.IBinder.DeathRecipient
import top.xjunz.tasker.engine.dto.toDTO
import top.xjunz.tasker.engine.task.TaskManager
import top.xjunz.tasker.engine.task.TaskSnapshot
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.ktx.whenAlive

/**
 * @author xjunz 2022/12/17
 */
object LocalTaskManager : TaskManager<XTask, XTask>() {

    var isInitialized = false

    private var peer: IRemoteTaskManager? = null

    private val peerDeathRecipient: DeathRecipient by lazy {
        DeathRecipient {
            peer?.asBinder()?.unlinkToDeath(peerDeathRecipient, 0)
            peer = null
        }
    }

    val XTask.isEnabled: Boolean get() = isResident && tasks.contains(this)

    fun setRemotePeer(peer: IRemoteTaskManager) {
        this.peer?.asBinder()?.linkToDeath(peerDeathRecipient, 0)
        this.peer = peer
        peer.asBinder().linkToDeath(peerDeathRecipient, 0)
    }

    override fun removeTask(identifier: XTask) {
        super.removeTask(identifier)
        peer?.whenAlive {
            it.removeTask(identifier.checksum)
        }
    }

    override fun enableResidentTask(carrier: XTask) {
        super.enableResidentTask(carrier)
        peer?.whenAlive {
            it.enableResidentTask(carrier.toDTO())
        }
    }

    override fun addOneshotTaskIfAbsent(carrier: XTask) {
        super.addOneshotTaskIfAbsent(carrier)
        peer?.whenAlive {
            it.addOneshotTaskIfAbsent(carrier.toDTO())
        }
    }

    override fun updateTask(previousChecksum: Long, updated: XTask) {
        super.updateTask(previousChecksum, updated)
        peer?.whenAlive {
            it.updateTask(previousChecksum, updated.toDTO())
        }
    }

    override fun getSnapshotCount(id: XTask): Int {
        val remote = peer
        if (remote != null) {
            return remote.getSnapshotCount(id.checksum)
        }
        return super.getSnapshotCount(id)
    }

    override fun getAllSnapshots(id: XTask): Array<TaskSnapshot> {
        val remote = peer
        if (remote != null) {
            return remote.getAllSnapshots(id.checksum)
        }
        return super.getAllSnapshots(id)
    }

    override fun clearSnapshots(id: XTask) {
        super.clearSnapshots(id)
        peer?.whenAlive {
            it.clearSnapshots(id.checksum)
        }
    }

    override fun asTask(carrier: XTask): XTask {
        return carrier
    }

    override fun List<XTask>.indexOfTask(identifier: XTask): Int {
        return indexOf(identifier)
    }

    override fun clearLog(checksum: Long, snapshotId: String) {
        super.clearLog(checksum, snapshotId)
        peer?.whenAlive {
            it.clearLog(checksum, snapshotId)
        }
    }

    override val XTask.identifier: XTask get() = this

}