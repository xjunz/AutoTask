/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.action

import top.xjunz.tasker.engine.applet.action.SingleArgAction
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.task.applet.value.LongDuration

/**
 * @author xjunz 2023/08/17
 */
class PauseFor : SingleArgAction<Long>() {

    override suspend fun doAction(arg: Long?, runtime: TaskRuntime): AppletResult {
        val duration = checkNotNull(arg)
        runtime.attachingTask.pause(LongDuration.parse(duration).toMilliseconds())
        return AppletResult.EMPTY_SUCCESS
    }
}