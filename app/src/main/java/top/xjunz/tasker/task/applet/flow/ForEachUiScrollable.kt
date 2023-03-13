/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow

import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.runtime.Referent
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.ktx.ensureRefresh
import top.xjunz.tasker.service.uiDevice
import top.xjunz.tasker.task.applet.value.ScrollMetrics

/**
 * @author xjunz 2023/03/13
 */
class ForEachUiScrollable : Flow(), Referent {

    override val valueType: Int = VAL_TYPE_LONG

    private val metrics by lazy {
        ScrollMetrics.parse(value as Long)
    }

    private var currentChild: AccessibilityNodeInfo? = null

    private var traversedCount: Int = 0

    override fun onPrepareApply(runtime: TaskRuntime) {
        super.onPreApply(runtime)
        runtime.registerReferent(this)
        runtime.setTarget(UiObjectTarget())
    }

    override fun onPreApply(runtime: TaskRuntime) {
        super.onPreApply(runtime)
        traversedCount = 0
    }

    override fun onPostApply(runtime: TaskRuntime) {
        super.onPostApply(runtime)
        currentChild = null
    }

    override suspend fun applyFlow(runtime: TaskRuntime): AppletResult {
        val referentNode = runtime.getReferentOf(this, 0) as AccessibilityNodeInfo
        val scrollable = uiDevice.wrapUiScrollable(metrics.isVertical, referentNode)
        var hitBottomCount = 0
        do {
            referentNode.ensureRefresh()
            for (i in 0 until referentNode.childCount) {
                val child = referentNode.getChild(i) ?: continue
                try {
                    if (!child.isVisibleToUser) continue
                    currentChild = child
                    runtime.getTarget<UiObjectTarget>().source = child
                    super.applyFlow(runtime)
                    traversedCount++
                } finally {
                    AccessibilityNodeInfoCompat.wrap(child).recycle()
                }
            }
            if (!scrollable.scrollForward(metrics.steps)) {
                hitBottomCount++
            }
        } while (hitBottomCount < 2)
        return AppletResult.EMPTY_SUCCESS
    }

    override fun getReferredValue(runtime: TaskRuntime, which: Int): Any? {
        return when (which) {
            0 -> currentChild
            1 -> traversedCount
            else -> super.getReferredValue(runtime, which)
        }
    }
}