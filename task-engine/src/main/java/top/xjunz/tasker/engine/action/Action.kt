package top.xjunz.tasker.engine.action

import top.xjunz.tasker.engine.AppletContext
import top.xjunz.tasker.engine.FlowRuntime
import top.xjunz.tasker.engine.base.Applet

/**
 * @author xjunz 2022/08/11
 */
abstract class Action : Applet() {

}

/**
 * Create an action that processes a value to another value of the same type.
 */
fun <V : Any> ProcessorAction(action: (V) -> V): Action {
    return object : Action() {
        override fun apply(context: AppletContext, runtime: FlowRuntime) {
            runtime.setTarget(action(runtime.getTarget()))
        }
    }
}