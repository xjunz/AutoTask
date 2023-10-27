/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.base

import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/11/03
 */
open class Do : ControlFlow() {

    override var relation: Int = REL_AND

    override fun shouldBeSkipped(runtime: TaskRuntime): Boolean {
        return runtime.ifSuccessful != true
    }
}