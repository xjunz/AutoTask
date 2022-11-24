package top.xjunz.tasker.task.applet.flow

import android.view.accessibility.AccessibilityNodeInfo
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.service.uiAutomation

/**
 * @author xjunz 2022/08/25
 */
class UiObjectFlow : Flow() {

    override fun doApply(runtime: TaskRuntime) {
        val ctx = UiObjectContext()
        runtime.setTarget(ctx)
        val node = runtime.getOrPutCrossTaskVariable(id) {
            uiAutomation.rootInActiveWindow
        }.findFirst {
            ctx.source = it
            super.doApply(runtime)
            runtime.isSuccessful
        }
        if (node != null) {
            referred.forEach { (which, id) ->
                runtime.registerResult(id, getReferredValue(which, node))
            }
            runtime.isSuccessful = true
        } else {
            runtime.isSuccessful = false
        }
    }

    private fun AccessibilityNodeInfo.findFirst(condition: (AccessibilityNodeInfo) -> Boolean)
            : AccessibilityNodeInfo? {
        for (i in 0 until childCount) {
            val child = getChild(i) ?: continue
            // Need this?
            if (!child.isVisibleToUser) continue
            return try {
                if (condition(child)) child else child.findFirst(condition)
            } finally {
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }
        return null
    }

    override fun getReferredValue(which: Int, ret: Any): Any? {
        if (which == 1) {
            return (ret as AccessibilityNodeInfo).text?.toString()
        }
        return super.getReferredValue(which, ret)
    }
}