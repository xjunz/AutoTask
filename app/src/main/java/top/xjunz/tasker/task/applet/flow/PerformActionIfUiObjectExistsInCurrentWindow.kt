/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow

import android.view.accessibility.AccessibilityNodeInfo
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.service.uiAutomation
import top.xjunz.tasker.service.uiDevice
import top.xjunz.tasker.uiautomator.CoroutineUiObject

/**
 * @author xjunz 2023/09/15
 */
abstract class PerformActionIfUiObjectExistsInCurrentWindow : ContainsUiObject() {

    final override var isInvertible: Boolean = false

    abstract val isCriterionSpecified: Boolean

    override val minSize: Int
        get() = if (isCriterionSpecified) 0 else super.minSize

    override val maxSize: Int
        get() = if (isCriterionSpecified) 0 else super.maxSize

    final override val supportsAnywayRelation: Boolean = true

    protected abstract suspend fun performActionIfFound(
        runtime: TaskRuntime,
        target: CoroutineUiObject
    )

    protected open fun processResult(origin: AppletResult): AppletResult {
        return origin
    }

    final override fun getUiObjectSearchRoot(runtime: TaskRuntime): AccessibilityNodeInfo {
        return uiAutomation.rootInActiveWindow
    }

    override suspend fun matchUiObject(node: AccessibilityNodeInfo, runtime: TaskRuntime): Boolean {
        return super.matchUiObject(node, runtime)
    }

    final override suspend fun applyFlow(runtime: TaskRuntime): AppletResult {
        val result = super.applyFlow(runtime)
        if (result.isSuccessful) {
            val node = requireNotNull(matchedNode)
            performActionIfFound(runtime, uiDevice.wrapUiObject(node))
        }
        return processResult(result)
    }
}