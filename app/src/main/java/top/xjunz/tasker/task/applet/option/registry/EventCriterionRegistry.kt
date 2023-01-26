/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry


import top.xjunz.tasker.R
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.task.applet.criterion.EventCriterion
import top.xjunz.tasker.task.applet.flow.ComponentInfoWrapper

/**
 * @author xjunz 2022/08/12
 */
class EventCriterionRegistry(id: Int) : AppletOptionRegistry(id) {

    private fun eventFilterOption(@Event.EventType event: Int, label: Int) =
        appletOption(label) {
            EventCriterion(event)
        }.hasInnateValue()

    @AppletOrdinal(0)
    val pkgEntered = eventFilterOption(Event.EVENT_ON_PACKAGE_ENTERED, R.string.on_package_entered)
        .withResult<ComponentInfoWrapper>(R.string.app_entered)

    @AppletOrdinal(1)
    val pkgExited = eventFilterOption(Event.EVENT_ON_PACKAGE_EXITED, R.string.on_package_left)
        .withResult<ComponentInfoWrapper>(R.string.app_left)

    @AppletOrdinal(2)
    val contentChanged =
        eventFilterOption(Event.EVENT_ON_CONTENT_CHANGED, R.string.on_content_changed)
            .withResult<ComponentInfoWrapper>(R.string.current_app)

    @AppletOrdinal(3)
    val notificationReceived =
        eventFilterOption(Event.EVENT_ON_NOTIFICATION_RECEIVED, R.string.on_notification_received)
            .withResult<String>(R.string.notification_content)
            .withResult<ComponentInfoWrapper>(R.string.notification_owner_app)
}