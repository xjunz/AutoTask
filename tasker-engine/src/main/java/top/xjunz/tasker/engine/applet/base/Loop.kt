/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.base

import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2023/03/16
 */
abstract class Loop : Flow() {

    override val supportsAnywayRelation: Boolean = true

    var shouldBreak = false

    var shouldContinue = false

    protected var currentCount: Int = 0

    override fun onPrepareApply(runtime: TaskRuntime) {
        super.onPrepareApply(runtime)
        currentCount = 0
        runtime.currentLoop = this
    }

    override fun onPostApply(runtime: TaskRuntime) {
        super.onPostApply(runtime)
        runtime.currentLoop = null
        shouldBreak = false
    }

    override suspend fun applyFlow(runtime: TaskRuntime): AppletResult {
        val result = super.applyFlow(runtime)
        shouldContinue = false
        return result
    }
}