/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.criterion.PropertyCriterion
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.task.applet.flow.model.NotificationReferent

/**
 * @author xjunz 2022/11/16
 */
class NotificationCriterionRegistry(id: Int) : AppletOptionRegistry(id) {

    @AppletOrdinal(0x00_00)
    val isToast = appletOption(R.string.is_toast) {
        PropertyCriterion<NotificationReferent> {
            it.isToast
        }
    }.withRefArgument<NotificationReferent>(R.string.notification)
        .withTitleModifier("Toast")
}