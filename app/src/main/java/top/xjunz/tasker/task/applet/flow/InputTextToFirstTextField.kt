/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow

import android.view.accessibility.AccessibilityNodeInfo
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.uiautomator.CoroutineUiObject

/**
 * @author xjunz 2023/09/13
 */
class InputTextToFirstTextField : PerformActionIfUiObjectExistsInCurrentWindow() {

    override val valueType: Int = VAL_TYPE_TEXT

    override val isCriterionSpecified: Boolean = true

    override suspend fun performActionIfFound(runtime: TaskRuntime, target: CoroutineUiObject) {
        val text = (runtime.getReferentOf(this, 0) ?: value) as CharSequence
        target.setText(text)
    }

    override suspend fun matchUiObject(node: AccessibilityNodeInfo, runtime: TaskRuntime): Boolean {
        return node.isEditable
    }

    override fun processResult(origin: AppletResult): AppletResult {
        return AppletResult.emptyResult(origin.isSuccessful)
    }

}