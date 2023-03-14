/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.base

import top.xjunz.tasker.engine.applet.action.Action
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2023/01/15
 */
class Break : Action<Any>(VAL_TYPE_IRRELEVANT) {

    override suspend fun doAction(value: Any?, runtime: TaskRuntime): AppletResult {
        runtime.currentLoop?.shouldBreak = true
        return AppletResult.EMPTY_SUCCESS
    }
}