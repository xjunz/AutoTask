/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.criterion

import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.applet.base.ScopeFlow
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * The base criterion applet abstraction.
 *
 * @param T the target to be matched
 * @param V the value used to match a target
 *
 * @author xjunz 2022/08/06
 */
abstract class Criterion<T : Any, V : Any> : Applet() {

    protected inline val isScoped get() = parent is ScopeFlow<*>

    protected open fun T.getActualValue(): Any? {
        return this
    }

    private fun getTarget(runtime: TaskRuntime): T {
        if (isScoped) {
            return runtime.getTarget()
        }
        return runtime.getArgument(this, 0)!!.casted()
    }

    /**
     * The default value when [value] is null. The default implementation is the second argument.
     */
    open fun getDefaultValue(runtime: TaskRuntime): V {
        throw NotImplementedError("Default value is not defined while value is null!")
    }

    final override suspend fun apply(runtime: TaskRuntime): AppletResult {
        val expected = value?.casted() ?: getDefaultValue(runtime)
        val target = getTarget(runtime)
        return if (isInverted != matchTarget(target, expected)) {
            AppletResult.SUCCESS
        } else {
            AppletResult.failed(expected, target.getActualValue())
        }
    }

    /**
     * Check whether the [target] and [value] are matched.
     */
    protected abstract fun matchTarget(target: T, value: V): Boolean
}
