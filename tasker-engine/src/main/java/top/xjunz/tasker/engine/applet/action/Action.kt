package top.xjunz.tasker.engine.applet.action

import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.AutomatorTask
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.serialization.AppletValues
import top.xjunz.tasker.engine.runtime.FlowRuntime

/**
 * @author xjunz 2022/08/11
 */
abstract class Action : Applet()

inline fun <V> newAction(
    valueType: Int, crossinline block: (V?, FlowRuntime) -> Boolean
): Action {
    return object : Action() {

        override val valueType: Int = valueType

        override fun apply(task: AutomatorTask, runtime: FlowRuntime) {
            runtime.isSuccessful = block.invoke(value?.casted(), runtime)
        }
    }
}

inline fun simpleAction(crossinline block: (FlowRuntime) -> Boolean): Action {
    return newAction<Any>(AppletValues.VAL_TYPE_IRRELEVANT) { _, runtime ->
        block(runtime)
    }
}

inline fun pureAction(crossinline block: (FlowRuntime) -> Unit): Action {
    return simpleAction {
        block(it)
        true
    }
}
