/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow

import android.view.accessibility.AccessibilityNodeInfo
import top.xjunz.tasker.engine.applet.base.ControlFlow
import top.xjunz.tasker.engine.runtime.Referent
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.service.currentService
import top.xjunz.tasker.service.uiAutomation
import top.xjunz.tasker.util.formatCurrentTime

/**
 * PreloadFlow initialize global referents at [onPrepareApply].
 *
 * @author xjunz 2023/01/23
 */
class PreloadFlow : ControlFlow(), Referent {

    override val requiredIndex: Int = 0

    override val maxSize: Int = 0

    override val minSize: Int = 0

    override fun onPrepareApply(runtime: TaskRuntime) {
        super.onPrepareApply(runtime)
        runtime.registerReferent(this)
    }

    override fun getReferredValue(which: Int, runtime: TaskRuntime): Any? {
        return when (which) {
            0 -> currentService.getCurrentComponentInfo()
            1 -> currentService.getCurrentComponentInfo().packageName
            2 -> currentService.getCurrentComponentInfo().label
            3 -> uiAutomation.rootInActiveWindow
            4 -> uiAutomation.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            5 -> formatCurrentTime()
            else -> super.getReferredValue(which, runtime)
        }
    }
}