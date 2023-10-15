/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow

import android.view.accessibility.AccessibilityNodeInfo
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.applet.base.Loop
import top.xjunz.tasker.engine.runtime.Referent
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.ktx.ensureRefresh
import top.xjunz.tasker.service.uiDevice
import top.xjunz.tasker.task.applet.value.ScrollMetrics

/**
 * @author xjunz 2023/03/13
 */
class ForEachUiScrollable : Loop(), Referent {

    private val metrics by lazy {
        ScrollMetrics.parse(firstValue as Long)
    }

    private var currentChild: AccessibilityNodeInfo? = null

    override fun onPrepareApply(runtime: TaskRuntime) {
        super.onPrepareApply(runtime)
        runtime.registerReferent(this)
        runtime.setTarget(UiObjectTarget())
    }

    override fun onPostApply(runtime: TaskRuntime) {
        super.onPostApply(runtime)
        currentChild = null
    }

    override suspend fun applyFlow(runtime: TaskRuntime): AppletResult {
        val referentNode = runtime.getReferenceArgument(this, 0) as AccessibilityNodeInfo
        val scrollable = uiDevice.wrapUiScrollable(metrics.isVertical, referentNode)
        var hitBottomCount = 0
        do {
            referentNode.ensureRefresh()
            for (i in 0 until referentNode.childCount) {
                val child = referentNode.getChild(i) ?: continue
                child.ensureRefresh()
                if (!child.isVisibleToUser) continue
                currentChild = child
                runtime.getTarget<UiObjectTarget>().source = child
                super.applyFlow(runtime)
                currentCount++
                if (shouldBreak) break
            }
            if (!scrollable.scrollForward(metrics.steps)) {
                hitBottomCount++
            }
        } while (hitBottomCount < 2)
        return AppletResult.EMPTY_SUCCESS
    }

    override fun getReferredValue(which: Int, runtime: TaskRuntime): Any? {
        return when (which) {
            0 -> currentChild
            1 -> currentCount
            else -> super.getReferredValue(which, runtime)
        }
    }
}