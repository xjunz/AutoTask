/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.criterion

import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.task.applet.flow.ComponentInfoWrapper

/**
 * @author xjunz 2022/08/25
 */
class EventCriterion(eventType: Int) : Applet() {

    init {
        value = eventType
    }

    override val valueType: Int = VAL_TYPE_INT

    override suspend fun apply(runtime: TaskRuntime): AppletResult {
        val hit = runtime.events.find {
            it.type == value
        }
        return if (hit == null) {
            AppletResult.failure(value, runtime.events)
        } else {
            val wrapper = ComponentInfoWrapper.wrap(hit.componentInfo)
            if (hit.type == Event.EVENT_ON_NOTIFICATION_RECEIVED) {
                AppletResult.successWithReturn(hit.componentInfo.paneTitle, wrapper)
            } else {
                AppletResult.successWithReturn(wrapper)
            }
        }
    }

}