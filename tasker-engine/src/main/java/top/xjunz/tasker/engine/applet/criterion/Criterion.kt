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
abstract class Criterion<T, V> : Applet() {

    protected val isScoped by lazy {
        var p = parent
        while (p is ContainerFlow) {
            p = p?.parent
        }
        p is ScopeFlow<*>
    }

    /**
     * Get the match target.
     *
     * If this applet [isScoped], returns the target from its scope. Otherwise, by default, we expect
     * the match target to be the first reference. If the match target is not from arguments, this
     * returns a [Unit]. For example, as for a criterion checking whether the device is charging, the
     * match target (charging status) is obtained from a system API directly.
     *
     */
    private fun getMatchTarget(runtime: TaskRuntime): T {
        if (isScoped) return runtime.getTarget()
        val target = runtime.getReferenceArgument(this, 0)?.casted<T>()
        if (target != null) {
            return target
        }
        return Unit.casted()
    }

    /**
     * Get the match value.
     *
     * We expect the first value to be the match value. If the first value is null, then this returns
     * the default value (from [getDefaultMatchValue]).
     */
    private fun getMatchValue(runtime: TaskRuntime): V {
        return values.values.firstOrNull()?.casted() ?: getDefaultMatchValue(runtime)
    }

    /**
     * The default value when [getMatchValue] is null.
     */
    open fun getDefaultMatchValue(runtime: TaskRuntime): V {
        throw NotImplementedError("Default value is not defined while value is null!")
    }

    final override suspend fun apply(runtime: TaskRuntime): AppletResult {
        val value = getMatchValue(runtime)
        val target = getMatchTarget(runtime)
        val raw = matchTarget(target, value)
        runtime.updateFingerprint(raw.actual)
        val result: AppletResult
        if (isInverted) {
            result = if (raw.isSuccessful) {
                AppletResult.failed(raw.actual)
            } else {
                AppletResult.EMPTY_SUCCESS
            }
            raw.recycle()
        } else {
            // Clear unnecessary actual value
            if (raw.isSuccessful) {
                raw.actual = null
            }
            result = raw
        }
        return result
    }

    /**
     * Check whether the [target] and [value] are matched.
     */
    protected abstract fun matchTarget(target: T, value: V): AppletResult
}