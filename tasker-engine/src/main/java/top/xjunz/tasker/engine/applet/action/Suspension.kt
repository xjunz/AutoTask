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
import java.lang.ref.WeakReference

/**
 * @author xjunz 2022/11/15
 */
class Suspension : Action<Int>(VAL_TYPE_INT) {

    private var suspendingScope: WeakReference<CoroutineScope>? = null

    override suspend fun doAction(value: Int?, runtime: TaskRuntime): AppletResult {
        check(value != null)
        suspendingScope?.get()?.cancel()
        coroutineScope {
            suspendingScope = WeakReference(this)
            runtime.isSuspending = true
            delay(value.toLong())
            runtime.isSuspending = false
        }
        return AppletResult.SUCCESS
    }

}