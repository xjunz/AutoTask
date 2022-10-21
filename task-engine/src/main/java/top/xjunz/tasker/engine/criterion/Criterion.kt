package top.xjunz.tasker.engine.criterion

import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.AppletContext
import top.xjunz.tasker.engine.FlowRuntime
import top.xjunz.tasker.engine.base.Applet

/**
 * The base criterion applet abstraction.
 *
 * @param T the target to be matched
 * @param V the value used to match a target
 *
 * @author xjunz 2022/08/06
 */
abstract class Criterion<T : Any, V : Any> : Applet() {

    override fun apply(context: AppletContext, runtime: FlowRuntime) {
        runtime.isSuccessful = isInverted != matchTarget(runtime.getTarget(), value.casted())
    }

    /**
     * Check whether the [target] and [value] are matched.
     */
    protected abstract fun matchTarget(target: T, value: V): Boolean
}