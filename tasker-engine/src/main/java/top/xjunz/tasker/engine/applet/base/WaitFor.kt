/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.base

import android.os.SystemClock
import kotlinx.coroutines.*
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.runtime.TaskRuntime
import java.lang.ref.WeakReference

/**
 * Wait for a certain event to occur.
 *
 * @author xjunz 2023/02/11
 */
class WaitFor : When() {

    override val requiredIndex: Int = -1

    private var waitingScope: WeakReference<CoroutineScope>? = null

    override val defaultValue: Int = 5_000

    private val timeout by lazy {
        value?.casted<Int>() ?: defaultValue
    }

    fun trigger() {
        waitingScope?.get()?.cancel(TriggeredCancellationException())
    }

    private class TriggeredCancellationException : CancellationException("Triggered!")

    override suspend fun applyFlow(runtime: TaskRuntime): AppletResult {
        var elapsed = 0L
        runtime.waitingFor = this
        while (elapsed < timeout) {
            val start = SystemClock.uptimeMillis()
            coroutineScope {
                try {
                    waitingScope = WeakReference(this)
                    delay(timeout.toLong())
                } catch (t: TriggeredCancellationException) {
                    // ignored
                }
            }
            elapsed += SystemClock.uptimeMillis() - start
            if (super.applyFlow(runtime).isSuccessful) {
                return AppletResult.EMPTY_SUCCESS
            }
        }
        return AppletResult.EMPTY_FAILURE
    }
}