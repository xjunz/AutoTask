/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.base

import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/08/11
 */
open class When : ControlFlow() {

    override val minSize: Int = 1

    override val requiredIndex: Int = 1

    override fun staticCheckMyself(): Int {
        if (requireParent().getOrNull(index + 1) == null) {
            return StaticError.ERR_WHEN_NO_FELLOW
        }
        return super.staticCheckMyself()
    }

    override fun onPostApply(runtime: TaskRuntime) {
        super.onPostApply(runtime)
        runtime.ifSuccessful = runtime.isSuccessful
        if (!runtime.isSuccessful) {
            runtime.shouldStop = true
        }
    }
}