/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow

import top.xjunz.tasker.engine.applet.base.ControlFlow
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.service.currentService

/**
 * @author xjunz 2023/01/23
 */
class PreloadFlow : ControlFlow() {

    override val requiredIndex: Int = 0

    override val maxSize: Int = 0

    override val minSize: Int = 0

    override fun onPrepare(runtime: TaskRuntime) {
        super.onPrepare(runtime)
        runtime.registerReferent(this, 0) {
            currentService.a11yEventDispatcher.getCurrentComponentInfo()
        }
    }
}