/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.action

import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/11/21
 */
abstract class Processor<Result> : ArgumentAction() {

    abstract suspend fun process(args: Array<Any?>, runtime: TaskRuntime): Result?

    final override suspend fun doAction(args: Array<Any?>, runtime: TaskRuntime): AppletResult {
        val ret = process(args, runtime)
        return if (ret != null) AppletResult.succeeded(ret) else AppletResult.EMPTY_FAILURE
    }

}

fun <Result> createProcessor(block: suspend (arg:Array<Any?>,runtime: TaskRuntime) -> Result?): Processor<Result> {
    return object : Processor<Result>() {
        override suspend fun process(args: Array<Any?>, runtime: TaskRuntime): Result? {
            return block(args, runtime)
        }
    }
}