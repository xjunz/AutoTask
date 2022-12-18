package top.xjunz.tasker.engine.applet.action

import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.applet.dto.AppletValues
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/11/15
 */
abstract class ReferenceAction<V>(override val valueType: Int) : Action() {

    override suspend fun apply(runtime: TaskRuntime) {
        check(references.isNotEmpty()) {
            "Need references!"
        }
        val args = Array(references.size) {
            runtime.getResultByRefid(references[it])
        }
        runtime.isSuccessful = doAction(args, value?.casted(), runtime)
    }

    abstract fun doAction(args: Array<Any?>, value: V?, runtime: TaskRuntime): Boolean
}

inline fun <V> referenceAction(
    valueType: Int,
    crossinline action: (args: Array<Any?>, value: V?, runtime: TaskRuntime) -> Boolean
): ReferenceAction<V> {
    return object : ReferenceAction<V>(valueType) {
        override fun doAction(args: Array<Any?>, value: V?, runtime: TaskRuntime): Boolean {
            return action(args, value, runtime)
        }
    }
}

inline fun <reified Arg, V> singleArgAction(
    valueType: Int = AppletValues.VAL_TYPE_IRRELEVANT,
    crossinline action: (Arg?, V?) -> Boolean
): ReferenceAction<V> {
    return referenceAction(valueType) { args, v, _ ->
        action(args.single()?.casted(), v)
    }
}

inline fun <reified Arg1, reified Arg2, V> dualArgsAction(
    valueType: Int = AppletValues.VAL_TYPE_IRRELEVANT,
    crossinline action: (Arg1?, Arg2?, V?) -> Boolean
): ReferenceAction<V> {
    return referenceAction(valueType) { args, v, _ ->
        check(args.size == 2)
        action(args[0]?.casted(), args[1]?.casted(), v)
    }
}