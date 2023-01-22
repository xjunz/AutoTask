/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.action

import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/11/15
 */
abstract class ReferenceAction<V>(valueType: Int) : Action<V>(valueType) {

    abstract suspend fun doWithArgs(
        args: Array<Any?>,
        value: V?,
        runtime: TaskRuntime
    ): AppletResult

    final override suspend fun doAction(value: V?, runtime: TaskRuntime): AppletResult {
        check(references.isNotEmpty()) {
            "Need references!"
        }
        return doWithArgs(runtime.getArguments(this), value?.casted(), runtime)
    }
}

class LambdaReferenceAction<V>(
    valueType: Int,
    private inline val action: suspend (args: Array<Any?>, value: V?, runtime: TaskRuntime) -> Boolean
) : ReferenceAction<V>(valueType) {
    override suspend fun doWithArgs(
        args: Array<Any?>,
        value: V?,
        runtime: TaskRuntime
    ): AppletResult {
        return if (action(args, value, runtime)) AppletResult.SUCCESS else AppletResult.FAILURE
    }
}

inline fun <reified Arg, reified V> singleArgAction(
    crossinline action: (Arg?, V?) -> Boolean
): ReferenceAction<V> {
    return LambdaReferenceAction(Applet.judgeValueType<V>()) { args, v, _ ->
        action(args.single()?.casted(), v)
    }
}

inline fun <reified ArgOrValue> unaryArgAction(
    crossinline action: (ArgOrValue) -> Boolean
): ReferenceAction<ArgOrValue> {
    return LambdaReferenceAction(Applet.judgeValueType<ArgOrValue>()) { args, v, _ ->
        action(requireNotNull(args.singleOrNull()?.casted() ?: v) {
            "Neither ref nor value is specified!"
        })
    }
}

inline fun <reified Arg1, reified Arg2, V> dualArgsAction(
    valueType: Int,
    crossinline action: (Arg1?, Arg2?, V?) -> Boolean
): ReferenceAction<V> {
    return LambdaReferenceAction(valueType) { args, v, _ ->
        check(args.size == 2)
        action(args[0]?.casted(), args[1]?.casted(), v)
    }
}