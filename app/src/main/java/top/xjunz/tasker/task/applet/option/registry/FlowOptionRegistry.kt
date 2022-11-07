package top.xjunz.tasker.task.applet.option.registry

import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.base.*
import top.xjunz.tasker.task.applet.anno.AppletCategory
import top.xjunz.tasker.task.applet.flow.PackageFlow
import top.xjunz.tasker.task.applet.flow.TimeFlow
import top.xjunz.tasker.task.applet.flow.UiObjectFlow
import top.xjunz.tasker.task.applet.option.AppletOption

open class FlowOptionRegistry : AppletOptionRegistry(ID_FLOW_OPTION_REGISTRY) {

    companion object {

        private const val ID_FLOW_OPTION_REGISTRY = 0
        const val ID_EVENT_FILTER_REGISTRY: Int = 0x01
        const val ID_PKG_OPTION_REGISTRY = 0x10
        const val ID_UI_OBJECT_OPTION_REGISTRY = 0x20
        const val ID_TIME_OPTION_REGISTRY = 0x30
    }

    override val title: Int get() = AppletOption.TITLE_NONE

    override val categoryNames: IntArray? = null

    private inline fun <reified F : Flow> FlowOption(flowId: Int, title: Int): AppletOption {
        return NotInvertibleAppletOption(flowId, title) {
            F::class.java.newInstance()
        }
    }

    val controlFlowOptions: Array<AppletOption> by lazy {
        arrayOf(whenFlow, ifFlow, doFlow, elseIfFlow, elseFlow, componentFlow)
    }

    val appletFlowOptions: Array<AppletOption> by lazy {
        arrayOf(componentFlow, uiObjectFlow, timeFlow)
    }

    @AppletCategory(0x0001)
    val whenFlow = FlowOption<When>(ID_EVENT_FILTER_REGISTRY, R.string._when)

    @AppletCategory(0x0002)
    val ifFlow = FlowOption<If>(2, R.string._if)

    @AppletCategory(0x0003)
    val doFlow = FlowOption<Do>(3, R.string._do)

    @AppletCategory(0x0004)
    val elseIfFlow = FlowOption<ElseIf>(4, R.string.else_if)

    @AppletCategory(0x0005)
    val elseFlow = FlowOption<ElseIf>(5, R.string._else)

    @AppletCategory(0x0010)
    val componentFlow =
        FlowOption<PackageFlow>(ID_PKG_OPTION_REGISTRY, R.string.current_package_matches)

    @AppletCategory(0x0020)
    val uiObjectFlow =
        FlowOption<UiObjectFlow>(ID_UI_OBJECT_OPTION_REGISTRY, R.string.ui_object_exists)

    @AppletCategory(0x0030)
    val timeFlow =
        FlowOption<TimeFlow>(ID_TIME_OPTION_REGISTRY, R.string.current_time_matches)

}