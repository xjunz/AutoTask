/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow

import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.applet.base.ScopedFlow
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/11/16
 */
class NotificationFlow : ScopedFlow<NotificationFlow.NotificationTarget>() {

    data class NotificationTarget(val packageName: String, val content: CharSequence?)

    override suspend fun applyFlow(runtime: TaskRuntime): AppletResult {
        return if (runtime.hitEvent.type != Event.EVENT_ON_NOTIFICATION_RECEIVED) {
            runtime.observer?.onSkipped(this, runtime)
            AppletResult.FAILURE
        } else {
            super.applyFlow(runtime)
        }
    }

    override fun initializeTarget(runtime: TaskRuntime): NotificationTarget {
        return NotificationTarget(
            runtime.hitEvent.componentInfo.packageName,
            runtime.hitEvent.componentInfo.paneTitle
        )
    }
}