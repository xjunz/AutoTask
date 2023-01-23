/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.action

import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/11/21
 */
class Processor<V, R>(
    override val valueType: Int,
    private inline val processor: (args: Array<Any?>, value: V?, runtime: TaskRuntime) -> R?
) : ReferenceAction<V>(valueType) {

    override suspend fun doWithArgs(
        args: Array<Any?>,
        value: V?,
        runtime: TaskRuntime
    ): AppletResult {
        val ret = processor(args, value, runtime)
        return if (ret != null) AppletResult.succeeded(ret) else AppletResult.FAILURE
    }

}

inline fun <reified Arg, reified V : Any> unaryArgProcessor(
    crossinline action: (Arg?, V?) -> V?
): Processor<V, V> {
    return Processor(Applet.judgeValueType<V>()) { args, v, _ ->
        action(args.single()?.casted(), v)
    }
}

inline fun <reified Arg1, reified Arg2, V, R> dualArgsProcessor(
    valueType: Int = Applet.VAL_TYPE_IRRELEVANT,
    crossinline action: (Arg1?, Arg2?, V?) -> R?
): Processor<V, R> {
    return Processor(valueType) { args, v, _ ->
        check(args.size == 2)
        action(args[0]?.casted(), args[1]?.casted(), v)
    }
}

