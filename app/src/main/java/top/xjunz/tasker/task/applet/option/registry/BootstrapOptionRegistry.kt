/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import android.view.accessibility.AccessibilityNodeInfo
import top.xjunz.shared.utils.illegalArgument
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.base.Anyway
import top.xjunz.tasker.engine.applet.base.ContainerFlow
import top.xjunz.tasker.engine.applet.base.ControlFlow
import top.xjunz.tasker.engine.applet.base.Do
import top.xjunz.tasker.engine.applet.base.Else
import top.xjunz.tasker.engine.applet.base.ElseIf
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.base.If
import top.xjunz.tasker.engine.applet.base.RootFlow
import top.xjunz.tasker.engine.applet.base.Then
import top.xjunz.tasker.engine.applet.base.WaitFor
import top.xjunz.tasker.engine.applet.base.WaitUntil
import top.xjunz.tasker.engine.applet.base.When
import top.xjunz.tasker.ktx.foreColored
import top.xjunz.tasker.ktx.formatSpans
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.task.applet.flow.ElseStopship
import top.xjunz.tasker.task.applet.flow.PhantomFlow
import top.xjunz.tasker.task.applet.flow.PreloadFlow
import top.xjunz.tasker.task.applet.flow.TimeFlow
import top.xjunz.tasker.task.applet.flow.ref.ComponentInfoWrapper
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.value.VariantArgType
import top.xjunz.tasker.util.formatMinSecMills

open class BootstrapOptionRegistry : AppletOptionRegistry(ID_BOOTSTRAP_REGISTRY) {

    companion object {

        const val ID_BOOTSTRAP_REGISTRY = 0
        const val ID_EVENT_FILTER_REGISTRY: Int = 0xF
        const val ID_APP_CRITERION_REGISTRY = 0x10
        const val ID_TIME_CRITERION_REGISTRY = 0x12
        const val ID_GLOBAL_CRITERION_REGISTRY = 0x13
        const val ID_NOTIFICATION_CRITERION_REGISTRY = 0x14
        const val ID_TEXT_CRITERION_REGISTRY = 0x15
        const val ID_UI_OBJECT_CRITERION_REGISTRY = 0x16
        const val ID_NETWORK_CRITERION_REGISTRY = 0x17

        const val ID_GLOBAL_ACTION_REGISTRY = 0x50
        const val ID_UI_OBJECT_ACTION_REGISTRY = 0x51
        const val ID_CONTROL_ACTION_REGISTRY = 0x52
        const val ID_APP_ACTION_REGISTRY = 0x53
        const val ID_TEXT_ACTION_REGISTRY = 0x54
        const val ID_GESTURE_ACTION_REGISTRY = 0x55
        const val ID_SHELL_CMD_ACTION_REGISTRY = 0x56
        const val ID_FILE_ACTION_REGISTRY = 0x57
        const val ID_VIBRATION_ACTION_REGISTRY = 0x58

        const val ID_UI_OBJECT_FLOW_REGISTRY = 0x60
    }

    private inline fun <reified F : Flow> flowOptionWithId(
        appletId: Int,
        title: Int
    ): AppletOption {
        return appletOption(title) {
            F::class.java.getConstructor().newInstance()
        }.also {
            it.appletId = appletId
        }
    }

    private inline fun <reified F : Flow> flowOption(title: Int): AppletOption {
        return flowOptionWithId<F>(-1, title)
    }

    fun getPeerOptions(flow: ControlFlow, before: Boolean): Array<AppletOption> {
        return when (flow) {
            is When -> if (before) emptyArray()
            else arrayOf(ifFlow, doFlow, waitUntilFlow, waitForFlow)

            is If -> if (before) emptyArray() else arrayOf(doFlow, elseFlow, elseStopshipFlow)
            is Else -> if (before) emptyArray()
            else arrayOf(thenFlow, ifFlow, waitUntilFlow, waitForFlow, anywayFlow)

            is Do -> if (before) emptyArray()
            else arrayOf(
                ifFlow,
                elseIfFlow,
                elseFlow,
                elseStopshipFlow,
                thenFlow,
                anywayFlow,
                waitForFlow,
                waitUntilFlow
            )

            else -> illegalArgument("control flow", flow)
        }
    }

    /**
     * Applet flow is a container flow whose child has the same target.
     */
    private val criterionFlowOptions: Array<AppletOption> by lazy {
        arrayOf(
            appCriteria,
            uiObjectFlows,
            textCriteria,
            timeCriteria,
            globalCriteria,
            networkCriteria
        )
    }

    private val actionFlowOptions: Array<AppletOption> by lazy {
        arrayOf(
            controlActions,
            globalActions,
            uiObjectActions,
            gestureActions,
            textActions,
            appActions,
            shellCmdActions,
            fileActions,
            vibrationActions
        )
    }

    fun getRegistryOptions(flow: Flow): Array<AppletOption> {
        return when (flow) {
            is When, is WaitFor -> arrayOf(eventCriteria)

            is If -> criterionFlowOptions

            is Do, is RootFlow -> actionFlowOptions

            else -> illegalArgument("control flow", flow)
        }
    }

    @AppletOrdinal(0x0000)
    val rootFlow = flowOption<RootFlow>(R.string.add_rules)

