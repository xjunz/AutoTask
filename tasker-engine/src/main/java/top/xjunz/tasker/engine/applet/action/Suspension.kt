/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.action

import android.util.ArrayMap
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.runtime.TaskRuntime
import java.lang.ref.WeakReference

/**
 * @author xjunz 2022/11/15
 */
class Suspension : SingleArgAction<Int>() {

    private val scopes = ArrayMap<Long, WeakReference<Job>>()

    override val defaultValue: Int = 0

    override suspend fun doAction(arg: Int?, runtime: TaskRuntime): AppletResult {
        check(arg != null)
        val fingerprint = runtime.fingerprint
        // Remove and cancel existent scope if there is one with an identical fingerprint
        scopes.remove(fingerprint)?.get()?.cancel("Suspension interrupted by peer!")
        coroutineScope {
            val job = launch(start = CoroutineStart.LAZY) {
                runtime.isSuspending = true
                delay(arg.toLong())
                runtime.isSuspending = false
                scopes.remove(fingerprint)
            }
            scopes[fingerprint] = WeakReference(job)
            job.join()
        }
        return AppletResult.EMPTY_SUCCESS
    }

}