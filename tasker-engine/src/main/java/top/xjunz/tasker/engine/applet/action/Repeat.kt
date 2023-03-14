/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.action

import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.applet.base.Loop
import top.xjunz.tasker.engine.runtime.Referent
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/12/04
 */
class Repeat : Loop(), Referent {

    override val valueType: Int = VAL_TYPE_INT

    override val isRepetitive: Boolean = true

    private val count by lazy {
        value as Int
    }

    override fun getReferredValue(which: Int, runtime: TaskRuntime): Any? {
        return when (which) {
            1 -> currentCount
            2 -> currentCount.toString()
            else -> super.getReferredValue(which, runtime)
        }
    }

    override fun staticCheckMyself(): Int {
        check(value != null && (value as Int) > 0) {
            "Repeat count must be specified!"
        }
        return super.staticCheckMyself()
    }

    override fun onPrepareApply(runtime: TaskRuntime) {
        super.onPrepareApply(runtime)
        runtime.registerReferent(this)
    }

    override suspend fun applyFlow(runtime: TaskRuntime): AppletResult {
        for (i in 0 until count) {
            currentCount = i + 1
            super.applyFlow(runtime)
            if (shouldBreak) break
        }
        return AppletResult.EMPTY_SUCCESS
    }

}