    @AppletOrdinal(0x0001)
    val preloadFlow = flowOption<PreloadFlow>(R.string.global)
        .withResult<ComponentInfoWrapper>(R.string.current_top_app)
        .withResult<String>(R.string.current_package_name)
        .withResult<String>(R.string.current_package_label)
        .withResult<AccessibilityNodeInfo>(R.string.current_window)
        .withResult<AccessibilityNodeInfo>(R.string.current_focus_input)
        .withResult<String>(R.string.current_time)

    @AppletOrdinal(0x0002)
    val whenFlow = flowOption<When>(R.string._when)

    @AppletOrdinal(0x0003)
    val ifFlow = flowOption<If>(R.string._if)

    @AppletOrdinal(0x0004)
    val doFlow = flowOption<Do>(R.string._do)

    @AppletOrdinal(0x0005)
    val elseIfFlow = flowOption<ElseIf>(R.string.else_if)

    @AppletOrdinal(0x0006)
    val elseFlow = flowOption<Else>(R.string._else)

    @AppletOrdinal(0x0007)
    val containerFlow = flowOption<ContainerFlow>(AppletOption.TITLE_NONE)

    @AppletOrdinal(0x0008)
    val waitUntilFlow = flowOption<WaitUntil>(R.string.wait_until)
        .withValueArgument<Int>(R.string.wait_timeout, VariantArgType.INT_INTERVAL)
        .withHelperText(R.string.tip_wait_timeout)
        .withSingleValueDescriber<Int> {
            R.string.format_max_wait_duration.formatSpans(formatMinSecMills(it).foreColored())
        }

    @AppletOrdinal(0x0009)
    val waitForFlow = flowOption<WaitFor>(R.string.wait_for_event)
        .withValueArgument<Int>(R.string.wait_timeout, VariantArgType.INT_INTERVAL)
        .withHelperText(R.string.tip_wait_timeout)
        .withSingleValueDescriber<Int> {
            R.string.format_max_wait_duration.formatSpans(formatMinSecMills(it).foreColored())
        }

    @AppletOrdinal(0x000A)
    val anywayFlow = flowOption<Anyway>(R.string.anyway)

    @AppletOrdinal(0x000B)
    val elseStopshipFlow = flowOption<ElseStopship>(R.string.else_stopship)

    @AppletOrdinal(0x000C)
    val thenFlow = flowOption<Then>(R.string.then)

    @AppletOrdinal(0x0010)
    val eventCriteria = flowOptionWithId<PhantomFlow>(ID_EVENT_FILTER_REGISTRY, R.string.event)

    @AppletOrdinal(0x0011)
    val appCriteria = flowOptionWithId<PhantomFlow>(ID_APP_CRITERION_REGISTRY, R.string.app_info)

    @AppletOrdinal(0x0012)
    val uiObjectCriteria = flowOptionWithId<PhantomFlow>(
        ID_UI_OBJECT_CRITERION_REGISTRY, R.string.ui_object_conditions
    )

    @AppletOrdinal(0x0013)
    val timeCriteria = flowOptionWithId<TimeFlow>(ID_TIME_CRITERION_REGISTRY, R.string.current_time)

    @AppletOrdinal(0x0014)
    val globalCriteria =
        flowOptionWithId<PhantomFlow>(ID_GLOBAL_CRITERION_REGISTRY, R.string.device_status)

    @AppletOrdinal(0x0015)
    val textCriteria = flowOptionWithId<PhantomFlow>(
        ID_TEXT_CRITERION_REGISTRY, R.string.text_criteria
    )

    @AppletOrdinal(0x0016)
    val networkCriteria =
        flowOptionWithId<PhantomFlow>(ID_NETWORK_CRITERION_REGISTRY, R.string.network_status)

    @AppletOrdinal(0x0020)
    val globalActions =
        flowOptionWithId<PhantomFlow>(ID_GLOBAL_ACTION_REGISTRY, R.string.global_actions)

    @AppletOrdinal(0x0021)
    val uiObjectActions =
        flowOptionWithId<PhantomFlow>(ID_UI_OBJECT_ACTION_REGISTRY, R.string.ui_object_operations)

    @AppletOrdinal(0x0022)
    val gestureActions =
        flowOptionWithId<PhantomFlow>(ID_GESTURE_ACTION_REGISTRY, R.string.gesture_operations)

    @AppletOrdinal(0x0023)
    val textActions =
        flowOptionWithId<PhantomFlow>(ID_TEXT_ACTION_REGISTRY, R.string.text_operations)

    @AppletOrdinal(0x0024)
    val appActions =
        flowOptionWithId<PhantomFlow>(ID_APP_ACTION_REGISTRY, R.string.app_operations)

    @AppletOrdinal(0x0025)
    val controlActions =
        flowOptionWithId<PhantomFlow>(ID_CONTROL_ACTION_REGISTRY, R.string.control_actions)

    @AppletOrdinal(0x0030)
    val uiObjectFlows =
        flowOptionWithId<PhantomFlow>(ID_UI_OBJECT_FLOW_REGISTRY, R.string.ui_object_conditions)

    @AppletOrdinal(0x0040)
    val shellCmdActions =
        flowOptionWithId<PhantomFlow>(ID_SHELL_CMD_ACTION_REGISTRY, R.string.shell_cmd)

    @AppletOrdinal(0x0041)
    val fileActions =
        flowOptionWithId<PhantomFlow>(ID_FILE_ACTION_REGISTRY, R.string.file_operations)

    @AppletOrdinal(0x0042)
    val vibrationActions =
        flowOptionWithId<PhantomFlow>(ID_VIBRATION_ACTION_REGISTRY, R.string.vibrate)
}