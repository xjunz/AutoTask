package top.xjunz.tasker.task.applet.option.registry
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.base.When
import top.xjunz.tasker.task.applet.anno.AppletCategory
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.option.NotInvertibleAppletOption
import top.xjunz.tasker.task.flow.PackageFlow
import top.xjunz.tasker.task.flow.TimeFlow
import top.xjunz.tasker.task.flow.UiObjectFlow

class FlowOptionRegistry : AppletOptionRegistry(ID_FLOW_FACTORY) {

    companion object {
        const val ID_FLOW_FACTORY = 0
        const val ID_EVENT_FILTER_FACTORY = 1
        const val ID_PKG_APPLET_FACTORY = 2
        const val ID_UI_OBJECT_APPLET_FACTORY = 3
        const val ID_TIME_APPLET_FACTORY = 5
    }

    override val title: Int get() = AppletOption.TITLE_NONE

    override val categoryNames: IntArray get() = intArrayOf(AppletOption.TITLE_NONE)

    private inline fun <reified F : Flow> FlowOption(flowId: Int, title: Int): AppletOption {
        return NotInvertibleAppletOption(flowId, title) {
            F::class.java.newInstance()
        }
    }

    @AppletCategory(0x0000)
    private val eventFilter = FlowOption<When>(ID_EVENT_FILTER_FACTORY, R.string.flow_when)

    @AppletCategory(0x0001)
    private val packageFlow =
        FlowOption<PackageFlow>(ID_PKG_APPLET_FACTORY, R.string.current_package_matches)

    @AppletCategory(0x0002)
    private val uiObjectFlow =
        FlowOption<UiObjectFlow>(ID_UI_OBJECT_APPLET_FACTORY, R.string.ui_object_exists)

    @AppletCategory(0x0003)
    private val timeFlow =
        FlowOption<TimeFlow>(ID_TIME_APPLET_FACTORY, R.string.current_time_matches)
}