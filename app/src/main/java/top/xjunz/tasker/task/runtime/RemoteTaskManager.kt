/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.runtime

import android.util.ArraySet
import top.xjunz.tasker.annotation.Privileged
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.engine.task.dto.XTaskDTO
import top.xjunz.tasker.task.applet.option.AppletOptionFactory

/**
 * @author xjunz 2022/12/25
 */
@Privileged
object RemoteTaskManager : IRemoteTaskManager.Stub(), TaskManager<Long, XTaskDTO> {

    private var initialized: Boolean = false

    private val cachedTasks = ArraySet<XTask>()

    override val enabledTasks = ArraySet<XTask>()

    override fun initialize(dtos: List<XTaskDTO>) {
        super.initialize(dtos)
        cachedTasks.addAll(enabledTasks)
        initialized = true
    }

    override fun isInitialized(): Boolean {
        return initialized
    }

    override fun enableNewResidentTask(carrier: XTaskDTO) {
        val task = carrier.asTask()
        check(cachedTasks.add(task))
        check(enabledTasks.add(task))
    }

    override fun enableCachedResidentTask(identifier: Long) {
        check(enabledTasks.add(checkNotNull(cachedTasks.findTask(identifier))))
    }

    override fun removeCachedTask(checksum: Long) {
        check(cachedTasks.removeIf { it.checksum == checksum })
    }

    fun drainCache() {
        cachedTasks.removeIf {
            !enabledTasks.contains(it)
        }
    }

    override fun removeResidentTask(identifier: Long) {
        super.removeResidentTask(identifier)
    }

    override fun isTaskCached(checksum: Long): Boolean {
        return cachedTasks.findTask(checksum) != null
    }

    override fun ArraySet<XTask>.findTask(identifier: Long): XTask? {
        return find { it.checksum == identifier }
    }

    override fun XTaskDTO.asTask(): XTask {
        return toXTask(AppletOptionFactory)
    }


}