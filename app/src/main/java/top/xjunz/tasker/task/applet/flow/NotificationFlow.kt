package top.xjunz.tasker.task.applet.flow

import top.xjunz.tasker.engine.AutomatorTask
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.runtime.FlowRuntime

/**
 * @author xjunz 2022/11/16
 */
class NotificationFlow : Flow() {

    data class NotificationInfo(val packageName: String, val content: CharSequence?)

    override fun shouldSkipAll(task: AutomatorTask, runtime: FlowRuntime): Boolean {
        val shouldSkip = runtime.hitEvent.type != Event.EVENT_ON_NOTIFICATION_RECEIVED
        if (shouldSkip) {
            runtime.isSuccessful = false
        }
        return shouldSkip
    }

    override fun onPrepare(task: AutomatorTask, runtime: FlowRuntime) {
        super.onPrepare(task, runtime)
        runtime.setTarget(task.getOrPutCrossTaskVariable(id) {
            NotificationInfo(
                runtime.hitEvent.componentInfo.pkgName,
                runtime.hitEvent.componentInfo.paneTitle
            )
        })
    }
}