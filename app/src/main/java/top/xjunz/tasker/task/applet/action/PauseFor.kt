/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.action

import top.xjunz.tasker.engine.applet.action.Action
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.task.applet.value.LongDuration

/**
 * @author xjunz 2023/08/17
 */
class PauseFor : Action<Long>(VAL_TYPE_LONG) {

    override suspend fun doAction(value: Long?, runtime: TaskRuntime): AppletResult {
        val duration = checkNotNull(value)
        runtime.attachingTask.pause(LongDuration.parse(duration).toMilliseconds())
        return AppletResult.EMPTY_SUCCESS
    }
}