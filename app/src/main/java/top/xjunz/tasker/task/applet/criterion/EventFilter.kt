/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.criterion

import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.task.applet.flow.ref.ComponentInfoWrapper
import top.xjunz.tasker.task.applet.flow.ref.NotificationReferent
import top.xjunz.tasker.task.event.ClipboardEventDispatcher
import top.xjunz.tasker.task.event.NetworkEventDispatcher

/**
 * @author xjunz 2022/08/25
 */
class EventFilter(private val eventType: Int) : Applet() {

    @Deprecated("Only for compatibility use.")
    override val isValueInnate: Boolean = true

    override var relation: Int = REL_OR

    override suspend fun apply(runtime: TaskRuntime): AppletResult {
        val hit = runtime.events?.find {
            it.type == eventType
        }
        return if (hit == null) {
            AppletResult.EMPTY_FAILURE
        } else {
            when (hit.type) {
                Event.EVENT_ON_NOTIFICATION_RECEIVED, Event.EVENT_ON_TOAST_RECEIVED -> {
                    NotificationReferent(ComponentInfoWrapper.wrap(hit.componentInfo)).asResult()
                }

                Event.EVENT_ON_PRIMARY_CLIP_CHANGED -> {
                    AppletResult.succeeded(hit.getExtra(ClipboardEventDispatcher.EXTRA_PRIMARY_CLIP_TEXT))
                }

                Event.EVENT_ON_TICK -> AppletResult.EMPTY_SUCCESS

                Event.EVENT_ON_WIFI_CONNECTED, Event.EVENT_ON_WIFI_DISCONNECTED -> {
                    AppletResult.succeeded(hit.getExtra(NetworkEventDispatcher.EXTRA_WIFI_SSID))
                }

                else -> ComponentInfoWrapper.wrap(hit.componentInfo).asResult()
            }
        }
    }

}