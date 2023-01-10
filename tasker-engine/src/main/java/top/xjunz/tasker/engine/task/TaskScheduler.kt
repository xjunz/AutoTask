/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.task

import kotlinx.coroutines.CoroutineScope
import top.xjunz.tasker.engine.runtime.ComponentInfoWrapper

/**
 * @author xjunz 2022/12/04
 */
interface TaskScheduler : CoroutineScope {

    fun getCurrentComponentInfo(): ComponentInfoWrapper

    fun scheduleTasks()

    fun destroy()
}