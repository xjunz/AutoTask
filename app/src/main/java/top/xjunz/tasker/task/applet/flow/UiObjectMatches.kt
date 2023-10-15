/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow

import android.view.accessibility.AccessibilityNodeInfo
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.applet.base.ScopeFlow
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.ktx.ensureRefresh

/**
 * @author xjunz 2023/03/13
 */
class UiObjectMatches : ScopeFlow<UiObjectTarget>() {

    override fun initializeTarget(runtime: TaskRuntime): UiObjectTarget {
        return UiObjectTarget()
    }

    override suspend fun applyFlow(runtime: TaskRuntime): AppletResult {
        val referentNode = runtime.getReferenceArgument(this, 0) as AccessibilityNodeInfo
        referentNode.ensureRefresh()
        runtime.target.source = referentNode
        return super.applyFlow(runtime)
    }
}