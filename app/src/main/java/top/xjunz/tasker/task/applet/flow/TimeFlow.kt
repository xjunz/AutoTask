/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow

import top.xjunz.tasker.engine.applet.base.ScopeFlow
import top.xjunz.tasker.engine.runtime.TaskRuntime
import java.util.*

/**
 * @author xjunz 2022/10/01
 */
class TimeFlow : ScopeFlow<Calendar>() {

    override fun initializeTarget(runtime: TaskRuntime): Calendar {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        return calendar
    }
}