/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.action

import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/08/11
 */
inline fun createAction(crossinline block: suspend (args: Array<Any?>, runtime: TaskRuntime) -> Boolean): Action {
    return object : ArgumentAction() {
        override suspend fun doAction(args: Array<Any?>, runtime: TaskRuntime): AppletResult {
            return AppletResult.emptyResult(block(args, runtime))
        }
    }
}

/**
 * Always expect the action returns `true`.
 */
fun optimisticAction(block: suspend (args: Array<Any?>, runtime: TaskRuntime) -> Unit): Action {
    return createAction { args, runtime ->
        block(args, runtime)
        true
    }
}

fun emptyArgAction(block: suspend (TaskRuntime) -> Boolean): Action {
    return createAction { _, runtime ->
        block(runtime)
    }
}

fun emptyArgOptimisticAction(block: suspend (TaskRuntime) -> Unit): Action {
    return emptyArgAction {
        block(it)
        true
    }
}

fun <Arg1, Arg2> doubleArgsAction(block: suspend (Arg1?, Arg2?, runtime: TaskRuntime) -> Boolean): Action {
    return createAction { args, runtime ->
        block(args[0]?.casted(), args[1]?.casted(), runtime)
    }
}