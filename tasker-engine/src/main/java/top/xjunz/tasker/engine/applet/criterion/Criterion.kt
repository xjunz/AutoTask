/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.criterion

import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.applet.base.ContainerFlow
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

    protected open fun T.getActualValue(): Any? {
        return this
    }

    protected val isScoped by lazy {
        var p = parent
        while (p is ContainerFlow) {
            p = p?.parent
        }
        p is ScopeFlow<*>
    }

    private fun getTarget(runtime: TaskRuntime): T {
        if (isScoped) {
            return runtime.getTarget()
        }
        val arg = runtime.getArgument(this, 0)?.casted<T>()
        if (arg != null) {
            runtime.updateFingerprint(arg)
            return arg
        }
        return Unit.casted()
    }

    /**
     * The default value when [value] is null.
     */
    open fun getDefaultValue(runtime: TaskRuntime): V {
        throw NotImplementedError("Default value is not defined while value is null!")
    }

    final override suspend fun apply(runtime: TaskRuntime): AppletResult {
        val expected = value?.casted() ?: getDefaultValue(runtime)
        val actual = getTarget(runtime)
        return if (isInverted != matchTarget(actual, expected)) {
            AppletResult.SUCCESS
        } else if (actual === Unit) {
            AppletResult.FAILURE
        } else {
            AppletResult.failed(actual.getActualValue())
        }
    }

    /**
     * Check whether the [target] and [value] are matched.
     */
    protected abstract fun matchTarget(target: T, value: V): Boolean
}
