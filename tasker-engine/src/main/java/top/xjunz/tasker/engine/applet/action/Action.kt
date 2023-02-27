/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.action

import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/08/11
 */
abstract class Action<V>(override val valueType: Int) : Applet() {

    abstract suspend fun doAction(value: V?, runtime: TaskRuntime): AppletResult

    override val supportsAnywayRelation: Boolean = true

    final override suspend fun apply(runtime: TaskRuntime): AppletResult {
        return doAction(value?.casted(), runtime)
    }

}

class LambdaAction<V>(
    valueType: Int,
    private inline val action: suspend (V?, TaskRuntime) -> Boolean
) : Action<V>(valueType) {
    override suspend fun doAction(value: V?, runtime: TaskRuntime): AppletResult {
        return AppletResult.emptyResult(action(value, runtime))
    }
}

inline fun <reified V> singleValueAction(crossinline block: suspend (V) -> Boolean): Action<*> {
    return LambdaAction<V>(Applet.judgeValueType<V>()) { v, _ ->
        check(v != null) {
            "Value is not defined!"
        }
        block(v)
    }
}

inline fun simpleAction(crossinline block: (TaskRuntime) -> Boolean): Action<*> {
    return LambdaAction<Any>(Applet.VAL_TYPE_IRRELEVANT) { _, runtime ->
        block(runtime)
    }
}

inline fun pureAction(crossinline block: (TaskRuntime) -> Unit): Action<*> {
    return simpleAction {
        block(it)
        true
    }
}
