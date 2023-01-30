/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.criterion

import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * The target is value or the argument.
 *
 * @author xjunz 2023/01/27
 */
class ArgumentCriterion<T : Any, V : Any, Arg : Any>(
    override val valueType: Int,
    private inline val mapper: (Arg) -> V,
    private inline val matcher: (T, V) -> Boolean
) : Criterion<T, V>() {

    override fun getDefaultValue(runtime: TaskRuntime): V {
        val value = mapper(runtime.getArgument(this, if (isScoped) 0 else 1)!!.casted())
        runtime.updateFingerprint(value)
        return value
    }

    override fun matchTarget(target: T, value: V): Boolean {
        return matcher(target, value)
    }

}