/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.criterion

import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/08/25
 */
class EventCriterion(eventType: Int) : Applet() {

    init {
        value = eventType
    }

    override val valueType: Int = VAL_TYPE_INT

    override suspend fun apply(runtime: TaskRuntime): Boolean {
        val hit = runtime.events.find { it.type == value }
        return if (hit == null) {
            false
        } else {
            runtime.hitEvent = hit
            true
        }
    }

}