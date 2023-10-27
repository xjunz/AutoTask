/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.base

import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2023/03/15
 */
class Anyway : Do() {

    override var relation: Int = REL_ANYWAY
        set(value) {
            check(value == REL_ANYWAY)
            field = value
        }

    override fun shouldBeSkipped(runtime: TaskRuntime): Boolean {
        return runtime.ifSuccessful == null
    }
}