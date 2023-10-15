/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow

import android.view.accessibility.AccessibilityNodeInfo
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.applet.base.ScopeFlow
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.ktx.ensureRefresh
import top.xjunz.tasker.ktx.findFirst
import top.xjunz.tasker.task.applet.flow.ref.UiObjectReferent

/**
 * @author xjunz 2022/08/25
 */
open class ContainsUiObject : ScopeFlow<UiObjectTarget>() {

    override var isInvertible: Boolean = true

    override val isRepetitive: Boolean = true

    protected var matchedNode: AccessibilityNodeInfo? = null

    override fun initializeTarget(runtime: TaskRuntime): UiObjectTarget {
        return UiObjectTarget()
    }

    protected open fun getUiObjectSearchRoot(runtime: TaskRuntime): AccessibilityNodeInfo {
        return runtime.getReferenceArgument(this, 0) as AccessibilityNodeInfo
    }

    protected open suspend fun matchUiObject(
        node: AccessibilityNodeInfo,
        runtime: TaskRuntime
    ): Boolean {
        return super.applyFlow(runtime).isSuccessful
    }

    override suspend fun applyFlow(runtime: TaskRuntime): AppletResult {
        val ctx = runtime.target
        val root = getUiObjectSearchRoot(runtime)
        root.ensureRefresh()
        val node = root.findFirst(false) {
            ctx.source = it
            matchUiObject(it, runtime)
        }
        if (isInverted) {
            return AppletResult.emptyResult(node == null)
        }
        matchedNode = node
        return if (node != null) UiObjectReferent(node).asResult() else AppletResult.EMPTY_FAILURE
    }

    override fun onPostApply(runtime: TaskRuntime) {
        super.onPostApply(runtime)
        matchedNode = null
    }
}