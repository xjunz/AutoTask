/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow

import android.view.accessibility.AccessibilityNodeInfo
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.applet.base.ScopedFlow
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.service.uiAutomation

/**
 * @author xjunz 2022/08/25
 */
class UiObjectFlow : ScopedFlow<UiObjectTarget>() {

    private val rootNodeKey = generateUniqueKey(1)

    override fun initializeTarget(runtime: TaskRuntime): UiObjectTarget {
        return UiObjectTarget()
    }

    override suspend fun applyFlow(runtime: TaskRuntime): AppletResult {
        val ctx = runtime.target
        val node = runtime.getGlobal(rootNodeKey) {
            uiAutomation.rootInActiveWindow
        }.findFirst {
            runtime.ensureActive()
            ctx.source = it
            super.applyFlow(runtime).isSuccessful
        }
        return if (node != null) AppletResult.successWithReturn(node) else AppletResult.FAILURE
    }

    private suspend fun AccessibilityNodeInfo.findFirst(condition: suspend (AccessibilityNodeInfo) -> Boolean)
            : AccessibilityNodeInfo? {
        for (i in 0 until childCount) {
            val child = getChild(i) ?: continue
            if (!child.isVisibleToUser) continue
            try {
                if (condition(child)) {
                    return child
                } else if (child.childCount > 0) {
                    val ret = child.findFirst(condition)
                    if (ret != null) return ret
                }
            } finally {
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }
        return null
    }
}