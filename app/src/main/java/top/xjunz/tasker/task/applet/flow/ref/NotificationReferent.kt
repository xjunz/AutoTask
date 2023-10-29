/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow.ref

import top.xjunz.tasker.engine.runtime.Referent
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.task.applet.option.registry.EventCriterionRegistry

/**
 * @see [EventCriterionRegistry.notificationReceived]
 *
 * @author xjunz 2023/02/12
 */
class NotificationReferent(private val componentInfo: ComponentInfoWrapper) : Referent {

    override fun getReferredValue(which: Int, runtime: TaskRuntime): Any? {
        return when (which) {
            0 -> this
            // Notification content
            1 -> componentInfo.paneTitle
            // ComponentInfo which sends the notification
            2 -> componentInfo
            3 -> componentInfo.label
            else -> super.getReferredValue(which, runtime)
        }
    }
}