package top.xjunz.tasker.task.applet.flow

import top.xjunz.tasker.engine.applet.base.ScopedFlow
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/11/16
 */
class NotificationFlow : ScopedFlow<NotificationFlow.NotificationInfo>() {

    data class NotificationInfo(val packageName: String, val content: CharSequence?)

    override fun shouldSkipAll(runtime: TaskRuntime): Boolean {
        val shouldSkip = runtime.hitEvent.type != Event.EVENT_ON_NOTIFICATION_RECEIVED
        if (shouldSkip) {
            runtime.isSuccessful = false
        }
        return shouldSkip
    }

    override fun initializeTarget(runtime: TaskRuntime): NotificationInfo {
        return NotificationInfo(
            runtime.hitEvent.componentInfo.pkgName,
            runtime.hitEvent.componentInfo.paneTitle
        )
    }
}