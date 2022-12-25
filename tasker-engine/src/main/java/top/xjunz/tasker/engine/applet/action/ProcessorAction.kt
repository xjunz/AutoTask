package top.xjunz.tasker.engine.applet.action

import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.applet.dto.AppletValues
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/11/21
 */
class ProcessorAction<V, R>(
    override val valueType: Int,
    private inline val processor: (args: Array<Any?>, value: V?, runtime: TaskRuntime) -> R?
) : ReferenceAction<V>(valueType) {

    override suspend fun doActionWithReferences(
        args: Array<Any?>,
        value: V?,
        runtime: TaskRuntime
    ): Boolean {
        val ret = processor(args, value, runtime)
        if (ret != null) {
            refids.forEach { (which, refid) ->
                runtime.registerResult(refid, getReferredValue(which, ret))
            }
        }
        return ret != null
    }

}

inline fun <reified Arg, V> unaryArgProcessor(
    valueType: Int = AppletValues.VAL_TYPE_IRRELEVANT,
    crossinline action: (Arg?, V?) -> V?
): ProcessorAction<V, V> {
    return ProcessorAction(valueType) { args, v, _ ->
        action(args.single()?.casted(), v)
    }
}

inline fun <reified Arg1, reified Arg2, V, R> dualArgsProcessor(
    valueType: Int = AppletValues.VAL_TYPE_IRRELEVANT,
    crossinline action: (Arg1?, Arg2?, V?) -> R?
): ProcessorAction<V, R> {
    return ProcessorAction(valueType) { args, v, _ ->
        check(args.size == 2)
        action(args[0]?.casted(), args[1]?.casted(), v)
    }
}

