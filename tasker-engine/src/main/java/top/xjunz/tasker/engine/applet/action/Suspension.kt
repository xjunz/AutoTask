/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.action

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.runtime.TaskRuntime
import java.util.*

/**
 * @author xjunz 2022/11/15
 */
class Suspension : Action<Int>(VAL_TYPE_INT) {

    private val scopes = WeakHashMap<Long, CoroutineScope>()

    override val defaultValue: Int = 1000

    override suspend fun doAction(value: Int?, runtime: TaskRuntime): AppletResult {
        check(value != null)
        val fingerprint = runtime.fingerprint
        // Remove and cancel existent scope if there is one with identical fingerprint
        scopes.remove(fingerprint)?.cancel()
        coroutineScope {
            scopes[fingerprint] = this
            runtime.isSuspending = true
            delay(value.toLong())
            runtime.isSuspending = false
            scopes.remove(fingerprint)
        }
        return AppletResult.EMPTY_SUCCESS
    }

}