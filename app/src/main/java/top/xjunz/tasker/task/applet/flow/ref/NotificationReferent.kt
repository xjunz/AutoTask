/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow.ref

import top.xjunz.tasker.engine.runtime.Referent
import top.xjunz.tasker.task.applet.option.registry.EventCriterionRegistry

/**
 * @see [EventCriterionRegistry.notificationReceived]
 *
 * @author xjunz 2023/02/12
 */
class NotificationReferent(private val componentInfo: ComponentInfoWrapper, val isToast: Boolean) :
    Referent {

    companion object {
        const val EXTRA_IS_TOAST = 1
    }

    override fun getReferredValue(which: Int): Any? {
        return when (which) {
            // Notification content
            1 -> componentInfo.paneTitle
            // ComponentInfo which sends the notification
            2 -> componentInfo
            else -> super.getReferredValue(which)
        }
    }
}