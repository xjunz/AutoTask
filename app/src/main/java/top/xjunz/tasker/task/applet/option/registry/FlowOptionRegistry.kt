package top.xjunz.tasker.task.applet.option.registry

import android.view.accessibility.AccessibilityNodeInfo
import top.xjunz.shared.utils.illegalArgument
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.base.*
import top.xjunz.tasker.ktx.foreColored
import top.xjunz.tasker.ktx.formatSpans
import top.xjunz.tasker.task.applet.anno.AppletCategory
import top.xjunz.tasker.task.applet.flatSize
import top.xjunz.tasker.task.applet.flow.*
import top.xjunz.tasker.task.applet.option.AppletOption

open class FlowOptionRegistry : AppletOptionRegistry(ID_FLOW_OPTION_REGISTRY) {

    companion object {

        private const val ID_FLOW_OPTION_REGISTRY = 0
        const val ID_EVENT_FILTER_REGISTRY: Int = 0xF
        const val ID_PKG_OPTION_REGISTRY = 0x10
        const val ID_UI_OBJECT_OPTION_REGISTRY = 0x11
        const val ID_TIME_OPTION_REGISTRY = 0x12
        const val ID_GLOBAL_OPTION_REGISTRY = 0x13
        const val ID_NOTIFICATION_OPTION_REGISTRY = 0x14

        const val ID_GLOBAL_ACTION_REGISTRY = 0x50
        const val ID_UI_OBJECT_ACTION_REGISTRY = 0x51
    }

    override val categoryNames: IntArray? = null

    private inline fun <reified F : Flow> flowOption(flowId: Int, title: Int): AppletOption {
        return appletOption(flowId, title) {
            F::class.java.newInstance()
        }
    }

    fun getPeerOptions(flow: ControlFlow, before: Boolean): Array<AppletOption> {
        return when (flow) {
            is ElseIf -> if (before) arrayOf(ifFlow, elseIfFlow)
            else arrayOf(ifFlow, elseIfFlow, elseFlow, doFlow)
            is If -> if (before) arrayOf(ifFlow) else arrayOf(ifFlow, elseIfFlow, elseFlow, doFlow)
            is When -> if (!before) arrayOf(ifFlow, doFlow)
            else illegalArgument("No before peer for When")
            is Else -> if (before) arrayOf(ifFlow, elseIfFlow) else arrayOf(ifFlow, doFlow)
            is Do -> if (before) arrayOf(ifFlow, elseIfFlow, elseFlow) else arrayOf(ifFlow, doFlow)
            else -> illegalArgument("control flow", flow)
        }
    }

    /**
     * Applet flow is a container flow whose child has the same target.
     */
    val criterionFlowOptions: Array<AppletOption> by lazy {
        arrayOf(componentFlow, uiObjectFlow, timeFlow, notificationFlow, globalFlow)
    }

    val actionFlowOptions: Array<AppletOption> by lazy {
        arrayOf(globalActionFlow, uiObjectActionFlow)
    }

    @AppletCategory(0x0001)
    val ifFlow = flowOption<If>(1, R.string._if)

    @AppletCategory(0x0002)
    val doFlow = flowOption<Do>(2, R.string._do)

    @AppletCategory(0x0003)
    val elseIfFlow = flowOption<ElseIf>(3, R.string.else_if)

    @AppletCategory(0x0004)
    val elseFlow = flowOption<Else>(4, R.string._else)

    @AppletCategory(0x0005)
    val containerFlow = flowOption<Flow>(5, AppletOption.TITLE_NONE)
        .withDescriber<Any> { applet, _ ->
            val size = (applet as Flow).flatSize.toString().foreColored()
            R.string.format_applet_count.formatSpans(size)
        }

    @AppletCategory(0x000F)
    val whenFlow = flowOption<When>(ID_EVENT_FILTER_REGISTRY, R.string._when)

    @AppletCategory(0x0010)
    val componentFlow = flowOption<PackageFlow>(ID_PKG_OPTION_REGISTRY, R.string.current_app)
        .withResult<String>(R.string.package_name)

    @AppletCategory(0x0011)
    val uiObjectFlow =
        flowOption<UiObjectFlow>(ID_UI_OBJECT_OPTION_REGISTRY, R.string.ui_object_exists)
            .withResult<AccessibilityNodeInfo>(R.string.ui_object)
            .withResult<String>(R.string.matched_ui_object_text)

    @AppletCategory(0x0012)
    val timeFlow = flowOption<TimeFlow>(ID_TIME_OPTION_REGISTRY, R.string.current_time)

    @AppletCategory(0x0013)
    val globalFlow = flowOption<PhantomFlow>(ID_GLOBAL_OPTION_REGISTRY, R.string.device_status)

    @AppletCategory(0x0014)
    val notificationFlow =
        flowOption<NotificationFlow>(ID_NOTIFICATION_OPTION_REGISTRY, R.string.current_notification)
            .withResult<String>(R.string.notification_content)

    @AppletCategory(0x0020)
    val globalActionFlow =
        flowOption<PhantomFlow>(ID_GLOBAL_ACTION_REGISTRY, R.string.global_actions)

    @AppletCategory(0x0021)
    val uiObjectActionFlow =
        flowOption<PhantomFlow>(ID_UI_OBJECT_ACTION_REGISTRY, R.string.ui_object_operations)

}