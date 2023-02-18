/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow

import android.view.accessibility.AccessibilityNodeInfo
import top.xjunz.shared.trace.logcat
import top.xjunz.tasker.bridge.DisplayManagerBridge
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.applet.base.ScopeFlow
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.service.uiAutomation
import top.xjunz.tasker.task.applet.flow.ref.UiObjectReferent

/**
 * @author xjunz 2022/08/25
 */
class UiObjectFlow : ScopeFlow<UiObjectFlow.UiObjectTarget>() {

    private val ROOT_NODE_KEY = generateUniqueKey(1)

    override val isRepetitive: Boolean = true

    override fun initializeTarget(runtime: TaskRuntime): UiObjectTarget {
        return UiObjectTarget()
    }

    override suspend fun applyFlow(runtime: TaskRuntime): AppletResult {
        val ctx = runtime.target
        val node = runtime.getGlobalValue(TaskRuntime.GLOBAL_SCOPE_EVENT, ROOT_NODE_KEY) {
            uiAutomation.rootInActiveWindow
        }.findFirst(false) {
            ctx.source = it
            super.applyFlow(runtime).isSuccessful
        }
        return if (node != null) {
            AppletResult.succeeded(UiObjectReferent(node))
        } else AppletResult.EMPTY_FAILURE
    }

    /**
     * todo: Use [AccessibilityNodeInfo.findAccessibilityNodeInfosByText] or
     * [AccessibilityNodeInfo.findAccessibilityNodeInfosByViewId] if possible.
     */
    private suspend fun AccessibilityNodeInfo.findFirst(
        checkSelf: Boolean,
        condition: suspend (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (checkSelf) {
            if (!isVisibleToUser) return null
            if (condition(this)) {
                logcat("found self")
                return this
            }
        }
        for (i in 0 until childCount) {
            val child = getChild(i) ?: continue
            if (!child.isVisibleToUser) continue
            try {
                if (condition(child)) {
                    return child
                } else if (child.childCount > 0) {
                    val ret = child.findFirst(false, condition)
                    if (ret != null) return ret
                }
            } finally {
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }
        return null
    }

    class UiObjectTarget {

        lateinit var source: AccessibilityNodeInfo

        val density: Float by lazy {
            DisplayManagerBridge.density
        }

        private val realSize by lazy {
            DisplayManagerBridge.size
        }

        val screenWidthPixels get() = realSize.x

        val screenHeightPixels get() = realSize.y
    }
}