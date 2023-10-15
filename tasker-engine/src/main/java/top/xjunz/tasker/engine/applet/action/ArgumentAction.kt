/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.action

import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2023/09/26
 */
abstract class ArgumentAction : Action() {

    override suspend fun apply(runtime: TaskRuntime): AppletResult {
        val args = Array(values.size + references.size) {
            getArgument(it, runtime)
        }
        return doAction(args, runtime)
    }

    abstract suspend fun doAction(args: Array<Any?>, runtime: TaskRuntime): AppletResult
}