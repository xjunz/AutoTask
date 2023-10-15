/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.base

import android.os.SystemClock
import kotlinx.coroutines.delay
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * Wait until something happens...
 *
 * @author xjunz 2023/02/10
 */
class WaitUntil : If() {

    companion object {
        const val POLL_INTERVAL = 100L
    }

    override val isRepetitive: Boolean = true

    override val defaultValue: Int = 5_000

    private val timeout by lazy {
        values[0]?.casted<Int>() ?: defaultValue
    }

    override suspend fun applyFlow(runtime: TaskRuntime): AppletResult {
        val start = SystemClock.uptimeMillis()
        var successful = false
        while (SystemClock.uptimeMillis() - start < timeout) {
            if (super.applyFlow(runtime).isSuccessful) {
                successful = true
                break
            }
            delay(POLL_INTERVAL)
        }
        return AppletResult.emptyResult(successful)
    }
}