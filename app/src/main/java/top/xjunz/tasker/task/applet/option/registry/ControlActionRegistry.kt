/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.action.Break
import top.xjunz.tasker.engine.applet.action.Continue
import top.xjunz.tasker.engine.applet.action.Repeat
import top.xjunz.tasker.engine.applet.action.Suspension
import top.xjunz.tasker.engine.applet.action.emptyArgOptimisticAction
import top.xjunz.tasker.engine.applet.base.If
import top.xjunz.tasker.engine.applet.base.WaitFor
import top.xjunz.tasker.engine.applet.base.WaitUntil
import top.xjunz.tasker.ktx.foreColored
import top.xjunz.tasker.ktx.formatSpans
import top.xjunz.tasker.ktx.str
import top.xjunz.tasker.task.applet.action.PauseFor
import top.xjunz.tasker.task.applet.action.PauseUntilTomorrow
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.task.applet.value.LongDuration
import top.xjunz.tasker.task.applet.value.VariantArgType
import top.xjunz.tasker.task.runtime.LocalTaskManager
import top.xjunz.tasker.task.storage.TaskStorage
import top.xjunz.tasker.util.formatMinSecMills

/**
 * @author xjunz 2022/12/04
 */
class ControlActionRegistry(id: Int) : AppletOptionRegistry(id) {

    @AppletOrdinal(0x00_00)
    val ifAction = appletOption(R.string._if) { If() }

    @AppletOrdinal(0x00_01)
    val waitUntilAction = appletOption(R.string.wait_until) {
        WaitUntil()
    }.withValueArgument<Int>(R.string.wait_timeout, VariantArgType.INT_INTERVAL)
        .withHelperText(R.string.tip_wait_timeout)
        .withSingleValueDescriber<Int> {
            R.string.format_max_wait_duration.formatSpans(formatMinSecMills(it).foreColored())
        }

    @AppletOrdinal(0x0002)
    val waitForFlow = appletOption(R.string.wait_for_event) {
        WaitFor()
    }.withValueArgument<Int>(R.string.wait_timeout, VariantArgType.INT_INTERVAL)
        .withHelperText(R.string.tip_wait_timeout)
        .withSingleValueDescriber<Int> {
            R.string.format_max_wait_duration.formatSpans(formatMinSecMills(it).foreColored())
        }

    @AppletOrdinal(0x00_03)
    val suspension = appletOption(R.string.delay) {
        Suspension()
    }.withValueArgument<Int>(R.string.delay_interval, VariantArgType.INT_INTERVAL)
        .withSingleValueAppletDescriber<Int> { applet, t ->
            R.string.format_delay.formatSpans(
                formatMinSecMills(t!!).foreColored().clickToEdit(applet)
            )
        }.descAsTitle()

    @AppletOrdinal(0x00_04)
    val repeatFlow = appletOption(R.string.loop) {
        Repeat()
    }.descAsTitle()
        .withValueArgument<Int>(R.string.loop_count)
        .withResult<Repeat>(R.string.loop)
        .withResult<Int>(R.string.repeated_count)
        .withResult<String>(R.string.repeated_count)
        .withSingleValueAppletDescriber<Int> { applet, t ->
            R.string.format_repeat.formatSpans(t.toString().foreColored().clickToEdit(applet))
        }

    @AppletOrdinal(0x00_05)
    val breakAction = appletOption(R.string.break_current_loop) {
        Break()
    }

    @AppletOrdinal(0x00_06)
    val continueAction = appletOption(R.string.continue_current_loop) {
        Continue()
    }.withTitleModifier(R.string.tip_continue_loop)

    @AppletOrdinal(0x0010)
    val stopshipTask = appletOption(R.string.stopship_current_task) {
        emptyArgOptimisticAction {
            it.shouldStop = true
        }
    }

    @AppletOrdinal(0x0011)
    val disableTask = appletOption(R.string.disable_current_task) {
        emptyArgOptimisticAction {
            LocalTaskManager.removeTask(it.attachingTask)
            TaskStorage.toggleTaskFilename(it.attachingTask)
        }
    }

    @AppletOrdinal(0x0020)
    val pauseUntilTomorrow = appletOption(R.string.pause_until_tomorrow) {
        PauseUntilTomorrow()
    }

    @AppletOrdinal(0x0020)
    val pauseFor = appletOption(R.string.pause_for) {
        PauseFor()
    }.withValueArgument<Long>(R.string.specified_duration, VariantArgType.BITS_LONG_DURATION)
        .withSingleValueAppletDescriber<Long> { _, duration ->
            checkNotNull(duration)
            val parsed = LongDuration.parse(duration)
            buildString {
                if (parsed.day != 0) {
                    append(parsed.day)
                    append(R.string.day.str)
                }
                if (parsed.hour != 0) {
                    append(parsed.hour)
                    append(R.string.hour.str)
                }
                if (parsed.min != 0) {
                    append(parsed.min)
                    append(R.string.minute.str)
                }
                if (parsed.sec != 0) {
                    append(parsed.sec)
                    append(R.string.second.str)
                }
            }
        }
}