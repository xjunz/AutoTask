/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.action.*
import top.xjunz.tasker.engine.applet.base.*
import top.xjunz.tasker.ktx.clickable
import top.xjunz.tasker.ktx.foreColored
import top.xjunz.tasker.ktx.formatSpans
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.value.VariantType
import top.xjunz.tasker.task.runtime.LocalTaskManager
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
    }.withValueArgument<Int>(R.string.wait_timeout, VariantType.INT_INTERVAL)
        .withHelperText(R.string.tip_wait_timeout)
        .withValueDescriber<Int> {
            R.string.format_max_wait_duration.formatSpans(formatMinSecMills(it).foreColored())
        }

    @AppletOrdinal(0x0002)
    val waitForFlow = appletOption(R.string.wait_for_event) {
        WaitFor()
    }.withValueArgument<Int>(R.string.wait_timeout, VariantType.INT_INTERVAL)
        .withHelperText(R.string.tip_wait_timeout)
        .withValueDescriber<Int> {
            R.string.format_max_wait_duration.formatSpans(formatMinSecMills(it).foreColored())
        }

    @AppletOrdinal(0x00_03)
    val suspension = appletOption(R.string.delay) {
        Suspension()
    }.withValueArgument<Int>(R.string.delay_interval, VariantType.INT_INTERVAL)
        .withDescriber<Int> { applet, t ->
            R.string.format_delay.formatSpans(formatMinSecMills(t!!).foreColored().clickable {
                AppletOption.deliverEvent(it, AppletOption.EVENT_EDIT_VALUE, applet)
            })
        }.descAsTitle()

    @AppletOrdinal(0x00_04)
    val repeatFlow = appletOption(R.string.loop) {
        Repeat()
    }.withDescriber<Int> { applet, t ->
        R.string.format_repeat.formatSpans(t.toString().foreColored().clickable {
            AppletOption.deliverEvent(it, AppletOption.EVENT_EDIT_VALUE, applet)
        })
    }.descAsTitle().withHelperText(R.string.input_repeat_count)
        .withResult<Repeat>(R.string.loop)
        .withResult<Int>(R.string.repeated_count)
        .withResult<String>(R.string.repeated_count)

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
        pureAction {
            it.shouldStop = true
        }
    }

    @AppletOrdinal(0x0011)
    val disableTask = appletOption(R.string.disable_current_task) {
        pureAction {
            LocalTaskManager.removeTask(it.attachingTask)
        }
    }
}