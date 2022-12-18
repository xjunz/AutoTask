package top.xjunz.tasker.engine.applet.action

import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.dto.AppletValues
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/08/11
 */
abstract class Action : Applet()

inline fun <V> newAction(
    valueType: Int, crossinline block: (V?, TaskRuntime) -> Boolean
): Action {
    return object : Action() {

        override val valueType: Int = valueType

        override suspend fun apply(runtime: TaskRuntime) {
            runtime.isSuccessful = block.invoke(value?.casted(), runtime)
        }
    }
}

inline fun simpleAction(crossinline block: (TaskRuntime) -> Boolean): Action {
    return newAction<Any>(AppletValues.VAL_TYPE_IRRELEVANT) { _, runtime ->
        block(runtime)
    }
}

inline fun pureAction(crossinline block: (TaskRuntime) -> Unit): Action {
    return simpleAction {
        block(it)
        true
    }
}
