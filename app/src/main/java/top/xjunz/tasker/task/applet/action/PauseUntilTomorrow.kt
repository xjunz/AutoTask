/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.action

import top.xjunz.tasker.engine.applet.action.Action
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.runtime.TaskRuntime
import java.util.Calendar
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/**
 * @author xjunz 2023/07/06
 */
class PauseUntilTomorrow : Action<Unit>(VAL_TYPE_IRRELEVANT) {

    override suspend fun doAction(value: Unit?, runtime: TaskRuntime): AppletResult {
        val calender = Calendar.getInstance()
        calender.timeInMillis = System.currentTimeMillis()
        val hour = calender.get(Calendar.HOUR_OF_DAY)
        val min = calender.get(Calendar.MINUTE)
        val sec = calender.get(Calendar.SECOND)
        val mills = calender.get(Calendar.MILLISECOND)
        runtime.attachingTask.pause(
            (24.hours - hour.hours - min.minutes - sec.seconds - mills.milliseconds)
                .toLong(DurationUnit.MILLISECONDS)
        )
        return AppletResult.EMPTY_SUCCESS
    }
}