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
class ContainsUiObject : ScopeFlow<UiObjectTarget>() {

    override var isInvertible: Boolean = true

    override val isRepetitive: Boolean = true

    override fun initializeTarget(runtime: TaskRuntime): UiObjectTarget {
        return UiObjectTarget()
    }

    override suspend fun applyFlow(runtime: TaskRuntime): AppletResult {
        val ctx = runtime.target
        val referentNode = runtime.getReferentOf(this, 0) as AccessibilityNodeInfo
        referentNode.ensureRefresh()
        val node = referentNode.findFirst(false) {
            ctx.source = it
            super.applyFlow(runtime).isSuccessful
        }
        if (isInverted) {
            return AppletResult.emptyResult(node == null)
        }
        return if (node != null) UiObjectReferent(node).asResult() else AppletResult.EMPTY_FAILURE
    }
}