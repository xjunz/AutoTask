/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow

import top.xjunz.tasker.engine.applet.base.ScopeFlow
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.task.event.TickEventDispatcher
import java.util.Calendar

/**
 * @author xjunz 2022/10/01
 */
class TimeFlow : ScopeFlow<Calendar>() {

    override fun initializeTarget(runtime: TaskRuntime): Calendar {
        val calendar = Calendar.getInstance()
        val current = runtime.events?.find { it.type == Event.EVENT_ON_TICK }
            ?.getExtra<Long>(TickEventDispatcher.EXTRA_TICK_TIME_MILLS)
            ?: System.currentTimeMillis()
        calendar.timeInMillis = current
        return calendar
    }
}