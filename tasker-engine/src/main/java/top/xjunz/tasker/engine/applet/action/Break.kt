/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.action

import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2023/01/15
 */
class Break : Action<Repeat>(VAL_TYPE_IRRELEVANT) {

    override suspend fun doAction(value: Repeat?, runtime: TaskRuntime): AppletResult {
        value?.shouldBreak = true
        return AppletResult.SUCCESS
    }
}