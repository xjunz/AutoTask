/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.action

import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2023/09/26
 */
abstract class VarRefAction<V> : Action() {

    final override suspend fun apply(runtime: TaskRuntime): AppletResult {
        val refs = arrayOfNulls<Any?>(references.size)
        references.keys.forEachIndexed { index, key ->
            refs[index] = runtime.getReferenceArgument(this, key)
        }
        return doAction(values.getValue(0).casted(), refs, runtime)
    }

    abstract fun doAction(value: V, refs: Array<Any?>, runtime: TaskRuntime): AppletResult
}

fun <V> optimisticVarRefAction(block: (value: V, refs: Array<Any?>, runtime: TaskRuntime) -> Unit): Action {
    return object : VarRefAction<V>() {
        override fun doAction(value: V, refs: Array<Any?>, runtime: TaskRuntime): AppletResult {
            block(value, refs, runtime)
            return AppletResult.EMPTY_SUCCESS
        }
    }
}