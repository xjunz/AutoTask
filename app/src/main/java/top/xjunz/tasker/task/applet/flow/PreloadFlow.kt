/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow

import android.view.accessibility.AccessibilityNodeInfo
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.applet.base.ControlFlow
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.service.currentService
import top.xjunz.tasker.service.uiAutomation

/**
 * PreloadFlow initialize global referents at [onPrepare].
 *
 * @author xjunz 2023/01/23
 */
class PreloadFlow : ControlFlow() {

    override val requiredIndex: Int = 0

    override val maxSize: Int = 0

    override val minSize: Int = 0

    override fun onPrepare(runtime: TaskRuntime) {
        super.onPrepare(runtime)
        runtime.registerReferent(this, 0, TaskRuntime.Referred {
            currentService.a11yEventDispatcher.getCurrentComponentInfo()
        })
        runtime.registerReferent(this, 1, TaskRuntime.Referred {
            uiAutomation.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        })
    }

    override suspend fun applyFlow(runtime: TaskRuntime): AppletResult {
        return super.applyFlow(runtime)
    }
}