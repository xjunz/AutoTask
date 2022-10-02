package top.xjunz.tasker.task.flow

import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import kotlinx.serialization.SerialName
import top.xjunz.tasker.engine.AppletContext
import top.xjunz.tasker.engine.FlowRuntime
import top.xjunz.tasker.engine.flow.Flow

/**
 * @author xjunz 2022/08/25
 */
@SerialName("UiObjectFlow")
class UiObjectFlow : Flow() {

    override fun doApply(context: AppletContext, runtime: FlowRuntime) {
        val uiObjectContext = UiObjectContext()
        runtime.setTarget(uiObjectContext)
        val node = context.getOrPutArgument(id) {
            context.task.uiAutomation.rootInActiveWindow
        }.findFirst {
            uiObjectContext.source = it
            super.doApply(context, runtime)
            runtime.isSuccessful
        }
        if (node != null) {
            if (isReferred) runtime.registerResult(remark!!, node)
            runtime.isSuccessful = true
        } else {
            runtime.isSuccessful = false
        }
    }

    private fun AccessibilityNodeInfo.findFirst(condition: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        for (i in 0 until childCount) {
            val child = getChild(i) ?: continue
            return try {
                if (condition(child)) child else child.findFirst(condition)
            } finally {
                AccessibilityNodeInfoCompat.wrap(child).recycle()
            }
        }
        return null
    }
}