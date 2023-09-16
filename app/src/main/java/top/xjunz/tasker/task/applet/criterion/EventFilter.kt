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

/**
 * @author xjunz 2022/08/25
 */
class EventFilter(eventType: Int) : Applet() {

    init {
        value = eventType
    }

    override val valueType: Int = VAL_TYPE_INT

    override suspend fun apply(runtime: TaskRuntime): AppletResult {
        val hit = runtime.events?.find {
            it.type == value
        }
        return if (hit == null) {
            AppletResult.EMPTY_FAILURE
        } else {
            when (hit.type) {
                Event.EVENT_ON_NOTIFICATION_RECEIVED -> {
                    NotificationReferent(ComponentInfoWrapper.wrap(hit.componentInfo)).asResult()
                }

                Event.EVENT_ON_PRIMARY_CLIP_CHANGED -> {
                    AppletResult.succeeded(hit.getExtra(ClipboardEventDispatcher.EXTRA_PRIMARY_CLIP_TEXT))
                }

                Event.EVENT_ON_TICK -> {
                    AppletResult.EMPTY_SUCCESS
                }

                else -> ComponentInfoWrapper.wrap(hit.componentInfo).asResult()
            }
        }
    }

}