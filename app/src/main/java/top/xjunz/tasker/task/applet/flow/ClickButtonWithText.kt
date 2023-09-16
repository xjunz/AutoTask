/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow

import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.uiautomator.CoroutineUiObject

/**
 * @author xjunz 2023/09/14
 */
class ClickButtonWithText : PerformActionIfUiObjectExistsInCurrentWindow() {

    override val valueType: Int = VAL_TYPE_TEXT

    override val isCriterionSpecified: Boolean = true

    private var specifiedText: String? = null

    override fun onPrepareApply(runtime: TaskRuntime) {
        super.onPrepareApply(runtime)
        specifiedText = (runtime.getReferentOf(this, 0) ?: value)?.toString()
    }

    override suspend fun matchUiObject(node: AccessibilityNodeInfo, runtime: TaskRuntime): Boolean {
        return node.className == Button::class.java.name && node.text?.toString() == specifiedText
    }

    override suspend fun performActionIfFound(runtime: TaskRuntime, target: CoroutineUiObject) {
        target.click()
    }

}