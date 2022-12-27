/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.action

import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/11/15
 */
abstract class ReferenceAction<V>(valueType: Int) : Action<V>(valueType) {

    abstract suspend fun doActionWithReferences(
        args: Array<Any?>,
        value: V?,
        runtime: TaskRuntime
    ): Boolean

    final override suspend fun doAction(value: V?, runtime: TaskRuntime): Boolean {
        check(references.isNotEmpty()) {
            "Need references!"
        }
        val args = Array(references.size) {
            runtime.getResultByRefid(references[it])
        }
        return doActionWithReferences(args, value?.casted(), runtime)
    }
}

class LambdaReferenceAction<V>(
    valueType: Int,
    private inline val action: suspend (args: Array<Any?>, value: V?, runtime: TaskRuntime) -> Boolean
) : ReferenceAction<V>(valueType) {
    override suspend fun doActionWithReferences(
        args: Array<Any?>,
        value: V?,
        runtime: TaskRuntime
    ): Boolean {
        return action(args, value, runtime)
    }
}

inline fun <reified Arg, V> singleArgAction(
    valueType: Int,
    crossinline action: (Arg?, V?) -> Boolean
): ReferenceAction<V> {
    return LambdaReferenceAction(valueType) { args, v, _ ->
        action(args.single()?.casted(), v)
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