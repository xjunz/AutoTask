/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.action

import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/12/04
 */
class Repeat : Flow() {

    override val valueType: Int = VAL_TYPE_INT

    private var count: Int = 0

    var shouldBreak: Boolean = false

    override fun staticCheckMyself(): Int {
        check(value != null && value!!.casted<Int>() > 0) {
            "Repeat count must be specified!"
        }
        return super.staticCheckMyself()
    }

    override fun onPreApply(runtime: TaskRuntime) {
        super.onPreApply(runtime)
        runtime.registerReferents(this, this)
    }

    override suspend fun applyFlow(runtime: TaskRuntime): AppletResult {
        for (i in 0 until count) {
            if (shouldBreak) {
                shouldBreak = false
                break
            }
            val result = super.applyFlow(runtime)
            if (!result.isSuccessful) {
                return result
            }
        }
        return AppletResult.SUCCESS
    }

}