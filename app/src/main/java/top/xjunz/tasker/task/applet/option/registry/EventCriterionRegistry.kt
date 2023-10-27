/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry


import top.xjunz.tasker.R
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.task.applet.criterion.EventFilter
import top.xjunz.tasker.task.applet.criterion.FileEventCriterion
import top.xjunz.tasker.task.applet.flow.ref.ComponentInfoWrapper
import top.xjunz.tasker.task.applet.flow.ref.NotificationReferent
import top.xjunz.tasker.task.applet.value.VariantArgType

/**
 * @author xjunz 2022/08/12
 */
class EventCriterionRegistry(id: Int) : AppletOptionRegistry(id) {

    private fun eventFilterOption(event: Int, label: Int) = appletOption(label) {
        EventFilter(event)
    }

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
        eventFilterOption(
            Event.EVENT_ON_NOTIFICATION_RECEIVED,
            R.string.on_status_bar_notification_received
        ).withResult<NotificationReferent>(R.string.notification_received)
            .withResult<String>(R.string.notification_content)
            .withResult<ComponentInfoWrapper>(R.string.notification_owner_app)

    @AppletOrdinal(0x0004)
    val toastReceived =
        eventFilterOption(Event.EVENT_ON_TOAST_RECEIVED, R.string.on_toast_notification_received)
            .withResult<NotificationReferent>(R.string.notification_received)
            .withResult<String>(R.string.notification_content)
            .withResult<ComponentInfoWrapper>(R.string.notification_owner_app)

    @AppletOrdinal(0x0005)
    val newWindow = eventFilterOption(Event.EVENT_ON_NEW_WINDOW, R.string.on_new_window)
        .withTitleModifier(R.string.tip_new_window)

    @AppletOrdinal(0x0006)
    val timeChanged = eventFilterOption(Event.EVENT_ON_TICK, R.string.on_tik_tok)
        .withTitleModifier(R.string.tip_on_tik_tok)

    @AppletOrdinal(0x0007)
    val fileCreated = appletOption(R.string.on_file_created) {
        FileEventCriterion(Event.EVENT_ON_FILE_CREATED)
    }.withValueArgument<String>(R.string.file_path, VariantArgType.TEXT_FILE_PATH)
        .withResult<String>(R.string.file_path, VariantArgType.TEXT_FILE_PATH)
        .shizukuOnly()

    @AppletOrdinal(0x0008)
    val fileDeleted = appletOption(R.string.on_file_deleted) {
        FileEventCriterion(Event.EVENT_ON_FILE_DELETED)
    }.withValueArgument<String>(R.string.file_path, VariantArgType.TEXT_FILE_PATH)
        .withResult<String>(R.string.file_path, VariantArgType.TEXT_FILE_PATH)
        .shizukuOnly()

    @AppletOrdinal(0x0009)
    val wifiConnected =
        eventFilterOption(Event.EVENT_ON_WIFI_CONNECTED, R.string.on_wifi_connected)
            .withResult<String>(R.string.connected_wifi_ssid)

    @AppletOrdinal(0x0010)
    val wifiDisconnected =
        eventFilterOption(Event.EVENT_ON_WIFI_DISCONNECTED, R.string.on_wifi_disconnected)
            .withResult<String>(R.string.disconnected_wifi_ssid)

    @AppletOrdinal(0X0011)
    val networkAvailable =
        eventFilterOption(Event.EVENT_ON_NETWORK_AVAILABLE, R.string.on_network_available)

    @AppletOrdinal(0X0012)
    val networkUnavailable =
        eventFilterOption(Event.EVENT_ON_NETWORK_AVAILABLE, R.string.on_network_unavailable)
    /* @AppletOrdinal(0x0005)
     val primaryClipChanged =
         eventFilterOption(Event.EVENT_ON_PRIMARY_CLIP_CHANGED, R.string.on_primary_clip_changed)
             .withResult<String>(R.string.current_primary_clip_text)*/
}