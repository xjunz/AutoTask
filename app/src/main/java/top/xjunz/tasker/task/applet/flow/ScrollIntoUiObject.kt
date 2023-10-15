/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow

import android.view.accessibility.AccessibilityNodeInfo
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.applet.base.ScopeFlow
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.ktx.ensureRefresh
import top.xjunz.tasker.ktx.findFirst
import top.xjunz.tasker.service.uiDevice
import top.xjunz.tasker.task.applet.flow.ref.UiObjectReferent
import top.xjunz.tasker.task.applet.value.ScrollMetrics

/**
 * @author xjunz 2023/03/13
 */
class ScrollIntoUiObject : ScopeFlow<UiObjectTarget>() {

    companion object {
        const val MAX_SCROLL_INTO_STEPS = 30
    }

    private val metrics by lazy {
        ScrollMetrics.parse(firstValue as Long)
    }

    override fun initializeTarget(runtime: TaskRuntime): UiObjectTarget {
        return UiObjectTarget()
    }

    override suspend fun applyFlow(runtime: TaskRuntime): AppletResult {
        val resultNode: AccessibilityNodeInfo? = runtime.getReferenceArgument(this, 0)
            ?.casted<AccessibilityNodeInfo>()?.scrollIntoView(runtime)
        return if (resultNode != null) {
            UiObjectReferent(resultNode).asResult()
        } else {
            AppletResult.EMPTY_FAILURE
        }
    }

    private suspend fun AccessibilityNodeInfo.exists(runtime: TaskRuntime): AccessibilityNodeInfo? {
        var node: AccessibilityNodeInfo? = null
        ensureRefresh()
        findFirst(false) {
            runtime.target.source = it
            if (super.applyFlow(runtime).isSuccessful) {
                node = it
                true
            } else {
                false
            }
        }
        return node
    }

    /**
     * Perform a scroll forward action to move through the scrollable layout
     * element until a visible item that matches the selector is found.
     */
    private suspend fun AccessibilityNodeInfo.scrollIntoView(runtime: TaskRuntime): AccessibilityNodeInfo? {
        var node: AccessibilityNodeInfo? = exists(runtime)
        if (node == null) {
            for (x in 0 until MAX_SCROLL_INTO_STEPS) {
                val scrolled =
                    uiDevice.wrapUiScrollable(metrics.isVertical, this).scrollForward(metrics.steps)
                node = exists(runtime)
                if (node != null || !scrolled) {
                    break
                }
            }
        }
        return node
    }
}