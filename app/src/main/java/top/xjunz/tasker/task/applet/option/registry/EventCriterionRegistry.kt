/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry


import top.xjunz.tasker.R
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.runtime.Event.*
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.task.applet.criterion.EventFilter
import top.xjunz.tasker.task.applet.flow.ref.ComponentInfoWrapper
import top.xjunz.tasker.task.applet.flow.ref.NotificationReferent

/**
 * @author xjunz 2022/08/12
 */
class EventCriterionRegistry(id: Int) : AppletOptionRegistry(id) {

    private fun eventFilterOption(@EventType event: Int, label: Int) =
        appletOption(label) {
            EventFilter(event)
        }.hasInnateValue()

    @AppletOrdinal(0x0000)
    val pkgEntered = eventFilterOption(Event.EVENT_ON_PACKAGE_ENTERED, R.string.on_package_entered)
        .withResult<ComponentInfoWrapper>(R.string.app_entered)
        .withResult<String>(R.string.pkg_name_of_app_entered)
        .withResult<String>(R.string.name_of_app_entered)

    @AppletOrdinal(0x0001)
    val pkgExited = eventFilterOption(Event.EVENT_ON_PACKAGE_EXITED, R.string.on_package_left)
        .withResult<ComponentInfoWrapper>(R.string.app_left)
        .withResult<String>(R.string.pkg_name_of_app_exited)
        .withResult<String>(R.string.name_of_app_exited)

    @AppletOrdinal(0x0002)
    val contentChanged =
        eventFilterOption(Event.EVENT_ON_CONTENT_CHANGED, R.string.on_content_changed)
            .withResult<ComponentInfoWrapper>(R.string.current_app)

    @AppletOrdinal(0x0003)
    val notificationReceived =
        eventFilterOption(Event.EVENT_ON_NOTIFICATION_RECEIVED, R.string.on_notification_received)
            .withResult<NotificationReferent>(R.string.notification_received)
            .withResult<String>(R.string.notification_content)
            .withResult<ComponentInfoWrapper>(R.string.notification_owner_app)

    @AppletOrdinal(0x0004)
    val newWindow = eventFilterOption(Event.EVENT_ON_NEW_WINDOW, R.string.on_new_window)
        .withTitleModifier(R.string.tip_new_window)

    @AppletOrdinal(0x0005)
    val primaryClipChanged =
        eventFilterOption(Event.EVENT_ON_PRIMARY_CLIP_CHANGED, R.string.on_primary_clip_changed)
            .withResult<String>(R.string.current_primary_clip_text)
}