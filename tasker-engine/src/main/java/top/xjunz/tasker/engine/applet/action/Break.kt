/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.action

import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2023/01/15
 */
class Break : ArgumentAction() {

    override suspend fun doAction(args: Array<Any?>, runtime: TaskRuntime): AppletResult {
        runtime.currentLoop?.shouldBreak = true
        return AppletResult.EMPTY_SUCCESS
    }
}