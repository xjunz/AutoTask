/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.action

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import top.xjunz.tasker.engine.runtime.TaskRuntime
import java.lang.ref.WeakReference

/**
 * @author xjunz 2022/11/15
 */
class DelayAction : Action<Int>(VAL_TYPE_INT) {

    private var suspendingScope: WeakReference<CoroutineScope>? = null

    override suspend fun doAction(value: Int?, runtime: TaskRuntime): Boolean {
        suspendingScope?.get()?.cancel()
        coroutineScope {
            suspendingScope = WeakReference(this)
            delay(value!!.toLong())
        }
        return true
    }

}