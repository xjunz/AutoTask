/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow

import android.view.accessibility.AccessibilityNodeInfo
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.uiautomator.CoroutineUiObject

/**
 * @author xjunz 2023/09/15
 */
class ClickUiObjectWithText : PerformActionIfUiObjectExistsInCurrentWindow() {

    override val isCriterionSpecified: Boolean = true

    private var specifiedText: String? = null

    override fun onPrepareApply(runtime: TaskRuntime) {
        super.onPrepareApply(runtime)
        specifiedText = getArgument(0, runtime)?.toString()
    }

    override suspend fun performActionIfFound(runtime: TaskRuntime, target: CoroutineUiObject) {
        target.click()
    }

    override suspend fun matchUiObject(node: AccessibilityNodeInfo, runtime: TaskRuntime): Boolean {
        return node.text?.toString() == specifiedText
    }
}