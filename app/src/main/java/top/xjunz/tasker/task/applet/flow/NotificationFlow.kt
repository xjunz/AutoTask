/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow

import top.xjunz.tasker.engine.applet.base.ScopedFlow
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/11/16
 */
class NotificationFlow : ScopedFlow<NotificationFlow.NotificationInfo>() {

    data class NotificationInfo(val packageName: String, val content: CharSequence?)

    override suspend fun doApply(runtime: TaskRuntime): Boolean {
        return if (runtime.hitEvent.type != Event.EVENT_ON_NOTIFICATION_RECEIVED) {
            runtime.observer?.onSkipped(this, runtime)
            false
        } else {
            super.doApply(runtime)
        }
    }

    override fun initializeTarget(runtime: TaskRuntime): NotificationInfo {
        return NotificationInfo(
            runtime.hitEvent.componentInfo.pkgName,
            runtime.hitEvent.componentInfo.paneTitle
        )
    }
}