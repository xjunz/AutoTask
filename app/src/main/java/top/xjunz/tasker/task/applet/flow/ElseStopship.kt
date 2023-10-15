/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow

import top.xjunz.tasker.engine.applet.base.Else
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2023/09/13
 */
class ElseStopship : Else() {

    override val maxSize: Int = 0

    override val minSize: Int = 0

    override fun onPostApply(runtime: TaskRuntime) {
        super.onPostApply(runtime)
        runtime.shouldStop = true
    }
}