package top.xjunz.tasker.engine.applet.action

import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.applet.serialization.AppletValues
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/11/21
 */
abstract class ProcessorAction<V, R>(override val valueType: Int) : ReferenceAction<V>(valueType) {

    final override fun doAction(args: Array<Any?>, value: V?, runtime: TaskRuntime): Boolean {
        val ret = doProcess(args, value, runtime)
        if (ret != null) {
            referred.forEach { (which, refid) ->
                runtime.registerResult(refid, getReferredValue(which, ret))
            }
        }
        return ret != null
    }

    abstract fun doProcess(args: Array<Any?>, value: V?, runtime: TaskRuntime): R?

}

inline fun <V, R> processorAction(
    valueType: Int,
    crossinline action: (args: Array<Any?>, value: V?, runtime: TaskRuntime) -> R?
): ProcessorAction<V, R> {
    return object : ProcessorAction<V, R>(valueType) {
        override fun doProcess(args: Array<Any?>, value: V?, runtime: TaskRuntime): R? {
            return action(args, value, runtime)
        }
    }
}

inline fun <reified Arg, V, R> singleArgProcessor(
    valueType: Int = AppletValues.VAL_TYPE_IRRELEVANT,
    crossinline action: (Arg?, V?) -> R?
): ProcessorAction<V, R> {
    return processorAction(valueType) { args, v, _ ->
        action(args.single()?.casted(), v)
    }
}

inline fun <reified Arg, V> unaryArgProcessor(
    valueType: Int = AppletValues.VAL_TYPE_IRRELEVANT,
    crossinline action: (Arg?, V?) -> V?
): ProcessorAction<V, V> {
    return processorAction(valueType) { args, v, _ ->
        action(args.single()?.casted(), v)
    }
}

inline fun <reified Arg1, reified Arg2, V, R> dualArgsProcessor(
    valueType: Int = AppletValues.VAL_TYPE_IRRELEVANT,
    crossinline action: (Arg1?, Arg2?, V?) -> R?
): ProcessorAction<V, R> {
    return processorAction(valueType) { args, v, _ ->
        check(args.size == 2)
        action(args[0]?.casted(), args[1]?.casted(), v)
    }
}

