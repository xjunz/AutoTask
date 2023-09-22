/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.action

import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/11/21
 */
class Processor<V, R>(
    override val valueType: Int,
    private inline val processor: (args: Array<Any?>, value: V?, runtime: TaskRuntime) -> R?
) : ReferenceAction<V>(valueType) {

    companion object {

        inline fun <Arg, reified V : Any> unaryArgProcessor(
            crossinline action: (Arg?, V?) -> V?
        ): Processor<V, V> {
            return Processor(judgeValueType<V>()) { args, v, _ ->
                action(args.single()?.casted(), v)
            }
        }
    }

    override suspend fun doAction(
        refs: Array<Any?>,
        value: V?,
        runtime: TaskRuntime
    ): AppletResult {
        val ret = processor(refs, value, runtime)
        return if (ret != null) AppletResult.succeeded(ret) else AppletResult.EMPTY_FAILURE
    }

}
