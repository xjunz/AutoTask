/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.action

import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.dto.AppletValues
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/08/11
 */
abstract class Action<V>(override val valueType: Int) : Applet() {

    abstract suspend fun doAction(value: V?, runtime: TaskRuntime): Boolean

    final override suspend fun apply(runtime: TaskRuntime) {
        runtime.isSuccessful = doAction(value?.casted(), runtime)
    }
}

open class LambdaAction<V>(
    valueType: Int,
    private inline val action: suspend (V?, TaskRuntime) -> Boolean
) : Action<V>(valueType) {
    override suspend fun doAction(value: V?, runtime: TaskRuntime): Boolean {
        return action(value, runtime)
    }
}

inline fun simpleAction(crossinline block: (TaskRuntime) -> Boolean): Action<*> {
    return LambdaAction<Any>(AppletValues.VAL_TYPE_IRRELEVANT) { _, runtime ->
        block(runtime)
    }
}

inline fun pureAction(crossinline block: (TaskRuntime) -> Unit): Action<*> {
    return simpleAction {
        block(it)
        true
    }
}
