/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.base

import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/11/01
 */
internal class DslFlow(private val initialTarget: Any? = null) : RootFlow() {

    override fun onPrepareApply(runtime: TaskRuntime) {
        if (initialTarget != null)
            runtime.setTarget(initialTarget)
    }
}